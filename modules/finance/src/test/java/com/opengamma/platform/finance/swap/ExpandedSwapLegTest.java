/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.rate.IborRate;

/**
 * Test.
 */
@Test
public class ExpandedSwapLegTest {

  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final LocalDate DATE_2014_09_30 = date(2014, 9, 30);
  private static final LocalDate DATE_2014_10_01 = date(2014, 10, 1);
  private static final IborRate GBP_LIBOR_3M_2014_06_28 = IborRate.of(GBP_LIBOR_3M, date(2014, 6, 28));
  private static final NotionalExchange NOTIONAL_EXCHANGE =
      NotionalExchange.of(DATE_2014_10_01, CurrencyAmount.of(GBP, 2000d));
  private static final RateAccrualPeriod RAP = RateAccrualPeriod.builder()
      .startDate(DATE_2014_06_30)
      .endDate(DATE_2014_09_30)
      .yearFraction(0.25d)
      .rate(GBP_LIBOR_3M_2014_06_28)
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

  public void test_builder() {
    ExpandedSwapLeg test = ExpandedSwapLeg.builder()
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertEquals(test.getStartDate(), DATE_2014_06_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPaymentPeriods(), ImmutableList.of(RPP1));
    assertEquals(test.getPaymentEvents(), ImmutableList.of(NOTIONAL_EXCHANGE));
  }

  public void test_builder_invalidMixedCurrency() {
    assertThrowsIllegalArg(() -> ExpandedSwapLeg.builder()
        .paymentPeriods(RPP3)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedSwapLeg test = ExpandedSwapLeg.builder()
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    coverImmutableBean(test);
    ExpandedSwapLeg test2 = ExpandedSwapLeg.builder()
        .paymentPeriods(RPP2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ExpandedSwapLeg test = ExpandedSwapLeg.builder()
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertSerialization(test);
  }

}
