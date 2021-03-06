package de.skymatic.appstore_invoices.parser;

import de.skymatic.appstore_invoices.model.apple.AppleReport;
import de.skymatic.appstore_invoices.model.apple.AppleSubsidiaryReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Month;

/**
 * Unit tests for {@link AppleParser}.
 *
 * TODO: refactor them to fit to the Invoice interface
 */
public class AppleParserTest {

	/**
	 * TODO: currently this cannot be achieved.
	 *
	 * @param tmpDir
	 * @throws IOException
	 * @throws ReportParseException
	 */
	@Test
	@Disabled
	public void testParseResultContainsAllSalesEntry(@TempDir Path tmpDir) throws IOException, ReportParseException {
		int expectedAmountOfSales = 2;
		Path reportFilePath = Files.createFile(tmpDir.resolve("tmpReport"));
		String report = "\"iTunes Connect - Payments and Financial Reports\t(September, 2014)\",,,,,,,,,,,,\n" +
				"\"Australia (AUD)\",\"29\",\"33.15\",\"33.15\",\"0\",\"0\",\"0\",\"33.15\",\"0.80030\",\"26.53\",\"EUR\",,\n" +
				"\"New-Zealand (NZD)\",\"19\",\"206.89\",\"206.89\",\"0\",\"0\",\"0\",\"206.89\",\"1.00000\",\"206.89\",\"EUR\",,\n";
		Files.writeString(reportFilePath, report);
		AppleParser parser = new AppleParser();
		AppleReport result = parser.parse(reportFilePath);
		Assertions.assertEquals(expectedAmountOfSales, result.getSubReports().size());
	}

	@Test
	public void testParseResultContainsSum(@TempDir Path tmpDir) throws IOException, ReportParseException {
		BigDecimal ausProceeds = BigDecimal.valueOf(26.53);
		BigDecimal newzeaProceeds = BigDecimal.valueOf(206.89);
		Path reportFilePath = Files.createFile(tmpDir.resolve("tmpReport"));
		String report = "\"iTunes Connect - Payments and Financial Reports\t(September, 2014)\",,,,,,,,,,,,\n" +
				"\"Australia (AUD)\",\"29\",\"33.15\",\"33.15\",\"0\",\"0\",\"0\",\"33.15\",\"0.80030\",\""+ausProceeds+"\",\"EUR\",,\n" +
				"\"New-Zealand (NZD)\",\"19\",\"206.89\",\"206.89\",\"0\",\"0\",\"0\",\"206.89\",\"1.00000\",\""+newzeaProceeds +"\",\"EUR\",,\n";
		Files.writeString(reportFilePath, report);
		AppleParser parser = new AppleParser();
		AppleReport result = parser.parse(reportFilePath);
		Assertions.assertEquals(ausProceeds.add(newzeaProceeds), result.getSubReports().stream().map(AppleSubsidiaryReport::getProceeds).reduce(BigDecimal.ZERO,BigDecimal::add, BigDecimal::add));
	}

	@Test
	public void testParseResultContainsYearAndMonth(@TempDir Path tmpDir) throws IOException, ReportParseException {
		int expectedYear = 2014;
		Month expectedMonth = Month.SEPTEMBER;
		Path reportFilePath = Files.createFile(tmpDir.resolve("tmpReport"));
		String report = "\"iTunes Connect - Payments and Financial Reports\t(" + expectedMonth.name() + ", " + expectedYear + ")\",,,,,,,,,,,,\n";
		Files.writeString(reportFilePath, report);
		AppleParser parser = new AppleParser();
		AppleReport result = parser.parse(reportFilePath);
		Assertions.assertEquals(expectedYear, result.getBillingMonth().getYear());
		Assertions.assertEquals(expectedMonth, result.getBillingMonth().getMonth());

	}

	@Test
	public void testParserThrowsParseExceptionIfEntryNotNumber(@TempDir Path tmpDir) throws IOException {
		Path reportFilePath = Files.createFile(tmpDir.resolve("tmpReport"));
		String report = "\"iTunes Connect - Payments and Financial Reports\t(September, 2014)\",,,,,,,,,,,,\n" +
				"\"Japan (JPY)\",\"2\",\"179\",\"179\",\"0\",\"0\",\"-37\",\"142\",\"0.00817\",\"abcde\",\"EUR\",,\n";
		Files.writeString(reportFilePath, report);
		AppleParser parser = new AppleParser();
		Assertions.assertThrows(ReportParseException.class, () -> parser.parse(reportFilePath));
	}

	@Test
	public void testParserThrowsParseExceptionIfEntryTooShort(@TempDir Path tmpDir) throws IOException {
		Path reportFilePath = Files.createFile(tmpDir.resolve("tmpReport"));
		String report = "\"iTunes Connect - Payments and Financial Reports\t(September, 2014)\",,,,,,,,,,,,\n" +
				"\"Switzerland (CHF)\",\"29\",\"33.15\",\"33.15\",\"0\",\"0\",\"33.15\",\"0.80030\",\"26.53\",\"EUR\",,\n";
		Files.writeString(reportFilePath, report);
		AppleParser parser = new AppleParser();
		Assertions.assertThrows(ReportParseException.class, () -> parser.parse(reportFilePath));
	}

	@Test
	public void testParserThrowsParseExceptionIfRPCUnknown(@TempDir Path tmpDir) throws IOException {
		Path reportFilePath = Files.createFile(tmpDir.resolve("tmpReport"));
		String report = "\"iTunes Connect - Payments and Financial Reports\t(September, 2014)\",,,,,,,,,,,,\n" +
				"\"Moon (MNY)\",\"29\",\"33.15\",\"33.15\",\"0\",\"0\",\"0\",\"33.15\",\"0.80030\",\"26.53\",\"EUR\",,\n";
		Files.writeString(reportFilePath, report);
		AppleParser parser = new AppleParser();
		Assertions.assertThrows(ReportParseException.class, () -> parser.parse(reportFilePath));
	}
}
