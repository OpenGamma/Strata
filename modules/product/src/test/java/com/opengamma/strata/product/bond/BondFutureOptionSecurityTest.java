/*
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
import static com.opengamma.strata.product.common.PutCall.CALL;
import static com.opengamma.strata.product.common.PutCall.PUT;
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
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * Test {@link BondFutureOptionSecurity}.
 */
@Test
public class BondFutureOptionSecurityTest {

  private static final BondFutureOption PRODUCT = BondFutureOptionTest.sut();
  private static final BondFutureOption PRODUCT2 = BondFutureOptionTest.sut2();
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(GBP, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(PRODUCT.getSecurityId(), PRICE_INFO);
  private static final SecurityInfo INFO2 = SecurityInfo.of(PRODUCT2.getSecurityId(), PRICE_INFO);

  //-------------------------------------------------------------------------
  public void test_builder() {
    BondFutureOptionSecurity test = sut();
    assertEquals(test.getInfo(), INFO);
    assertEquals(test.getSecurityId(), PRODUCT.getSecurityId());
    assertEquals(test.getCurrency(), PRODUCT.getCurrency());
    assertEquals(test.getUnderlyingIds(), ImmutableSet.of(PRODUCT.getUnderlyingFuture().getSecurityId()));
  }

  //-------------------------------------------------------------------------
  public void test_createProduct() {
    BondFutureOptionSecurity test = sut();
    BondFuture future = PRODUCT.getUnderlyingFuture();
    BondFutureSecurity futureSec = BondFutureSecurityTest.sut();
    ImmutableList<FixedCouponBond> basket = future.getDeliveryBasket();
    FixedCouponBondSecurity bondSec0 = FixedCouponBondSecurityTest.createSecurity(future.getDeliveryBasket().get(0));
    FixedCouponBondSecurity bondSec1 = FixedCouponBondSecurityTest.createSecurity(future.getDeliveryBasket().get(1));
    ReferenceData refData = ImmutableReferenceData.of(ImmutableMap.of(
        test.getUnderlyingFutureId(), futureSec,
        basket.get(0).getSecurityId(), bondSec0,
        basket.get(1).getSecurityId(), bondSec1));
    BondFutureOption product = test.createProduct(refData);
    assertEquals(product.getUnderlyingFuture().getDeliveryBasket().get(0), future.getDeliveryBasket().get(0));
    assertEquals(product.getUnderlyingFuture().getDeliveryBasket().get(1), future.getDeliveryBasket().get(1));
    TradeInfo tradeInfo = TradeInfo.of(date(2016, 6, 30));
    BondFutureOptionTrade expectedTrade = BondFutureOptionTrade.builder()
        .info(tradeInfo)
        .product(product)
        .quantity(100)
        .price(123.50)
        .build();
    assertEquals(test.createTrade(tradeInfo, 100, 123.50, refData), expectedTrade);
  }

  public void test_createProduct_wrongType() {
    BondFutureOptionSecurity test = sut();
    BondFuture future = PRODUCT.getUnderlyingFuture();
    SecurityId secId = future.getSecurityId();
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
  static BondFutureOptionSecurity sut() {
    return BondFutureOptionSecurity.builder()
        .info(INFO)
        .currency(PRODUCT.getCurrency())
        .putCall(CALL)
        .strikePrice(PRODUCT.getStrikePrice())
        .expiryDate(PRODUCT.getExpiryDate())
        .expiryTime(PRODUCT.getExpiryTime())
        .expiryZone(PRODUCT.getExpiryZone())
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .rounding(PRODUCT.getRounding())
        .underlyingFutureId(PRODUCT.getUnderlyingFuture().getSecurityId())
        .build();
  }

  static BondFutureOptionSecurity sut2() {
    return BondFutureOptionSecurity.builder()
        .info(INFO2)
        .currency(PRODUCT2.getCurrency())
        .putCall(PUT)
        .strikePrice(PRODUCT2.getStrikePrice())
        .expiryDate(PRODUCT2.getExpiryDate())
        .expiryTime(PRODUCT2.getExpiryTime())
        .expiryZone(PRODUCT2.getExpiryZone())
        .premiumStyle(FutureOptionPremiumStyle.UPFRONT_PREMIUM)
        .rounding(PRODUCT2.getRounding())
        .underlyingFutureId(PRODUCT2.getUnderlyingFuture().getSecurityId())
        .build();
  }

}
