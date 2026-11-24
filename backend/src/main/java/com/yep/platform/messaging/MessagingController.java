package com.yep.platform.messaging;

import com.yep.platform.messaging.ConversationService.*;
import com.yep.platform.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/conversations")
public class MessagingController {
    public record DirectRequest(@NotNull Long userId) {}
    public record SendRequest(@NotBlank @Size(max = 2000) String body) {}

    private final ConversationService service;
    public MessagingController(ConversationService service) { this.service = service; }

    @GetMapping public List<ConversationView> list(@AuthenticationPrincipal AuthUser me) { return service.list(me); }
    @GetMapping("/unread-count") public Map<String, Long> unread(@AuthenticationPrincipal AuthUser me) { return Map.of("count", service.unreadTotal(me)); }
    @PostMapping("/direct") public ConversationView direct(@Valid @RequestBody DirectRequest r, @AuthenticationPrincipal AuthUser me) { return service.direct(me, r.userId()); }
    @GetMapping("/{id}/messages") public List<MessageView> messages(@PathVariable Long id, @RequestParam(required = false) Long after, @AuthenticationPrincipal AuthUser me) { return service.messages(me, id, after); }
    @PostMapping("/{id}/messages") @ResponseStatus(HttpStatus.CREATED) public MessageView send(@PathVariable Long id, @Valid @RequestBody SendRequest r, @AuthenticationPrincipal AuthUser me) { return service.send(me, id, r.body()); }
    @PostMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) public void read(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { service.markRead(me, id); }
}
