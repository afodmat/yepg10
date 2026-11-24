package com.yep.platform.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/** Tokens live only in httpOnly cookies so page JavaScript (and any XSS) can never read them. */
@Service
public class CookieService {
    public static final String ACCESS = "yep_at";
    public static final String REFRESH = "yep_rt";
    private static final String REFRESH_PATH = "/api/auth";

    private final boolean secure;
    private final String sameSite;
    private final JwtProperties jwt;

    public CookieService(@Value("${yep.cookies.secure}") boolean secure, @Value("${yep.cookies.same-site}") String sameSite, JwtProperties jwt) {
        this.secure = secure; this.sameSite = sameSite; this.jwt = jwt;
    }

    public void writeAccess(HttpServletResponse res, String token) { add(res, ACCESS, token, "/", jwt.accessTtl()); }
    public void writeRefresh(HttpServletResponse res, String token) { add(res, REFRESH, token, REFRESH_PATH, jwt.refreshTtl()); }
    public void clear(HttpServletResponse res) {
        add(res, ACCESS, "", "/", Duration.ZERO);
        add(res, REFRESH, "", REFRESH_PATH, Duration.ZERO);
    }

    public Optional<String> read(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return Optional.empty();
        return Arrays.stream(req.getCookies()).filter(c -> c.getName().equals(name)).map(Cookie::getValue).filter(v -> !v.isEmpty()).findFirst();
    }

    private void add(HttpServletResponse res, String name, String value, String path, Duration maxAge) {
        res.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value).httpOnly(true).secure(secure)
                .sameSite(sameSite).path(path).maxAge(maxAge).build().toString());
    }
}
