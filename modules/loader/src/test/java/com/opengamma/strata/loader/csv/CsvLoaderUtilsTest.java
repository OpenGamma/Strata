/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.etd.EtdOptionType;
import com.opengamma.strata.product.etd.EtdSettlementType;

/**
 * Test {@link CsvLoaderUtils}.
 */
@Test
public class CsvLoaderUtilsTest {

  public void test_parseEtdSettlementType() {
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("C"), EtdSettlementType.CASH);
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("CASH"), EtdSettlementType.CASH);
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("c"), EtdSettlementType.CASH);
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("E"), EtdSettlementType.PHYSICAL);
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("PHYSICAL"), EtdSettlementType.PHYSICAL);
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("e"), EtdSettlementType.PHYSICAL);
    assertThatIllegalArgumentException().isThrownBy(() -> CsvLoaderUtils.parseEtdSettlementType(""));
  }

  public void test_parseEtdOptionType() {
    assertEquals(CsvLoaderUtils.parseEtdOptionType("A"), EtdOptionType.AMERICAN);
    assertEquals(CsvLoaderUtils.parseEtdOptionType("AMERICAN"), EtdOptionType.AMERICAN);
    assertEquals(CsvLoaderUtils.parseEtdOptionType("a"), EtdOptionType.AMERICAN);
    assertEquals(CsvLoaderUtils.parseEtdOptionType("E"), EtdOptionType.EUROPEAN);
    assertEquals(CsvLoaderUtils.parseEtdOptionType("EUROPEAN"), EtdOptionType.EUROPEAN);
    assertEquals(CsvLoaderUtils.parseEtdOptionType("e"), EtdOptionType.EUROPEAN);
    assertThatIllegalArgumentException().isThrownBy(() -> CsvLoaderUtils.parseEtdOptionType(""));
  }

  //-------------------------------------------------------------------------
  public void test_parseAdjustableDate() {
    ImmutableList<String> headers = ImmutableList.of("DTE", "CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("2019-03-01", "F", "GBLO");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertEquals(
        CsvLoaderUtils.parseAdjustableDate(row, "DTE", "CNV", "CAL"),
        AdjustableDate.of(
            LocalDate.of(2019, 3, 1),
            BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendarIds.GBLO)));
  }

  public void test_parseAdjustableDate_noAdjustment() {
    ImmutableList<String> headers = ImmutableList.of("DTE");
    ImmutableList<String> firstRow = ImmutableList.of("2019-03-01");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertEquals(
        CsvLoaderUtils.parseAdjustableDate(row, "DTE", "CNV", "CAL"),
        AdjustableDate.of(LocalDate.of(2019, 3, 1), BusinessDayAdjustment.NONE));
  }

  public void test_parseAdjustableDate_noAdjustmentCalendar() {
    ImmutableList<String> headers = ImmutableList.of("DTE", "CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("2019-03-01", "F", "");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertEquals(
        CsvLoaderUtils.parseAdjustableDate(row, "DTE", "CNV", "CAL"),
        AdjustableDate.of(LocalDate.of(2019, 3, 1), BusinessDayAdjustment.NONE));
  }

  public void test_parseAdjustableDate_defaulting() {
    ImmutableList<String> headers = ImmutableList.of("DTE", "CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("2019-03-01", "F", "GBLO");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertEquals(
        CsvLoaderUtils.parseAdjustableDate(row, "DTE", "CNV", "CAL", FOLLOWING, Currency.EUR),
        AdjustableDate.of(
            LocalDate.of(2019, 3, 1),
            BusinessDayAdjustment.of(FOLLOWING, HolidayCalendarIds.GBLO)));
  }

  public void test_parseAdjustableDate_defaulting_noAdjustment() {
    ImmutableList<String> headers = ImmutableList.of("DTE");
    ImmutableList<String> firstRow = ImmutableList.of("2019-03-01");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertEquals(
        CsvLoaderUtils.parseAdjustableDate(row, "DTE", "CNV", "CAL", FOLLOWING, Currency.EUR),
        AdjustableDate.of(LocalDate.of(2019, 3, 1), BusinessDayAdjustment.of(FOLLOWING, EUTA)));
  }

  //-------------------------------------------------------------------------
  public void test_parseBusinessDayAdjustment() {
    ImmutableList<String> headers = ImmutableList.of("CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("F", "GBLO");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertEquals(
        CsvLoaderUtils.parseBusinessDayAdjustment(row, "CNV", "CAL"),
        Optional.of(BusinessDayAdjustment.of(FOLLOWING, HolidayCalendarIds.GBLO)));
  }

  public void test_parseBusinessDayAdjustment_none() {
    ImmutableList<String> headers = ImmutableList.of("CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("NONE", "GBLO");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertEquals(
        CsvLoaderUtils.parseBusinessDayAdjustment(row, "CNV", "CAL"),
        Optional.of(BusinessDayAdjustment.NONE));
  }

  public void test_parseBusinessDayAdjustment_notFound() {
    ImmutableList<String> headers = ImmutableList.of("CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("F", "GBLO");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertEquals(CsvLoaderUtils.parseBusinessDayAdjustment(row, "CNV", "CAX"), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void test_parseCurrencyAmount() {
    ImmutableList<String> headers = ImmutableList.of("CCY", "AMT");
    ImmutableList<String> firstRow = ImmutableList.of("GBP", "123.4");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertEquals(CsvLoaderUtils.parseCurrencyAmount(row, "CCY", "AMT"), CurrencyAmount.of(Currency.GBP, 123.4));
  }

  public void test_parseCurrencyAmount_notFound() {
    ImmutableList<String> headers = ImmutableList.of("CCY", "AMT");
    ImmutableList<String> firstRow = ImmutableList.of("GBP", "123.4");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThatIllegalArgumentException().isThrownBy(() -> CsvLoaderUtils.parseCurrencyAmount(row, "CCY", "AMX"));
  }

  //-------------------------------------------------------------------------
  public void test_parseCurrencyAmountWithDirection() {
    ImmutableList<String> headers = ImmutableList.of("CCY", "AMT", "DIR");
    ImmutableList<String> firstRow = ImmutableList.of("GBP", "123.4", "Pay");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertEquals(CsvLoaderUtils.parseCurrencyAmountWithDirection(row, "CCY", "AMT", "DIR"),
        CurrencyAmount.of(Currency.GBP, -123.4));
  }

  public void test_parseCurrencyAmountWithDirection_notFound() {
    ImmutableList<String> headers = ImmutableList.of("CCY", "AMT", "DIR");
    ImmutableList<String> firstRow = ImmutableList.of("GBP", "123.4", "Pay");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CsvLoaderUtils.parseCurrencyAmountWithDirection(row, "CCX", "AMT", "DIR"));
  }

  //-------------------------------------------------------------------------
  public void test_formattedDouble() {
    assertEquals(CsvLoaderUtils.formattedDouble(123.45d), "123.45");
    assertEquals(CsvLoaderUtils.formattedDouble(0.7d), "0.7");
    assertEquals(CsvLoaderUtils.formattedDouble(0.08d), "0.08");
    assertEquals(CsvLoaderUtils.formattedDouble(789d), "789");
  }

  public void test_formattedPercentage() {
    assertEquals(CsvLoaderUtils.formattedPercentage(1.2345d), "123.45");
    assertEquals(CsvLoaderUtils.formattedPercentage(0.007d), "0.7");
    assertEquals(CsvLoaderUtils.formattedPercentage(0.08d), "8");
    assertEquals(CsvLoaderUtils.formattedPercentage(7.89d), "789");
  }

}
