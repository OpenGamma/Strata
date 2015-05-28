/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.finance.rate.IborRateObservation;

/**
 * Test {@link ExpandedTermDeposit}.
 */
@Test
public class ExpandedIborFixingDepositTest {
  private static final LocalDate FIXING_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate START_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate END_DATE = LocalDate.of(2015, 7, 20);
  private static final double YEAR_FRACTION = ACT_365F.yearFraction(START_DATE, END_DATE);
  private static final IborRateObservation RATE_OBS = IborRateObservation.of(GBP_LIBOR_6M, FIXING_DATE);
  private static final double NOTIONAL = 100000000d;
  private static final double RATE = 0.0250;

  public void test_builder() {
    ExpandedIborFixingDeposit test = ExpandedIborFixingDeposit.builder()
        .currency(GBP)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .floatingRate(RATE_OBS)
        .notional(NOTIONAL)
        .rate(RATE)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getEndDate(), END_DATE);
    assertEquals(test.getFloatingRate(), RATE_OBS);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getRate(), RATE);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getYearFraction(), YEAR_FRACTION);
  }

  public void test_builder_wrongDates() {
    assertThrowsIllegalArg(() -> ExpandedIborFixingDeposit.builder()
        .currency(GBP)
        .startDate(LocalDate.of(2015, 8, 20))
        .endDate(END_DATE)
        .floatingRate(RATE_OBS)
        .notional(NOTIONAL)
        .rate(RATE)
        .yearFraction(YEAR_FRACTION)
        .build());
  }

  public void test_expand() {
    ExpandedIborFixingDeposit test = ExpandedIborFixingDeposit.builder()
        .currency(GBP)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .floatingRate(RATE_OBS)
        .notional(NOTIONAL)
        .rate(RATE)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertEquals(test.expand(), test);
  }

  public void coverage() {
    ExpandedIborFixingDeposit test1 = ExpandedIborFixingDeposit.builder()
        .currency(GBP)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .floatingRate(RATE_OBS)
        .notional(NOTIONAL)
        .rate(RATE)
        .yearFraction(YEAR_FRACTION)
        .build();
    coverImmutableBean(test1);
    ExpandedIborFixingDeposit test2 = ExpandedIborFixingDeposit.builder()
        .currency(GBP)
        .startDate(START_DATE)
        .endDate(LocalDate.of(2015, 4, 20))
        .floatingRate(IborRateObservation.of(GBP_LIBOR_3M, FIXING_DATE))
        .notional(-100000000d)
        .rate(0.0375)
        .yearFraction(0.25)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ExpandedIborFixingDeposit test = ExpandedIborFixingDeposit.builder()
        .currency(GBP)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .floatingRate(RATE_OBS)
        .notional(NOTIONAL)
        .rate(RATE)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertSerialization(test);
  }

}
