package com.crm.service;

import com.crm.dto.response.DocumentDTO;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.Document;
import com.crm.model.entity.User;
import com.crm.repository.DocumentRepository;
import com.crm.util.FileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final DocumentRepository documentRepository;
    private final Path uploadDir;

    public FileStorageService(DocumentRepository documentRepository,
                              @Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + this.uploadDir, e);
        }
    }

    @Transactional
    public DocumentDTO uploadDocument(MultipartFile file, Customer customer, User currentUser) {
        FileValidator.validate(file);

        String detectedMime = FileValidator.detectMimeType(file);
        if (detectedMime == null) {
            throw new BadRequestException("File type not allowed. Allowed types: PDF, DOCX, XLSX, JPG, PNG");
        }

        String originalFilename = FileValidator.sanitizeFilename(file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String storedName = UUID.randomUUID() + extension;

        Path targetPath = this.uploadDir.resolve(storedName).normalize();
        // Prevent path traversal
        if (!targetPath.startsWith(this.uploadDir)) {
            throw new BadRequestException("Invalid file path detected");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new BadRequestException("Failed to store file");
        }

        Document document = Document.builder()
                .customer(customer)
                .originalFilename(originalFilename)
                .storedPath(storedName)
                .fileSize(file.getSize())
                .mimeType(detectedMime)
                .uploadedBy(currentUser)
                .build();

        try {
            Document saved = documentRepository.save(document);
            return DocumentDTO.fromEntity(saved);
        } catch (Exception e) {
            // Clean up orphaned file on DB failure
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {
                log.warn("Failed to clean up orphaned file: {}", targetPath);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Page<DocumentDTO> getDocumentsByCustomer(UUID customerId, Pageable pageable) {
        return documentRepository.findByCustomerIdAndDeletedFalse(customerId, pageable)
                .map(DocumentDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Document getDocumentById(UUID id) {
        return documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Path getFilePath(Document document) {
        Path filePath = this.uploadDir.resolve(document.getStoredPath()).normalize();
        if (!filePath.startsWith(this.uploadDir)) {
            throw new BadRequestException("Invalid file path");
        }
        return filePath;
    }

    @Transactional
    public void softDeleteDocument(UUID id) {
        Document document = getDocumentById(id);
        document.setDeleted(true);
        documentRepository.save(document);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
