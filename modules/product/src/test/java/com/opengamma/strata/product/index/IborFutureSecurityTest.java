/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link IborFutureSecurity}.
 */
public class IborFutureSecurityTest {

  private static final IborFuture PRODUCT = IborFutureTest.sut();
  private static final IborFuture PRODUCT2 = IborFutureTest.sut2();
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(GBP, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(PRODUCT.getSecurityId(), PRICE_INFO);
  private static final SecurityInfo INFO2 = SecurityInfo.of(PRODUCT2.getSecurityId(), PRICE_INFO);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    IborFutureSecurity test = sut();
    assertThat(test.getInfo()).isEqualTo(INFO);
    assertThat(test.getSecurityId()).isEqualTo(PRODUCT.getSecurityId());
    assertThat(test.getCurrency()).isEqualTo(PRODUCT.getCurrency());
    assertThat(test.getUnderlyingIds()).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createProduct() {
    IborFutureSecurity test = sut();
    assertThat(test.createProduct(ReferenceData.empty())).isEqualTo(PRODUCT);
    TradeInfo tradeInfo = TradeInfo.of(date(2016, 6, 30));
    IborFutureTrade expectedTrade = IborFutureTrade.builder()
        .info(tradeInfo)
        .product(PRODUCT)
        .quantity(100)
        .price(0.995)
        .build();
    assertThat(test.createTrade(tradeInfo, 100, 0.995, ReferenceData.empty())).isEqualTo(expectedTrade);

    PositionInfo positionInfo = PositionInfo.empty();
    IborFuturePosition expectedPosition1 = IborFuturePosition.builder()
        .info(positionInfo)
        .product(PRODUCT)
        .longQuantity(100)
        .build();
    TestHelper.assertEqualsBean(test.createPosition(positionInfo, 100, ReferenceData.empty()), expectedPosition1);
    IborFuturePosition expectedPosition2 = IborFuturePosition.builder()
        .info(positionInfo)
        .product(PRODUCT)
        .longQuantity(100)
        .shortQuantity(50)
        .build();
    assertThat(test.createPosition(positionInfo, 100, 50, ReferenceData.empty())).isEqualTo(expectedPosition2);
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
  static IborFutureSecurity sut() {
    return IborFutureSecurity.builder()
        .info(INFO)
        .notional(PRODUCT.getNotional())
        .index(PRODUCT.getIndex())
        .lastTradeDate(PRODUCT.getLastTradeDate())
        .rounding(PRODUCT.getRounding())
        .build();
  }

  static IborFutureSecurity sut2() {
    return IborFutureSecurity.builder()
        .info(INFO2)
        .notional(PRODUCT2.getNotional())
        .index(PRODUCT2.getIndex())
        .lastTradeDate(PRODUCT2.getLastTradeDate())
        .rounding(PRODUCT2.getRounding())
        .build();
  }

}
