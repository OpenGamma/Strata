/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link BondFutureSecurity}.
 */
@Test
public class BondFutureSecurityTest {

  private static final BondFuture PRODUCT = BondFutureTest.sut();
  private static final BondFuture PRODUCT2 = BondFutureTest.sut2();
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(GBP, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(PRODUCT.getSecurityId(), PRICE_INFO);
  private static final SecurityInfo INFO2 = SecurityInfo.of(PRODUCT2.getSecurityId(), PRICE_INFO);

  //-------------------------------------------------------------------------
  public void test_builder() {
    BondFutureSecurity test = sut();
    assertEquals(test.getInfo(), INFO);
    assertEquals(test.getSecurityId(), PRODUCT.getSecurityId());
    assertEquals(test.getCurrency(), PRODUCT.getCurrency());
    assertEquals(test.getFirstDeliveryDate(), PRODUCT.getFirstDeliveryDate());
    assertEquals(test.getLastDeliveryDate(), PRODUCT.getLastDeliveryDate());
    ImmutableList<FixedCouponBond> basket = PRODUCT.getDeliveryBasket();
    assertEquals(test.getUnderlyingIds(), ImmutableSet.of(basket.get(0).getSecurityId(), basket.get(1).getSecurityId()));
  }

  //-------------------------------------------------------------------------
  public void test_createProduct() {
    BondFutureSecurity test = sut();
    ImmutableList<FixedCouponBond> basket = PRODUCT.getDeliveryBasket();
    FixedCouponBondSecurity bondSec0 = FixedCouponBondSecurityTest.createSecurity(PRODUCT.getDeliveryBasket().get(0));
    FixedCouponBondSecurity bondSec1 = FixedCouponBondSecurityTest.createSecurity(PRODUCT.getDeliveryBasket().get(1));
    ReferenceData refData = ImmutableReferenceData.of(ImmutableMap.of(
        basket.get(0).getSecurityId(), bondSec0,
        basket.get(1).getSecurityId(), bondSec1));
    BondFuture product = test.createProduct(refData);
    assertEquals(product.getDeliveryBasket().get(0), PRODUCT.getDeliveryBasket().get(0));
    assertEquals(product.getDeliveryBasket().get(1), PRODUCT.getDeliveryBasket().get(1));
    TradeInfo tradeInfo = TradeInfo.of(date(2016, 6, 30));
    BondFutureTrade expectedTrade = BondFutureTrade.builder()
        .info(tradeInfo)
        .product(product)
        .quantity(100)
        .price(123.50)
        .build();
    assertEquals(test.createTrade(tradeInfo, 100, 123.50, refData), expectedTrade);
  }

  public void test_createProduct_wrongType() {
    BondFutureSecurity test = sut();
    ImmutableList<FixedCouponBond> basket = PRODUCT.getDeliveryBasket();
    SecurityId secId = basket.get(0).getSecurityId();
    GenericSecurity sec = GenericSecurity.of(INFO);
    ReferenceData refData = ImmutableReferenceData.of(secId, sec);
    assertThrows(() -> test.createProduct(refData), ClassCastException.class);
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
  static BondFutureSecurity sut() {
    ImmutableList<FixedCouponBond> basket = PRODUCT.getDeliveryBasket();
    return BondFutureSecurity.builder()
        .info(INFO)
        .currency(PRODUCT.getCurrency())
        .deliveryBasketIds(basket.get(0).getSecurityId(), basket.get(1).getSecurityId())
        .conversionFactors(1d, 2d)
        .firstNoticeDate(PRODUCT.getFirstNoticeDate())
        .firstDeliveryDate(PRODUCT.getFirstDeliveryDate().get())
        .lastNoticeDate(PRODUCT.getLastNoticeDate())
        .lastDeliveryDate(PRODUCT.getLastDeliveryDate().get())
        .lastTradeDate(PRODUCT.getLastTradeDate())
        .rounding(PRODUCT.getRounding())
        .build();
  }

  static BondFutureSecurity sut2() {
    ImmutableList<FixedCouponBond> basket = PRODUCT2.getDeliveryBasket();
    return BondFutureSecurity.builder()
        .info(INFO2)
        .currency(PRODUCT2.getCurrency())
        .deliveryBasketIds(basket.get(0).getSecurityId())
        .conversionFactors(3d)
        .firstNoticeDate(PRODUCT2.getFirstNoticeDate())
        .lastNoticeDate(PRODUCT2.getLastNoticeDate())
        .lastTradeDate(PRODUCT2.getLastTradeDate())
        .rounding(PRODUCT2.getRounding())
        .build();
  }

}
