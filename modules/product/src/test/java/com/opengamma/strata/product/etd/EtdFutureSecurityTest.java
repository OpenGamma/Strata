/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
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
@Test
public class EtdFutureSecurityTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void test() {
    EtdFutureSecurity test = sut();
    assertEquals(test.getVariant(), EtdVariant.MONTHLY);
    assertEquals(test.getType(), EtdType.FUTURE);
    assertEquals(test.getCurrency(), Currency.GBP);
    assertEquals(test.getUnderlyingIds(), ImmutableSet.of());
    assertEquals(test.createProduct(REF_DATA), test);
    assertEquals(
        test.createTrade(TradeInfo.empty(), 1, 2, ReferenceData.empty()),
        EtdFutureTrade.of(TradeInfo.empty(), test, 1, 2));
    assertEquals(
        test.createPosition(PositionInfo.empty(), 1, ReferenceData.empty()),
        EtdFuturePosition.ofNet(PositionInfo.empty(), test, 1));
    assertEquals(
        test.createPosition(PositionInfo.empty(), 1, 2, ReferenceData.empty()),
        EtdFuturePosition.ofLongShort(PositionInfo.empty(), test, 1, 2));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  public void test_summaryDescription() {
    assertEquals(sut().summaryDescription(), "Jun17");
    assertEquals(sut2().summaryDescription(), "W2Sep17");
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
