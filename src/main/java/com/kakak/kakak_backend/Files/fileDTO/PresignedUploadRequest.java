package com.kakak.kakak_backend.Files.fileDTO;

import com.kakak.kakak_backend.Files.fileEntity.FileModule;
import lombok.Data;

@Data
public class PresignedUploadRequest {
    private String fileName;
    private String contentType;
    private FileModule module;
}
