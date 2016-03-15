/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.equity;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.product.SecurityInfoType;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link EquitySecurity}.
 */
@Test
public class EquitySecurityTest {

  private static final Equity PRODUCT = EquityTest.sut();
  private static final Equity PRODUCT2 = EquityTest.sut2();
  private static final ImmutableMap<SecurityInfoType<?>, Object> INFO_MAP = ImmutableMap.of(SecurityInfoType.NAME, "Test");

  //-------------------------------------------------------------------------
  public void test_builder() {
    EquitySecurity test = sut();
    assertEquals(test.getInfo(), INFO_MAP);
    assertEquals(test.getInfo(SecurityInfoType.NAME), "Test");
    assertEquals(test.findInfo(SecurityInfoType.NAME), Optional.of("Test"));
    assertEquals(test.getSecurityId(), PRODUCT.getSecurityId());
    assertEquals(test.getCurrency(), PRODUCT.getCurrency());
    assertEquals(test.getUnderlyingIds(), ImmutableSet.of());
    assertEquals(sut2().findInfo(SecurityInfoType.NAME), Optional.empty());
    assertThrowsIllegalArg(() -> sut2().getInfo(SecurityInfoType.NAME));
  }

  //-------------------------------------------------------------------------
  public void test_createProduct() {
    EquitySecurity test = sut();
    assertEquals(test.createProduct(ReferenceData.empty()), PRODUCT);
    TradeInfo tradeInfo = TradeInfo.builder().tradeDate(date(2016, 6, 30)).build();
    EquityTrade expectedTrade = EquityTrade.builder()
        .tradeInfo(tradeInfo)
        .product(PRODUCT)
        .quantity(100)
        .price(123.50)
        .build();
    assertEquals(test.createTrade(date(2016, 6, 30), 100, 123.50, ReferenceData.empty()), expectedTrade);
    assertEquals(test.createTrade(tradeInfo, 100, 123.50, ReferenceData.empty()), expectedTrade);
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
  static EquitySecurity sut() {
    return EquitySecurity.builder()
        .product(PRODUCT)
        .info(INFO_MAP)
        .build();
  }

  static EquitySecurity sut2() {
    return EquitySecurity.builder()
        .product(PRODUCT2)
        .build();
  }

}
