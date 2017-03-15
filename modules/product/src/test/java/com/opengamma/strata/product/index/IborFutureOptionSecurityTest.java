/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

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
 * Test {@link IborFutureOptionSecurity}.
 */
@Test
public class IborFutureOptionSecurityTest {

  private static final IborFutureOption OPTION = IborFutureOptionTest.sut();
  private static final IborFutureOption OPTION2 = IborFutureOptionTest.sut2();
  private static final IborFuture FUTURE = OPTION.getUnderlyingFuture();
  private static final IborFuture FUTURE2 = OPTION2.getUnderlyingFuture();
  private static final IborFutureSecurity FUTURE_SECURITY = IborFutureSecurityTest.sut();
  private static final SecurityId FUTURE_ID = FUTURE.getSecurityId();
  private static final SecurityId FUTURE_ID2 = FUTURE2.getSecurityId();
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(GBP, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(OPTION.getSecurityId(), PRICE_INFO);
  private static final SecurityInfo INFO2 = SecurityInfo.of(OPTION2.getSecurityId(), PRICE_INFO);

  //-------------------------------------------------------------------------
  public void test_builder() {
    IborFutureOptionSecurity test = sut();
    assertEquals(test.getInfo(), INFO);
    assertEquals(test.getSecurityId(), OPTION.getSecurityId());
    assertEquals(test.getCurrency(), OPTION.getCurrency());
    assertEquals(test.getPutCall(), OPTION.getPutCall());
    assertEquals(test.getPremiumStyle(), OPTION.getPremiumStyle());
    assertEquals(test.getUnderlyingFutureId(), FUTURE_ID);
    assertEquals(test.getUnderlyingIds(), ImmutableSet.of(FUTURE_ID));
  }

  public void test_builder_badPrice() {
    assertThrowsIllegalArg(() -> sut().toBuilder().strikePrice(2.1).build());
  }

  //-------------------------------------------------------------------------
  public void test_createProduct() {
    IborFutureOptionSecurity test = sut();
    ReferenceData refData = ImmutableReferenceData.of(FUTURE_ID, FUTURE_SECURITY);
    assertEquals(test.createProduct(refData), OPTION);
    TradeInfo tradeInfo = TradeInfo.of(date(2016, 6, 30));
    IborFutureOptionTrade expectedTrade = IborFutureOptionTrade.builder()
        .info(tradeInfo)
        .product(OPTION)
        .quantity(100)
        .price(123.50)
        .build();
    assertEquals(test.createTrade(tradeInfo, 100, 123.50, refData), expectedTrade);
  }

  public void test_createProduct_wrongType() {
    IborFutureOptionSecurity test = sut();
    IborFuture future = OPTION.getUnderlyingFuture();
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
  static IborFutureOptionSecurity sut() {
    return IborFutureOptionSecurity.builder()
        .info(INFO)
        .currency(OPTION.getCurrency())
        .putCall(OPTION.getPutCall())
        .strikePrice(OPTION.getStrikePrice())
        .expiryDate(OPTION.getExpiryDate())
        .expiryTime(OPTION.getExpiryTime())
        .expiryZone(OPTION.getExpiryZone())
        .premiumStyle(OPTION.getPremiumStyle())
        .rounding(OPTION.getRounding())
        .underlyingFutureId(FUTURE_ID)
        .build();
  }

  static IborFutureOptionSecurity sut2() {
    return IborFutureOptionSecurity.builder()
        .info(INFO2)
        .currency(OPTION2.getCurrency())
        .putCall(OPTION2.getPutCall())
        .strikePrice(OPTION2.getStrikePrice())
        .expiryDate(OPTION2.getExpiryDate())
        .expiryTime(OPTION2.getExpiryTime())
        .expiryZone(OPTION2.getExpiryZone())
        .premiumStyle(OPTION2.getPremiumStyle())
        .rounding(OPTION2.getRounding())
        .underlyingFutureId(FUTURE_ID2)
        .build();
  }

}
