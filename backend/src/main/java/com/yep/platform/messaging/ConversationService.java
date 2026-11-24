package com.yep.platform.messaging;

import com.yep.platform.common.ApiException;
import com.yep.platform.messaging.MessagingRepositories.*;
import com.yep.platform.notification.*;
import com.yep.platform.security.AuthUser;
import com.yep.platform.user.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistent messaging over REST (clients poll `?after=<lastId>`). The service layer is transport-agnostic,
 * so a STOMP/WebSocket endpoint can later call send() and push to subscribers without schema changes.
 */
@Service
public class ConversationService {
    public record ConversationView(Long id, String type, String title, String subtitle, String lastMessage, Instant lastAt, long unread) {}
    public record MessageView(Long id, Long senderId, String sender, String body, Instant createdAt, boolean mine) {}

    private final ConversationRepository conversations;
    private final ParticipantRepository participants;
    private final MessageRepository messages;
    private final UserDirectory directory;
    private final NotificationService notifications;
    private final JdbcTemplate jdbc;

    public ConversationService(ConversationRepository conversations, ParticipantRepository participants, MessageRepository messages, UserDirectory directory,
                               NotificationService notifications, JdbcTemplate jdbc) {
        this.conversations = conversations; this.participants = participants; this.messages = messages; this.directory = directory; this.notifications = notifications; this.jdbc = jdbc;
    }

    // ----- creating conversations -----
    /** Starts (or reuses) a direct chat, enforcing who may contact whom. */
    @Transactional
    public ConversationView direct(AuthUser me, Long otherId) {
        if (otherId.equals(me.id())) throw ApiException.badRequest("You cannot message yourself.");
        var other = directory.user(otherId).filter(u -> u.getStatus() == UserStatus.ACTIVE).orElseThrow(() -> ApiException.notFound("User not found"));
        if (!canMessage(me, other)) throw ApiException.forbidden("You can message mentors, facilitators and your teammates.");
        return view(ensureDirect(me.id(), otherId), me.id());
    }

    /** No authorization: for trusted internal callers (e.g. an accepted mentorship). */
    @Transactional
    public Conversation ensureDirect(Long a, Long b) {
        String key = Math.min(a, b) + ":" + Math.max(a, b);
        return conversations.findByDirectKey(key).orElseGet(() -> {
            var c = new Conversation();
            c.type = "DIRECT"; c.directKey = key;
            conversations.save(c);
            participants.save(new ConversationParticipant(c.id, a));
            participants.save(new ConversationParticipant(c.id, b));
            return c;
        });
    }

    @Transactional
    public void ensureTeamConversation(Long teamId) {
        if (conversations.findByTeamId(teamId).isEmpty()) { var c = new Conversation(); c.type = "TEAM"; c.teamId = teamId; conversations.save(c); }
    }
    @Transactional
    public void addToTeam(Long teamId, Long userId) {
        conversations.findByTeamId(teamId).ifPresent(c -> { if (!participants.existsById(new ConversationParticipant.Key(c.id, userId))) participants.save(new ConversationParticipant(c.id, userId)); });
    }
    @Transactional
    public void removeFromTeam(Long teamId, Long userId) {
        conversations.findByTeamId(teamId).ifPresent(c -> participants.deleteById(new ConversationParticipant.Key(c.id, userId)));
    }

    private boolean canMessage(AuthUser me, User other) {
        boolean meStaff = me.role() != Role.PARTICIPANT, otherStaff = other.getRole() != Role.PARTICIPANT;
        if (meStaff || otherStaff) return true;
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from team_members a join team_members b on a.team_id = b.team_id where a.user_id = ? and b.user_id = ?)", Boolean.class, me.id(), other.getId()));
    }

    // ----- reading -----
    @Transactional(readOnly = true)
    public List<ConversationView> list(AuthUser me) {
        var out = new ArrayList<ConversationView>();
        for (var p : participants.findByUserId(me.id())) out.add(view(conversations.findById(p.conversationId).orElseThrow(), me.id()));
        out.sort(Comparator.comparing((ConversationView v) -> v.lastAt() == null ? Instant.EPOCH : v.lastAt()).reversed());
        return out;
    }

    @Transactional(readOnly = true)
    public long unreadTotal(AuthUser me) { return list(me).stream().mapToLong(ConversationView::unread).sum(); }

    @Transactional(readOnly = true)
    public List<MessageView> messages(AuthUser me, Long conversationId, Long after) {
        requireParticipant(conversationId, me.id());
        var page = PageRequest.of(0, 100);
        List<Message> list;
        if (after != null && after > 0) list = messages.findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, after, page);
        else { list = new ArrayList<>(messages.findByConversationIdOrderByIdDesc(conversationId, PageRequest.of(0, 50))); Collections.reverse(list); }
        var names = directory.names(list.stream().map(m -> m.senderId).toList());
        return list.stream().map(m -> new MessageView(m.id, m.senderId, names.get(m.senderId), m.body, m.createdAt, m.senderId.equals(me.id()))).toList();
    }

    // ----- writing -----
    @Transactional
    public MessageView send(AuthUser me, Long conversationId, String body) {
        var c = conversations.findById(conversationId).orElseThrow(() -> ApiException.notFound("Conversation not found"));
        requireParticipant(conversationId, me.id());
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) throw ApiException.badRequest("Write a message first.");
        if (text.length() > 2000) throw ApiException.badRequest("Messages can be up to 2000 characters.");
        var m = new Message();
        m.conversationId = conversationId; m.senderId = me.id(); m.body = text;
        messages.save(m);
        markRead(me, conversationId);
        if ("DIRECT".equals(c.type) && me.role() != Role.PARTICIPANT) {
            participants.findByConversationId(conversationId).stream().filter(p -> !p.userId.equals(me.id())).forEach(p ->
                    notifications.notify(p.userId, NotificationType.MENTOR_MESSAGE, "New message from " + directory.name(me.id()), text.length() > 80 ? text.substring(0, 80) + "…" : text, "messages"));
        }
        return new MessageView(m.id, me.id(), directory.name(me.id()), m.body, m.createdAt, true);
    }

    @Transactional
    public void markRead(AuthUser me, Long conversationId) {
        var p = participants.findById(new ConversationParticipant.Key(conversationId, me.id())).orElseThrow(() -> ApiException.notFound("Conversation not found"));
        p.lastReadAt = Instant.now();
    }

    // ----- helpers -----
    private void requireParticipant(Long conversationId, Long userId) {
        if (!participants.existsById(new ConversationParticipant.Key(conversationId, userId))) throw ApiException.notFound("Conversation not found");
    }

    private ConversationView view(Conversation c, Long meId) {
        var mine = participants.findById(new ConversationParticipant.Key(c.id, meId)).orElseThrow();
        String title, subtitle;
        if ("TEAM".equals(c.type)) {
            title = jdbc.queryForList("select name from teams where id = ?", String.class, c.teamId).stream().findFirst().orElse("Team chat");
            subtitle = "Team conversation";
        } else {
            var otherId = jdbc.queryForList("select user_id from conversation_participants where conversation_id = ? and user_id <> ?", Long.class, c.id, meId).stream().findFirst().orElse(null);
            title = directory.name(otherId);
            subtitle = otherId == null ? "" : directory.user(otherId).map(u -> switch (u.getRole()) { case MENTOR -> "Mentor"; case FACILITATOR -> "Facilitator"; case ADMIN -> "YEP team"; default -> "Participant"; }).orElse("");
        }
        var last = messages.findByConversationIdOrderByIdDesc(c.id, PageRequest.of(0, 1));
        long unread = messages.unread(c.id, meId, mine.lastReadAt == null ? Instant.EPOCH : mine.lastReadAt);
        return new ConversationView(c.id, c.type, title, subtitle, last.isEmpty() ? null : last.get(0).body, last.isEmpty() ? null : last.get(0).createdAt, unread);
    }
}
