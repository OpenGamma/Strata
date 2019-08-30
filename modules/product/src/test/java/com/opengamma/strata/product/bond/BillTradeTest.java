/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link BillTrade}.
 */
public class BillTradeTest {

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
  private static final double QUANTITY = 123;
  private static final double YIELD = 0.0123;
  private static final double PRICE = 0.9877;
  private static final double PRICE2 = 0.975;
  private static final Bill PRODUCT = BillTest.US_BILL;
  private static final Bill PRODUCT2 = BillTest.BILL_2;
  private static final double TOLERANCE_PRICE = 1.0E-8;

  //-------------------------------------------------------------------------
  @Test
  public void test_ofYield() {
    BillTrade test = sut_yield();
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    double price = 1.0d -
        YIELD * PRODUCT.getDayCount().relativeYearFraction(SETTLEMENT_DATE, PRODUCT.getNotional().getDate().getUnadjusted());
    assertThat(test.getPrice()).isCloseTo(price, offset(TOLERANCE_PRICE));
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withPrice(129).getPrice()).isCloseTo(129d, offset(0d));
  }

  @Test
  public void test_ofPrice() {
    BillTrade test = sut_price();
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getPrice()).isEqualTo(PRICE);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withPrice(129).getPrice()).isCloseTo(129d, offset(0d));
  }

  @Test
  public void test_builder_price() {
    BillTrade test = sut_price();
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getPrice()).isCloseTo(PRICE, offset(TOLERANCE_PRICE));
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withPrice(129).getPrice()).isCloseTo(129d, offset(0d));
  }

  @Test
  public void test_price() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BillTrade.builder()
            .info(TRADE_INFO)
            .product(PRODUCT)
            .quantity(QUANTITY)
            .build());
  }

  @Test
  public void test_settle_or_trade() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BillTrade.builder()
            .info(TradeInfo.empty())
            .product(PRODUCT)
            .quantity(QUANTITY)
            .price(PRICE)
            .build());
  }

  @Test
  public void test_of_yield_settledate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BillTrade.ofYield(TradeInfo.builder().tradeDate(TRADE_DATE).build(), PRODUCT, QUANTITY, YIELD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    BillTrade trade = sut_yield();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.BILL)
        .currencies(Currency.USD)
        .description("Bill2019-05-23 x 123")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    Payment settle = Payment
        .of(PRODUCT.getNotional().getValue().multipliedBy(-PRICE * QUANTITY), SETTLEMENT_DATE);
    ResolvedBillTrade expected = ResolvedBillTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .settlement(settle)
        .build();
    assertThat(sut_price().resolve(REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withQuantity() {
    BillTrade base = sut_price();
    double quantity = 75343d;
    BillTrade computed = base.withQuantity(quantity);
    BillTrade expected = BillTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(quantity)
        .price(PRICE)
        .build();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_withPrice() {
    BillTrade base = sut_yield();
    double price = 135d;
    BillTrade computed = base.withPrice(price);
    BillTrade expected = BillTrade.builder()
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
    coverImmutableBean(sut_yield());
    coverBeanEquals(sut_yield(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut_yield());
  }

  //-------------------------------------------------------------------------
  static BillTrade sut_yield() {
    return BillTrade.ofYield(TRADE_INFO, PRODUCT, QUANTITY, YIELD);
  }

  static BillTrade sut_price() {
    return BillTrade.ofPrice(TRADE_INFO, PRODUCT, QUANTITY, PRICE);
  }

  static BillTrade sut2() {
    return BillTrade.builder()
        .info(TRADE_INFO2)
        .product(PRODUCT2)
        .quantity(100L)
        .price(PRICE2)
        .build();
  }

}
