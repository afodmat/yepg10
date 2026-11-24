package com.yep.platform.exchange;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public final class ExchangeRepositories {
    private ExchangeRepositories() {}

    public interface SessionRepository extends JpaRepository<ExchangeSession, Long> {
        @Query("""
            select s from ExchangeSession s
            where lower(s.title) like lower(concat('%', :q, '%'))
              and (:staff = true or s.status <> com.yep.platform.exchange.ExchangeStatus.CANCELLED)
              and (:when = ''
                   or (:when = 'upcoming' and s.status = com.yep.platform.exchange.ExchangeStatus.SCHEDULED and s.startsAt >= :cutoff)
                   or (:when = 'completed' and (s.status = com.yep.platform.exchange.ExchangeStatus.COMPLETED
                        or (s.status = com.yep.platform.exchange.ExchangeStatus.SCHEDULED and s.startsAt < :cutoff))))
            """)
        Page<ExchangeSession> search(@Param("q") String q, @Param("when") String when, @Param("cutoff") Instant cutoff, @Param("staff") boolean staff, Pageable p);
    }

    public interface ParticipantRepository extends JpaRepository<ExchangeParticipant, Long> {
        Optional<ExchangeParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);
        long countBySessionId(Long sessionId);
        List<ExchangeParticipant> findBySessionId(Long sessionId);
    }
}
