/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link CapitalIndexedBondTrade}.
 */
@Test
public class CapitalIndexedBondTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double QUANTITY = 10;
  private static final double QUANTITY2 = 20;
  private static final double PRICE = 0.995;
  private static final double PRICE2 = 0.9;
  private static final BusinessDayAdjustment SCHEDULE_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY);
  private static final CapitalIndexedBond PRODUCT = CapitalIndexedBondTest.sut();
  private static final CapitalIndexedBond PRODUCT1 = CapitalIndexedBondTest.sut1();
  private static final CapitalIndexedBond PRODUCT2 = CapitalIndexedBondTest.sut2();
  private static final LocalDate START = PRODUCT.getAccrualSchedule().getStartDate();
  private static final LocalDate TRADE = START.plusDays(7);
  private static final LocalDate SETTLEMENT_DATE = SCHEDULE_ADJ.adjust(TRADE, REF_DATA);
  private static final TradeInfo TRADE_INFO =
      TradeInfo.builder().tradeDate(TRADE).settlementDate(SETTLEMENT_DATE).build();
  private static final TradeInfo TRADE_INFO_EARLY =
      TradeInfo.builder().tradeDate(date(2008, 1, 1)).settlementDate(date(2008, 1, 1)).build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    CapitalIndexedBondTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
  }

  //-------------------------------------------------------------------------
  private static final CapitalIndexedBondPaymentPeriod SETTLEMENT = CapitalIndexedBondPaymentPeriod.builder()
      .startDate(SCHEDULE_ADJ.adjust(START, REF_DATA))
      .unadjustedStartDate(START)
      .endDate(SETTLEMENT_DATE)
      .currency(USD)
      .rateComputation(PRODUCT.getRateCalculation().createRateComputation(SETTLEMENT_DATE))
      .notional(
          -PRODUCT.getNotional() * QUANTITY *
              (PRICE + PRODUCT.resolve(REF_DATA).accruedInterest(SETTLEMENT_DATE) / PRODUCT.getNotional()))
      .realCoupon(1d)
      .build();

  private static final KnownAmountBondPaymentPeriod SETTLEMENT1 = KnownAmountBondPaymentPeriod
      .builder()
      .startDate(SCHEDULE_ADJ.adjust(START, REF_DATA))
      .unadjustedStartDate(START)
      .endDate(SETTLEMENT_DATE)
      .payment(
          Payment.of(USD, -PRODUCT1.getNotional() * QUANTITY *
              (PRICE + PRODUCT1.resolve(REF_DATA).accruedInterest(SETTLEMENT_DATE) / PRODUCT1.getNotional()),
              SETTLEMENT_DATE))
      .build();

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedCapitalIndexedBondTrade test = sut().resolve(REF_DATA);
    ResolvedCapitalIndexedBondTrade expected = ResolvedCapitalIndexedBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .price(PRICE)
        .settlement(SETTLEMENT)
        .build();
    assertEquals(test, expected);
  }

  public void test_resolve1() {
    ResolvedCapitalIndexedBondTrade test = sut1().resolve(REF_DATA);
    ResolvedCapitalIndexedBondTrade expected = ResolvedCapitalIndexedBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT1.resolve(REF_DATA))
        .quantity(QUANTITY)
        .price(PRICE)
        .settlement(SETTLEMENT1)
        .build();
    assertEquals(test, expected);
  }

  public void test_resolve_invalid() {
    CapitalIndexedBondTrade test = sut().toBuilder().info(TRADE_INFO_EARLY).build();
    assertThrowsIllegalArg(() -> test.resolve(REF_DATA));
  }

  public void test_resolve_noTradeOrSettlementDate() {
    CapitalIndexedBondTrade test = CapitalIndexedBondTrade.builder()
        .info(TradeInfo.empty())
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertThrows(() -> test.resolve(REF_DATA), IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static CapitalIndexedBondTrade sut() {
    return CapitalIndexedBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static CapitalIndexedBondTrade sut1() {
    return CapitalIndexedBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT1)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static CapitalIndexedBondTrade sut2() {
    return CapitalIndexedBondTrade.builder()
        .info(TradeInfo.builder().tradeDate(START.plusDays(7)).build())
        .product(PRODUCT2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
