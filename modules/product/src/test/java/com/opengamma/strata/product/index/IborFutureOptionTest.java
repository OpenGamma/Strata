/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static com.opengamma.strata.product.common.PutCall.PUT;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * Test {@link IborFutureOption}. 
 */
@Test
public class IborFutureOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFuture FUTURE = IborFutureTest.sut();
  private static final IborFuture FUTURE2 = IborFutureTest.sut2();
  private static final LocalDate LAST_TRADE_DATE = date(2015, 6, 15);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);
  private static final LocalDate EXPIRY_DATE = date(2015, 5, 20);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Europe/London");
  private static final double STRIKE_PRICE = 0.993;
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "IborFutureOption");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "IborFutureOption2");

  //-------------------------------------------------------------------------
  public void test_builder() {
    IborFutureOption test = sut();
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrikePrice(), STRIKE_PRICE);
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
    assertEquals(test.getExpiry(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertEquals(test.getRounding(), Rounding.none());
    assertEquals(test.getUnderlyingFuture(), FUTURE);
    assertEquals(test.getCurrency(), FUTURE.getCurrency());
    assertEquals(test.getIndex(), FUTURE.getIndex());
  }

  public void test_builder_expiryNotAfterTradeDate() {
    assertThrowsIllegalArg(() -> IborFutureOption.builder()
        .securityId(SECURITY_ID)
        .putCall(CALL)
        .expiryDate(LAST_TRADE_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .strikePrice(STRIKE_PRICE)
        .underlyingFuture(FUTURE)
        .build());
  }

  public void test_builder_badPrice() {
    assertThrowsIllegalArg(() -> sut().toBuilder().strikePrice(2.1).build());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    IborFutureOption test = sut();
    ResolvedIborFutureOption expected = ResolvedIborFutureOption.builder()
        .securityId(SECURITY_ID)
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiry(EXPIRY_DATE.atTime(EXPIRY_TIME).atZone(EXPIRY_ZONE))
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingFuture(FUTURE.resolve(REF_DATA))
        .build();
    assertEquals(test.resolve(REF_DATA), expected);
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
  static IborFutureOption sut() {
    return IborFutureOption.builder()
        .securityId(SECURITY_ID)
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingFuture(FUTURE)
        .build();
  }

  static IborFutureOption sut2() {
    return IborFutureOption.builder()
        .securityId(SECURITY_ID2)
        .putCall(PUT)
        .strikePrice(STRIKE_PRICE + 0.001)
        .expiryDate(EXPIRY_DATE.plusDays(1))
        .expiryTime(LocalTime.of(12, 0))
        .expiryZone(ZoneId.of("Europe/Paris"))
        .premiumStyle(FutureOptionPremiumStyle.UPFRONT_PREMIUM)
        .rounding(ROUNDING)
        .underlyingFuture(FUTURE2)
        .build();
  }

}
