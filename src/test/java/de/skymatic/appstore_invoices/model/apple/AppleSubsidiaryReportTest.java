package de.skymatic.appstore_invoices.model.apple;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.YearMonth;

public class AppleSubsidiaryReportTest {

	private static final YearMonth dummyYearMonth = YearMonth.of(1999,1);
	private static final LocalDate dummyIssueDate = LocalDate.now();

	@Test
	public void testEmptyInvoiceReturnsZero() {
		AppleSalesEntry s = Mockito.mock(AppleSalesEntry.class);
		Mockito.when(s.getRpc()).thenReturn(RegionPlusCurrency.AMERICAS_USD);
		AppleSubsidiaryReport i = new AppleSubsidiaryReport("0",dummyYearMonth, dummyIssueDate, s);
		Assertions.assertEquals(0, i.sum());
	}

	@Test
	public void testAfterCreationSubsidiaryMatchesFirstAddedSalesEntry() {
		//RegionPlusCurrency.JAPAN_JPY will be mapped to Subsidiary.JAPAN, see AppleUtility class
		final AppleSubsidiary expectedAppleSubsidiary = AppleSubsidiary.JAPAN;
		final RegionPlusCurrency expectedRPC = RegionPlusCurrency.JAPAN_JPY;
		AppleSalesEntry s = Mockito.mock(AppleSalesEntry.class);
		Mockito.when(s.getRpc()).thenReturn(expectedRPC);
		AppleSubsidiaryReport i = new AppleSubsidiaryReport("0", dummyYearMonth, dummyIssueDate, s);
		Assertions.assertEquals(expectedAppleSubsidiary, i.getAppleSubsidiary());
	}

	@Test
	public void testSumFunctionCalculatesSum() {
		final double expectedSum = 100.0;
		AppleSalesEntry s1 = Mockito.mock(AppleSalesEntry.class);
		Mockito.when(s1.getRpc()).thenReturn(RegionPlusCurrency.AUSTRALIA_AUD);
		Mockito.when(s1.getProceeds()).thenReturn((50.0));
		AppleSalesEntry s2 = Mockito.mock(AppleSalesEntry.class);
		Mockito.when(s2.getRpc()).thenReturn(RegionPlusCurrency.NEW_ZEALAND_NZD);
		Mockito.when(s2.getProceeds()).thenReturn((50.0));
		AppleSubsidiaryReport i = new AppleSubsidiaryReport("0", dummyYearMonth, dummyIssueDate, s1);
		i.addSales(s2);
		Assertions.assertEquals(expectedSum, i.sum());
	}

	@Test
	public void testAmountFunctionCalculatesAmount() {
		final int expectedAmount = 100;
		AppleSalesEntry s1 = Mockito.mock(AppleSalesEntry.class);
		Mockito.when(s1.getRpc()).thenReturn(RegionPlusCurrency.AUSTRALIA_AUD);
		Mockito.when(s1.getUnitsSold()).thenReturn((50));
		AppleSalesEntry s2 = Mockito.mock(AppleSalesEntry.class);
		Mockito.when(s2.getRpc()).thenReturn(RegionPlusCurrency.NEW_ZEALAND_NZD);
		Mockito.when(s2.getUnitsSold()).thenReturn((50));
		AppleSubsidiaryReport i = new AppleSubsidiaryReport("0", dummyYearMonth, dummyIssueDate, s1);
		i.addSales(s2);
		Assertions.assertEquals(expectedAmount, i.getAmount());
	}

	@Test
	public void testAlreadyExistingRPCThrowsException() {
		AppleSalesEntry s1 = Mockito.mock(AppleSalesEntry.class);
		Mockito.when(s1.getRpc()).thenReturn(RegionPlusCurrency.AUSTRALIA_AUD);
		AppleSubsidiaryReport i = new AppleSubsidiaryReport("0", dummyYearMonth, dummyIssueDate, s1);
		Assertions.assertThrows(IllegalArgumentException.class, () -> i.addSales(s1));
	}

	@Test
	public void testAddingNonMatchingSubsidiaryToInvoiceThrowsException() {
		AppleSalesEntry s1 = Mockito.mock(AppleSalesEntry.class);
		Mockito.when(s1.getRpc()).thenReturn(RegionPlusCurrency.AUSTRALIA_AUD);
		AppleSalesEntry s2 = Mockito.mock(AppleSalesEntry.class);
		Mockito.when(s2.getRpc()).thenReturn(RegionPlusCurrency.JAPAN_JPY);
		AppleSubsidiaryReport i = new AppleSubsidiaryReport("0", dummyYearMonth, dummyIssueDate, s1);
		Assertions.assertThrows(IllegalArgumentException.class, () -> i.addSales(s2));
	}
}