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

import com.opengamma.strata.basics.ImmutableReferenceData;

/**
 * Test {@link SecurityTrade}.
 */
public class SecurityTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2016, 6, 30));
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "Id");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "Id2");
  private static final double QUANTITY = 100;
  private static final double QUANTITY2 = 200;
  private static final double PRICE = 123.50;
  private static final double PRICE2 = 120.50;

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SecurityTrade test = SecurityTrade.of(TRADE_INFO, SECURITY_ID, QUANTITY, PRICE);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getPrice()).isEqualTo(PRICE);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withPrice(129).getPrice()).isCloseTo(129d, offset(0d));
  }

  @Test
  public void test_builder() {
    SecurityTrade test = sut();
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getPrice()).isEqualTo(PRICE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    SecurityTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.SECURITY)
        .description("Id x 100")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolveTarget() {
    GenericSecurity security = GenericSecurityTest.sut();
    Trade test = sut().resolveTarget(ImmutableReferenceData.of(SECURITY_ID, security));
    GenericSecurityTrade expected = GenericSecurityTrade.of(TRADE_INFO, security, QUANTITY, PRICE);
    assertThat(test).isEqualTo(expected);
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
  static SecurityTrade sut() {
    return SecurityTrade.builder()
        .info(TRADE_INFO)
        .securityId(SECURITY_ID)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static SecurityTrade sut2() {
    return SecurityTrade.builder()
        .info(TradeInfo.empty())
        .securityId(SECURITY_ID2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
