/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link LoaderUtils}.
 */
@Test
public class LoaderUtilsTest {

  public void test_findIndex() {
    assertEquals(LoaderUtils.findIndex("GBP-LIBOR-3M"), IborIndices.GBP_LIBOR_3M);
    assertEquals(LoaderUtils.findIndex("GBP-SONIA"), OvernightIndices.GBP_SONIA);
    assertEquals(LoaderUtils.findIndex("GB-RPI"), PriceIndices.GB_RPI);
    assertEquals(LoaderUtils.findIndex("GBP/USD-WM"), FxIndices.GBP_USD_WM);
    assertThrowsIllegalArg(() -> LoaderUtils.findIndex("Rubbish"));
  }

  public void test_parseBoolean() {
    assertEquals(LoaderUtils.parseBoolean("TRUE"), true);
    assertEquals(LoaderUtils.parseBoolean("True"), true);
    assertEquals(LoaderUtils.parseBoolean("true"), true);
    assertEquals(LoaderUtils.parseBoolean("t"), true);
    assertEquals(LoaderUtils.parseBoolean("yes"), true);
    assertEquals(LoaderUtils.parseBoolean("y"), true);
    assertEquals(LoaderUtils.parseBoolean("FALSE"), false);
    assertEquals(LoaderUtils.parseBoolean("False"), false);
    assertEquals(LoaderUtils.parseBoolean("false"), false);
    assertEquals(LoaderUtils.parseBoolean("f"), false);
    assertEquals(LoaderUtils.parseBoolean("no"), false);
    assertEquals(LoaderUtils.parseBoolean("n"), false);
    assertThrowsIllegalArg(() -> LoaderUtils.parseBoolean("Rubbish"));
  }

  public void test_parseInteger() {
    assertEquals(LoaderUtils.parseInteger("2"), 2);
    assertThrowsIllegalArg(() -> LoaderUtils.parseInteger("Rubbish"));
  }

  public void test_parseDouble() {
    assertEquals(LoaderUtils.parseDouble("1.2"), 1.2d, 1e-10);
    assertThrowsIllegalArg(() -> LoaderUtils.parseDouble("Rubbish"));
  }

  public void test_parseDoublePercent() {
    assertEquals(LoaderUtils.parseDoublePercent("1.2"), 0.012d, 1e-10);
    assertThrowsIllegalArg(() -> LoaderUtils.parseDouble("Rubbish"));
  }

  public void test_parseDate() {
    assertEquals(LoaderUtils.parseDate("2012-06-30"), LocalDate.of(2012, 6, 30));
    assertEquals(LoaderUtils.parseDate("20120630"), LocalDate.of(2012, 6, 30));
    assertEquals(LoaderUtils.parseDate("2012/06/30"), LocalDate.of(2012, 6, 30));
    assertEquals(LoaderUtils.parseDate("30/06/2012"), LocalDate.of(2012, 6, 30));
    assertEquals(LoaderUtils.parseDate("30/06/12"), LocalDate.of(2012, 6, 30));
    assertEquals(LoaderUtils.parseDate("30-Jun-2012"), LocalDate.of(2012, 6, 30));
    assertEquals(LoaderUtils.parseDate("30-Jun-12"), LocalDate.of(2012, 6, 30));
    assertEquals(LoaderUtils.parseDate("30Jun2012"), LocalDate.of(2012, 6, 30));
    assertEquals(LoaderUtils.parseDate("30Jun12"), LocalDate.of(2012, 6, 30));

    assertEquals(LoaderUtils.parseDate("2012-05-04"), LocalDate.of(2012, 5, 4));
    assertEquals(LoaderUtils.parseDate("20120504"), LocalDate.of(2012, 5, 4));
    assertEquals(LoaderUtils.parseDate("2012/5/4"), LocalDate.of(2012, 5, 4));
    assertEquals(LoaderUtils.parseDate("4/5/2012"), LocalDate.of(2012, 5, 4));
    assertEquals(LoaderUtils.parseDate("4/5/12"), LocalDate.of(2012, 5, 4));
    assertEquals(LoaderUtils.parseDate("4-May-2012"), LocalDate.of(2012, 5, 4));
    assertEquals(LoaderUtils.parseDate("4-May-12"), LocalDate.of(2012, 5, 4));
    assertEquals(LoaderUtils.parseDate("4May2012"), LocalDate.of(2012, 5, 4));
    assertEquals(LoaderUtils.parseDate("4May12"), LocalDate.of(2012, 5, 4));
    assertThrowsIllegalArg(() -> LoaderUtils.parseDate("040512"));
    assertThrowsIllegalArg(() -> LoaderUtils.parseDate("Rubbish"));
  }

  public void test_parseYearMonth() {
    assertEquals(LoaderUtils.parseYearMonth("2012-06"), YearMonth.of(2012, 6));
    assertEquals(LoaderUtils.parseYearMonth("201206"), YearMonth.of(2012, 6));
    assertEquals(LoaderUtils.parseYearMonth("Jun-2012"), YearMonth.of(2012, 6));
    assertEquals(LoaderUtils.parseYearMonth("Jun-12"), YearMonth.of(2012, 6));
    assertEquals(LoaderUtils.parseYearMonth("Jun2012"), YearMonth.of(2012, 6));
    assertEquals(LoaderUtils.parseYearMonth("Jun12"), YearMonth.of(2012, 6));
    assertEquals(LoaderUtils.parseYearMonth("1/6/2012"), YearMonth.of(2012, 6));
    assertEquals(LoaderUtils.parseYearMonth("01/6/2012"), YearMonth.of(2012, 6));
    assertEquals(LoaderUtils.parseYearMonth("1/06/2012"), YearMonth.of(2012, 6));
    assertEquals(LoaderUtils.parseYearMonth("01/06/2012"), YearMonth.of(2012, 6));
    assertThrowsIllegalArg(() -> LoaderUtils.parseYearMonth("2/6/2012"));
    assertThrowsIllegalArg(() -> LoaderUtils.parseYearMonth("1/6/12"));
    assertThrowsIllegalArg(() -> LoaderUtils.parseYearMonth("Jun1"));
    assertThrowsIllegalArg(() -> LoaderUtils.parseYearMonth("12345678"));
    assertThrowsIllegalArg(() -> LoaderUtils.parseYearMonth("Rubbish"));
  }

  public void test_parseTime() {
    assertEquals(LoaderUtils.parseTime("2"), LocalTime.of(2, 0));
    assertEquals(LoaderUtils.parseTime("11"), LocalTime.of(11, 0));
    assertEquals(LoaderUtils.parseTime("11:30"), LocalTime.of(11, 30));
    assertEquals(LoaderUtils.parseTime("11:30:20"), LocalTime.of(11, 30, 20));
    assertEquals(LoaderUtils.parseTime("11:30:20.123"), LocalTime.of(11, 30, 20, 123_000_000));
    assertThrowsIllegalArg(() -> LoaderUtils.parseTime("Rubbish"));
  }

  public void test_parsePeriod() {
    assertEquals(LoaderUtils.parsePeriod("P2D"), Period.ofDays(2));
    assertEquals(LoaderUtils.parsePeriod("2D"), Period.ofDays(2));
    assertThrowsIllegalArg(() -> LoaderUtils.parsePeriod("2"));
  }

  public void test_parseTenor() {
    assertEquals(LoaderUtils.parseTenor("P2D"), Tenor.ofDays(2));
    assertEquals(LoaderUtils.parseTenor("2D"), Tenor.ofDays(2));
    assertThrowsIllegalArg(() -> LoaderUtils.parseTenor("2"));
  }

  public void test_parseBuySell() {
    assertEquals(LoaderUtils.parseBuySell("BUY"), BuySell.BUY);
    assertEquals(LoaderUtils.parseBuySell("Buy"), BuySell.BUY);
    assertEquals(LoaderUtils.parseBuySell("buy"), BuySell.BUY);
    assertEquals(LoaderUtils.parseBuySell("b"), BuySell.BUY);
    assertEquals(LoaderUtils.parseBuySell("SELL"), BuySell.SELL);
    assertEquals(LoaderUtils.parseBuySell("Sell"), BuySell.SELL);
    assertEquals(LoaderUtils.parseBuySell("sell"), BuySell.SELL);
    assertEquals(LoaderUtils.parseBuySell("s"), BuySell.SELL);
    assertThrowsIllegalArg(() -> LoaderUtils.parseBoolean("Rubbish"));
  }

  public void test_parsePayReceive() {
    assertEquals(LoaderUtils.parsePayReceive("PAY"), PayReceive.PAY);
    assertEquals(LoaderUtils.parsePayReceive("Pay"), PayReceive.PAY);
    assertEquals(LoaderUtils.parsePayReceive("pay"), PayReceive.PAY);
    assertEquals(LoaderUtils.parsePayReceive("p"), PayReceive.PAY);
    assertEquals(LoaderUtils.parsePayReceive("RECEIVE"), PayReceive.RECEIVE);
    assertEquals(LoaderUtils.parsePayReceive("Receive"), PayReceive.RECEIVE);
    assertEquals(LoaderUtils.parsePayReceive("receive"), PayReceive.RECEIVE);
    assertEquals(LoaderUtils.parsePayReceive("rec"), PayReceive.RECEIVE);
    assertEquals(LoaderUtils.parsePayReceive("r"), PayReceive.RECEIVE);
    assertThrowsIllegalArg(() -> LoaderUtils.parseBoolean("Rubbish"));
  }

  public void test_parsePutCall() {
    assertEquals(LoaderUtils.parsePutCall("PUT"), PutCall.PUT);
    assertEquals(LoaderUtils.parsePutCall("Put"), PutCall.PUT);
    assertEquals(LoaderUtils.parsePutCall("put"), PutCall.PUT);
    assertEquals(LoaderUtils.parsePutCall("p"), PutCall.PUT);
    assertEquals(LoaderUtils.parsePutCall("CALL"), PutCall.CALL);
    assertEquals(LoaderUtils.parsePutCall("Call"), PutCall.CALL);
    assertEquals(LoaderUtils.parsePutCall("call"), PutCall.CALL);
    assertEquals(LoaderUtils.parsePutCall("c"), PutCall.CALL);
    assertThrowsIllegalArg(() -> LoaderUtils.parseBoolean("Rubbish"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(LoaderUtils.class);
  }

}
