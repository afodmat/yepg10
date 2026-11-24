package com.yep.platform.certificate;

import com.yep.platform.certificate.CertificateService.*;
import com.yep.platform.security.AuthUser;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/certificates")
public class CertificateController {
    private final CertificateService service;
    public CertificateController(CertificateService service) { this.service = service; }

    @GetMapping public List<CertificateView> mine(@AuthenticationPrincipal AuthUser me) { return service.mine(me.id()); }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("YEP-certificate-" + id + ".pdf").build().toString())
                .header("X-Content-Type-Options", "nosniff").body(service.pdf(id, me));
    }

    /** Public verification (permitted in SecurityConfig). */
    @GetMapping("/verify/{code}") public VerifyView verify(@PathVariable String code) { return service.verify(code); }
}
