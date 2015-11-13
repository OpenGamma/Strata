/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.future.type;

import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.product.rate.future.IborFutureTrade;

/**
 * Tests {@link IborFutureTemplate}.
 */
@Test
public class IborFutureTemplateTest {

  private static final IborFutureConvention CONVENTION = ImmutableIborFutureConvention.of(USD_LIBOR_3M, QUARTERLY_IMM);
  private static final IborFutureConvention CONVENTION2 = ImmutableIborFutureConvention.of(USD_LIBOR_6M, QUARTERLY_IMM);
  private static final Period MIN_PERIOD = Period.ofMonths(2);
  private static final int NUMBER = 2;

  public void test_of() {
    IborFutureTemplate test = IborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    assertEquals(test.getMinimumPeriod(), MIN_PERIOD);
    assertEquals(test.getSequenceNumber(), NUMBER);
    assertEquals(test.getConvention(), CONVENTION);
  }

  public void test_builder_insufficientInfo() {
    assertThrowsIllegalArg(() -> IborFutureTemplate.builder().convention(CONVENTION).build());
    assertThrowsIllegalArg(() -> IborFutureTemplate.builder().minimumPeriod(MIN_PERIOD).build());
    assertThrowsIllegalArg(() -> IborFutureTemplate.builder().sequenceNumber(NUMBER).build());
    assertThrowsIllegalArg(() -> IborFutureTemplate.builder().minimumPeriod(MIN_PERIOD).sequenceNumber(NUMBER).build());
    assertThrowsIllegalArg(() -> IborFutureTemplate.builder().sequenceNumber(NUMBER).convention(CONVENTION).build());
  }

  //-------------------------------------------------------------------------
  public void test_toTrade() {
    IborFutureTemplate base = IborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    LocalDate date = LocalDate.of(2015, 10, 20);
    long quantity = 3;
    double price = 0.99;
    double notional = 100.0;
    IborFutureTrade trade = base.toTrade(date, quantity, notional, price);
    IborFutureTrade expected = CONVENTION.toTrade(date, MIN_PERIOD, NUMBER, quantity, notional, price);
    assertEquals(trade, expected);
  }

  public void test_referenceDate() {
    IborFutureTemplate base = IborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    LocalDate date = LocalDate.of(2015, 10, 20);  // 2nd Quarterly IMM at least 2 months later from this date
    LocalDate expected = LocalDate.of(2016, 6, 15);  // 1st is March 2016, 2nd is Jun 2016
    assertEquals(base.referenceDate(date), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFutureTemplate test = IborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    coverImmutableBean(test);
    IborFutureTemplate test2 = IborFutureTemplate.builder()
        .minimumPeriod(Period.ofMonths(3))
        .sequenceNumber(NUMBER)
        .convention(CONVENTION2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborFutureTemplate test = IborFutureTemplate.of(MIN_PERIOD, NUMBER, CONVENTION);
    assertSerialization(test);
  }

}
