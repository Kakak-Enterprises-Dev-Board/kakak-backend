package com.kakak.kakak_backend.Files.fileService;

import com.kakak.kakak_backend.Files.fileDTO.ConfirmUploadRequest;
import com.kakak.kakak_backend.Files.fileDTO.FileResponse;
import com.kakak.kakak_backend.Files.fileDTO.PresignedUploadRequest;
import com.kakak.kakak_backend.Files.fileDTO.PresignedUploadResponse;
import com.kakak.kakak_backend.Files.fileEntity.FileModule;
import com.kakak.kakak_backend.Files.fileEntity.files;
import com.kakak.kakak_backend.Files.fileRepository.FilesRepository;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authRepository.UsersRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final int UPLOAD_URL_EXPIRES_IN_SECONDS = 900;
    private static final String LOCAL_BUCKET_NAME = "local";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );
    private static final Map<String, String> FILE_EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png"
    );

    private final FilesRepository filesRepository;
    private final UsersRepo usersRepo;

    @Value("${app.file-storage.directory:uploads}")
    private String storageDirectory;

    @Value("${app.file-storage.max-pdf-size-bytes:10485760}")
    private long maxPdfSizeBytes;

    @Value("${app.file-storage.max-image-size-bytes:5242880}")
    private long maxImageSizeBytes;

    public PresignedUploadResponse createPresignedUpload(PresignedUploadRequest request, Authentication authentication) {
        requireAuthenticated(authentication);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (!StringUtils.hasText(request.getFileName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName is required");
        }
        if (!StringUtils.hasText(request.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType is required");
        }
        if (request.getModule() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "module is required");
        }
        validateContentType(request.getContentType());
        sanitizeOriginalFileName(request.getFileName());

        String storedFileName = UUID.randomUUID() + FILE_EXTENSIONS_BY_CONTENT_TYPE.get(request.getContentType());
        String fileKey = request.getModule().name() + "/" + storedFileName;
        String uploadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/local-upload/")
                .path(fileKey)
                .toUriString();

        return new PresignedUploadResponse(uploadUrl, fileKey, UPLOAD_URL_EXPIRES_IN_SECONDS);
    }

    public void uploadLocalFile(String module, String storedFileName, InputStream inputStream, Authentication authentication) {
        requireAuthenticated(authentication);
        validateModule(module);
        String cleanStoredFileName = sanitizeStoredFileName(storedFileName);
        String fileKey = module + "/" + cleanStoredFileName;
        Path target = resolveFileKey(fileKey);

        try {
            java.nio.file.Files.createDirectories(target.getParent());
            copyWithSizeLimit(inputStream, target, maxSizeForStoredFileName(cleanStoredFileName));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store file", ex);
        }
    }

    @Transactional
    public FileResponse confirmUpload(ConfirmUploadRequest request, Authentication authentication) {
        AuthUsers uploadedBy = currentUser(authentication);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (!StringUtils.hasText(request.getFileKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileKey is required");
        }
        if (!StringUtils.hasText(request.getFileName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName is required");
        }
        if (!StringUtils.hasText(request.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType is required");
        }
        validateContentType(request.getContentType());

        String cleanFileKey = sanitizeFileKey(request.getFileKey());
        if (filesRepository.existsByFileKey(cleanFileKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "File already confirmed");
        }
        Path localFile = resolveFileKey(cleanFileKey);
        if (!java.nio.file.Files.exists(localFile)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file does not exist");
        }

        String module = moduleFromFileKey(cleanFileKey);
        validateModule(module);
        String actualContentType = detectContentType(localFile);
        validateContentType(actualContentType);
        if (!actualContentType.equals(request.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file type does not match contentType");
        }
        long actualFileSize = actualFileSize(localFile);
        validateFileSize(actualContentType, actualFileSize);
        String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/content/")
                .path(cleanFileKey)
                .toUriString();

        files file = new files();
        file.setUploaded_by(uploadedBy);
        file.setBucket_name(LOCAL_BUCKET_NAME);
        file.setFile_name(sanitizeOriginalFileName(request.getFileName()));
        file.setFile_key(cleanFileKey);
        file.setMime_type(actualContentType);
        file.setFile_size(BigInteger.valueOf(actualFileSize));
        file.setPublic_url(fileUrl);
        file.setModule(module);

        return toResponse(filesRepository.save(file));
    }

    public FileResponse getFile(UUID id, Authentication authentication) {
        requireAuthenticated(authentication);
        files file = findFile(id);
        authorizeFileAccess(file, authentication);
        return toResponse(file);
    }

    public files getFileByKey(String module, String storedFileName, Authentication authentication) {
        requireAuthenticated(authentication);
        validateModule(module);
        String fileKey = module + "/" + sanitizeStoredFileName(storedFileName);
        files file = findFileByKey(fileKey);
        authorizeFileAccess(file, authentication);
        return file;
    }

    public files findFileByKey(String fileKey) {
        return filesRepository.findByFileKey(sanitizeFileKey(fileKey))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    public Resource getFileContent(String module, String storedFileName, Authentication authentication) {
        requireAuthenticated(authentication);
        validateModule(module);
        String cleanStoredFileName = sanitizeStoredFileName(storedFileName);
        String fileKey = module + "/" + cleanStoredFileName;
        files file = findFileByKey(fileKey);
        authorizeFileAccess(file, authentication);
        Path path = resolveFileKey(fileKey);
        if (!java.nio.file.Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File content not found");
        }
        return new FileSystemResource(path);
    }

    @Transactional
    public void deleteFile(UUID id, Authentication authentication) {
        requireAuthenticated(authentication);
        files file = findFile(id);
        authorizeFileAccess(file, authentication);
        Path filePath = resolveFileKey(file.getFile_key());
        filesRepository.delete(file);
        filesRepository.flush();
        try {
            java.nio.file.Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete file content", ex);
        }
    }

    public files findFile(UUID id) {
        return filesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    public FileResponse toResponse(files file) {
        return new FileResponse(
                file.getId(),
                file.getFile_name(),
                file.getMime_type(),
                file.getFile_size(),
                file.getPublic_url(),
                file.getModule(),
                file.getUploaded_by().getId(),
                file.getUploaded_at()
        );
    }

    private Path resolveFileKey(String fileKey) {
        String cleanFileKey = sanitizeFileKey(fileKey);
        Path base = Path.of(storageDirectory).toAbsolutePath().normalize();
        Path resolved = base.resolve(cleanFileKey).normalize();
        if (!resolved.startsWith(base)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid fileKey");
        }
        return resolved;
    }

    private String moduleFromFileKey(String fileKey) {
        int slashIndex = fileKey.indexOf('/');
        if (slashIndex <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid fileKey");
        }
        return fileKey.substring(0, slashIndex);
    }

    private void validateModule(String module) {
        try {
            FileModule.valueOf(module);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid module");
        }
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported contentType");
        }
    }

    private void validateFileSize(String contentType, long fileSize) {
        long maxSize = "application/pdf".equals(contentType) ? maxPdfSizeBytes : maxImageSizeBytes;
        if (fileSize > maxSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds allowed limit");
        }
    }

    private long actualFileSize(Path file) {
        try {
            return java.nio.file.Files.size(file);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read file size", ex);
        }
    }

    private void copyWithSizeLimit(InputStream inputStream, Path target, long maxSizeBytes) throws IOException {
        long totalBytes = 0;
        byte[] buffer = new byte[8192];
        boolean sizeExceeded = false;
        try (OutputStream outputStream = java.nio.file.Files.newOutputStream(target)) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > maxSizeBytes) {
                    sizeExceeded = true;
                    break;
                }
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        if (sizeExceeded) {
            java.nio.file.Files.deleteIfExists(target);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds allowed limit");
        }
    }

    private long maxSizeForStoredFileName(String storedFileName) {
        if (storedFileName.endsWith(".pdf")) {
            return maxPdfSizeBytes;
        }
        return maxImageSizeBytes;
    }

    private String detectContentType(Path file) {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(file)) {
            byte[] header = inputStream.readNBytes(8);
            if (header.length >= 4
                    && header[0] == 0x25
                    && header[1] == 0x50
                    && header[2] == 0x44
                    && header[3] == 0x46) {
                return "application/pdf";
            }
            if (header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            if (header.length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A) {
                return "image/png";
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify file type", ex);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported uploaded file type");
    }

    private void authorizeFileAccess(files file, Authentication authentication) {
        requireAuthenticated(authentication);
        if (isAdmin(authentication)) {
            return;
        }
        AuthUsers currentUser = currentUser(authentication);
        boolean isUploader = file.getUploaded_by() != null
                && file.getUploaded_by().getId().equals(currentUser.getId());
        if (!isUploader) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private AuthUsers currentUser(Authentication authentication) {
        requireAuthenticated(authentication);
        return usersRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ADMIN".equals(authority) || "ROLE_ADMIN".equals(authority));
    }

    private String sanitizeFileKey(String fileKey) {
        if (!StringUtils.hasText(fileKey) || fileKey.contains("\\") || fileKey.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid fileKey");
        }
        if (Path.of(fileKey).isAbsolute()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid fileKey");
        }
        String normalized = fileKey.replace('\\', '/');
        String[] parts = normalized.split("/");
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid fileKey");
        }
        validateModule(parts[0]);
        return parts[0] + "/" + sanitizeStoredFileName(parts[1]);
    }

    private String sanitizeStoredFileName(String storedFileName) {
        if (!StringUtils.hasText(storedFileName)
                || storedFileName.contains("/")
                || storedFileName.contains("\\")
                || storedFileName.contains("..")
                || Path.of(storedFileName).isAbsolute()
                || !storedFileName.matches("[A-Fa-f0-9-]{36}\\.(pdf|jpg|png)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storedFileName");
        }
        return storedFileName;
    }

    private String sanitizeOriginalFileName(String fileName) {
        String cleanName = Path.of(fileName).getFileName().toString();
        cleanName = cleanName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!StringUtils.hasText(cleanName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid fileName");
        }
        return cleanName;
    }
}
