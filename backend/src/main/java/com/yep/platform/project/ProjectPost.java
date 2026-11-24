package com.yep.platform.project;

import jakarta.persistence.*;
import java.time.Instant;

/** Shared shape of project updates and comments (two tables, one mapping each below). */
public final class ProjectPost {
    private ProjectPost() {}

    @Entity @Table(name = "project_updates")
    public static class Update {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(name = "project_id") public Long projectId;
        @Column(name = "author_id") public Long authorId;
        public String content;
        @Column(name = "created_at") public Instant createdAt = Instant.now();
        public Update() {}
    }

    @Entity @Table(name = "project_comments")
    public static class Comment {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(name = "project_id") public Long projectId;
        @Column(name = "author_id") public Long authorId;
        public String content;
        @Column(name = "created_at") public Instant createdAt = Instant.now();
        public Comment() {}
    }
}
