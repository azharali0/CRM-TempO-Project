package com.crm.util;

import com.crm.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Validates uploaded files by magic bytes (file signatures), not by file extension.
 * This prevents renaming malware.exe to report.pdf.
 */
public final class FileValidator {

    private FileValidator() {
    }

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    // Magic byte signatures for allowed file types
    private static final Map<String, byte[][]> MAGIC_BYTES = Map.of(
            "application/pdf", new byte[][]{
                    {0x25, 0x50, 0x44, 0x46} // %PDF
            },
            "image/jpeg", new byte[][]{
                    {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
            },
            "image/png", new byte[][]{
                    {(byte) 0x89, 0x50, 0x4E, 0x47} // .PNG
            },
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[][]{
                    {0x50, 0x4B, 0x03, 0x04} // PK (ZIP-based, DOCX)
            },
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[][]{
                    {0x50, 0x4B, 0x03, 0x04} // PK (ZIP-based, XLSX)
            }
    );

    /**
     * Validates the file: checks size and magic bytes.
     *
     * @param file the uploaded file
     * @throws BadRequestException if validation fails
     */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed size of 10 MB");
        }

        String detectedType = detectMimeType(file);
        if (detectedType == null) {
            throw new BadRequestException(
                    "File type not allowed. Allowed types: PDF, DOCX, XLSX, JPG, PNG");
        }
    }

    /**
     * Detects the MIME type from magic bytes.
     *
     * @param file the uploaded file
     * @return the detected MIME type, or null if not recognized
     */
    public static String detectMimeType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int bytesRead = is.read(header);
            if (bytesRead < 3) {
                return null;
            }

            for (Map.Entry<String, byte[][]> entry : MAGIC_BYTES.entrySet()) {
                for (byte[] signature : entry.getValue()) {
                    if (bytesRead >= signature.length && startsWith(header, signature)) {
                        // For ZIP-based formats (DOCX, XLSX), use the original filename extension
                        if (signature[0] == 0x50 && signature[1] == 0x4B) {
                            return resolveZipBasedType(file.getOriginalFilename());
                        }
                        return entry.getKey();
                    }
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Unable to read file content");
        }

        return null;
    }

    /**
     * Sanitizes a filename by removing path traversal characters.
     *
     * @param filename the original filename
     * @return sanitized filename
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        // Remove path traversal sequences and directory separators
        return filename
                .replace("..", "")
                .replace("/", "")
                .replace("\\", "")
                .replace("\0", "")
                .trim();
    }

    private static boolean startsWith(byte[] data, byte[] signature) {
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static String resolveZipBasedType(String filename) {
        if (filename == null) {
            return null;
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        return null; // Unknown ZIP-based type
    }
}
