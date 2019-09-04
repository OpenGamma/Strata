/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link GenericSecurity}.
 */
public class GenericSecurityTest {

  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(GBP, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(SecurityId.of("Test", "1"), PRICE_INFO);
  private static final SecurityInfo INFO2 = SecurityInfo.of(SecurityId.of("Test", "2"), PRICE_INFO);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    GenericSecurity test = sut();
    assertThat(test.getInfo()).isEqualTo(INFO);
    assertThat(test.getSecurityId()).isEqualTo(INFO.getId());
    assertThat(test.getCurrency()).isEqualTo(INFO.getPriceInfo().getCurrency());
    assertThat(test.getUnderlyingIds()).isEmpty();
    assertThat(test).isEqualTo(GenericSecurity.of(INFO));
    assertThat(test.createProduct(ReferenceData.empty())).isEqualTo(test);
    assertThat(test.createTrade(TradeInfo.empty(), 1, 2, ReferenceData.empty()))
        .isEqualTo(GenericSecurityTrade.of(TradeInfo.empty(), GenericSecurity.of(INFO), 1, 2));
    assertThat(test.createPosition(PositionInfo.empty(), 1, ReferenceData.empty()))
        .isEqualTo(GenericSecurityPosition.ofNet(PositionInfo.empty(), GenericSecurity.of(INFO), 1));
    assertThat(test.createPosition(PositionInfo.empty(), 1, 2, ReferenceData.empty()))
        .isEqualTo(GenericSecurityPosition.ofLongShort(PositionInfo.empty(), GenericSecurity.of(INFO), 1, 2));
  }

  @Test
  public void test_withInfo() {
    GenericSecurity base = sut();
    assertThat(base.getInfo()).isEqualTo(INFO);
    GenericSecurity test = base.withInfo(INFO2);
    assertThat(test.getInfo()).isEqualTo(INFO2);
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
  static GenericSecurity sut() {
    return GenericSecurity.of(INFO);
  }

  static GenericSecurity sut2() {
    return GenericSecurity.of(INFO2);
  }

}
