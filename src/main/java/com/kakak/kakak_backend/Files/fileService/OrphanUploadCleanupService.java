package com.kakak.kakak_backend.Files.fileService;

import com.kakak.kakak_backend.Files.fileEntity.FileModule;
import com.kakak.kakak_backend.Files.fileRepository.FilesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class OrphanUploadCleanupService {

    private static final Pattern GENERATED_FILE_NAME_PATTERN =
            Pattern.compile("[A-Fa-f0-9-]{36}\\.(pdf|jpg|png)");

    private final FilesRepository filesRepository;

    @Value("${app.file-storage.directory:uploads}")
    private String storageDirectory;

    @Value("${app.file-storage.orphan-cleanup-min-age-ms:3600000}")
    private long orphanCleanupMinAgeMs;

    @Scheduled(fixedDelayString = "${app.file-storage.orphan-cleanup-delay-ms:3600000}")
    public void cleanupOrphanUploads() {
        Path basePath = Path.of(storageDirectory).toAbsolutePath().normalize();
        if (!java.nio.file.Files.isDirectory(basePath)) {
            return;
        }

        for (FileModule module : FileModule.values()) {
            cleanupModuleDirectory(basePath, module.name());
        }
    }

    private void cleanupModuleDirectory(Path basePath, String module) {
        Path modulePath = basePath.resolve(module).normalize();
        if (!modulePath.startsWith(basePath) || !java.nio.file.Files.isDirectory(modulePath)) {
            return;
        }

        try (var stream = java.nio.file.Files.list(modulePath)) {
            stream.filter(java.nio.file.Files::isRegularFile)
                    .filter(this::isGeneratedStorageFile)
                    .filter(this::isOldEnoughForCleanup)
                    .filter(file -> !filesRepository.existsByFileKey(module + "/" + file.getFileName()))
                    .forEach(this::deleteIfStillSafe);
        } catch (IOException ignored) {
            // Cleanup is best effort and must not affect API requests.
        }
    }

    private boolean isGeneratedStorageFile(Path file) {
        return GENERATED_FILE_NAME_PATTERN.matcher(file.getFileName().toString()).matches();
    }

    private boolean isOldEnoughForCleanup(Path file) {
        try {
            Instant lastModified = java.nio.file.Files.getLastModifiedTime(file).toInstant();
            return lastModified.plus(Duration.ofMillis(orphanCleanupMinAgeMs)).isBefore(Instant.now());
        } catch (IOException ex) {
            return false;
        }
    }

    private void deleteIfStillSafe(Path file) {
        try {
            java.nio.file.Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Leave the file for the next cleanup attempt.
        }
    }
}
