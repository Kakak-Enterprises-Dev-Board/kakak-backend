package com.kakak.kakak_backend.Employer.EmployerDTO;

import com.kakak.kakak_backend.Employer.EmployerEnum.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EmployerDocumentRequest {
    @NotNull
    private UUID fileId;
    @NotNull
    private DocumentType documentType;
}
