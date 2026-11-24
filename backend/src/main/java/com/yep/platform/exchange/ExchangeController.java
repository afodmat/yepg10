package com.yep.platform.exchange;

import com.yep.platform.common.PageResponse;
import com.yep.platform.exchange.ExchangeDtos.*;
import com.yep.platform.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/exchange-sessions")
public class ExchangeController {
    private final ExchangeService service;
    public ExchangeController(ExchangeService service) { this.service = service; }

    @GetMapping public PageResponse<SessionView> list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String when,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @AuthenticationPrincipal AuthUser me) { return service.search(q, when, page, size, me); }

    @GetMapping("/{id}") public SessionView get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.get(id, me); }

    @PostMapping("/{id}/register") @ResponseStatus(HttpStatus.CREATED) public SessionView register(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.register(id, me); }
    @DeleteMapping("/{id}/register") @ResponseStatus(HttpStatus.NO_CONTENT) public void unregister(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { service.unregister(id, me); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public SessionView create(@Valid @RequestBody SessionRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.create(r, me, req.getRemoteAddr()); }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public SessionView update(@PathVariable Long id, @Valid @RequestBody SessionRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.update(id, r, me, req.getRemoteAddr()); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { service.delete(id, me, req.getRemoteAddr()); }
}
