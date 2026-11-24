package com.yep.platform.file;

import com.yep.platform.common.ApiException;
import com.yep.platform.security.AuthUser;
import com.yep.platform.user.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
    public record FileView(Long id, String name, String contentType, long size) {
    }

    public record Download(byte[] content, String contentType, String name, boolean inline) {
    }

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 25_000_000L; // decoded-size guard
    private static final int MAX_ZIP_ENTRIES = 2_000;
    private static final long MAX_INFLATED_BYTES = 100L * 1024 * 1024;
    private static final int MAX_IMAGE_EDGE = 1280; // low-bandwidth: images are downscaled and re-encoded as JPEG
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png");
    private static final Set<String> DOC_EXT = Set.of("pdf", "docx", "pptx", "xlsx", "txt", "csv");
    private static final Pattern DANGEROUS = Pattern
            .compile("(?i)\\.(exe|bat|cmd|com|scr|msi|dll|sh|js|jar|php|jsp|py|html?|svg|swf|vbs|ps1)(\\.|$)");

    private final FileRepository files;
    private final StorageService storage;
    private final ProfileRepository profiles;
    private final JdbcTemplate jdbc;

    public FileService(FileRepository files, StorageService storage, ProfileRepository profiles, JdbcTemplate jdbc) {
        this.files = files;
        this.storage = storage;
        this.profiles = profiles;
        this.jdbc = jdbc;
    }

    @Transactional
    public FileView upload(MultipartFile file, String purpose, AuthUser me) {
        if (file == null || file.isEmpty())
            throw ApiException.badRequest("Choose a file to upload.");
        if (file.getSize() > MAX_BYTES)
            throw ApiException.badRequest("Files can be up to 5 MB.");
        if (!List.of("PROFILE_PHOTO", "PROJECT_DOC", "PROJECT_IMAGE", "LEARNING_RESOURCE").contains(purpose))
            throw ApiException.badRequest("Unknown file purpose.");
        if (purpose.equals("LEARNING_RESOURCE") && me.role() != Role.FACILITATOR && me.role() != Role.ADMIN)
            throw ApiException.forbidden("Only facilitators can upload learning resources.");

        String name = cleanName(file.getOriginalFilename());
        if (DANGEROUS.matcher(name).find())
            throw ApiException.badRequest("This file type is not allowed.");
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        boolean imagePurpose = purpose.equals("PROFILE_PHOTO") || purpose.equals("PROJECT_IMAGE");
        if (imagePurpose ? !IMAGE_EXT.contains(ext) : !(DOC_EXT.contains(ext) || IMAGE_EXT.contains(ext)))
            throw ApiException.badRequest("This file type is not allowed for this upload.");

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file.");
        }
        if (!signatureMatches(ext, bytes))
            throw ApiException.badRequest("The file contents do not match its type.");
        if (!structureMatches(ext, bytes))
            throw ApiException.badRequest("The file contents do not match its type.");

        String contentType;
        if (IMAGE_EXT.contains(ext)) {
            bytes = reencode(bytes);
            ext = "jpg";
            contentType = "image/jpeg";
            name = name.replaceAll("\\.[A-Za-z]+$", "") + ".jpg";
        } else
            contentType = switch (ext) {
                case "pdf" -> "application/pdf";
                case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                case "csv" -> "text/csv";
                default -> "text/plain";
            };

        String stored = UUID.randomUUID() + "." + ext;
        try {
            storage.store(stored, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Storage failure", e);
        }
        var f = new FileAsset();
        f.ownerId = me.id();
        f.purpose = purpose;
        f.originalName = name;
        f.storedName = stored;
        f.contentType = contentType;
        f.sizeBytes = bytes.length;
        files.save(f);
        return new FileView(f.id, f.originalName, f.contentType, f.sizeBytes);
    }

    @Transactional(readOnly = true)
    public Download download(Long id, AuthUser me) {
        var f = files.findById(id).orElseThrow(() -> ApiException.notFound("File not found"));
        if (!canRead(f, me))
            throw ApiException.notFound("File not found");
        try {
            return new Download(storage.load(f.storedName), f.contentType, f.originalName,
                    f.contentType.startsWith("image/"));
        } catch (IOException e) {
            throw ApiException.notFound("File not found");
        }
    }

    @Transactional
    public void setProfilePhoto(Long userId, Long fileId) {
        var f = files.findById(fileId).filter(x -> x.ownerId.equals(userId) && x.purpose.equals("PROFILE_PHOTO"))
                .orElseThrow(() -> ApiException.badRequest("Upload the photo first."));
        profiles.findById(userId).orElseThrow().setPhotoFileId(f.id);
    }

    /**
     * Photos: any signed-in user. Others: owner, staff, or someone who can see the
     * project the file is attached to.
     */
    private boolean canRead(FileAsset f, AuthUser me) {
        if (f.purpose.equals("PROFILE_PHOTO") || f.purpose.equals("LEARNING_RESOURCE"))
            return true;
        if (f.ownerId.equals(me.id()) || me.role() == Role.ADMIN || me.role() == Role.FACILITATOR)
            return true;
        return Boolean.TRUE.equals(jdbc.queryForObject(
                """
                        select exists(select 1 from project_attachments pa join impact_projects p on p.id = pa.project_id
                          where pa.file_id = ? and (
                            exists(select 1 from team_members m where m.team_id = p.team_id and m.user_id = ?)
                            or exists(select 1 from mentorship_assignments a where a.mentor_id = ? and a.status = 'ACTIVE'
                                      and (a.team_id = p.team_id or a.participant_id in (select user_id from team_members where team_id = p.team_id)))))
                        """,
                Boolean.class, f.id, me.id(), me.id()));
    }

    private static String cleanName(String raw) {
        String n = raw == null ? "file" : raw.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (n.length() > 100)
            n = n.substring(n.length() - 100);
        return n.isEmpty() || n.startsWith(".") ? "file" + n : n;
    }

    private static boolean signatureMatches(String ext, byte[] b) {
        if (b.length < 4)
            return false;
        return switch (ext) {
            case "jpg", "jpeg" -> (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case "png" -> (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "pdf" -> b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
            case "docx", "pptx", "xlsx" -> b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4;
            default -> {
                for (byte x : b)
                    if (x == 0)
                        yield false;
                yield true;
            } // txt/csv: reject binary
        };
    }

    /**
     * Office files are zip containers: require the marker entries and cap entry
     * count / inflated size (zip-bomb guard).
     */
    private static boolean structureMatches(String ext, byte[] b) {
        String marker = switch (ext) {
            case "docx" -> "word/";
            case "pptx" -> "ppt/";
            case "xlsx" -> "xl/";
            default -> null;
        };
        if (marker == null)
            return true;
        boolean contentTypes = false, part = false;
        long total = 0;
        int entries = 0;
        try (var zin = new java.util.zip.ZipInputStream(new ByteArrayInputStream(b))) {
            java.util.zip.ZipEntry e;
            byte[] buf = new byte[8192];
            while ((e = zin.getNextEntry()) != null) {
                if (++entries > MAX_ZIP_ENTRIES)
                    return false;
                String n = e.getName();
                if (n.startsWith("/") || n.contains(".."))
                    return false;
                if (n.equals("[Content_Types].xml"))
                    contentTypes = true;
                if (n.startsWith(marker))
                    part = true;
                int r;
                while ((r = zin.read(buf)) > 0) {
                    total += r;
                    if (total > MAX_INFLATED_BYTES)
                        return false;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        return contentTypes && part;
    }

    private static byte[] reencode(byte[] bytes) {
        try (var iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            // Check declared dimensions BEFORE decoding: a tiny, highly compressed file can
            // declare gigapixels and exhaust memory.
            var readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext())
                throw ApiException.badRequest("That image could not be read.");
            var reader = readers.next();
            BufferedImage src;
            try {
                reader.setInput(iis, true, true);
                long w0 = reader.getWidth(0), h0 = reader.getHeight(0);
                if (w0 <= 0 || h0 <= 0 || w0 * h0 > MAX_PIXELS)
                    throw ApiException.badRequest("That image is too large. Please use one under 25 megapixels.");
                src = reader.read(0);
            } finally {
                reader.dispose();
            }
            if (src == null)
                throw ApiException.badRequest("That image could not be read.");
            double scale = Math.min(1.0, (double) MAX_IMAGE_EDGE / Math.max(src.getWidth(), src.getHeight()));
            int w = Math.max(1, (int) (src.getWidth() * scale)), h = Math.max(1, (int) (src.getHeight() * scale));
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();
            var bos = new ByteArrayOutputStream();
            ImageIO.write(out, "jpg", bos); // re-encoding also strips metadata and any embedded payload
            return bos.toByteArray();
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException a)
                throw a;
            throw ApiException.badRequest("That image could not be processed.");
        }
    }
}
