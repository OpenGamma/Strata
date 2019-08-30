/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

/**
 * Test {@link GenericSecurityTrade}.
 */
public class GenericSecurityTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2016, 6, 30));
  private static final GenericSecurity SECURITY = GenericSecurityTest.sut();
  private static final GenericSecurity SECURITY2 = GenericSecurityTest.sut2();
  private static final double QUANTITY = 100;
  private static final double QUANTITY2 = 200;
  private static final double PRICE = 123.50;
  private static final double PRICE2 = 120.50;

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    GenericSecurityTrade test = GenericSecurityTrade.of(TRADE_INFO, SECURITY, QUANTITY, PRICE);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getPrice()).isEqualTo(PRICE);
    assertThat(test.getProduct()).isEqualTo(SECURITY);
    assertThat(test.getCurrency()).isEqualTo(SECURITY.getCurrency());
    assertThat(test.getSecurityId()).isEqualTo(SECURITY.getSecurityId());
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withPrice(129).getPrice()).isCloseTo(129d, offset(0d));
  }

  @Test
  public void test_builder() {
    GenericSecurityTrade test = sut();
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getPrice()).isEqualTo(PRICE);
    assertThat(test.getCurrency()).isEqualTo(SECURITY.getCurrency());
    assertThat(test.getSecurityId()).isEqualTo(SECURITY.getSecurityId());
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
  @Test
  public void test_summarize() {
    GenericSecurityTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.SECURITY)
        .currencies(SECURITY.getCurrency())
        .description("1 x 100")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  static GenericSecurityTrade sut() {
    return GenericSecurityTrade.builder()
        .info(TRADE_INFO)
        .security(SECURITY)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static GenericSecurityTrade sut2() {
    return GenericSecurityTrade.builder()
        .info(TradeInfo.empty())
        .security(SECURITY2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
