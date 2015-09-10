/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.finance.rate.swap.SwapLegType.FIXED;
import static com.opengamma.strata.finance.rate.swap.SwapLegType.IBOR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.finance.rate.IborRateObservation;

/**
 * Test.
 */
@Test
public class ExpandedSwapLegTest {

  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final LocalDate DATE_2014_09_30 = date(2014, 9, 30);
  private static final LocalDate DATE_2014_12_30 = date(2014, 12, 30);
  private static final LocalDate DATE_2014_10_01 = date(2014, 10, 1);
  private static final LocalDate DATE_2015_01_01 = date(2015, 1, 1);
  private static final IborRateObservation GBP_LIBOR_3M_2014_06_28 = IborRateObservation.of(GBP_LIBOR_3M, date(2014, 6, 28));
  private static final IborRateObservation GBP_LIBOR_3M_2014_09_28 = IborRateObservation.of(GBP_LIBOR_3M, date(2014, 9, 28));
  private static final NotionalExchange NOTIONAL_EXCHANGE =
      NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, 2000d));
  private static final RateAccrualPeriod RAP1 = RateAccrualPeriod.builder()
      .startDate(DATE_2014_06_30)
      .endDate(DATE_2014_09_30)
      .yearFraction(0.25d)
      .rateObservation(GBP_LIBOR_3M_2014_06_28)
      .build();
  private static final RateAccrualPeriod RAP2 = RateAccrualPeriod.builder()
      .startDate(DATE_2014_09_30)
      .endDate(DATE_2014_12_30)
      .yearFraction(0.25d)
      .rateObservation(GBP_LIBOR_3M_2014_09_28)
      .build();
  private static final RatePaymentPeriod RPP1 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP1)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(5000d)
      .build();
  private static final RatePaymentPeriod RPP2 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2015_01_01)
      .accrualPeriods(RAP2)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(6000d)
      .build();
  private static final RatePaymentPeriod RPP3 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP1)
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(6000d)
      .build();

  public void test_builder() {
    ExpandedSwapLeg test = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertEquals(test.getType(), IBOR);
    assertEquals(test.getPayReceive(), RECEIVE);
    assertEquals(test.getStartDate(), DATE_2014_06_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPaymentPeriods(), ImmutableList.of(RPP1));
    assertEquals(test.getPaymentEvents(), ImmutableList.of(NOTIONAL_EXCHANGE));
  }

  public void test_builder_invalidMixedCurrency() {
    assertThrowsIllegalArg(() -> ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP3)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_findPaymentPeriod() {
    ExpandedSwapLeg test = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1, RPP2)
        .build();
    assertEquals(test.findPaymentPeriod(RPP1.getStartDate()), Optional.empty());
    assertEquals(test.findPaymentPeriod(RPP1.getStartDate().plusDays(1)), Optional.of(RPP1));
    assertEquals(test.findPaymentPeriod(RPP1.getEndDate()), Optional.of(RPP1));
    assertEquals(test.findPaymentPeriod(RPP2.getStartDate()), Optional.of(RPP1));
    assertEquals(test.findPaymentPeriod(RPP2.getStartDate().plusDays(1)), Optional.of(RPP2));
    assertEquals(test.findPaymentPeriod(RPP2.getEndDate()), Optional.of(RPP2));
    assertEquals(test.findPaymentPeriod(RPP2.getEndDate().plusDays(1)), Optional.empty());
  }

  public void test_collectIndices() {
    ExpandedSwapLeg test = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  public void test_expand() {
    ExpandedSwapLeg test = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertSame(test.expand(), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedSwapLeg test = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    coverImmutableBean(test);
    ExpandedSwapLeg test2 = ExpandedSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(RPP2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ExpandedSwapLeg test = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertSerialization(test);
  }

}
