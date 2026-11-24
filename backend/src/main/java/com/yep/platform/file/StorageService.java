package com.yep.platform.file;

import java.io.IOException;

/** Storage abstraction: swap LocalStorageService for an S3/GCS/Azure implementation without touching callers. */
public interface StorageService {
    void store(String storedName, byte[] content) throws IOException;
    byte[] load(String storedName) throws IOException;
    void delete(String storedName) throws IOException;
}
