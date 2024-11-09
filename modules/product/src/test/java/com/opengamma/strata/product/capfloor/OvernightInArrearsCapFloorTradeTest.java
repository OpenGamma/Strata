/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_ESTR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

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
import com.opengamma.strata.product.swap.OvernightRateCalculation;

/**
 * Test {@link OvernightInArrearsCapFloorTrade}.
 */
public class OvernightInArrearsCapFloorTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate START = LocalDate.of(2011, 3, 17);
  private static final LocalDate END = LocalDate.of(2016, 3, 17);
  private static final OvernightRateCalculation RATE_CALCULATION = OvernightRateCalculation.of(EUR_ESTR);
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
  private static final OvernightInArrearsCapFloorLeg CAP_LEG = OvernightInArrearsCapFloorLeg.builder()
      .calculation(RATE_CALCULATION)
      .capSchedule(CAP)
      .notional(NOTIONAL)
      .paymentDateOffset(PAYMENT_OFFSET)
      .paymentSchedule(SCHEDULE)
      .payReceive(RECEIVE)
      .build();
  private static final OvernightInArrearsCapFloorLeg FLOOR_LEG = OvernightInArrearsCapFloorLeg.builder()
      .calculation(RATE_CALCULATION)
      .floorSchedule(CAP)
      .notional(NOTIONAL)
      .paymentDateOffset(PAYMENT_OFFSET)
      .paymentSchedule(SCHEDULE)
      .payReceive(RECEIVE)
      .build();
  private static final OvernightInArrearsCapFloor PRODUCT = OvernightInArrearsCapFloor.of(CAP_LEG);
  private static final OvernightInArrearsCapFloor PRODUCT_FLOOR = OvernightInArrearsCapFloor.of(FLOOR_LEG);
  private static final AdjustablePayment PREMIUM =
      AdjustablePayment.of(CurrencyAmount.of(EUR, NOTIONAL_VALUE), LocalDate.of(2011, 3, 18));
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(LocalDate.of(2011, 3, 15))
      .build();

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_full() {
    OvernightInArrearsCapFloorTrade test = sut();
    assertThat(test.getPremium().get()).isEqualTo(PREMIUM);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder_min() {
    OvernightInArrearsCapFloorTrade test = OvernightInArrearsCapFloorTrade.builder()
        .product(PRODUCT)
        .build();
    assertThat(test.getPremium()).isNotPresent();
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getInfo()).isEqualTo(TradeInfo.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    OvernightInArrearsCapFloorTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.OVERNIGHT_IN_ARREARS_CAP_FLOOR)
        .currencies(Currency.EUR)
        .description("5Y EUR 1mm Rec Compounded EUR-ESTR Cap 3.25% / Pay Premium : 17Mar11-17Mar16")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  @Test
  public void test_summarize_floor() {
    OvernightInArrearsCapFloorTrade trade = OvernightInArrearsCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT_FLOOR)
        .build();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.OVERNIGHT_IN_ARREARS_CAP_FLOOR)
        .currencies(Currency.EUR)
        .description("5Y EUR 1mm Rec Compounded EUR-ESTR Floor 3.25% : 17Mar11-17Mar16")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    OvernightInArrearsCapFloorTrade test = sut();
    ResolvedOvernightInArrearsCapFloorTrade expected = ResolvedOvernightInArrearsCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .premium(PREMIUM.resolve(REF_DATA))
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_resolve_noPremium() {
    OvernightInArrearsCapFloorTrade test = OvernightInArrearsCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .build();
    ResolvedOvernightInArrearsCapFloorTrade expected = ResolvedOvernightInArrearsCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightInArrearsCapFloorTrade test1 = sut();
    coverImmutableBean(test1);
    OvernightInArrearsCapFloor product = OvernightInArrearsCapFloor.of(
        OvernightInArrearsCapFloorLeg.builder()
            .calculation(RATE_CALCULATION)
            .floorSchedule(CAP)
            .notional(NOTIONAL)
            .paymentDateOffset(PAYMENT_OFFSET)
            .paymentSchedule(SCHEDULE)
            .payReceive(PAY)
            .build());
    OvernightInArrearsCapFloorTrade test2 = OvernightInArrearsCapFloorTrade.builder()
        .product(product)
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    OvernightInArrearsCapFloorTrade test = sut();
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  OvernightInArrearsCapFloorTrade sut() {
    return OvernightInArrearsCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
  }

}
