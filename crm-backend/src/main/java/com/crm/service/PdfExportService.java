package com.crm.service;

import com.crm.exception.AccessDeniedException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.model.entity.Customer;
import com.crm.model.entity.Interaction;
import com.crm.model.entity.Lead;
import com.crm.model.entity.User;
import com.crm.model.enums.LeadStage;
import com.crm.model.enums.UserRole;
import com.crm.repository.CustomerRepository;
import com.crm.repository.InteractionRepository;
import com.crm.repository.LeadRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExportService {

    private static final Logger log = LoggerFactory.getLogger(PdfExportService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final InteractionRepository interactionRepository;

    public PdfExportService(LeadRepository leadRepository,
                            CustomerRepository customerRepository,
                            InteractionRepository interactionRepository) {
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.interactionRepository = interactionRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateMonthlySalesReport(User currentUser) {
        if (currentUser.getRole() == UserRole.SALES_REP) {
            throw new AccessDeniedException("Only MANAGER or ADMIN can generate monthly sales reports");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            addWatermark(writer, currentUser.getName());

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Monthly Sales Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Date range
            LocalDate now = LocalDate.now();
            LocalDate startOfMonth = now.withDayOfMonth(1);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            Paragraph dateRange = new Paragraph(
                    "Period: " + startOfMonth.format(DATE_FMT) + " to " + now.format(DATE_FMT),
                    normalFont);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(5);
            document.add(dateRange);

            Paragraph company = new Paragraph("CRM TempO Project", normalFont);
            company.setAlignment(Element.ALIGN_CENTER);
            company.setSpacingAfter(20);
            document.add(company);

            // Won leads table
            List<Lead> wonLeads = leadRepository.findByStage(LeadStage.WON);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 3, 2, 2});

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            BaseColor headerBg = new BaseColor(52, 73, 94);
            addTableHeader(table, "Lead Title", headerFont, headerBg);
            addTableHeader(table, "Customer", headerFont, headerBg);
            addTableHeader(table, "Value", headerFont, headerBg);
            addTableHeader(table, "Close Date", headerFont, headerBg);

            BigDecimal totalRevenue = BigDecimal.ZERO;
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
            for (Lead lead : wonLeads) {
                table.addCell(new Phrase(lead.getTitle(), cellFont));
                String customerName = lead.getCustomer() != null ? lead.getCustomer().getName() : "N/A";
                table.addCell(new Phrase(customerName, cellFont));
                BigDecimal val = lead.getValue() != null ? lead.getValue() : BigDecimal.ZERO;
                table.addCell(new Phrase("$" + val.toPlainString(), cellFont));
                String closeDate = lead.getExpectedCloseDate() != null
                        ? lead.getExpectedCloseDate().format(DATE_FMT) : "N/A";
                table.addCell(new Phrase(closeDate, cellFont));
                totalRevenue = totalRevenue.add(val);
            }

            document.add(table);

            // Total revenue
            Font totalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Paragraph total = new Paragraph(
                    "Total Revenue: $" + totalRevenue.toPlainString(), totalFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(15);
            document.add(total);

            document.close();
            return baos.toByteArray();
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating monthly sales report: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateCustomerSummaryReport(java.util.UUID customerId, User currentUser) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        // Access check: ADMIN sees all, MANAGER sees all, SALES_REP sees only assigned
        if (currentUser.getRole() == UserRole.SALES_REP) {
            if (customer.getAssignedTo() == null ||
                    !customer.getAssignedTo().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have access to this customer");
            }
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            addWatermark(writer, currentUser.getName());

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Customer Summary Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Customer profile
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

            document.add(new Paragraph("Customer Profile", sectionFont));
            document.add(new Paragraph("Name: " + customer.getName(), normalFont));
            document.add(new Paragraph("Company: " + (customer.getCompany() != null ? customer.getCompany() : "N/A"), normalFont));
            document.add(new Paragraph("Status: " + (customer.getStatus() != null ? customer.getStatus().name() : "N/A"), normalFont));
            document.add(new Paragraph(" "));

            // Interactions
            document.add(new Paragraph("Recent Interactions", sectionFont));
            Page<Interaction> interactions = interactionRepository.findByCustomerIdOrderByCreatedAtDesc(
                    customerId, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

            if (interactions.isEmpty()) {
                document.add(new Paragraph("No interactions recorded.", normalFont));
            } else {
                PdfPTable intTable = new PdfPTable(3);
                intTable.setWidthPercentage(100);
                intTable.setWidths(new float[]{2, 4, 2});
                intTable.setSpacingBefore(5);

                Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
                BaseColor headerBg = new BaseColor(52, 73, 94);
                addTableHeader(intTable, "Type", headerFont, headerBg);
                addTableHeader(intTable, "Subject", headerFont, headerBg);
                addTableHeader(intTable, "Date", headerFont, headerBg);

                Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
                for (Interaction interaction : interactions.getContent()) {
                    intTable.addCell(new Phrase(interaction.getType().name(), cellFont));
                    intTable.addCell(new Phrase(
                            interaction.getSubject() != null ? interaction.getSubject() : "N/A", cellFont));
                    String date = interaction.getCreatedAt() != null
                            ? interaction.getCreatedAt().format(DATETIME_FMT) : "N/A";
                    intTable.addCell(new Phrase(date, cellFont));
                }
                document.add(intTable);
            }

            document.add(new Paragraph(" "));

            // Leads
            document.add(new Paragraph("Leads", sectionFont));
            Page<Lead> leads = leadRepository.findByOwnerId(
                    currentUser.getId(), PageRequest.of(0, 50));
            // Filter leads by customer
            List<Lead> customerLeads = leads.getContent().stream()
                    .filter(l -> l.getCustomer() != null && l.getCustomer().getId().equals(customerId))
                    .toList();

            if (customerLeads.isEmpty()) {
                document.add(new Paragraph("No leads for this customer.", normalFont));
            } else {
                PdfPTable leadTable = new PdfPTable(3);
                leadTable.setWidthPercentage(100);
                leadTable.setWidths(new float[]{4, 2, 2});
                leadTable.setSpacingBefore(5);

                Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
                BaseColor headerBg = new BaseColor(52, 73, 94);
                addTableHeader(leadTable, "Title", headerFont, headerBg);
                addTableHeader(leadTable, "Stage", headerFont, headerBg);
                addTableHeader(leadTable, "Value", headerFont, headerBg);

                Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
                for (Lead lead : customerLeads) {
                    leadTable.addCell(new Phrase(lead.getTitle(), cellFont));
                    leadTable.addCell(new Phrase(lead.getStage().name(), cellFont));
                    BigDecimal val = lead.getValue() != null ? lead.getValue() : BigDecimal.ZERO;
                    leadTable.addCell(new Phrase("$" + val.toPlainString(), cellFont));
                }
                document.add(leadTable);
            }

            document.close();
            return baos.toByteArray();
        } catch (AccessDeniedException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating customer summary report: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void addWatermark(PdfWriter writer, String userName) {
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter w, Document doc) {
                try {
                    PdfContentByte canvas = w.getDirectContentUnder();
                    Font watermarkFont = new Font(Font.FontFamily.HELVETICA, 36, Font.BOLD,
                            new BaseColor(200, 200, 200, 128));
                    String watermarkText = "Generated by " + userName + " on "
                            + LocalDateTime.now().format(DATETIME_FMT) + " - CONFIDENTIAL";
                    Phrase phrase = new Phrase(watermarkText, watermarkFont);
                    ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, phrase,
                            doc.getPageSize().getWidth() / 2,
                            doc.getPageSize().getHeight() / 2,
                            45);
                } catch (Exception e) {
                    log.warn("Failed to add watermark: {}", e.getMessage());
                }
            }
        });
    }

    private void addTableHeader(PdfPTable table, String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        table.addCell(cell);
    }
}
