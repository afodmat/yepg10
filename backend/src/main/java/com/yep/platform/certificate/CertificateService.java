package com.yep.platform.certificate;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.yep.platform.activity.LearningActivityRepository;
import com.yep.platform.audit.AuditService;
import com.yep.platform.common.*;
import com.yep.platform.security.AuthUser;
import com.yep.platform.user.Role;
import com.yep.platform.user.UserDirectory;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificateService {
    public record CertificateView(Long id, String number, String activityTitle, String holderName, String issuer, Instant issuedAt, String verificationCode, String status) {}
    public record VerifyView(boolean valid, String status, String holderName, String activityTitle, String number, String issuer, Instant issuedAt) {}

    private final CertificateRepository repo;
    private final LearningActivityRepository activities;
    private final UserDirectory directory;
    private final AuditService audit;
    private final String frontendUrl;

    public CertificateService(CertificateRepository repo, LearningActivityRepository activities, UserDirectory directory, AuditService audit, @Value("${yep.frontend-url}") String frontendUrl) {
        this.repo = repo; this.activities = activities; this.directory = directory; this.audit = audit; this.frontendUrl = frontendUrl;
    }

    @Transactional(readOnly = true)
    public List<CertificateView> mine(Long userId) { return repo.findByUserIdOrderByIssuedAtDesc(userId).stream().map(this::view).toList(); }

    @Transactional(readOnly = true)
    public PageResponse<CertificateView> all(int page, int size) { return PageResponse.of(repo.findAllBy(PageResponse.request(page, size, Sort.by(Sort.Direction.DESC, "issuedAt"))), this::view); }

    /** Public: anyone holding a code can check it. Reveals only name, course and dates. */
    @Transactional(readOnly = true)
    public VerifyView verify(String code) {
        var c = repo.findByVerificationCode(code.trim().toUpperCase());
        if (c.isEmpty()) return new VerifyView(false, "NOT_FOUND", null, null, null, null, null);
        var v = view(c.get());
        return new VerifyView("VALID".equals(v.status()), v.status(), v.holderName(), v.activityTitle(), v.number(), v.issuer(), v.issuedAt());
    }

    @Transactional
    public void revoke(Long id, AuthUser admin, String ip) {
        var c = repo.findById(id).orElseThrow(() -> ApiException.notFound("Certificate not found"));
        c.status = "REVOKED";
        audit.record(null, admin.email(), "CERTIFICATE_REVOKED", "Certificate", String.valueOf(id), Map.of("number", c.certificateNumber), ip);
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Long id, AuthUser me) {
        var c = repo.findById(id).orElseThrow(() -> ApiException.notFound("Certificate not found"));
        if (!c.userId.equals(me.id()) && me.role() != Role.ADMIN) throw ApiException.notFound("Certificate not found");
        if (!"VALID".equals(c.status)) throw ApiException.conflict("This certificate has been revoked.");
        return render(view(c));
    }

    private byte[] render(CertificateView v) {
        var out = new ByteArrayOutputStream();
        var doc = new Document(PageSize.A4.rotate(), 60, 60, 60, 60);
        try {
            var writer = PdfWriter.getInstance(doc, out);
            doc.addTitle("YEP Certificate " + v.number()); doc.addAuthor(v.issuer());
            doc.open();
            Color teal = new Color(0x0a, 0x4c, 0x63), amber = new Color(0xf5, 0xa6, 0x23), slate = new Color(0x64, 0x74, 0x8b);
            PdfContentByte cb = writer.getDirectContent();
            cb.setColorStroke(teal); cb.setLineWidth(3f); cb.rectangle(24, 24, PageSize.A4.getHeight() - 48, PageSize.A4.getWidth() - 48); cb.stroke();
            cb.setColorStroke(amber); cb.setLineWidth(1f); cb.rectangle(32, 32, PageSize.A4.getHeight() - 64, PageSize.A4.getWidth() - 64); cb.stroke();
            doc.add(line("YEP · YOUTH EMPOWERMENT PROJECT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, teal), 20));
            doc.add(line("Certificate of Completion", FontFactory.getFont(FontFactory.TIMES_BOLD, 38, teal), 24));
            doc.add(line("This certifies that", FontFactory.getFont(FontFactory.HELVETICA, 13, slate), 14));
            doc.add(line(v.holderName(), FontFactory.getFont(FontFactory.TIMES_BOLDITALIC, 32, Color.BLACK), 14));
            doc.add(line("has successfully completed", FontFactory.getFont(FontFactory.HELVETICA, 13, slate), 10));
            doc.add(line(v.activityTitle(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, teal), 26));
            var date = DateTimeFormatter.ofPattern("d MMMM yyyy").withZone(ZoneOffset.UTC).format(v.issuedAt());
            doc.add(line("Issued " + date + " by " + v.issuer(), FontFactory.getFont(FontFactory.HELVETICA, 12, slate), 6));
            doc.add(line("Certificate no. " + v.number(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, teal), 6));
            doc.add(line("Verify at " + frontendUrl + "/#verify/" + v.verificationCode(), FontFactory.getFont(FontFactory.HELVETICA, 10, slate), 0));
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate certificate", e);
        }
        return out.toByteArray();
    }

    private static Paragraph line(String text, Font font, float spacingAfter) {
        var p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER); p.setSpacingAfter(spacingAfter);
        return p;
    }

    private CertificateView view(Certificate c) {
        var title = activities.findById(c.activityId).map(a -> a.title).orElse("Learning activity");
        return new CertificateView(c.id, c.certificateNumber, title, directory.name(c.userId), c.issuer, c.issuedAt, c.verificationCode, c.status);
    }
}
