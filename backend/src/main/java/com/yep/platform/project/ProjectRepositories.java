package com.yep.platform.project;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public final class ProjectRepositories {
    private ProjectRepositories() {}

    public interface ProjectRepository extends JpaRepository<ImpactProject, Long> {
        Optional<ImpactProject> findByTeamId(Long teamId);

        @Query("select p from ImpactProject p where (lower(p.name) like lower(concat('%', :q, '%')) or lower(p.description) like lower(concat('%', :q, '%'))) and (:category = '' or p.category = :category) and p.status <> 'DRAFT'")
        Page<ImpactProject> browse(@Param("q") String q, @Param("category") String category, Pageable p);

        @Query("select p from ImpactProject p where p.teamId in (select m.teamId from com.yep.platform.team.TeamMember m where m.userId = :uid)")
        Page<ImpactProject> mine(@Param("uid") Long userId, Pageable p);
    }
    public interface MilestoneRepository extends JpaRepository<ProjectMilestone, Long> { List<ProjectMilestone> findByProjectIdOrderByPosition(Long projectId); }
    public interface UpdateRepository extends JpaRepository<ProjectPost.Update, Long> { List<ProjectPost.Update> findTop30ByProjectIdOrderByCreatedAtDesc(Long projectId); }
    public interface CommentRepository extends JpaRepository<ProjectPost.Comment, Long> { List<ProjectPost.Comment> findTop50ByProjectIdOrderByCreatedAtDesc(Long projectId); }
}
