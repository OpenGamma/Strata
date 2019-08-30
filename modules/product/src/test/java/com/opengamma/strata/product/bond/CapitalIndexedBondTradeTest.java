/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link CapitalIndexedBondTrade}.
 */
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
  @Test
  public void test_builder() {
    CapitalIndexedBondTrade test = sut();
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getPrice()).isEqualTo(PRICE);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withPrice(129).getPrice()).isCloseTo(129d, offset(0d));
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
  @Test
  public void test_summarize() {
    CapitalIndexedBondTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.BOND)
        .currencies(Currency.USD)
        .description("Bond x 10")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    ResolvedCapitalIndexedBondTrade test = sut().resolve(REF_DATA);
    ResolvedCapitalIndexedBondTrade expected = ResolvedCapitalIndexedBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .settlement(ResolvedCapitalIndexedBondSettlement.of(SETTLEMENT_DATE, PRICE, SETTLEMENT))
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_resolve1() {
    ResolvedCapitalIndexedBondTrade test = sut1().resolve(REF_DATA);
    ResolvedCapitalIndexedBondTrade expected = ResolvedCapitalIndexedBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT1.resolve(REF_DATA))
        .quantity(QUANTITY)
        .settlement(ResolvedCapitalIndexedBondSettlement.of(SETTLEMENT_DATE, PRICE, SETTLEMENT1))
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_resolve_invalid() {
    CapitalIndexedBondTrade test = sut().toBuilder().info(TRADE_INFO_EARLY).build();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.resolve(REF_DATA));
  }

  @Test
  public void test_resolve_noTradeOrSettlementDate() {
    CapitalIndexedBondTrade test = CapitalIndexedBondTrade.builder()
        .info(TradeInfo.empty())
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertThatIllegalStateException()
        .isThrownBy(() -> test.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withQuantity() {
    CapitalIndexedBondTrade base = sut();
    double quantity = 3456d;
    CapitalIndexedBondTrade computed = base.withQuantity(quantity);
    CapitalIndexedBondTrade expected = CapitalIndexedBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(quantity)
        .price(PRICE)
        .build();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_withPrice() {
    CapitalIndexedBondTrade base = sut();
    double price = 0.95;
    CapitalIndexedBondTrade computed = base.withPrice(price);
    CapitalIndexedBondTrade expected = CapitalIndexedBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(price)
        .build();
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
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
