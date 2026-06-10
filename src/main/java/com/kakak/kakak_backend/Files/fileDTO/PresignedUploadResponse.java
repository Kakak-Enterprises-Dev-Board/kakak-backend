package com.kakak.kakak_backend.Files.fileDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignedUploadResponse {
    private String uploadUrl;
    private String fileKey;
    private Integer expiresIn;
}
