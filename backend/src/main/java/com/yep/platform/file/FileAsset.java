package com.yep.platform.file;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "files")
public class FileAsset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "owner_id") public Long ownerId;
    public String purpose;
    @Column(name = "original_name") public String originalName;
    @Column(name = "stored_name") public String storedName;
    @Column(name = "content_type") public String contentType;
    @Column(name = "size_bytes") public long sizeBytes;
    @Column(name = "created_at") public Instant createdAt = Instant.now();
    public FileAsset() {}
}
