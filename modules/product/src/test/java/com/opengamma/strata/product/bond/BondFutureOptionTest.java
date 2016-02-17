/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.PutCall.PUT;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;

/**
 * Test {@link BondFutureOption}.
 */
@Test
public class BondFutureOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // future
  private static final BondFuture FUTURE_PRODUCT = BondFutureTest.sut();
  private static final StandardId FUTURE_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND-FUT");
  private static final StandardId FUTURE_SECURITY_ID2 = StandardId.of("OG-Ticker", "GOVT1-BOND-FUT2");
  private static final Security<BondFuture> FUTURE_SECURITY = UnitSecurity.builder(FUTURE_PRODUCT)
      .standardId(FUTURE_SECURITY_ID)
      .build();
  private static final Security<BondFuture> FUTURE_SECURITY2 = UnitSecurity.builder(FUTURE_PRODUCT)
      .standardId(FUTURE_SECURITY_ID2)
      .build();
  // future option
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(3);
  private static final LocalDate EXPIRY_DATE = date(2011, 9, 20);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
  private static final double STRIKE_PRICE = 1.15;

  private static final LinkResolver RESOLVER = new LinkResolver() {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
      assertEquals(identifier, FUTURE_SECURITY.getStandardId());
      return (T) FUTURE_SECURITY;
    }
  };

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    BondFutureOption test = BondFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .rounding(ROUNDING)
        .underlyingLink(SecurityLink.resolvable(FUTURE_SECURITY_ID, BondFuture.class))
        .build();
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrikePrice(), STRIKE_PRICE);
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
    assertEquals(test.getExpiry(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertEquals(test.getRounding(), ROUNDING);
    assertEquals(test.getUnderlyingLink(), SecurityLink.resolvable(FUTURE_SECURITY_ID, BondFuture.class));
    assertThrows(() -> test.getUnderlyingSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getUnderlying(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    BondFutureOption test = sut();
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrikePrice(), STRIKE_PRICE);
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
    assertEquals(test.getExpiry(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertEquals(test.getRounding(), Rounding.none());
    assertEquals(test.getUnderlyingLink(), SecurityLink.resolved(FUTURE_SECURITY));
    assertEquals(test.getUnderlyingSecurity(), FUTURE_SECURITY);
    assertEquals(test.getUnderlying(), FUTURE_PRODUCT);
  }

  public void test_builder_expiryNotAfterTradeDate() {
    assertThrowsIllegalArg(() -> BondFutureOption.builder()
        .putCall(CALL)
        .expiryDate(FUTURE_PRODUCT.getLastTradeDate())
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .strikePrice(STRIKE_PRICE)
        .underlyingLink(SecurityLink.resolved(FUTURE_SECURITY))
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    BondFutureOption test = BondFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingLink(SecurityLink.resolvable(FUTURE_SECURITY_ID, BondFuture.class))
        .build();
    BondFutureOption expected = sut();
    assertEquals(test.resolveLinks(RESOLVER), expected);
  }

  public void test_resolveLinks_resolved() {
    BondFutureOption test = sut();
    assertEquals(test.resolveLinks(RESOLVER), test);
  }

  public void test_resolve() {
    BondFutureOption test = sut();
    ResolvedBondFutureOption expected = ResolvedBondFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiry(EXPIRY_DATE.atTime(EXPIRY_TIME).atZone(EXPIRY_ZONE))
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlying(FUTURE_PRODUCT.resolve(REF_DATA))
        .underlyingSecurityStandardId(FUTURE_SECURITY_ID)
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
  static BondFutureOption sut() {
    return BondFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingLink(SecurityLink.resolved(FUTURE_SECURITY))
        .build();
  }

  static BondFutureOption sut2() {
    return BondFutureOption.builder()
        .putCall(PUT)
        .strikePrice(1.075)
        .expiryDate(date(2011, 9, 21))
        .expiryTime(LocalTime.of(12, 0))
        .expiryZone(ZoneId.of("Europe/Paris"))
        .premiumStyle(FutureOptionPremiumStyle.UPFRONT_PREMIUM)
        .rounding(ROUNDING)
        .underlyingLink(SecurityLink.resolved(FUTURE_SECURITY2))
        .build();
  }

}
