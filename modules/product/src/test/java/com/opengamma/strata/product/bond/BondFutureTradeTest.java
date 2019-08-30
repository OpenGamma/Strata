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
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.TradedPrice;

/**
 * Test {@link BondFutureTrade}.
 */
public class BondFutureTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // future
  private static final BondFuture FUTURE = BondFutureTest.sut();
  private static final BondFuture FUTURE2 = BondFutureTest.sut2();
  // trade
  private static final LocalDate TRADE_DATE = date(2011, 6, 20);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(TRADE_DATE);
  private static final TradeInfo TRADE_INFO2 = TradeInfo.of(date(2016, 7, 1));
  private static final double QUANTITY = 1234L;
  private static final double QUANTITY2 = 100L;
  private static final double PRICE = 1.2345;
  private static final double PRICE2 = 1.3;

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    BondFutureTrade test = sut();
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getProduct()).isEqualTo(FUTURE);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getPrice()).isEqualTo(PRICE);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withPrice(129).getPrice()).isCloseTo(129d, offset(0d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    BondFutureTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.BOND_FUTURE)
        .currencies(Currency.USD)
        .description("BondFuture x 1234")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    ResolvedBondFutureTrade expected = ResolvedBondFutureTrade.builder()
        .info(TRADE_INFO)
        .product(FUTURE.resolve(REF_DATA))
        .quantity(QUANTITY)
        .tradedPrice(TradedPrice.of(TRADE_INFO.getTradeDate().get(), PRICE))
        .build();
    assertThat(sut().resolve(REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withQuantity() {
    BondFutureTrade base = sut();
    double quantity = 366d;
    BondFutureTrade computed = base.withQuantity(quantity);
    BondFutureTrade expected = BondFutureTrade.builder()
        .info(TRADE_INFO)
        .product(FUTURE)
        .quantity(quantity)
        .price(PRICE)
        .build();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_withPrice() {
    BondFutureTrade base = sut();
    double price = 1.5d;
    BondFutureTrade computed = base.withPrice(price);
    BondFutureTrade expected = BondFutureTrade.builder()
        .info(TRADE_INFO)
        .product(FUTURE)
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

  static BondFutureTrade sut() {
    return BondFutureTrade.builder()
        .info(TRADE_INFO)
        .product(FUTURE)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static BondFutureTrade sut2() {
    return BondFutureTrade.builder()
        .info(TRADE_INFO2)
        .product(FUTURE2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
