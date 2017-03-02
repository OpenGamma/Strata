/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Tests {@link RelativeIborFutureTemplate}.
 */
@Test
public class RelativeIborFutureTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFutureConvention CONVENTION = ImmutableIborFutureConvention.of(USD_LIBOR_3M, QUARTERLY_IMM);
  private static final IborFutureConvention CONVENTION2 = ImmutableIborFutureConvention.of(USD_LIBOR_6M, QUARTERLY_IMM);
  private static final Period MIN_PERIOD = Period.ofMonths(2);
  private static final int NUMBER = 2;

  //-------------------------------------------------------------------------
  public void test_of() {
    RelativeIborFutureTemplate test = RelativeIborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    assertEquals(test.getMinimumPeriod(), MIN_PERIOD);
    assertEquals(test.getSequenceNumber(), NUMBER);
    assertEquals(test.getConvention(), CONVENTION);
    assertEquals(test.getIndex(), CONVENTION.getIndex());
  }

  //-------------------------------------------------------------------------
  public void test_createTrade() {
    IborFutureTemplate base = IborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    LocalDate date = LocalDate.of(2015, 10, 20);
    double quantity = 3;
    double price = 0.99;
    double notional = 100.0;
    SecurityId secId = SecurityId.of("OG-Future", "GBP-LIBOR-3M-Jun16");
    IborFutureTrade trade = base.createTrade(date, secId, quantity, notional, price, REF_DATA);
    IborFutureTrade expected = CONVENTION.createTrade(date, secId, MIN_PERIOD, NUMBER, quantity, notional, price, REF_DATA);
    assertEquals(trade, expected);
  }

  public void test_calculateReferenceDateFromTradeDate() {
    IborFutureTemplate base = IborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    LocalDate date = LocalDate.of(2015, 10, 20);  // 2nd Quarterly IMM at least 2 months later from this date
    LocalDate expected = LocalDate.of(2016, 6, 15);  // 1st is March 2016, 2nd is Jun 2016
    assertEquals(base.calculateReferenceDateFromTradeDate(date, REF_DATA), expected);
  }

  public void test_approximateMaturity() {
    IborFutureTemplate base = IborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    assertEquals(base.approximateMaturity(LocalDate.of(2015, 10, 20)), 0.5d, 0.1d);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RelativeIborFutureTemplate test = RelativeIborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    coverImmutableBean(test);
    RelativeIborFutureTemplate test2 = RelativeIborFutureTemplate.of(Period.ofMonths(3), NUMBER + 1, CONVENTION2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RelativeIborFutureTemplate test = RelativeIborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    assertSerialization(test);
  }

}
