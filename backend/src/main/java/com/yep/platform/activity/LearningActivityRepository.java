package com.yep.platform.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningActivityRepository extends JpaRepository<LearningActivity, Long> {
    @Query("""
        select a from LearningActivity a
        where lower(a.title) like lower(concat('%', :q, '%'))
          and (:category = '' or a.category = :category)
          and (:level = '' or a.level = :level)
          and (:includeDraft = true or a.status <> com.yep.platform.activity.ActivityStatus.DRAFT)
        """)
    Page<LearningActivity> search(@Param("q") String q, @Param("category") String category, @Param("level") String level,
                                  @Param("includeDraft") boolean includeDraft, Pageable pageable);
}
