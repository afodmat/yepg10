package com.yep.platform.file;

import com.yep.platform.file.FileService.*;
import com.yep.platform.security.AuthUser;
import java.util.concurrent.TimeUnit;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileController {
    public record PhotoRequest(Long fileId) {
    }

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileView upload(@RequestParam("file") MultipartFile file, @RequestParam String purpose,
            @AuthenticationPrincipal AuthUser me) {
        return service.upload(file, purpose, me);
    }

    @GetMapping("/api/files/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        var d = service.download(id, me);
        var disposition = (d.inline() ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(d.name()).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate()).body(d.content());
    }

    @PutMapping("/api/users/me/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void photo(@RequestBody PhotoRequest r, @AuthenticationPrincipal AuthUser me) {
        service.setProfilePhoto(me.id(), r.fileId());
    }
}
