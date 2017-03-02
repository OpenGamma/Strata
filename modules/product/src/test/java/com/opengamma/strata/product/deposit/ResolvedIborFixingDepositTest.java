/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link ResolvedTermDeposit}.
 */
@Test
public class ResolvedIborFixingDepositTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate START_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate END_DATE = LocalDate.of(2015, 7, 20);
  private static final double YEAR_FRACTION = ACT_365F.yearFraction(START_DATE, END_DATE);
  private static final IborRateComputation RATE_COMP = IborRateComputation.of(GBP_LIBOR_6M, FIXING_DATE, REF_DATA);
  private static final double NOTIONAL = 100000000d;
  private static final double RATE = 0.0250;

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedIborFixingDeposit test = ResolvedIborFixingDeposit.builder()
        .currency(GBP)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .yearFraction(YEAR_FRACTION)
        .floatingRate(RATE_COMP)
        .fixedRate(RATE)
        .build();
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), END_DATE);
    assertEquals(test.getYearFraction(), YEAR_FRACTION);
    assertEquals(test.getFloatingRate(), RATE_COMP);
    assertEquals(test.getFixedRate(), RATE);
  }

  public void test_builder_wrongDates() {
    assertThrowsIllegalArg(() -> ResolvedIborFixingDeposit.builder()
        .currency(GBP)
        .notional(NOTIONAL)
        .startDate(LocalDate.of(2015, 8, 20))
        .endDate(END_DATE)
        .yearFraction(YEAR_FRACTION)
        .floatingRate(RATE_COMP)
        .fixedRate(RATE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedIborFixingDeposit test1 = ResolvedIborFixingDeposit.builder()
        .currency(GBP)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .yearFraction(YEAR_FRACTION)
        .floatingRate(RATE_COMP)
        .fixedRate(RATE)
        .build();
    coverImmutableBean(test1);
    ResolvedIborFixingDeposit test2 = ResolvedIborFixingDeposit.builder()
        .currency(GBP)
        .notional(-100000000d)
        .startDate(START_DATE)
        .endDate(LocalDate.of(2015, 4, 20))
        .yearFraction(0.25)
        .floatingRate(IborRateComputation.of(GBP_LIBOR_3M, FIXING_DATE, REF_DATA))
        .fixedRate(0.0375)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedIborFixingDeposit test = ResolvedIborFixingDeposit.builder()
        .currency(GBP)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .yearFraction(YEAR_FRACTION)
        .floatingRate(RATE_COMP)
        .fixedRate(RATE)
        .build();
    assertSerialization(test);
  }

}
