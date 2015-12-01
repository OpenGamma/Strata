/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link ExpandedTermDeposit}.
 */
@Test
public class ExpandedTermDepositTest {

  private static final LocalDate START_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate END_DATE = LocalDate.of(2015, 7, 20);
  private static final double YEAR_FRACTION = ACT_365F.yearFraction(START_DATE, END_DATE);
  private static final double PRINCIPAL = 100000000d;
  private static final double RATE = 0.0250;
  private static final double EPS = 1.0e-14;

  //-------------------------------------------------------------------------
  public void test_builder() {
    ExpandedTermDeposit test = ExpandedTermDeposit.builder()
        .currency(GBP)
        .notional(PRINCIPAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .yearFraction(YEAR_FRACTION)
        .rate(RATE)
        .build();
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), PRINCIPAL);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), END_DATE);
    assertEquals(test.getYearFraction(), YEAR_FRACTION);
    assertEquals(test.getRate(), RATE);
    assertEquals(test.getInterest(), RATE * YEAR_FRACTION * PRINCIPAL, PRINCIPAL * EPS);
  }

  public void test_builder_wrongDates() {
    assertThrowsIllegalArg(() -> ExpandedTermDeposit.builder()
        .currency(GBP)
        .notional(PRINCIPAL)
        .startDate(START_DATE)
        .endDate(LocalDate.of(2013, 1, 22))
        .yearFraction(YEAR_FRACTION)
        .rate(RATE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    ExpandedTermDeposit test = ExpandedTermDeposit.builder()
        .currency(GBP)
        .notional(PRINCIPAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .yearFraction(YEAR_FRACTION)
        .rate(RATE)
        .build();
    assertEquals(test.expand(), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedTermDeposit test1 = ExpandedTermDeposit.builder()
        .currency(GBP)
        .notional(PRINCIPAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .yearFraction(YEAR_FRACTION)
        .rate(RATE)
        .build();
    coverImmutableBean(test1);
    ExpandedTermDeposit test2 = ExpandedTermDeposit.builder()
        .currency(GBP)
        .notional(-50000000)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .yearFraction(YEAR_FRACTION)
        .rate(0.0145)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ExpandedTermDeposit test = ExpandedTermDeposit.builder()
        .currency(GBP)
        .notional(PRINCIPAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .yearFraction(YEAR_FRACTION)
        .rate(RATE)
        .build();
    assertSerialization(test);
  }

}
