/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static com.opengamma.strata.product.swap.SwapLegType.IBOR;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.product.rate.IborRateObservation;

/**
 * Test.
 */
@Test
public class RatePeriodSwapLegTest {

  private static final LocalDate DATE_2014_06_28 = date(2014, 6, 28);
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final LocalDate DATE_2014_09_28 = date(2014, 9, 28);
  private static final LocalDate DATE_2014_09_30 = date(2014, 9, 30);
  private static final LocalDate DATE_2014_10_01 = date(2014, 10, 1);
  private static final LocalDate DATE_2014_12_30 = date(2014, 12, 30);
  private static final LocalDate DATE_2014_01_02 = date(2014, 1, 2);
  private static final IborRateObservation GBPLIBOR3M_2014_06_28 = IborRateObservation.of(GBP_LIBOR_3M, DATE_2014_06_28);
  private static final IborRateObservation GBPLIBOR3M_2014_09_28 = IborRateObservation.of(GBP_LIBOR_3M, DATE_2014_09_28);
  private static final NotionalExchange NOTIONAL_EXCHANGE =
      NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, 2000d));
  private static final RateAccrualPeriod RAP1 = RateAccrualPeriod.builder()
      .startDate(DATE_2014_06_30)
      .endDate(DATE_2014_09_30)
      .yearFraction(0.25d)
      .rateObservation(GBPLIBOR3M_2014_06_28)
      .build();
  private static final RateAccrualPeriod RAP2 = RateAccrualPeriod.builder()
      .startDate(DATE_2014_09_30)
      .endDate(DATE_2014_12_30)
      .yearFraction(0.25d)
      .rateObservation(GBPLIBOR3M_2014_09_28)
      .build();
  private static final RatePaymentPeriod RPP1 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP1)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(5000d)
      .build();
  private static final RatePaymentPeriod RPP1_FXRESET = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP1)
      .dayCount(ACT_365F)
      .currency(GBP)
      .fxReset(FxReset.of(GBP_USD_WM, USD, DATE_2014_06_28))
      .notional(8000d)
      .build();
  private static final RatePaymentPeriod RPP2 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_01_02)
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
  private static final BusinessDayAdjustment FOLLOWING_GBLO = BusinessDayAdjustment.of(FOLLOWING, GBLO);

  //-------------------------------------------------------------------------
  public void test_builder() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .initialExchange(true)
        .intermediateExchange(true)
        .finalExchange(true)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .paymentBusinessDayAdjustment(FOLLOWING_GBLO)
        .build();
    assertEquals(test.getType(), IBOR);
    assertEquals(test.getPayReceive(), RECEIVE);
    assertEquals(test.getStartDate(), DATE_2014_06_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPaymentPeriods(), ImmutableList.of(RPP1));
    assertEquals(test.getPaymentEvents(), ImmutableList.of(NOTIONAL_EXCHANGE));
    assertEquals(test.isInitialExchange(), true);
    assertEquals(test.isIntermediateExchange(), true);
    assertEquals(test.isFinalExchange(), true);
    assertEquals(test.getPaymentBusinessDayAdjustment(), FOLLOWING_GBLO);
  }

  public void test_builder_defaults() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .build();
    assertEquals(test.getPayReceive(), RECEIVE);
    assertEquals(test.getStartDate(), DATE_2014_06_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPaymentPeriods(), ImmutableList.of(RPP1));
    assertEquals(test.getPaymentEvents(), ImmutableList.of());
    assertEquals(test.isInitialExchange(), false);
    assertEquals(test.isIntermediateExchange(), false);
    assertEquals(test.isFinalExchange(), false);
    assertEquals(test.getPaymentBusinessDayAdjustment(), BusinessDayAdjustment.NONE);
  }

  public void test_builder_invalidMixedCurrency() {
    assertThrowsIllegalArg(() -> RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP3)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertEquals(test.expand(), expected);
  }

  public void test_expand_createNotionalExchange() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .initialExchange(true)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(
            NotionalExchange.of(DATE_2014_06_30, CurrencyAmount.of(GBP, -5000d)),
            NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, 5000d)))
        .build();
    assertEquals(test.expand(), expected);
  }

  public void test_expand_fxResetNotionalExchange() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1_FXRESET, RPP2)
        .initialExchange(true)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    FxResetNotionalExchange ne1a = FxResetNotionalExchange.builder()
        .paymentDate(DATE_2014_06_30)
        .referenceCurrency(USD)
        .notional(-8000d)
        .index(GBP_USD_WM)
        .fixingDate(DATE_2014_06_28)
        .build();
    FxResetNotionalExchange ne1b = FxResetNotionalExchange.builder()
        .paymentDate(DATE_2014_10_01)
        .referenceCurrency(USD)
        .notional(8000d)
        .index(GBP_USD_WM)
        .fixingDate(DATE_2014_06_28)
        .build();
    NotionalExchange ne2a = NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, -6000d));
    NotionalExchange ne2b = NotionalExchange.of(DATE_2014_01_02, CurrencyAmount.of(GBP, 6000d));
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1_FXRESET, RPP2)
        .paymentEvents(ne1a, ne1b, ne2a, ne2b)
        .build();
    assertEquals(test.expand(), expected);
  }

  public void test_expand_omitFxResetNotionalExchange() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1_FXRESET)
        .initialExchange(true)
        .intermediateExchange(false)
        .finalExchange(true)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1_FXRESET)
        .build();
    assertEquals(test.expand(), expected);
  }

  public void test_expand_createNotionalExchange_noInitial() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .initialExchange(false)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, 5000d)))
        .build();
    assertEquals(test.expand(), expected);
  }

  public void test_expand_createNotionalExchange_initialOnly() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .initialExchange(true)
        .intermediateExchange(false)
        .finalExchange(false)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NotionalExchange.of(DATE_2014_06_30, CurrencyAmount.of(GBP, -5000d)))
        .build();
    assertEquals(test.expand(), expected);
  }

  public void test_expand_createNotionalExchange_finalOnly() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .initialExchange(false)
        .intermediateExchange(false)
        .finalExchange(true)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, 5000d)))
        .build();
    assertEquals(test.expand(), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .paymentBusinessDayAdjustment(FOLLOWING_GBLO)
        .initialExchange(true)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    coverImmutableBean(test);
    RatePeriodSwapLeg test2 = RatePeriodSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(RPP2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertSerialization(test);
  }

}
