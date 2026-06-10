package com.kakak.kakak_backend.Files.fileController;

import com.kakak.kakak_backend.Files.fileDTO.ConfirmUploadRequest;
import com.kakak.kakak_backend.Files.fileDTO.FileResponse;
import com.kakak.kakak_backend.Files.fileDTO.PresignedUploadRequest;
import com.kakak.kakak_backend.Files.fileDTO.PresignedUploadResponse;
import com.kakak.kakak_backend.Files.fileEntity.files;
import com.kakak.kakak_backend.Files.fileService.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/presigned-upload")
    public ResponseEntity<PresignedUploadResponse> createPresignedUpload(
            @RequestBody PresignedUploadRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(fileStorageService.createPresignedUpload(request, authentication));
    }

    @PutMapping("/local-upload/{module}/{storedFileName}")
    public ResponseEntity<Void> uploadLocalFile(
            @PathVariable String module,
            @PathVariable String storedFileName,
            HttpServletRequest request,
            Authentication authentication) throws IOException {
        fileStorageService.uploadLocalFile(module, storedFileName, request.getInputStream(), authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm-upload")
    public ResponseEntity<FileResponse> confirmUpload(
            @RequestBody ConfirmUploadRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(fileStorageService.confirmUpload(request, authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getFile(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(fileStorageService.getFile(id, authentication));
    }

    @GetMapping("/content/{module}/{storedFileName}")
    public ResponseEntity<Resource> getFileContent(
            @PathVariable String module,
            @PathVariable String storedFileName,
            Authentication authentication) {
        files file = fileStorageService.getFileByKey(module, storedFileName, authentication);
        Resource resource = fileStorageService.getFileContent(module, storedFileName, authentication);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMime_type()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFile_name() + "\"")
                .body(resource);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id, Authentication authentication) {
        fileStorageService.deleteFile(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
