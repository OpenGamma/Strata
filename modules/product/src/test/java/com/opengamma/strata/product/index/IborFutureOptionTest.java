/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.PutCall.PUT;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;

/**
 * Test IborFutureOption. 
 */
@Test
public class IborFutureOptionTest {

  private static final double NOTIONAL_1 = 1_000d;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 9, 16);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);
  private static final StandardId ID_1 = StandardId.of("OG-Ticker", "Ibor1");
  private static final IborFuture IBOR_FUTURE_1 = IborFuture.builder()
      .currency(GBP)
      .index(GBP_LIBOR_2M)
      .notional(NOTIONAL_1)
      .lastTradeDate(LAST_TRADE_DATE_1)
      .rounding(ROUNDING)
      .build();
  private static final Security<IborFuture> IBOR_FUTURE_SECURITY_1 = UnitSecurity.builder(IBOR_FUTURE_1)
      .standardId(ID_1)
      .build();

  private static final LocalDate EXPIRY_DATE = date(2015, 5, 20);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Europe/London");
  private static final double STRIKE_PRICE = 1.075;

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    IborFutureOption test = IborFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .rounding(ROUNDING)
        .underlyingLink(SecurityLink.resolvable(ID_1, IborFuture.class))
        .build();
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrikePrice(), STRIKE_PRICE);
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
    assertEquals(test.getExpiry(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertEquals(test.getRounding(), ROUNDING);
    assertEquals(test.getUnderlyingLink(), SecurityLink.resolvable(ID_1, IborFuture.class));
    assertThrows(() -> test.getUnderlyingSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getUnderlying(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    IborFutureOption test = IborFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingLink(SecurityLink.resolved(IBOR_FUTURE_SECURITY_1))
        .build();
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrikePrice(), STRIKE_PRICE);
    assertEquals(test.getExpiryDate(), EXPIRY_DATE);
    assertEquals(test.getExpiryTime(), EXPIRY_TIME);
    assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
    assertEquals(test.getExpiry(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertEquals(test.getRounding(), Rounding.none());
    assertEquals(test.getUnderlyingLink(), SecurityLink.resolved(IBOR_FUTURE_SECURITY_1));
    assertEquals(test.getUnderlyingSecurity(), IBOR_FUTURE_SECURITY_1);
    assertEquals(test.getUnderlying(), IBOR_FUTURE_1);
  }

  public void test_builder_expiryNotAfterTradeDate() {
    assertThrowsIllegalArg(() -> IborFutureOption.builder()
        .putCall(CALL)
        .expiryDate(LAST_TRADE_DATE_2)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .strikePrice(STRIKE_PRICE)
        .underlyingLink(SecurityLink.resolved(IBOR_FUTURE_SECURITY_1))
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    IborFutureOption test = IborFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingLink(SecurityLink.resolvable(ID_1, IborFuture.class))
        .build();
    IborFutureOption expected = IborFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingLink(SecurityLink.resolved(IBOR_FUTURE_SECURITY_1))
        .build();
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        assertEquals(identifier, IBOR_FUTURE_SECURITY_1.getStandardId());
        return (T) IBOR_FUTURE_SECURITY_1;
      }
    };
    assertEquals(test.resolveLinks(resolver), expected);
  }

  public void test_resolveLinks_resolved() {
    IborFutureOption test = IborFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingLink(SecurityLink.resolved(IBOR_FUTURE_SECURITY_1))
        .build();
    LinkResolver resolver = new LinkResolver() {
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        fail();  // not invoked because link is already resolved
        return null;
      }
    };
    assertSame(test.resolveLinks(resolver), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFutureOption test = IborFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingLink(SecurityLink.resolved(IBOR_FUTURE_SECURITY_1))
        .build();
    coverImmutableBean(test);
    IborFutureOption test2 = IborFutureOption.builder()
        .putCall(PUT)
        .strikePrice(STRIKE_PRICE + 1)
        .expiryDate(LAST_TRADE_DATE_1)
        .expiryTime(LocalTime.of(12, 0))
        .expiryZone(ZoneId.of("Europe/Paris"))
        .premiumStyle(FutureOptionPremiumStyle.UPFRONT_PREMIUM)
        .rounding(ROUNDING)
        .underlyingLink(SecurityLink.resolvable(ID_1, IborFuture.class))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborFutureOption test = IborFutureOption.builder()
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingLink(SecurityLink.resolved(IBOR_FUTURE_SECURITY_1))
        .build();
    assertSerialization(test);
  }

}
