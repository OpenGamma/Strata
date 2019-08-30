/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link FixedCouponBondTrade}.
 */
public class FixedCouponBondTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate TRADE_DATE = date(2015, 3, 25);
  private static final LocalDate SETTLEMENT_DATE = date(2015, 3, 30);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .settlementDate(SETTLEMENT_DATE)
      .build();
  private static final TradeInfo TRADE_INFO2 = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .build();
  private static final double QUANTITY = 10;
  private static final double PRICE = 123;
  private static final double PRICE2 = 200;
  private static final FixedCouponBond PRODUCT = FixedCouponBondTest.sut();
  private static final FixedCouponBond PRODUCT2 = FixedCouponBondTest.sut2();

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_resolved() {
    FixedCouponBondTrade test = sut();
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getPrice()).isEqualTo(PRICE);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withPrice(129).getPrice()).isCloseTo(129d, offset(0d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    FixedCouponBondTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.BOND)
        .currencies(Currency.EUR)
        .description("Bond x 10")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    ResolvedFixedCouponBondTrade expected = ResolvedFixedCouponBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT_DATE, PRICE))
        .build();
    assertThat(sut().resolve(REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_resolve_noTradeOrSettlementDate() {
    FixedCouponBondTrade test = FixedCouponBondTrade.builder()
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
    FixedCouponBondTrade base = sut();
    double quantity = 75343d;
    FixedCouponBondTrade computed = base.withQuantity(quantity);
    FixedCouponBondTrade expected = FixedCouponBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(quantity)
        .price(PRICE)
        .build();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_withPrice() {
    FixedCouponBondTrade base = sut();
    double price = 135d;
    FixedCouponBondTrade computed = base.withPrice(price);
    FixedCouponBondTrade expected = FixedCouponBondTrade.builder()
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
  static FixedCouponBondTrade sut() {
    return FixedCouponBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static FixedCouponBondTrade sut2() {
    return FixedCouponBondTrade.builder()
        .info(TRADE_INFO2)
        .product(PRODUCT2)
        .quantity(100L)
        .price(PRICE2)
        .build();
  }

}
