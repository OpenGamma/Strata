/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.swap.IborRateCalculation;

/**
 * Test {@link IborCapFloorTrade}.
 */
@Test
public class IborCapFloorTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
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
  private static final IborCapFloorLeg CAP_LEG = IborCapFloorLeg.builder()
      .calculation(RATE_CALCULATION)
      .capSchedule(CAP)
      .notional(NOTIONAL)
      .paymentDateOffset(PAYMENT_OFFSET)
      .paymentSchedule(SCHEDULE)
      .payReceive(RECEIVE)
      .build();
  private static final IborCapFloorLeg FLOOR_LEG = IborCapFloorLeg.builder()
      .calculation(RATE_CALCULATION)
      .floorSchedule(CAP)
      .notional(NOTIONAL)
      .paymentDateOffset(PAYMENT_OFFSET)
      .paymentSchedule(SCHEDULE)
      .payReceive(RECEIVE)
      .build();
  private static final IborCapFloor PRODUCT = IborCapFloor.of(CAP_LEG);
  private static final IborCapFloor PRODUCT_FLOOR = IborCapFloor.of(FLOOR_LEG);
  private static final AdjustablePayment PREMIUM =
      AdjustablePayment.of(CurrencyAmount.of(EUR, NOTIONAL_VALUE), LocalDate.of(2011, 3, 18));
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(LocalDate.of(2011, 3, 15))
      .build();

  //-------------------------------------------------------------------------
  public void test_builder_full() {
    IborCapFloorTrade test = sut();
    assertEquals(test.getPremium().get(), PREMIUM);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
  }

  public void test_builder_min() {
    IborCapFloorTrade test = IborCapFloorTrade.builder()
    .product(PRODUCT)
    .build();
    assertEquals(test.getPremium().isPresent(), false);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TradeInfo.empty());
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    IborCapFloorTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.IBOR_CAP_FLOOR)
        .currencies(Currency.EUR)
        .description("5Y EUR 1mm Rec EUR-EURIBOR-3M Cap 3.25% / Pay Premium : 17Mar11-17Mar16")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  public void test_summarize_floor() {
    IborCapFloorTrade trade = IborCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT_FLOOR)
        .build();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.IBOR_CAP_FLOOR)
        .currencies(Currency.EUR)
        .description("5Y EUR 1mm Rec EUR-EURIBOR-3M Floor 3.25% : 17Mar11-17Mar16")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    IborCapFloorTrade test = sut();
    ResolvedIborCapFloorTrade expected = ResolvedIborCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .premium(PREMIUM.resolve(REF_DATA))
        .build();
    assertEquals(test.resolve(REF_DATA), expected);
  }

  public void test_resolve_noPremium() {
    IborCapFloorTrade test = IborCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .build();
    ResolvedIborCapFloorTrade expected = ResolvedIborCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .build();
    assertEquals(test.resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborCapFloorTrade test1 = sut();
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
    IborCapFloorTrade test = sut();
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  IborCapFloorTrade sut() {
    return IborCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
  }

}
