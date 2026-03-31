package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.DocumentDTO;
import com.crm.dto.response.ImportResultDTO;
import com.crm.exception.AccessDeniedException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.Document;
import com.crm.model.entity.User;
import com.crm.service.CustomerService;
import com.crm.service.ExcelImportService;
import com.crm.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final FileStorageService fileStorageService;
    private final ExcelImportService excelImportService;
    private final CustomerService customerService;

    public DocumentController(FileStorageService fileStorageService,
                              ExcelImportService excelImportService,
                              CustomerService customerService) {
        this.fileStorageService = fileStorageService;
        this.excelImportService = excelImportService;
        this.customerService = customerService;
    }

    @PostMapping("/customers/{customerId}/documents")
    public ResponseEntity<ApiResponse<DocumentDTO>> uploadDocument(
            @PathVariable UUID customerId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        Customer customer = customerService.getCustomerEntityWithAccessCheck(customerId, currentUser);

        DocumentDTO doc = fileStorageService.uploadDocument(file, customer, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", doc));
    }

    @GetMapping("/customers/{customerId}/documents")
    public ResponseEntity<ApiResponse<Page<DocumentDTO>>> getDocuments(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        customerService.getCustomerEntityWithAccessCheck(customerId, currentUser);

        Page<DocumentDTO> documents = fileStorageService.getDocumentsByCustomer(
                customerId, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "uploadedAt")));

        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", documents));
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID id,
            Authentication authentication) throws MalformedURLException {

        User currentUser = (User) authentication.getPrincipal();
        Document document = fileStorageService.getDocumentById(id);

        // Access check: user must have access to the customer
        customerService.getCustomerEntityWithAccessCheck(document.getCustomer().getId(), currentUser);

        Path filePath = fileStorageService.getFilePath(document);
        Resource resource = new UrlResource(filePath.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable UUID id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        Document document = fileStorageService.getDocumentById(id);

        // Access check
        customerService.getCustomerEntityWithAccessCheck(document.getCustomer().getId(), currentUser);

        fileStorageService.softDeleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully"));
    }

    @PostMapping("/customers/import")
    public ResponseEntity<ApiResponse<ImportResultDTO>> importCustomers(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        ImportResultDTO result = excelImportService.importCustomers(file, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Import completed", result));
    }
}
