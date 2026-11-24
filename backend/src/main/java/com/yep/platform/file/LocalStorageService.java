package com.yep.platform.file;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {
    private static final Pattern SAFE = Pattern.compile("^[a-f0-9-]{36}\\.[a-z0-9]{2,5}$");
    private final Path root;

    public LocalStorageService(@Value("${yep.storage.path:./uploads}") String path) throws IOException {
        this.root = Path.of(path).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override public void store(String name, byte[] content) throws IOException { Files.write(resolve(name), content, StandardOpenOption.CREATE_NEW); }
    @Override public byte[] load(String name) throws IOException { return Files.readAllBytes(resolve(name)); }
    @Override public void delete(String name) throws IOException { Files.deleteIfExists(resolve(name)); }

    /** Only server-generated names (uuid + extension) are accepted, so path traversal is impossible. */
    private Path resolve(String name) {
        if (!SAFE.matcher(name).matches()) throw new IllegalArgumentException("Invalid stored file name");
        Path p = root.resolve(name).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("Invalid stored file name");
        return p;
    }
}
