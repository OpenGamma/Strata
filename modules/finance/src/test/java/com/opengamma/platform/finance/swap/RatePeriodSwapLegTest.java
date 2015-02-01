/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.platform.finance.observation.IborRateObservation;

/**
 * Test.
 */
@Test
public class RatePeriodSwapLegTest {

  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final LocalDate DATE_2014_09_30 = date(2014, 9, 30);
  private static final LocalDate DATE_2014_10_01 = date(2014, 10, 1);
  private static final IborRateObservation GBP_LIBOR_3M_2014_06_28 = IborRateObservation.of(GBP_LIBOR_3M, date(2014, 6, 28));
  private static final NotionalExchange NOTIONAL_EXCHANGE =
      NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, 2000d));
  private static final RateAccrualPeriod RAP = RateAccrualPeriod.builder()
      .startDate(DATE_2014_06_30)
      .endDate(DATE_2014_09_30)
      .yearFraction(0.25d)
      .rateObservation(GBP_LIBOR_3M_2014_06_28)
      .build();
  private static final RatePaymentPeriod RPP1 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP)
      .currency(GBP)
      .notional(5000d)
      .build();
  private static final RatePaymentPeriod RPP2 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP)
      .currency(GBP)
      .notional(6000d)
      .build();
  private static final RatePaymentPeriod RPP3 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP)
      .currency(USD)
      .notional(6000d)
      .build();
  private static final BusinessDayAdjustment FOLLOWING_GBLO = BusinessDayAdjustment.of(FOLLOWING, GBLO);

  //-------------------------------------------------------------------------
  public void test_builder() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .paymentPeriods(RPP1)
        .initialExchangeDate(DATE_2014_06_30)
        .intermediateExchange(true)
        .finalExchange(true)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .paymentBusinessDayAdjustment(FOLLOWING_GBLO)
        .build();
    assertEquals(test.getStartDate(), DATE_2014_06_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPaymentPeriods(), ImmutableList.of(RPP1));
    assertEquals(test.getPaymentEvents(), ImmutableList.of(NOTIONAL_EXCHANGE));
    assertEquals(test.getInitialExchangeDate(), Optional.of(DATE_2014_06_30));
    assertEquals(test.isIntermediateExchange(), true);
    assertEquals(test.isFinalExchange(), true);
    assertEquals(test.getPaymentBusinessDayAdjustment(), FOLLOWING_GBLO);
  }

  public void test_builder_defaults() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .paymentPeriods(RPP1)
        .build();
    assertEquals(test.getStartDate(), DATE_2014_06_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPaymentPeriods(), ImmutableList.of(RPP1));
    assertEquals(test.getPaymentEvents(), ImmutableList.of());
    assertEquals(test.getInitialExchangeDate(), Optional.empty());
    assertEquals(test.isIntermediateExchange(), false);
    assertEquals(test.isFinalExchange(), false);
    assertEquals(test.getPaymentBusinessDayAdjustment(), BusinessDayAdjustment.NONE);
  }

  public void test_builder_invalidMixedCurrency() {
    assertThrowsIllegalArg(() -> RatePeriodSwapLeg.builder()
        .paymentPeriods(RPP3)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertEquals(test.expand(), expected);
  }

  public void test_expand_createNotionalExchange() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .paymentPeriods(RPP1)
        .initialExchangeDate(DATE_2014_06_30)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .paymentPeriods(RPP1)
        .paymentEvents(
            NotionalExchange.of(DATE_2014_06_30, CurrencyAmount.of(GBP, -5000d)),
            NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, 5000d)))
        .build();
    assertEquals(test.expand(), expected);
  }

  public void test_expand_createNotionalExchange_noInitial() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .paymentPeriods(RPP1)
        .initialExchangeDate(null)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .paymentPeriods(RPP1)
        .paymentEvents(NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, 5000d)))
        .build();
    assertEquals(test.expand(), expected);
  }

  public void test_expand_createNotionalExchange_finalOnly() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .paymentPeriods(RPP1)
        .initialExchangeDate(null)
        .intermediateExchange(false)
        .finalExchange(true)
        .build();
    ExpandedSwapLeg expected = ExpandedSwapLeg.builder()
        .paymentPeriods(RPP1)
        .paymentEvents(NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, 5000d)))
        .build();
    assertEquals(test.expand(), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .paymentBusinessDayAdjustment(FOLLOWING_GBLO)
        .initialExchangeDate(DATE_2014_06_30)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    coverImmutableBean(test);
    RatePeriodSwapLeg test2 = RatePeriodSwapLeg.builder()
        .paymentPeriods(RPP2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertSerialization(test);
  }

}
