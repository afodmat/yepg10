package com.yep.platform.mentorship;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public final class MentorshipRepositories {
    private MentorshipRepositories() {}
    public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long> {
        @Query("select m from MentorProfile m where lower(m.expertise) like lower(concat('%', :q, '%')) and m.userId in (select u.id from com.yep.platform.user.User u where u.status = com.yep.platform.user.UserStatus.ACTIVE)")
        Page<MentorProfile> search(@Param("q") String q, Pageable p);
    }
    public interface AssignmentRepository extends JpaRepository<MentorshipAssignment, Long> {
        List<MentorshipAssignment> findByMentorIdAndStatus(Long mentorId, String status);
        @Query("select a from MentorshipAssignment a where a.status = 'ACTIVE' and (a.participantId = :uid or a.teamId in (select m.teamId from com.yep.platform.team.TeamMember m where m.userId = :uid))")
        List<MentorshipAssignment> forMentee(@Param("uid") Long userId);
        long countByMentorIdAndStatus(Long mentorId, String status);
        Page<MentorshipAssignment> findByStatus(String status, Pageable p);
    }
    public interface RequestRepository extends JpaRepository<MentorshipRequest, Long> {
        List<MentorshipRequest> findByMentorIdAndStatusOrderByCreatedAtDesc(Long mentorId, String status);
        List<MentorshipRequest> findByParticipantIdOrderByCreatedAtDesc(Long participantId);
        boolean existsByParticipantIdAndMentorIdAndStatus(Long participantId, Long mentorId, String status);
    }
}
