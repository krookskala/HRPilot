package com.hrpilot.backend.payroll;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayslipService {

    public byte[] createPayslipPdf(PayrollRecord payrollRecord, List<PayrollComponent> components) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                float y = 780;

                y = writeLine(content, font, 16, 50, y, "HRPilot Payslip");
                y = writeLine(content, font, 12, 50, y - 12, payrollRecord.getEmployee().getFirstName() + " " + payrollRecord.getEmployee().getLastName());
                y = writeLine(content, font, 12, 50, y - 18, "Period: " + payrollRecord.getMonth() + "/" + payrollRecord.getYear());
                y = writeLine(content, font, 12, 50, y - 18, "Status: " + payrollRecord.getStatus());
                y = writeLine(content, font, 12, 50, y - 24, "Tax class: " + payrollRecord.getTaxClass());
                y = writeLine(content, font, 12, 50, y - 18, "Gross salary: EUR " + payrollRecord.getGrossSalary());
                y = writeLine(content, font, 12, 50, y - 18, "Net salary: EUR " + payrollRecord.getNetSalary());
                y = writeLine(content, font, 14, 50, y - 28, "Components");

                for (PayrollComponent component : components) {
                    y = writeLine(content, font, 11, 60, y - 16, component.getLabel() + ": EUR " + component.getAmount());
                    if (y < 80) {
                        break;
                    }
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate payslip PDF", e);
        }
    }

    private float writeLine(PDPageContentStream content, PDType1Font font, int fontSize, float x, float y, String text)
        throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y;
    }
}
