package com.crm.util;

import com.crm.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileValidatorTest {

    @Test
    void validate_acceptsPdfFile() {
        byte[] pdfHeader = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34}; // %PDF-1.4
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                "application/pdf", pdfHeader);
        assertDoesNotThrow(() -> FileValidator.validate(file));
    }

    @Test
    void validate_acceptsJpegFile() {
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg",
                "image/jpeg", jpegHeader);
        assertDoesNotThrow(() -> FileValidator.validate(file));
    }

    @Test
    void validate_acceptsPngFile() {
        byte[] pngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "image.png",
                "image/png", pngHeader);
        assertDoesNotThrow(() -> FileValidator.validate(file));
    }

    @Test
    void validate_rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf",
                "application/pdf", new byte[0]);
        assertThrows(BadRequestException.class, () -> FileValidator.validate(file));
    }

    @Test
    void validate_rejectsOversizedFile() {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        largeContent[0] = 0x25;
        largeContent[1] = 0x50;
        largeContent[2] = 0x44;
        largeContent[3] = 0x46;
        MockMultipartFile file = new MockMultipartFile("file", "large.pdf",
                "application/pdf", largeContent);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> FileValidator.validate(file));
        assertTrue(ex.getMessage().contains("10 MB"));
    }

    @Test
    void validate_rejectsInvalidFileType() {
        byte[] exeHeader = {0x4D, 0x5A}; // MZ (Windows executable)
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe",
                "application/octet-stream", exeHeader);
        assertThrows(BadRequestException.class, () -> FileValidator.validate(file));
    }

    @Test
    void validate_rejectsRenamedExecutable() {
        // Renaming malware.exe to report.pdf should still be rejected
        byte[] exeHeader = {0x4D, 0x5A, (byte) 0x90, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf",
                "application/pdf", exeHeader);
        assertThrows(BadRequestException.class, () -> FileValidator.validate(file));
    }

    @Test
    void sanitizeFilename_removesPathTraversal() {
        assertEquals("etcpasswd", FileValidator.sanitizeFilename("../../etc/passwd"));
    }

    @Test
    void sanitizeFilename_removesBackslashes() {
        assertEquals("etcpasswd", FileValidator.sanitizeFilename("..\\..\\etc\\passwd"));
    }

    @Test
    void sanitizeFilename_preservesNormalFilename() {
        assertEquals("report.pdf", FileValidator.sanitizeFilename("report.pdf"));
    }

    @Test
    void sanitizeFilename_handlesNull() {
        assertEquals("unknown", FileValidator.sanitizeFilename(null));
    }

    @Test
    void detectMimeType_identifiesPdf() {
        byte[] pdfHeader = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                "application/pdf", pdfHeader);
        assertEquals("application/pdf", FileValidator.detectMimeType(file));
    }

    @Test
    void detectMimeType_identifiesJpeg() {
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg",
                "image/jpeg", jpegHeader);
        assertEquals("image/jpeg", FileValidator.detectMimeType(file));
    }

    @Test
    void detectMimeType_returnsNullForUnknown() {
        byte[] randomHeader = {0x01, 0x02, 0x03, 0x04};
        MockMultipartFile file = new MockMultipartFile("file", "unknown.xyz",
                "application/octet-stream", randomHeader);
        assertNull(FileValidator.detectMimeType(file));
    }
}
