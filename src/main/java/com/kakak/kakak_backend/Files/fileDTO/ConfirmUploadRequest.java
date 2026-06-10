package com.kakak.kakak_backend.Files.fileDTO;

import lombok.Data;

@Data
public class ConfirmUploadRequest {
    private String fileKey;
    private String fileName;
    private String contentType;
    private Long fileSize;
}
