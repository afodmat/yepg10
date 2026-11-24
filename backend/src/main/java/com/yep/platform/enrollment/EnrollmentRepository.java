package com.yep.platform.enrollment;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByUserIdAndActivityId(Long userId, Long activityId);
    List<Enrollment> findByUserIdAndStatusNotOrderByEnrolledAtDesc(Long userId, EnrollmentStatus status);
    Page<Enrollment> findByActivityIdAndStatusNot(Long activityId, EnrollmentStatus status, Pageable p);

    @Query("select count(e) from Enrollment e where e.activityId = :id and e.status <> com.yep.platform.enrollment.EnrollmentStatus.CANCELLED")
    long countActive(@Param("id") Long activityId);

    @Query("select e.activityId, count(e) from Enrollment e where e.activityId in :ids and e.status <> com.yep.platform.enrollment.EnrollmentStatus.CANCELLED group by e.activityId")
    List<Object[]> countsFor(@Param("ids") Collection<Long> ids);
}
