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

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link EtdFutureSecurity}.
 */
public class EtdFutureSecurityTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @Test
  public void test() {
    EtdFutureSecurity test = sut();
    assertThat(test.getVariant()).isEqualTo(EtdVariant.MONTHLY);
    assertThat(test.getType()).isEqualTo(EtdType.FUTURE);
    assertThat(test.getCurrency()).isEqualTo(Currency.GBP);
    assertThat(test.getUnderlyingIds()).isEmpty();
    assertThat(test.createProduct(REF_DATA)).isEqualTo(test);
    assertThat(test.createTrade(TradeInfo.empty(), 1, 2, ReferenceData.empty()))
        .isEqualTo(EtdFutureTrade.of(TradeInfo.empty(), test, 1, 2));
    assertThat(test.createPosition(PositionInfo.empty(), 1, ReferenceData.empty()))
        .isEqualTo(EtdFuturePosition.ofNet(PositionInfo.empty(), test, 1));
    assertThat(test.createPosition(PositionInfo.empty(), 1, 2, ReferenceData.empty()))
        .isEqualTo(EtdFuturePosition.ofLongShort(PositionInfo.empty(), test, 1, 2));
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
  public void test_summaryDescription() {
    assertThat(sut().summaryDescription()).isEqualTo("Jun17");
    assertThat(sut2().summaryDescription()).isEqualTo("W2Sep17");
  }

  //-------------------------------------------------------------------------
  static EtdFutureSecurity sut() {
    return EtdFutureSecurity.builder()
        .info(SecurityInfo.of(SecurityId.of("A", "B"), SecurityPriceInfo.of(Currency.GBP, 100)))
        .contractSpecId(EtdContractSpecId.of("test", "123"))
        .expiry(YearMonth.of(2017, 6))
        .build();
  }

  static EtdFutureSecurity sut2() {
    return EtdFutureSecurity.builder()
        .info(SecurityInfo.of(SecurityId.of("B", "C"), SecurityPriceInfo.of(Currency.EUR, 10)))
        .contractSpecId(EtdContractSpecId.of("test", "234"))
        .expiry(YearMonth.of(2017, 9))
        .variant(EtdVariant.ofWeekly(2))
        .build();
  }

}
