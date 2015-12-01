/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
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
  // Underlying bonds
  private static final StandardId BOND_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BONDS");
  private static final StandardId ISSUER_ID = StandardId.of("OG-Ticker", "GOVT1");
  private static final YieldConvention YIELD_CONVENTION = YieldConvention.GERMAN_BONDS;
  private static final double NOTIONAL = 100000d;
  private static final HolidayCalendar CALENDAR = HolidayCalendars.EUTA;
  private static final DaysAdjustment SETTLEMENT_DAYS = DaysAdjustment.ofBusinessDays(3, CALENDAR);
  private static final DayCount DAY_COUNT = DayCounts.ACT_ACT_ICMA;
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR);
  private static final DaysAdjustment EX_COUPON = DaysAdjustment.NONE;
  private static final int NB_BOND = 3;
  private static final double[] RATE = new double[] {0.0375, 0.0350, 0.0100 };
  private static final LocalDate[] START_DATE = new LocalDate[] {
    LocalDate.of(2013, 1, 4), LocalDate.of(2013, 7, 4), LocalDate.of(2013, 2, 22) };
  private static final Period[] BOND_TENOR = new Period[] {Period.ofYears(6), Period.ofYears(6), Period.ofYears(6) };
  @SuppressWarnings("unchecked")
  private static final SecurityLink<FixedCouponBond>[] SECURITY_LINK = new SecurityLink[NB_BOND];
  private static final FixedCouponBond[] BOND_PRODUCT = new FixedCouponBond[NB_BOND];
  static {
    for (int i = 0; i < NB_BOND; ++i) {
      LocalDate endDate = START_DATE[i].plus(BOND_TENOR[i]);
      PeriodicSchedule periodSchedule = PeriodicSchedule.of(
          START_DATE[i], endDate, Frequency.P6M, BUSINESS_ADJUST, StubConvention.SHORT_INITIAL, false);
      FixedCouponBond product = FixedCouponBond.builder()
          .dayCount(DAY_COUNT)
          .fixedRate(RATE[i])
          .legalEntityId(ISSUER_ID)
          .currency(USD)
          .notional(NOTIONAL)
          .periodicSchedule(periodSchedule)
          .settlementDateOffset(SETTLEMENT_DAYS)
          .yieldConvention(YIELD_CONVENTION)
          .exCouponPeriod(EX_COUPON)
          .build();
      BOND_PRODUCT[i] = product;
      Security<FixedCouponBond> bondSecurity = UnitSecurity.builder(product).standardId(BOND_SECURITY_ID).build();
      SECURITY_LINK[i] = SecurityLink.resolved(bondSecurity);
    }
  }
  // future
  private static final Double[] CONVERSION_FACTOR = new Double[] {0.912067, 0.893437, 0.800111 };
  private static final LocalDate LAST_TRADING_DATE = LocalDate.of(2014, 6, 6);
  private static final LocalDate FIRST_NOTICE_DATE = LocalDate.of(2014, 6, 6);
  private static final LocalDate LAST_NOTICE_DATE = LocalDate.of(2014, 6, 6);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(3);
  private static final BondFuture FUTURE_PRODUCT = BondFuture.builder()
      .conversionFactor(CONVERSION_FACTOR)
      .deliveryBasket(SECURITY_LINK)
      .firstNoticeDate(FIRST_NOTICE_DATE)
      .lastNoticeDate(LAST_NOTICE_DATE)
      .lastTradeDate(LAST_TRADING_DATE)
      .rounding(ROUNDING)
      .build();
  private static final StandardId FUTURE_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND-FUT");
  private static final Security<BondFuture> FUTURE_SECURITY = UnitSecurity.builder(FUTURE_PRODUCT)
      .standardId(FUTURE_SECURITY_ID)
      .build();
  // future option
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
  private static final long QUANTITY = 1234;
  private static final Double INITIAL_PRICE = 0.01;
  private static final StandardId OPTION_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND-FUT-OPT");
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
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(OPTION_RESOLVABLE_FUTURE_RESOLVABLE)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), OPTION_RESOLVABLE_FUTURE_RESOLVABLE);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getInitialPrice(), OptionalDouble.of(INITIAL_PRICE));
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    BondFutureOptionTrade test = BondFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), OPTION_RESOLVED_FUTURE_RESOLVED);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getInitialPrice(), OptionalDouble.empty());
    assertEquals(test.getSecurity(), OPTION_SECURITY_RESOLVED);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    BondFutureOptionTrade test = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVABLE_FUTURE_RESOLVABLE)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    BondFutureOptionTrade expected = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertEquals(test.resolveLinks(RESOLVER), expected);
  }

  public void test_resolveLinks_partlyResolved() {
    BondFutureOptionTrade test = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVABLE)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    BondFutureOptionTrade expected = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertEquals(test.resolveLinks(RESOLVER), expected);
  }

  public void test_resolveLinks_fullyResolved() {
    BondFutureOptionTrade test = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertEquals(test.resolveLinks(RESOLVER), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BondFutureOptionTrade test1 = BondFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    coverImmutableBean(test1);
    BondFutureOptionTrade test2 = BondFutureOptionTrade.builder()
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVABLE)
        .quantity(100)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    BondFutureOptionTrade test = BondFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(OPTION_RESOLVED_FUTURE_RESOLVED)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertSerialization(test);
  }

}
