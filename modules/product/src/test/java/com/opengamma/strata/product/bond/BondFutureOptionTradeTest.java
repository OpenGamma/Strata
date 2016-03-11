/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;

/**
 * Test {@link BondFutureOptionTrade}. 
 */
@Test
public class BondFutureOptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // future
  private static final BondFuture FUTURE_PRODUCT = BondFutureTest.sut();
  private static final StandardId FUTURE_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND-FUT");
  private static final Security<BondFuture> FUTURE_SECURITY = UnitSecurity.builder(FUTURE_PRODUCT)
      .standardId(FUTURE_SECURITY_ID)
      .build();
  // future option
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(3);
  private static final LocalDate EXPIRY_DATE = date(2011, 9, 20);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
  private static final double STRIKE_PRICE = 1.15;
  private static final BondFutureOption OPTION_PRODUCT_RESOLVED = BondFutureOption.builder()
      .putCall(CALL)
      .strikePrice(STRIKE_PRICE)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(EXPIRY_ZONE)
      .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
      .rounding(ROUNDING)
      .underlyingLink(SecurityLink.resolved(FUTURE_SECURITY))
      .build();
  private static final BondFutureOption OPTION_PRODUCT_RESOLVABLE = BondFutureOption.builder()
      .putCall(CALL)
      .strikePrice(STRIKE_PRICE)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(EXPIRY_ZONE)
      .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
      .rounding(ROUNDING)
      .underlyingLink(SecurityLink.resolvable(FUTURE_SECURITY_ID, BondFuture.class))
      .build();
  // option trade
  private static final LocalDate TRADE_DATE = date(2014, 3, 31);

  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(TRADE_DATE).build();
  private static final long QUANTITY = 1234;
  private static final Double PRICE = 0.01;
  private static final StandardId OPTION_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND-FUT-OPT");
  private static final StandardId OPTION_SECURITY_ID2 = StandardId.of("OG-Ticker", "GOVT1-BOND-FUT-OPT2");
  private static final Security<BondFutureOption> OPTION_SECURITY_RESOLVED = UnitSecurity
      .builder(OPTION_PRODUCT_RESOLVED)
      .standardId(OPTION_SECURITY_ID)
      .build();
  private static final Security<BondFutureOption> OPTION_SECURITY_RESOLVABLE = UnitSecurity
      .builder(OPTION_PRODUCT_RESOLVABLE)
      .standardId(OPTION_SECURITY_ID)
      .build();
  private static final SecurityLink<BondFutureOption> OPTION_RESOLVABLE_FUTURE_RESOLVABLE =
      SecurityLink.resolvable(OPTION_SECURITY_ID, BondFutureOption.class);
  private static final SecurityLink<BondFutureOption> OPTION_RESOLVED_FUTURE_RESOLVABLE =
      SecurityLink.resolved(OPTION_SECURITY_RESOLVABLE);
  private static final SecurityLink<BondFutureOption> OPTION_RESOLVED_FUTURE_RESOLVED =
      SecurityLink.resolved(OPTION_SECURITY_RESOLVED);
  private static final SecurityLink<BondFutureOption> OPTION_RESOLVED_FUTURE_RESOLVED2 =
      SecurityLink.resolved(UnitSecurity
          .builder(OPTION_PRODUCT_RESOLVED)
          .standardId(OPTION_SECURITY_ID2)
          .build());

  private static final LinkResolver RESOLVER = new LinkResolver() {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
      if (identifier.equals(OPTION_SECURITY_ID)) {
        return (T) OPTION_SECURITY_RESOLVABLE;
      }
      assertEquals(identifier, FUTURE_SECURITY_ID);
      return (T) FUTURE_SECURITY;
    }
  };

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    BondFutureOptionTrade test = BondFutureOptionTrade.builder()
        .tradeInfo(TRADE_INFO)
        .securityLink(OPTION_RESOLVABLE_FUTURE_RESOLVABLE)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getSecurityLink(), OPTION_RESOLVABLE_FUTURE_RESOLVABLE);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), OptionalDouble.of(PRICE));
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    BondFutureOptionTrade test = BondFutureOptionTrade.builder()
        .tradeInfo(TRADE_INFO)
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getSecurityLink(), OPTION_RESOLVED_FUTURE_RESOLVED);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), OptionalDouble.empty());
    assertEquals(test.getSecurity(), OPTION_SECURITY_RESOLVED);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    BondFutureOptionTrade test = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVABLE_FUTURE_RESOLVABLE)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    BondFutureOptionTrade expected = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(test.resolveLinks(RESOLVER), expected);
  }

  public void test_resolveLinks_partlyResolved() {
    BondFutureOptionTrade test = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVABLE)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    BondFutureOptionTrade expected = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(test.resolveLinks(RESOLVER), expected);
  }

  public void test_resolveLinks_fullyResolved() {
    BondFutureOptionTrade test = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(test.resolveLinks(RESOLVER), test);
  }

  public void test_resolve() {
    ResolvedBondFutureOptionTrade expected = ResolvedBondFutureOptionTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(OPTION_PRODUCT_RESOLVED.resolve(REF_DATA))
        .securityStandardId(OPTION_SECURITY_ID)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(sut().resolve(REF_DATA), expected);
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
  static BondFutureOptionTrade sut() {
    return BondFutureOptionTrade.builder()
        .tradeInfo(TRADE_INFO)
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static BondFutureOptionTrade sut2() {
    return BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED2)
        .quantity(100)
        .build();
  }

}
