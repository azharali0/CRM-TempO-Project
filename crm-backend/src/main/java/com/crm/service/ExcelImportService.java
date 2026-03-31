package com.crm.service;

import com.crm.dto.response.ImportResultDTO;
import com.crm.exception.BadRequestException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.User;
import com.crm.repository.CustomerRepository;
import com.crm.util.CsvSanitizer;
import com.crm.util.InputSanitizer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportService.class);
    private static final int MAX_ROWS = 5000;
    private static final int BATCH_SIZE = 100;

    private final CustomerRepository customerRepository;

    public ExcelImportService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public ImportResultDTO importCustomers(MultipartFile file, User currentUser) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BadRequestException("File name is required");
        }

        String lower = filename.toLowerCase();
        if (lower.endsWith(".xlsx")) {
            return importFromExcel(file, currentUser);
        } else if (lower.endsWith(".csv")) {
            return importFromCsv(file, currentUser);
        } else {
            throw new BadRequestException("Only .xlsx and .csv files are supported");
        }
    }

    private ImportResultDTO importFromExcel(MultipartFile file, User currentUser) {
        List<String[]> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();

            if (rowCount - 1 > MAX_ROWS) { // minus header
                throw new BadRequestException("File exceeds maximum of " + MAX_ROWS + " rows");
            }

            boolean headerSkipped = false;
            for (Row row : sheet) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String name = getCellValue(row, 0);
                String email = getCellValue(row, 1);
                String phone = getCellValue(row, 2);
                String company = getCellValue(row, 3);
                String city = getCellValue(row, 4);
                rows.add(new String[]{name, email, phone, company, city});
            }
        } catch (IOException e) {
            throw new BadRequestException("Invalid Excel file format: " + e.getMessage());
        }

        return processRows(rows, currentUser);
    }

    private ImportResultDTO importFromCsv(MultipartFile file, User currentUser) {
        List<String[]> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean headerSkipped = false;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                lineCount++;
                if (lineCount > MAX_ROWS) {
                    throw new BadRequestException("File exceeds maximum of " + MAX_ROWS + " rows");
                }

                String[] parts = line.split(",", -1);
                String name = parts.length > 0 ? parts[0].trim() : "";
                String email = parts.length > 1 ? parts[1].trim() : "";
                String phone = parts.length > 2 ? parts[2].trim() : "";
                String company = parts.length > 3 ? parts[3].trim() : "";
                String city = parts.length > 4 ? parts[4].trim() : "";
                rows.add(new String[]{name, email, phone, company, city});
            }
        } catch (IOException e) {
            throw new BadRequestException("Invalid CSV file format: " + e.getMessage());
        }

        return processRows(rows, currentUser);
    }

    private ImportResultDTO processRows(List<String[]> rows, User currentUser) {
        int created = 0;
        int failed = 0;
        List<ImportResultDTO.ImportError> errors = new ArrayList<>();
        List<Customer> batch = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2; // 1-indexed + header
            String[] row = rows.get(i);

            String name = CsvSanitizer.sanitize(row[0]);
            String email = CsvSanitizer.sanitize(row[1]);
            String phone = CsvSanitizer.sanitize(row[2]);
            String company = CsvSanitizer.sanitize(row[3]);
            String city = CsvSanitizer.sanitize(row[4]);

            // Validate required fields
            if (name == null || name.isBlank()) {
                errors.add(ImportResultDTO.ImportError.builder()
                        .row(rowNum).field("name").message("Name is required").build());
                failed++;
                continue;
            }

            // Validate email format if provided
            if (email != null && !email.isBlank() && !email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                errors.add(ImportResultDTO.ImportError.builder()
                        .row(rowNum).field("email").message("Invalid email format").build());
                failed++;
                continue;
            }

            // Check for duplicate email
            if (email != null && !email.isBlank() && customerRepository.existsByEmail(email)) {
                errors.add(ImportResultDTO.ImportError.builder()
                        .row(rowNum).field("email").message("Email already exists").build());
                failed++;
                continue;
            }

            Customer customer = Customer.builder()
                    .name(InputSanitizer.sanitize(name))
                    .email(InputSanitizer.sanitizeOrNull(email))
                    .phone(InputSanitizer.sanitizeOrNull(phone))
                    .company(InputSanitizer.sanitizeOrNull(company))
                    .city(InputSanitizer.sanitizeOrNull(city))
                    .createdBy(currentUser)
                    .build();

            batch.add(customer);
            created++;

            // Batch commit every BATCH_SIZE rows
            if (batch.size() >= BATCH_SIZE) {
                customerRepository.saveAll(batch);
                customerRepository.flush();
                batch.clear();
            }
        }

        // Save remaining batch
        if (!batch.isEmpty()) {
            customerRepository.saveAll(batch);
            customerRepository.flush();
        }

        return ImportResultDTO.builder()
                .created(created)
                .failed(failed)
                .errors(errors)
                .build();
    }

    private String getCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
