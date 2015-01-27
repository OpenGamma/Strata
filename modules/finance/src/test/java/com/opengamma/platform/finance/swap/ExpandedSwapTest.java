/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.observation.IborRateObservation;

/**
 * Test.
 */
@Test
public class ExpandedSwapTest {

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
      .currency(USD)
      .notional(6000d)
      .build();
  private static final ExpandedSwapLeg LEG1 = ExpandedSwapLeg.builder()
      .paymentPeriods(RPP1)
      .paymentEvents(NOTIONAL_EXCHANGE)
      .build();
  private static final ExpandedSwapLeg LEG2 = ExpandedSwapLeg.builder()
      .paymentPeriods(RPP2)
      .build();

  public void test_of() {
    ExpandedSwap test = ExpandedSwap.of(LEG1);
    assertEquals(test.getLegs(), ImmutableSet.of(LEG1));
    assertEquals(test.getPaymentEvents(), ImmutableList.of());
    assertEquals(test.isCrossCurrency(), false);
  }

  public void test_of_crossCurrency() {
    ExpandedSwap test = ExpandedSwap.of(LEG1, LEG2);
    assertEquals(test.getLegs(), ImmutableSet.of(LEG1, LEG2));
    assertEquals(test.getPaymentEvents(), ImmutableList.of());
    assertEquals(test.isCrossCurrency(), true);
  }

  public void test_builder() {
    ExpandedSwap test = ExpandedSwap.builder()
        .legs(LEG1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertEquals(test.getLegs(), ImmutableSet.of(LEG1));
    assertEquals(test.getPaymentEvents(), ImmutableList.of(NOTIONAL_EXCHANGE));
    assertEquals(test.isCrossCurrency(), false);
  }

  public void test_expand() {
    ExpandedSwap test = ExpandedSwap.builder()
        .legs(LEG1)
        .build();
    assertSame(test.expand(), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedSwap test = ExpandedSwap.builder()
        .legs(LEG1)
        .build();
    coverImmutableBean(test);
    ExpandedSwap test2 = ExpandedSwap.builder()
        .legs(LEG2)
        .paymentEvents(ImmutableList.of(NOTIONAL_EXCHANGE))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ExpandedSwap test = ExpandedSwap.builder()
        .legs(LEG1)
        .build();
    assertSerialization(test);
  }

}
