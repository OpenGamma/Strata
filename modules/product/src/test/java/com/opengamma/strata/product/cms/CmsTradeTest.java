/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Test {@link CmsTrade}.
 */
@Test
public class CmsTradeTest {

  private static final ValueSchedule NOTIONAL = ValueSchedule.of(1.0e6);
  private static final SwapIndex INDEX = SwapIndices.EUR_EURIBOR_1100_10Y;
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2017, 10, 21);
  private static final Frequency FREQUENCY = Frequency.P12M;
  private static final BusinessDayAdjustment BUSS_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ, StubConvention.NONE, RollConventions.NONE);
  private static final ValueSchedule STRIKE = ValueSchedule.of(0.0125);

  private static final LocalDate TRADE = LocalDate.of(2015, 9, 21);
  private static final LocalDate SETTLE = LocalDate.of(2015, 9, 23);
  private static final TradeInfo INFO = TradeInfo.builder().tradeDate(TRADE).settlementDate(SETTLE).build();

  private static final Cms CAP = Cms.of(
      CmsLeg.builder()
          .capSchedule(STRIKE)
          .index(INDEX)
          .notional(NOTIONAL)
          .payReceive(RECEIVE)
          .paymentSchedule(SCHEDULE)
          .build());
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(EUR, -0.001 * 1.0e6), SETTLE);
  private static final Cms CMS = Cms.of(
      CmsLeg.builder()
          .capSchedule(STRIKE)
          .index(INDEX)
          .notional(NOTIONAL)
          .payReceive(RECEIVE)
          .paymentSchedule(SCHEDULE)
          .build(),
      RateCalculationSwapLeg.builder()
          .payReceive(PAY)
          .accrualSchedule(SCHEDULE)
          .calculation(FixedRateCalculation.of(0.01, ACT_360))
          .paymentSchedule(
              PaymentSchedule.builder().paymentFrequency(FREQUENCY).paymentDateOffset(DaysAdjustment.NONE).build())
          .notionalSchedule(NotionalSchedule.of(CurrencyAmount.of(EUR, 1.0e6)))
          .build());

  public void test_builder() {
    CmsTrade test = CmsTrade.builder().product(CAP).premium(PREMIUM).tradeInfo(INFO).build();
    assertEquals(test.getPremium().get(), PREMIUM);
    assertEquals(test.getProduct(), CAP);
    assertEquals(test.getTradeInfo(), INFO);
  }

  public void test_builder_noPrem() {
    CmsTrade test = CmsTrade.builder().product(CMS).tradeInfo(INFO).build();
    assertFalse(test.getPremium().isPresent());
    assertEquals(test.getProduct(), CMS);
    assertEquals(test.getTradeInfo(), INFO);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CmsTrade test1 = CmsTrade.builder().product(CAP).premium(PREMIUM).tradeInfo(INFO).build();
    coverImmutableBean(test1);
    CmsTrade test2 = CmsTrade.builder().product(CMS).tradeInfo(TradeInfo.EMPTY).build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    CmsTrade test = CmsTrade.builder().product(CAP).premium(PREMIUM).tradeInfo(INFO).build();
    assertSerialization(test);
  }

}
