/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.TradedPrice;

/**
 * Test {@link IborFutureTrade}.
 */
public class IborFutureTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate TRADE_DATE = date(2015, 3, 18);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(TRADE_DATE);
  private static final IborFuture PRODUCT = IborFutureTest.sut();
  private static final IborFuture PRODUCT2 = IborFutureTest.sut2();
  private static final double QUANTITY = 35;
  private static final double QUANTITY2 = 36;
  private static final double PRICE = 0.99;
  private static final double PRICE2 = 0.98;

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    IborFutureTrade test = sut();
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getPrice()).isEqualTo(PRICE);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withQuantity(0.9129).getQuantity()).isCloseTo(0.9129d, offset(1e-10));
    assertThat(test.withPrice(0.9129).getPrice()).isCloseTo(0.9129d, offset(1e-10));
  }

  @Test
  public void test_builder_badPrice() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> sut().toBuilder().price(2.1).build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    IborFutureTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.IBOR_FUTURE)
        .currencies(Currency.USD)
        .description("IborFuture x 35")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    IborFutureTrade test = sut();
    ResolvedIborFutureTrade resolved = test.resolve(REF_DATA);
    assertThat(resolved.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(resolved.getProduct()).isEqualTo(PRODUCT.resolve(REF_DATA));
    assertThat(resolved.getQuantity()).isEqualTo(QUANTITY);
    assertThat(resolved.getTradedPrice()).isEqualTo(Optional.of(TradedPrice.of(TRADE_DATE, PRICE)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withQuantity() {
    IborFutureTrade base = sut();
    double quantity = 65243;
    IborFutureTrade computed = base.withQuantity(quantity);
    IborFutureTrade expected = IborFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(quantity)
        .price(PRICE)
        .build();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_withPrice() {
    IborFutureTrade base = sut();
    double price = 0.95;
    IborFutureTrade computed = base.withPrice(price);
    IborFutureTrade expected = IborFutureTrade.builder()
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
  static IborFutureTrade sut() {
    return IborFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static IborFutureTrade sut2() {
    return IborFutureTrade.builder()
        .product(PRODUCT2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
