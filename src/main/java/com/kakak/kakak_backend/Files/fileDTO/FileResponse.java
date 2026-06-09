package com.kakak.kakak_backend.Files.fileDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
public class FileResponse {
    private UUID id;
    private String fileName;
    private String contentType;
    private BigInteger fileSize;
    private String fileUrl;
    private String module;
    private UUID uploadedBy;
    private Timestamp createdAt;
}
