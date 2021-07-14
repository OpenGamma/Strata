/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.StandardSchemes;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.MarketTenor;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link LoaderUtils}.
 */
public class LoaderUtilsTest {

  @Test
  public void test_findIndex() {
    assertThat(LoaderUtils.findIndex("GBP-LIBOR-3M")).isEqualTo(IborIndices.GBP_LIBOR_3M);
    assertThat(LoaderUtils.findIndex("GBP-SONIA")).isEqualTo(OvernightIndices.GBP_SONIA);
    assertThat(LoaderUtils.findIndex("GB-RPI")).isEqualTo(PriceIndices.GB_RPI);
    assertThat(LoaderUtils.findIndex("GBP/USD-WM")).isEqualTo(FxIndices.GBP_USD_WM);
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.findIndex("Rubbish"));
  }

  @Test
  public void test_parseBoolean() {
    assertThat(LoaderUtils.parseBoolean("TRUE")).isTrue();
    assertThat(LoaderUtils.parseBoolean("True")).isTrue();
    assertThat(LoaderUtils.parseBoolean("true")).isTrue();
    assertThat(LoaderUtils.parseBoolean("t")).isTrue();
    assertThat(LoaderUtils.parseBoolean("yes")).isTrue();
    assertThat(LoaderUtils.parseBoolean("y")).isTrue();
    assertThat(LoaderUtils.parseBoolean("FALSE")).isFalse();
    assertThat(LoaderUtils.parseBoolean("False")).isFalse();
    assertThat(LoaderUtils.parseBoolean("false")).isFalse();
    assertThat(LoaderUtils.parseBoolean("f")).isFalse();
    assertThat(LoaderUtils.parseBoolean("no")).isFalse();
    assertThat(LoaderUtils.parseBoolean("n")).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseBoolean("Rubbish"));
  }

  @Test
  public void test_parseInteger() {
    assertThat(LoaderUtils.parseInteger("2")).isEqualTo(2);
    assertThat(LoaderUtils.parseInteger("1,234,000")).isEqualTo(1_234_000);
    assertThat(LoaderUtils.parseInteger("(2)")).isEqualTo(-2);
    assertThat(LoaderUtils.parseInteger("(23)")).isEqualTo(-23);
    assertThat(LoaderUtils.parseInteger("(12,345,000)")).isEqualTo(-12_345_000);
    assertThat(LoaderUtils.parseInteger("12,345,6,7,8")).isEqualTo(12_345_678);
    assertThat(LoaderUtils.parseInteger("(12,345,6,7,8)")).isEqualTo(-12_345_678);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseInteger("12,,000"))
        .withMessage("Unable to parse integer from '12,,000'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseInteger("12,000,"))
        .withMessage("Unable to parse integer from '12,000,'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseInteger("(12,120,)"))
        .withMessage("Unable to parse integer from '(12,120,)'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseInteger("()"))
        .withMessage("Unable to parse integer from '()'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseInteger("(2)3)"))
        .withMessage("Unable to parse integer from '(2)3)'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseInteger("Rubbish"))
        .withMessage("Unable to parse integer from 'Rubbish'");
  }

  @Test
  public void test_parseDouble() {
    assertThat(LoaderUtils.parseDouble("1.2")).isEqualTo(1.2d, within(1e-10));
    assertThat(LoaderUtils.parseDouble("(1.2)")).isEqualTo(-1.2d, within(1e-10));
    assertThat(LoaderUtils.parseDouble("1,234,567.2")).isEqualTo(1_234_567.2d, within(1e-10));
    assertThat(LoaderUtils.parseDouble("(1,234,567.2)")).isEqualTo(-1_234_567.2d, within(1e-10));
    assertThat(LoaderUtils.parseDouble("1,234,5,6,7.2")).isEqualTo(1_234_567.2d, within(1e-10));
    assertThat(LoaderUtils.parseDouble("(1,234,5,6,7.2)")).isEqualTo(-1_234_567.2d, within(1e-10));
    assertThat(LoaderUtils.parseDouble("1,234.")).isEqualTo(1_234.0d, within(1e-10));
    assertThat(LoaderUtils.parseDouble("(1,234.)")).isEqualTo(-1_234.0d, within(1e-10));
    assertThat(LoaderUtils.parseDouble(".123")).isEqualTo(0.123d, within(1e-10));
    assertThat(LoaderUtils.parseDouble("(.123)")).isEqualTo(-0.123d, within(1e-10));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseDouble("12,,000.2"))
        .withMessage("Unable to parse double from '12,,000.2'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseDouble("12,000.2,"))
        .withMessage("Unable to parse double from '12,000.2,'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseDouble("12.345.678,12"))
        .withMessage("Unable to parse double from '12.345.678,12'"); // European formats are not supported
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseDouble("()"))
        .withMessage("Unable to parse double from '()'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseDouble("(1.2)3)"))
        .withMessage("Unable to parse double from '(1.2)3)'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseDouble("Rubbish"))
        .withMessage("Unable to parse double from 'Rubbish'");
  }

  @Test
  public void test_parseDoublePercent() {
    assertThat(LoaderUtils.parseDoublePercent("1.2")).isEqualTo(0.012d, within(1e-10));
    assertThat(LoaderUtils.parseDoublePercent("(1.2)")).isEqualTo(-0.012d, within(1e-10));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseDoublePercent("()"))
        .withMessage("Unable to parse percentage from '()'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseDoublePercent("(1.2(3)"))
        .withMessage("Unable to parse percentage from '(1.2(3)'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseDoublePercent("Rubbish"))
        .withMessage("Unable to parse percentage from 'Rubbish'");
  }

  @Test
  public void test_parseBigDecimal() {
    assertThat(LoaderUtils.parseBigDecimal("1.2")).isEqualTo(BigDecimal.valueOf(1.2d));
    assertThat(LoaderUtils.parseBigDecimal("(1.2)")).isEqualTo(BigDecimal.valueOf(-1.2d));
    assertThat(LoaderUtils.parseBigDecimal("1,234,567.2")).isEqualTo(BigDecimal.valueOf(1_234_567.2d));
    assertThat(LoaderUtils.parseBigDecimal("(1,234,567.2)")).isEqualTo(BigDecimal.valueOf(-1_234_567.2d));
    assertThat(LoaderUtils.parseBigDecimal("1,234,5,6,7.2")).isEqualTo(BigDecimal.valueOf(1_234_567.2d));
    assertThat(LoaderUtils.parseBigDecimal("(1,234,5,6,7.2)")).isEqualTo(BigDecimal.valueOf(-1_234_567.2d));
    assertThat(LoaderUtils.parseBigDecimal("1,234.")).isEqualTo(BigDecimal.valueOf(1_234d).setScale(0));
    assertThat(LoaderUtils.parseBigDecimal("(1,234.)")).isEqualTo(BigDecimal.valueOf(-1_234d).setScale(0));
    assertThat(LoaderUtils.parseBigDecimal(".123")).isEqualTo(BigDecimal.valueOf(0.123d));
    assertThat(LoaderUtils.parseBigDecimal("(.123)")).isEqualTo(BigDecimal.valueOf(-0.123d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimal("12,,000.2"))
        .withMessage("Unable to parse BigDecimal from '12,,000.2'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimal("12,000.2,"))
        .withMessage("Unable to parse BigDecimal from '12,000.2,'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimal("12.345.678,12"))
        .withMessage("Unable to parse BigDecimal from '12.345.678,12'"); // European formats are not supported
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimal("()"))
        .withMessage("Unable to parse BigDecimal from '()'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimal("(1.2(3)"))
        .withMessage("Unable to parse BigDecimal from '(1.2(3)'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimal("Rubbish"))
        .withMessage("Unable to parse BigDecimal from 'Rubbish'");
  }

  @Test
  public void test_parseBigDecimalPercent() {
    assertThat(LoaderUtils.parseBigDecimalPercent("1.2")).isEqualTo(BigDecimal.valueOf(0.012d));
    assertThat(LoaderUtils.parseBigDecimalPercent("(1.2)")).isEqualTo(BigDecimal.valueOf(-0.012d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimalPercent("()"))
        .withMessage("Unable to parse BigDecimal percentage from '()'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimalPercent("(1.2(3)"))
        .withMessage("Unable to parse BigDecimal percentage from '(1.2(3)'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimalPercent("Rubbish"))
        .withMessage("Unable to parse BigDecimal percentage from 'Rubbish'");
  }

  @Test
  public void test_parseBigDecimalBasisPoint() {
    assertThat(LoaderUtils.parseBigDecimalBasisPoint("1.2")).isEqualTo(BigDecimal.valueOf(0.00012d));
    assertThat(LoaderUtils.parseBigDecimalBasisPoint("(1.2)")).isEqualTo(BigDecimal.valueOf(-0.00012d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimalBasisPoint("()"))
        .withMessage("Unable to parse BigDecimal basis point from '()'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimalBasisPoint("(1.2(3)"))
        .withMessage("Unable to parse BigDecimal basis point from '(1.2(3)'");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseBigDecimalBasisPoint("Rubbish"))
        .withMessage("Unable to parse BigDecimal basis point from 'Rubbish'");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseDate_formatter() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy'y' MM'm' dd'd'", Locale.ENGLISH);
    DateTimeFormatter formatter2 = DateTimeFormatter.ISO_DATE;
    assertThat(LoaderUtils.parseDate("2012y 06m 30d", formatter, formatter2))
        .isEqualTo(LocalDate.of(2012, 6, 30));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LoaderUtils.parseDate("2012-06-30", formatter));
    assertThat(LoaderUtils.parseDate("2012-06-30", formatter, formatter2))
        .isEqualTo(LocalDate.of(2012, 6, 30));
  }

  @Test
  public void test_parseDate() {
    assertThat(LoaderUtils.parseDate("2012-06-30")).isEqualTo(LocalDate.of(2012, 6, 30));
    assertThat(LoaderUtils.parseDate("20120630")).isEqualTo(LocalDate.of(2012, 6, 30));
    assertThat(LoaderUtils.parseDate("2012/06/30")).isEqualTo(LocalDate.of(2012, 6, 30));
    assertThat(LoaderUtils.parseDate("30/06/2012")).isEqualTo(LocalDate.of(2012, 6, 30));
    assertThat(LoaderUtils.parseDate("30/06/12")).isEqualTo(LocalDate.of(2012, 6, 30));
    assertThat(LoaderUtils.parseDate("30-Jun-2012")).isEqualTo(LocalDate.of(2012, 6, 30));
    assertThat(LoaderUtils.parseDate("30-Jun-12")).isEqualTo(LocalDate.of(2012, 6, 30));
    assertThat(LoaderUtils.parseDate("30Jun2012")).isEqualTo(LocalDate.of(2012, 6, 30));
    assertThat(LoaderUtils.parseDate("30Jun12")).isEqualTo(LocalDate.of(2012, 6, 30));

    assertThat(LoaderUtils.parseDate("2012-05-04")).isEqualTo(LocalDate.of(2012, 5, 4));
    assertThat(LoaderUtils.parseDate("20120504")).isEqualTo(LocalDate.of(2012, 5, 4));
    assertThat(LoaderUtils.parseDate("2012/5/4")).isEqualTo(LocalDate.of(2012, 5, 4));
    assertThat(LoaderUtils.parseDate("4/5/2012")).isEqualTo(LocalDate.of(2012, 5, 4));
    assertThat(LoaderUtils.parseDate("4/5/12")).isEqualTo(LocalDate.of(2012, 5, 4));
    assertThat(LoaderUtils.parseDate("4-May-2012")).isEqualTo(LocalDate.of(2012, 5, 4));
    assertThat(LoaderUtils.parseDate("4-May-12")).isEqualTo(LocalDate.of(2012, 5, 4));
    assertThat(LoaderUtils.parseDate("4May2012")).isEqualTo(LocalDate.of(2012, 5, 4));
    assertThat(LoaderUtils.parseDate("4May12")).isEqualTo(LocalDate.of(2012, 5, 4));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseDate("040512"));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseDate("Rubbish"));
  }

  @Test
  public void test_parseYearMonth() {
    assertThat(LoaderUtils.parseYearMonth("2012-06")).isEqualTo(YearMonth.of(2012, 6));
    assertThat(LoaderUtils.parseYearMonth("201206")).isEqualTo(YearMonth.of(2012, 6));
    assertThat(LoaderUtils.parseYearMonth("Jun-2012")).isEqualTo(YearMonth.of(2012, 6));
    assertThat(LoaderUtils.parseYearMonth("Jun-12")).isEqualTo(YearMonth.of(2012, 6));
    assertThat(LoaderUtils.parseYearMonth("Jun2012")).isEqualTo(YearMonth.of(2012, 6));
    assertThat(LoaderUtils.parseYearMonth("Jun12")).isEqualTo(YearMonth.of(2012, 6));
    assertThat(LoaderUtils.parseYearMonth("1/6/2012")).isEqualTo(YearMonth.of(2012, 6));
    assertThat(LoaderUtils.parseYearMonth("01/6/2012")).isEqualTo(YearMonth.of(2012, 6));
    assertThat(LoaderUtils.parseYearMonth("1/06/2012")).isEqualTo(YearMonth.of(2012, 6));
    assertThat(LoaderUtils.parseYearMonth("01/06/2012")).isEqualTo(YearMonth.of(2012, 6));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseYearMonth("2/6/2012"));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseYearMonth("1/6/12"));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseYearMonth("Jun1"));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseYearMonth("12345678"));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseYearMonth("Rubbish"));
  }

  @Test
  public void test_parseTime() {
    assertThat(LoaderUtils.parseTime("2")).isEqualTo(LocalTime.of(2, 0));
    assertThat(LoaderUtils.parseTime("11")).isEqualTo(LocalTime.of(11, 0));
    assertThat(LoaderUtils.parseTime("11:30")).isEqualTo(LocalTime.of(11, 30));
    assertThat(LoaderUtils.parseTime("11:30:20")).isEqualTo(LocalTime.of(11, 30, 20));
    assertThat(LoaderUtils.parseTime("11:30:20.123")).isEqualTo(LocalTime.of(11, 30, 20, 123_000_000));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseTime("Rubbish"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parsePeriod() {
    assertThat(LoaderUtils.parsePeriod("P2D")).isEqualTo(Period.ofDays(2));
    assertThat(LoaderUtils.parsePeriod("2D")).isEqualTo(Period.ofDays(2));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parsePeriod("2"));
  }

  @Test
  public void test_tryParsePeriod() {
    assertThat(LoaderUtils.tryParsePeriod("P2D")).hasValue(Period.ofDays(2));
    assertThat(LoaderUtils.tryParsePeriod("2D")).hasValue(Period.ofDays(2));
    assertThat(LoaderUtils.tryParsePeriod("2X")).isEmpty();
    assertThat(LoaderUtils.tryParsePeriod("2")).isEmpty();
    assertThat(LoaderUtils.tryParsePeriod("")).isEmpty();
    assertThat(LoaderUtils.tryParsePeriod(null)).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseMarketTenor() {
    assertThat(LoaderUtils.parseMarketTenor("P2D")).isEqualTo(MarketTenor.ofSpotDays(2));
    assertThat(LoaderUtils.parseMarketTenor("2D")).isEqualTo(MarketTenor.ofSpotDays(2));
    assertThat(LoaderUtils.parseMarketTenor("ON")).isEqualTo(MarketTenor.ON);
    assertThat(LoaderUtils.parseMarketTenor("TN")).isEqualTo(MarketTenor.TN);
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseMarketTenor("2"));
  }

  @Test
  public void test_tryParseMarketTenor() {
    assertThat(LoaderUtils.tryParseMarketTenor("P2D")).hasValue(MarketTenor.ofSpotDays(2));
    assertThat(LoaderUtils.tryParseMarketTenor("2D")).hasValue(MarketTenor.ofSpotDays(2));
    assertThat(LoaderUtils.tryParseMarketTenor("ON")).hasValue(MarketTenor.ON);
    assertThat(LoaderUtils.tryParseMarketTenor("TN")).hasValue(MarketTenor.TN);
    assertThat(LoaderUtils.tryParseMarketTenor("2X")).isEmpty();
    assertThat(LoaderUtils.tryParseMarketTenor("2")).isEmpty();
    assertThat(LoaderUtils.tryParseMarketTenor("")).isEmpty();
    assertThat(LoaderUtils.tryParseMarketTenor(null)).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseTenor() {
    assertThat(LoaderUtils.parseTenor("P2D")).isEqualTo(Tenor.ofDays(2));
    assertThat(LoaderUtils.parseTenor("2D")).isEqualTo(Tenor.ofDays(2));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseTenor("2"));
  }

  @Test
  public void test_tryParseTenor() {
    assertThat(LoaderUtils.tryParseTenor("P2D")).hasValue(Tenor.ofDays(2));
    assertThat(LoaderUtils.tryParseTenor("2D")).hasValue(Tenor.ofDays(2));
    assertThat(LoaderUtils.tryParseTenor("2X")).isEmpty();
    assertThat(LoaderUtils.tryParseTenor("2")).isEmpty();
    assertThat(LoaderUtils.tryParseTenor("")).isEmpty();
    assertThat(LoaderUtils.tryParseTenor(null)).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseFrequency() {
    assertThat(LoaderUtils.parseFrequency("P2D")).isEqualTo(Frequency.ofDays(2));
    assertThat(LoaderUtils.parseFrequency("2D")).isEqualTo(Frequency.ofDays(2));
    assertThat(LoaderUtils.parseFrequency("TERM")).isEqualTo(Frequency.TERM);
    assertThat(LoaderUtils.parseFrequency("T")).isEqualTo(Frequency.TERM);
    assertThat(LoaderUtils.parseFrequency("0T")).isEqualTo(Frequency.TERM);
    assertThat(LoaderUtils.parseFrequency("1T")).isEqualTo(Frequency.TERM);
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseFrequency("2"));
  }

  @Test
  public void test_tryParseFrequency() {
    assertThat(LoaderUtils.tryParseFrequency("P2D")).hasValue(Frequency.ofDays(2));
    assertThat(LoaderUtils.tryParseFrequency("2D")).hasValue(Frequency.ofDays(2));
    assertThat(LoaderUtils.tryParseFrequency("TERM")).hasValue(Frequency.TERM);
    assertThat(LoaderUtils.tryParseFrequency("T")).hasValue(Frequency.TERM);
    assertThat(LoaderUtils.tryParseFrequency("0T")).hasValue(Frequency.TERM);
    assertThat(LoaderUtils.tryParseFrequency("1T")).hasValue(Frequency.TERM);
    assertThat(LoaderUtils.tryParseFrequency("2X")).isEmpty();
    assertThat(LoaderUtils.tryParseFrequency("2")).isEmpty();
    assertThat(LoaderUtils.tryParseFrequency("")).isEmpty();
    assertThat(LoaderUtils.tryParseFrequency(null)).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseCurrency() {
    assertThat(LoaderUtils.parseCurrency("GBP")).isEqualTo(Currency.GBP);
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseCurrency("A"));
  }

  @Test
  public void test_tryParseCurrency() {
    assertThat(LoaderUtils.tryParseCurrency("GBP")).isEqualTo(Optional.of(Currency.GBP));
    assertThat(LoaderUtils.tryParseCurrency("123")).isEqualTo(Optional.empty());
    assertThat(LoaderUtils.tryParseCurrency("G")).isEqualTo(Optional.empty());
    assertThat(LoaderUtils.tryParseCurrency("")).isEqualTo(Optional.empty());
    assertThat(LoaderUtils.tryParseCurrency(null)).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseBusinessDayConvention() {
    assertThat(LoaderUtils.parseBusinessDayConvention("MODFOLLOW")).isEqualTo(MODIFIED_FOLLOWING);
    assertThat(LoaderUtils.parseBusinessDayConvention("ModifiedFollowing")).isEqualTo(MODIFIED_FOLLOWING);
    assertThat(LoaderUtils.parseBusinessDayConvention("MF")).isEqualTo(MODIFIED_FOLLOWING);
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseBusinessDayConvention("Rubbish"));
  }

  @Test
  public void test_parseRollConvention() {
    assertThat(LoaderUtils.parseRollConvention("IMM")).isEqualTo(RollConventions.IMM);
    assertThat(LoaderUtils.parseRollConvention("imm")).isEqualTo(RollConventions.IMM);
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseRollConvention("Rubbish"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseBuySell() {
    assertThat(LoaderUtils.parseBuySell("BUY")).isEqualTo(BuySell.BUY);
    assertThat(LoaderUtils.parseBuySell("Buy")).isEqualTo(BuySell.BUY);
    assertThat(LoaderUtils.parseBuySell("buy")).isEqualTo(BuySell.BUY);
    assertThat(LoaderUtils.parseBuySell("b")).isEqualTo(BuySell.BUY);
    assertThat(LoaderUtils.parseBuySell("SELL")).isEqualTo(BuySell.SELL);
    assertThat(LoaderUtils.parseBuySell("Sell")).isEqualTo(BuySell.SELL);
    assertThat(LoaderUtils.parseBuySell("sell")).isEqualTo(BuySell.SELL);
    assertThat(LoaderUtils.parseBuySell("s")).isEqualTo(BuySell.SELL);
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseBuySell("Rubbish"));
  }

  @Test
  public void test_parsePayReceive() {
    assertThat(LoaderUtils.parsePayReceive("PAY")).isEqualTo(PayReceive.PAY);
    assertThat(LoaderUtils.parsePayReceive("Pay")).isEqualTo(PayReceive.PAY);
    assertThat(LoaderUtils.parsePayReceive("pay")).isEqualTo(PayReceive.PAY);
    assertThat(LoaderUtils.parsePayReceive("p")).isEqualTo(PayReceive.PAY);
    assertThat(LoaderUtils.parsePayReceive("RECEIVE")).isEqualTo(PayReceive.RECEIVE);
    assertThat(LoaderUtils.parsePayReceive("Receive")).isEqualTo(PayReceive.RECEIVE);
    assertThat(LoaderUtils.parsePayReceive("receive")).isEqualTo(PayReceive.RECEIVE);
    assertThat(LoaderUtils.parsePayReceive("rec")).isEqualTo(PayReceive.RECEIVE);
    assertThat(LoaderUtils.parsePayReceive("r")).isEqualTo(PayReceive.RECEIVE);
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parsePayReceive("Rubbish"));
  }

  @Test
  public void test_parsePutCall() {
    assertThat(LoaderUtils.parsePutCall("PUT")).isEqualTo(PutCall.PUT);
    assertThat(LoaderUtils.parsePutCall("Put")).isEqualTo(PutCall.PUT);
    assertThat(LoaderUtils.parsePutCall("put")).isEqualTo(PutCall.PUT);
    assertThat(LoaderUtils.parsePutCall("p")).isEqualTo(PutCall.PUT);
    assertThat(LoaderUtils.parsePutCall("CALL")).isEqualTo(PutCall.CALL);
    assertThat(LoaderUtils.parsePutCall("Call")).isEqualTo(PutCall.CALL);
    assertThat(LoaderUtils.parsePutCall("call")).isEqualTo(PutCall.CALL);
    assertThat(LoaderUtils.parsePutCall("c")).isEqualTo(PutCall.CALL);
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parsePutCall("Rubbish"));
  }

  @Test
  public void test_parseLongShort() {
    assertThat(LoaderUtils.parseLongShort("LONG")).isEqualTo(LongShort.LONG);
    assertThat(LoaderUtils.parseLongShort("Long")).isEqualTo(LongShort.LONG);
    assertThat(LoaderUtils.parseLongShort("long")).isEqualTo(LongShort.LONG);
    assertThat(LoaderUtils.parseLongShort("l")).isEqualTo(LongShort.LONG);
    assertThat(LoaderUtils.parseLongShort("SHORT")).isEqualTo(LongShort.SHORT);
    assertThat(LoaderUtils.parseLongShort("Short")).isEqualTo(LongShort.SHORT);
    assertThat(LoaderUtils.parseLongShort("short")).isEqualTo(LongShort.SHORT);
    assertThat(LoaderUtils.parseLongShort("s")).isEqualTo(LongShort.SHORT);
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseLongShort("Rubbish"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseRedCode() {
    assertThat(LoaderUtils.parseRedCode("123456")).isEqualTo(StandardId.of(StandardSchemes.RED6_SCHEME, "123456"));
    assertThat(LoaderUtils.parseRedCode("123456789")).isEqualTo(StandardId.of(StandardSchemes.RED9_SCHEME, "123456789"));
    assertThatIllegalArgumentException().isThrownBy(() -> LoaderUtils.parseRedCode("0"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(LoaderUtils.class);
  }

}
