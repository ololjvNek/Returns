package pl.returns.ledger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pl.returns.common.ApiException;
import pl.returns.returnsapp.LedgerPageDto;
import pl.returns.returnsapp.LedgerRowDto;

@Service
public class XlsxExportService {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	public byte[] export(LedgerPageDto page) {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet sheet = workbook.createSheet("Ewidencja");
			CellStyle title = workbook.createCellStyle();
			Font titleFont = workbook.createFont();
			titleFont.setBold(true);
			titleFont.setFontHeightInPoints((short) 16);
			title.setFont(titleFont);
			title.setAlignment(HorizontalAlignment.CENTER);

			CellStyle header = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			header.setFont(headerFont);
			header.setWrapText(true);
			header.setAlignment(HorizontalAlignment.CENTER);
			header.setVerticalAlignment(VerticalAlignment.CENTER);
			header.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
			header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			border(header);

			CellStyle text = workbook.createCellStyle();
			border(text);
			text.setVerticalAlignment(VerticalAlignment.CENTER);

			CellStyle date = workbook.createCellStyle();
			border(date);
			date.setAlignment(HorizontalAlignment.CENTER);

			CellStyle money = workbook.createCellStyle();
			border(money);
			money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

			CellStyle total = workbook.createCellStyle();
			Font totalFont = workbook.createFont();
			totalFont.setBold(true);
			total.setFont(totalFont);
			border(total);
			total.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

			Row titleRow = sheet.createRow(0);
			titleRow.setHeightInPoints(24);
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellValue(page.monthTitle());
			titleCell.setCellStyle(title);
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

			Row group = sheet.createRow(1);
			group.setHeightInPoints(22);
			String[] top = {
					"Lp.",
					"Nr Apilo",
					"Data sprzedaży",
					"Dane kupującego",
					"Nazwa towaru/usług i/kod produktu",
					"Data zwrotu",
					"Zwrot należności za towar/usługę",
					"",
					"Uwagi"
			};
			for (int i = 0; i < top.length; i++) {
				Cell cell = group.createCell(i);
				cell.setCellValue(top[i]);
				cell.setCellStyle(header);
			}
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 0));
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 1, 1));
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 2, 2));
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 3, 3));
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 4, 4));
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 5, 5));
			sheet.addMergedRegion(new CellRangeAddress(1, 1, 6, 7));
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 8, 8));

			Row sub = sheet.createRow(2);
			sub.setHeightInPoints(36);
			for (int i = 0; i < 9; i++) {
				Cell cell = sub.createCell(i);
				cell.setCellStyle(header);
			}
			sub.getCell(6).setCellValue("Wartość brutto towaru/usługi lub zwracana wartość brutto");
			sub.getCell(7).setCellValue("Podatek VAT należny");

			int rowIdx = 3;
			int lp = 1;
			for (LedgerRowDto item : page.rows()) {
				Row row = sheet.createRow(rowIdx++);
				row.createCell(0).setCellValue(lp++);
				row.getCell(0).setCellStyle(text);
				setText(row, 1, item.apiloOrderNumber(), text);
				setText(row, 2, item.saleDate() == null ? "" : DATE.format(item.saleDate()), date);
				setText(row, 3, item.buyerName(), text);
				setText(row, 4, item.productCodes(), text);
				setText(row, 5, item.returnDate() == null ? "" : DATE.format(item.returnDate()), date);
				Cell gross = row.createCell(6);
				if (item.grossAmount() != null) {
					gross.setCellValue(item.grossAmount().doubleValue());
				}
				gross.setCellStyle(money);
				Cell vat = row.createCell(7);
				if (item.vatAmount() != null) {
					vat.setCellValue(item.vatAmount().doubleValue());
				}
				vat.setCellStyle(money);
				setText(row, 8, item.notes(), text);
			}

			Row sum = sheet.createRow(rowIdx);
			Cell razem = sum.createCell(5);
			razem.setCellValue("Razem");
			razem.setCellStyle(total);
			Cell grossTotal = sum.createCell(6);
			grossTotal.setCellFormula("SUM(G4:G" + rowIdx + ")");
			grossTotal.setCellStyle(total);
			Cell vatTotal = sum.createCell(7);
			vatTotal.setCellFormula("SUM(H4:H" + rowIdx + ")");
			vatTotal.setCellStyle(total);
			for (int i = 0; i < 9; i++) {
				if (sum.getCell(i) == null) {
					sum.createCell(i).setCellStyle(text);
				}
			}

			int[] widths = {8, 16, 16, 24, 36, 16, 22, 16, 24};
			for (int i = 0; i < widths.length; i++) {
				sheet.setColumnWidth(i, widths[i] * 256);
			}
			workbook.write(out);
			return out.toByteArray();
		} catch (IOException ex) {
			throw ApiException.serviceUnavailable("Nie udało się wygenerować pliku XLSX");
		}
	}

	private static void setText(Row row, int col, String value, CellStyle style) {
		Cell cell = row.createCell(col);
		cell.setCellValue(value == null ? "" : value);
		cell.setCellStyle(style);
	}

	private static void border(CellStyle style) {
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
	}
}
