package com.yep.platform.team;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public final class TeamRepositories {
    private TeamRepositories() {}

    public interface TeamRepository extends JpaRepository<Team, Long> {
        @Query("select t from Team t where lower(t.name) like lower(concat('%', :q, '%')) and (:all = true or (t.status = 'ACTIVE' and t.open = true))")
        Page<Team> browse(@Param("q") String q, @Param("all") boolean all, Pageable p);

        @Query("select t from Team t where lower(t.name) like lower(concat('%', :q, '%')) and t.id in (select m.teamId from TeamMember m where m.userId = :uid)")
        Page<Team> mine(@Param("q") String q, @Param("uid") Long userId, Pageable p);
    }
    public interface MemberRepository extends JpaRepository<TeamMember, Long> {
        List<TeamMember> findByTeamIdOrderByJoinedAt(Long teamId);
        Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);
        long countByTeamId(Long teamId);
        List<TeamMember> findByUserId(Long userId);
    }
    public interface InvitationRepository extends JpaRepository<TeamInvitation, Long> {
        @Query("select i from TeamInvitation i where lower(i.email) = lower(:email) and i.status = 'PENDING' order by i.createdAt desc")
        List<TeamInvitation> pendingFor(@Param("email") String email);
        @Query("select count(i) > 0 from TeamInvitation i where i.teamId = :t and lower(i.email) = lower(:email) and i.status = 'PENDING'")
        boolean pendingExists(@Param("t") Long teamId, @Param("email") String email);
    }
}
