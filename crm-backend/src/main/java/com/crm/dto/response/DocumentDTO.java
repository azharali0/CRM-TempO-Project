package com.crm.dto.response;

import com.crm.model.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {

    private UUID id;
    private UUID customerId;
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
    private String uploadedByName;
    private LocalDateTime uploadedAt;

    public static DocumentDTO fromEntity(Document doc) {
        return DocumentDTO.builder()
                .id(doc.getId())
                .customerId(doc.getCustomer().getId())
                .originalFilename(doc.getOriginalFilename())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .uploadedByName(doc.getUploadedBy() != null ? doc.getUploadedBy().getName() : null)
                .uploadedAt(doc.getUploadedAt())
                .build();
    }

    public String getFormattedSize() {
        if (fileSize == null) return "0 B";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }
}
