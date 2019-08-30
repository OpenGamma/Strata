/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link EtdOptionTrade}.
 */
public class EtdOptionTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.of(LocalDate.of(2017, 1, 1));
  private static final EtdOptionSecurity SECURITY = EtdOptionSecurityTest.sut();

  @Test
  public void test_of() {
    EtdOptionTrade test = EtdOptionTrade.of(TRADE_INFO, SECURITY, 1000, 20);
    assertThat(test.getSecurity()).isEqualTo(SECURITY);
    assertThat(test.getQuantity()).isCloseTo(1000d, offset(0d));
    assertThat(test.getPrice()).isCloseTo(20d, offset(0d));
    assertThat(test.getSecurityId()).isEqualTo(SECURITY.getSecurityId());
    assertThat(test.getCurrency()).isEqualTo(SECURITY.getCurrency());
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
    assertThat(test.withPrice(129).getPrice()).isCloseTo(129d, offset(0d));
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
    EtdOptionTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.ETD_OPTION)
        .currencies(SECURITY.getCurrency())
        .description(SECURITY.getSecurityId().getStandardId().getValue() + " x 3000, Jun17 P2")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolveTarget() {
    GenericSecurity security = GenericSecurity.of(SECURITY.getInfo());
    Trade test = sut().resolveTarget(ImmutableReferenceData.of(SECURITY.getSecurityId(), security));
    GenericSecurityTrade expected = GenericSecurityTrade.of(TRADE_INFO, security, 3000, 20);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  static EtdOptionTrade sut() {
    return EtdOptionTrade.builder()
        .info(TRADE_INFO)
        .security(SECURITY)
        .quantity(3000)
        .price(20)
        .build();
  }

  static EtdOptionTrade sut2() {
    return EtdOptionTrade.builder()
        .security(EtdOptionSecurityTest.sut2())
        .quantity(4000)
        .price(30)
        .build();
  }

}
