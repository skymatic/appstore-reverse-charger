package de.skymatic.appstore_invoices.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.YearMonth;

public class AppleInvoiceTest {

	private static final YearMonth dummyYearMonth = YearMonth.of(1999,1);
	private static final LocalDate dummyIssueDate = LocalDate.now();

	@Test
	public void testEmptyInvoiceReturnsZero() {
		SalesEntry s = Mockito.mock(SalesEntry.class);
		Mockito.when(s.getRpc()).thenReturn(RegionPlusCurrency.AMERICAS_USD);
		AppleInvoice i = new AppleInvoice("0",dummyYearMonth, dummyIssueDate, s);
		Assertions.assertEquals(0, i.sum());
	}

	@Test
	public void testAfterCreationSubsidiaryMatchesFirstAddedSalesEntry() {
		//RegionPlusCurrency.JAPAN_JPY will be mapped to Subsidiary.JAPAN, see AppleUtility class
		final AppleSubsidiary expectedAppleSubsidiary = AppleSubsidiary.JAPAN;
		final RegionPlusCurrency expectedRPC = RegionPlusCurrency.JAPAN_JPY;
		SalesEntry s = Mockito.mock(SalesEntry.class);
		Mockito.when(s.getRpc()).thenReturn(expectedRPC);
		AppleInvoice i = new AppleInvoice("0", dummyYearMonth, dummyIssueDate, s);
		Assertions.assertEquals(expectedAppleSubsidiary, i.getAppleSubsidiary());
	}

	@Test
	public void testSumFunctionCalculatesSum() {
		final double expectedSum = 100.0;
		SalesEntry s1 = Mockito.mock(SalesEntry.class);
		Mockito.when(s1.getRpc()).thenReturn(RegionPlusCurrency.AUSTRALIA_AUD);
		Mockito.when(s1.getProceeds()).thenReturn((50.0));
		SalesEntry s2 = Mockito.mock(SalesEntry.class);
		Mockito.when(s2.getRpc()).thenReturn(RegionPlusCurrency.NEW_ZEALAND_NZD);
		Mockito.when(s2.getProceeds()).thenReturn((50.0));
		AppleInvoice i = new AppleInvoice("0", dummyYearMonth, dummyIssueDate, s1);
		i.addSales(s2);
		Assertions.assertEquals(expectedSum, i.sum());
	}

	@Test
	public void testAmountFunctionCalculatesAmount() {
		final int expectedAmount = 100;
		SalesEntry s1 = Mockito.mock(SalesEntry.class);
		Mockito.when(s1.getRpc()).thenReturn(RegionPlusCurrency.AUSTRALIA_AUD);
		Mockito.when(s1.getUnitsSold()).thenReturn((50));
		SalesEntry s2 = Mockito.mock(SalesEntry.class);
		Mockito.when(s2.getRpc()).thenReturn(RegionPlusCurrency.NEW_ZEALAND_NZD);
		Mockito.when(s2.getUnitsSold()).thenReturn((50));
		AppleInvoice i = new AppleInvoice("0", dummyYearMonth, dummyIssueDate, s1);
		i.addSales(s2);
		Assertions.assertEquals(expectedAmount, i.getAmount());
	}

	@Test
	public void testAlreadyExistingRPCThrowsException() {
		SalesEntry s1 = Mockito.mock(SalesEntry.class);
		Mockito.when(s1.getRpc()).thenReturn(RegionPlusCurrency.AUSTRALIA_AUD);
		AppleInvoice i = new AppleInvoice("0", dummyYearMonth, dummyIssueDate, s1);
		Assertions.assertThrows(IllegalArgumentException.class, () -> i.addSales(s1));
	}

	@Test
	public void testAddingNonMatchingSubsidiaryToInvoiceThrowsException() {
		SalesEntry s1 = Mockito.mock(SalesEntry.class);
		Mockito.when(s1.getRpc()).thenReturn(RegionPlusCurrency.AUSTRALIA_AUD);
		SalesEntry s2 = Mockito.mock(SalesEntry.class);
		Mockito.when(s2.getRpc()).thenReturn(RegionPlusCurrency.JAPAN_JPY);
		AppleInvoice i = new AppleInvoice("0", dummyYearMonth, dummyIssueDate, s1);
		Assertions.assertThrows(IllegalArgumentException.class, () -> i.addSales(s2));
	}
}
