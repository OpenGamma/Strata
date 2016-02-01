/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.swap.IborRateCalculation;

/**
 * Test {@link IborCapFloorTrade}.
 */
@Test
public class IborCapFloorTradeTest {

  private static final LocalDate START = LocalDate.of(2011, 3, 17);
  private static final LocalDate END = LocalDate.of(2016, 3, 17);
  private static final IborRateCalculation RATE_CALCULATION = IborRateCalculation.of(EUR_EURIBOR_3M);
  private static final Frequency FREQUENCY = Frequency.P3M;
  private static final BusinessDayAdjustment BUSS_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE = PeriodicSchedule.builder()
      .startDate(START)
      .endDate(END)
      .frequency(FREQUENCY)
      .businessDayAdjustment(BUSS_ADJ)
      .build();
  private static final DaysAdjustment PAYMENT_OFFSET = DaysAdjustment.ofBusinessDays(2, EUTA);
  private static final ValueSchedule CAP = ValueSchedule.of(0.0325);
  private static final double NOTIONAL_VALUE = 1.0e6;
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE);
  private static final IborCapFloorLeg CAPFLOOR_LEG = IborCapFloorLeg.builder()
      .calculation(RATE_CALCULATION)
      .capSchedule(CAP)
      .notional(NOTIONAL)
      .paymentDateOffset(PAYMENT_OFFSET)
      .paymentSchedule(SCHEDULE)
      .payReceive(RECEIVE)
      .build();
  private static final IborCapFloor PRODUCT = IborCapFloor.of(CAPFLOOR_LEG);
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(EUR, NOTIONAL_VALUE), LocalDate.of(2011, 3, 18));
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(LocalDate.of(2011, 3, 15))
      .build();

  public void test_builder_full() {
    IborCapFloorTrade test = IborCapFloorTrade.builder()
        .premium(PREMIUM)
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT)
        .build();
    assertEquals(test.getPremium().get(), PREMIUM);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  public void test_builder_min() {
    IborCapFloorTrade test = IborCapFloorTrade.builder()
        .product(PRODUCT)
        .build();
    assertEquals(test.getPremium().isPresent(), false);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborCapFloorTrade test1 = IborCapFloorTrade.builder()
        .premium(PREMIUM)
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT)
        .build();
    coverImmutableBean(test1);
    IborCapFloor product = IborCapFloor.of(
        IborCapFloorLeg.builder()
            .calculation(RATE_CALCULATION)
            .floorSchedule(CAP)
            .notional(NOTIONAL)
            .paymentDateOffset(PAYMENT_OFFSET)
            .paymentSchedule(SCHEDULE)
            .payReceive(PAY)
            .build());
    IborCapFloorTrade test2 = IborCapFloorTrade.builder()
        .product(product)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    IborCapFloorTrade test = IborCapFloorTrade.builder()
        .premium(PREMIUM)
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT)
        .build();
    assertSerialization(test);
  }

}
