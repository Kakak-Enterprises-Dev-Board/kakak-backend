package com.kakak.kakak_backend.jobs.jobdto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishJobRequest {

    @NotNull(message = "Draft ID is required")
    private UUID draftId;
}
