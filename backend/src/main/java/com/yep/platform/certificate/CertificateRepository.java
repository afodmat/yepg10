package com.yep.platform.certificate;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByUserIdOrderByIssuedAtDesc(Long userId);
    Optional<Certificate> findByUserIdAndActivityId(Long userId, Long activityId);
    Optional<Certificate> findByVerificationCode(String code);
    Page<Certificate> findAllBy(Pageable p);
}
