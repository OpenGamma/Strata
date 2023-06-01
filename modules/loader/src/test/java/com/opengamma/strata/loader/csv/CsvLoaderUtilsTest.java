/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.result.ParseFailureException;
import com.opengamma.strata.product.etd.EtdOptionType;
import com.opengamma.strata.product.etd.EtdSettlementType;

/**
 * Test {@link CsvLoaderUtils}.
 */
public class CsvLoaderUtilsTest {

  @Test
  public void test_parseEtdSettlementType() {
    assertThat(CsvLoaderUtils.parseEtdSettlementType("C")).isEqualTo(EtdSettlementType.CASH);
    assertThat(CsvLoaderUtils.parseEtdSettlementType("CASH")).isEqualTo(EtdSettlementType.CASH);
    assertThat(CsvLoaderUtils.parseEtdSettlementType("c")).isEqualTo(EtdSettlementType.CASH);
    assertThat(CsvLoaderUtils.parseEtdSettlementType("E")).isEqualTo(EtdSettlementType.PHYSICAL);
    assertThat(CsvLoaderUtils.parseEtdSettlementType("PHYSICAL")).isEqualTo(EtdSettlementType.PHYSICAL);
    assertThat(CsvLoaderUtils.parseEtdSettlementType("e")).isEqualTo(EtdSettlementType.PHYSICAL);
    assertThatExceptionOfType(ParseFailureException.class).isThrownBy(() -> CsvLoaderUtils.parseEtdSettlementType(""));
  }

  @Test
  public void test_parseEtdOptionType() {
    assertThat(CsvLoaderUtils.parseEtdOptionType("A")).isEqualTo(EtdOptionType.AMERICAN);
    assertThat(CsvLoaderUtils.parseEtdOptionType("AMERICAN")).isEqualTo(EtdOptionType.AMERICAN);
    assertThat(CsvLoaderUtils.parseEtdOptionType("a")).isEqualTo(EtdOptionType.AMERICAN);
    assertThat(CsvLoaderUtils.parseEtdOptionType("E")).isEqualTo(EtdOptionType.EUROPEAN);
    assertThat(CsvLoaderUtils.parseEtdOptionType("EUROPEAN")).isEqualTo(EtdOptionType.EUROPEAN);
    assertThat(CsvLoaderUtils.parseEtdOptionType("e")).isEqualTo(EtdOptionType.EUROPEAN);
    assertThat(CsvLoaderUtils.parseEtdOptionType("T")).isEqualTo(EtdOptionType.ASIAN);
    assertThat(CsvLoaderUtils.parseEtdOptionType("ASIAN")).isEqualTo(EtdOptionType.ASIAN);
    assertThat(CsvLoaderUtils.parseEtdOptionType("t")).isEqualTo(EtdOptionType.ASIAN);
    assertThatExceptionOfType(ParseFailureException.class).isThrownBy(() -> CsvLoaderUtils.parseEtdOptionType(""));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseAdjustableDate() {
    ImmutableList<String> headers = ImmutableList.of("DTE", "CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("2019-03-01", "F", "GBLO");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThat(CsvLoaderUtils.parseAdjustableDate(row, "DTE", "CNV", "CAL")).isEqualTo(AdjustableDate.of(
        LocalDate.of(2019, 3, 1),
        BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendarIds.GBLO)));
  }

  @Test
  public void test_parseAdjustableDate_noAdjustment() {
    ImmutableList<String> headers = ImmutableList.of("DTE");
    ImmutableList<String> firstRow = ImmutableList.of("2019-03-01");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThat(CsvLoaderUtils.parseAdjustableDate(row, "DTE", "CNV", "CAL"))
        .isEqualTo(AdjustableDate.of(LocalDate.of(2019, 3, 1), BusinessDayAdjustment.NONE));
  }

  @Test
  public void test_parseAdjustableDate_noAdjustmentCalendar() {
    ImmutableList<String> headers = ImmutableList.of("DTE", "CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("2019-03-01", "F", "");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThat(CsvLoaderUtils.parseAdjustableDate(row, "DTE", "CNV", "CAL"))
        .isEqualTo(AdjustableDate.of(LocalDate.of(2019, 3, 1), BusinessDayAdjustment.NONE));
  }

  @Test
  public void test_parseAdjustableDate_defaulting() {
    ImmutableList<String> headers = ImmutableList.of("DTE", "CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("2019-03-01", "F", "GBLO");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThat(CsvLoaderUtils.parseAdjustableDate(row, "DTE", "CNV", "CAL", FOLLOWING, Currency.EUR)).isEqualTo(AdjustableDate.of(
        LocalDate.of(2019, 3, 1),
        BusinessDayAdjustment.of(FOLLOWING, HolidayCalendarIds.GBLO)));
  }

  @Test
  public void test_parseAdjustableDate_defaulting_noAdjustment() {
    ImmutableList<String> headers = ImmutableList.of("DTE");
    ImmutableList<String> firstRow = ImmutableList.of("2019-03-01");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThat(CsvLoaderUtils.parseAdjustableDate(row, "DTE", "CNV", "CAL", FOLLOWING, Currency.EUR))
        .isEqualTo(AdjustableDate.of(LocalDate.of(2019, 3, 1), BusinessDayAdjustment.of(FOLLOWING, EUTA)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseBusinessDayAdjustment() {
    ImmutableList<String> headers = ImmutableList.of("CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("F", "GBLO");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThat(CsvLoaderUtils.parseBusinessDayAdjustment(row, "CNV", "CAL"))
        .isEqualTo(Optional.of(BusinessDayAdjustment.of(FOLLOWING, HolidayCalendarIds.GBLO)));
  }

  @Test
  public void test_parseBusinessDayAdjustment_none() {
    ImmutableList<String> headers = ImmutableList.of("CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("NONE", "GBLO");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThat(CsvLoaderUtils.parseBusinessDayAdjustment(row, "CNV", "CAL")).isEqualTo(Optional.of(BusinessDayAdjustment.NONE));
  }

  @Test
  public void test_parseBusinessDayAdjustment_notFound() {
    ImmutableList<String> headers = ImmutableList.of("CNV", "CAL");
    ImmutableList<String> firstRow = ImmutableList.of("F", "GBLO");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThat(CsvLoaderUtils.parseBusinessDayAdjustment(row, "CNV", "CAX")).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseCurrencyAmount() {
    ImmutableList<String> headers = ImmutableList.of("CCY", "AMT");
    ImmutableList<String> firstRow = ImmutableList.of("GBP", "123.4");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThat(CsvLoaderUtils.parseCurrencyAmount(row, "CCY", "AMT")).isEqualTo(CurrencyAmount.of(Currency.GBP, 123.4));
  }

  @Test
  public void test_parseCurrencyAmount_notFound() {
    ImmutableList<String> headers = ImmutableList.of("CCY", "AMT");
    ImmutableList<String> firstRow = ImmutableList.of("GBP", "123.4");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CsvLoaderUtils.parseCurrencyAmount(row, "CCY", "AMX"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseCurrencyAmountWithDirection() {
    ImmutableList<String> headers = ImmutableList.of("CCY", "AMT", "DIR");
    ImmutableList<String> firstRow = ImmutableList.of("GBP", "123.4", "Pay");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThat(CsvLoaderUtils.parseCurrencyAmountWithDirection(row, "CCY", "AMT", "DIR"))
        .isEqualTo(CurrencyAmount.of(Currency.GBP, -123.4));
  }

  @Test
  public void test_parseCurrencyAmountWithDirection_notFound() {
    ImmutableList<String> headers = ImmutableList.of("CCY", "AMT", "DIR");
    ImmutableList<String> firstRow = ImmutableList.of("GBP", "123.4", "Pay");
    CsvRow row = CsvFile.of(headers, ImmutableList.of(firstRow)).row(0);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CsvLoaderUtils.parseCurrencyAmountWithDirection(row, "CCX", "AMT", "DIR"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_formattedDouble() {
    assertThat(CsvLoaderUtils.formattedDouble(123.45d)).isEqualTo("123.45");
    assertThat(CsvLoaderUtils.formattedDouble(0.7d)).isEqualTo("0.7");
    assertThat(CsvLoaderUtils.formattedDouble(0.08d)).isEqualTo("0.08");
    assertThat(CsvLoaderUtils.formattedDouble(789d)).isEqualTo("789");
  }

  @Test
  public void test_formattedPercentage() {
    assertThat(CsvLoaderUtils.formattedPercentage(1.2345d)).isEqualTo("123.45");
    assertThat(CsvLoaderUtils.formattedPercentage(0.007d)).isEqualTo("0.7");
    assertThat(CsvLoaderUtils.formattedPercentage(0.08d)).isEqualTo("8");
    assertThat(CsvLoaderUtils.formattedPercentage(7.89d)).isEqualTo("789");
  }

}
