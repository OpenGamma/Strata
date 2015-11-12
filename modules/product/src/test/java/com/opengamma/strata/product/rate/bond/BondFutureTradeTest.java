/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
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

/**
 * Test {@link BondFutureTrade}.
 */
@Test
public class BondFutureTradeTest {

  // Underlying bonds
  private static final StandardId BOND_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BONDS"); // same repo curve for all bonds
  private static final StandardId ISSUER_ID = StandardId.of("OG-Ticker", "GOVT1");
  private static final YieldConvention YIELD_CONVENTION = YieldConvention.US_STREET;
  private static final double NOTIONAL = 100000d;
  private static final HolidayCalendar CALENDAR = HolidayCalendars.SAT_SUN;
  private static final DaysAdjustment SETTLEMENT_DAYS = DaysAdjustment.ofBusinessDays(1, CALENDAR);
  private static final DayCount DAY_COUNT = DayCounts.ACT_ACT_ICMA;
  private static final BusinessDayAdjustment BUSINESS_ADJUST = BusinessDayAdjustment.of(FOLLOWING, CALENDAR);
  private static final DaysAdjustment EX_COUPON = DaysAdjustment.NONE;
  private static final int NB_BOND = 7;
  private static final double[] RATE = new double[] {0.01375, 0.02125, 0.0200, 0.02125, 0.0225, 0.0200, 0.0175};
  private static final LocalDate[] START_DATE = new LocalDate[] {
      LocalDate.of(2010, 11, 30), LocalDate.of(2010, 12, 31), LocalDate.of(2011, 1, 31), LocalDate.of(2008, 2, 29),
      LocalDate.of(2011, 3, 31), LocalDate.of(2011, 4, 30), LocalDate.of(2011, 5, 31)};
  private static final Period[] BOND_TENOR = new Period[] {
      Period.ofYears(5), Period.ofYears(5), Period.ofYears(5), Period.ofYears(8),
      Period.ofYears(5), Period.ofYears(5), Period.ofYears(5)};
  @SuppressWarnings("unchecked")
  private static final SecurityLink<FixedCouponBond>[] BOND_SECURITY_LINK = new SecurityLink[NB_BOND];
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
      BOND_SECURITY_LINK[i] = SecurityLink.resolved(bondSecurity);
    }
  }

  // future
  private static final Double[] CONVERSION_FACTOR = new Double[] {.8317, .8565, .8493, .8516, .8540, .8417, .8292};
  private static final LocalDate LAST_TRADING_DATE = LocalDate.of(2011, 9, 30);
  private static final LocalDate FIRST_NOTICE_DATE = LocalDate.of(2011, 8, 31);
  private static final LocalDate LAST_NOTICE_DATE = LocalDate.of(2011, 10, 4);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(3);
  private static final BondFuture FUTURE_PRODUCT = BondFuture.builder()
      .conversionFactor(CONVERSION_FACTOR)
      .deliveryBasket(BOND_SECURITY_LINK)
      .firstNoticeDate(FIRST_NOTICE_DATE)
      .lastNoticeDate(LAST_NOTICE_DATE)
      .lastTradeDate(LAST_TRADING_DATE)
      .rounding(ROUNDING)
      .build();
  private static final StandardId FUTURE_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND-FUTURE");
  private static final Security<BondFuture> FUTURE_SECURITY = UnitSecurity.builder(FUTURE_PRODUCT)
      .standardId(FUTURE_SECURITY_ID).build();
  private static final SecurityLink<BondFuture> FUTURE_SECURITY_LINK_RESOLVED = SecurityLink.resolved(FUTURE_SECURITY);
  private static final SecurityLink<BondFuture> FUTURE_SECURITY_LINK_RESOLVABLE = SecurityLink.resolvable(FUTURE_SECURITY_ID, BondFuture.class);
  private static final LinkResolver RESOLVER = new LinkResolver() {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
      assertEquals(identifier, FUTURE_SECURITY_ID);
      return (T) FUTURE_SECURITY;
    }
  };
  // trade
  private static final LocalDate TRADE_DATE = LocalDate.of(2011, 6, 20);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(TRADE_DATE).build();
  private static final long QUANTITY = 1234L;
  private static final double PRICE_REFERENCE = 1.2345;

  public void test_of_resolved() {
    BondFutureTrade test = BondFutureTrade.builder()
        .initialPrice(PRICE_REFERENCE)
        .quantity(QUANTITY)
        .securityLink(FUTURE_SECURITY_LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .build();
    assertEquals(test.getInitialPrice(), PRICE_REFERENCE);
    assertEquals(test.getProduct(), FUTURE_PRODUCT);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurity(), FUTURE_SECURITY);
    assertEquals(test.getSecurityLink(), FUTURE_SECURITY_LINK_RESOLVED);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  public void test_of_resolvable() {
    BondFutureTrade test = BondFutureTrade.builder()
        .initialPrice(PRICE_REFERENCE)
        .quantity(QUANTITY)
        .securityLink(FUTURE_SECURITY_LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .build();
    assertEquals(test.getInitialPrice(), PRICE_REFERENCE);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurityLink(), FUTURE_SECURITY_LINK_RESOLVABLE);
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
  }

  public void test_resolveLinks_resolved() {
    BondFutureTrade base = BondFutureTrade.builder()
        .initialPrice(PRICE_REFERENCE)
        .quantity(QUANTITY)
        .securityLink(FUTURE_SECURITY_LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .build();
    assertEquals(base.resolveLinks(RESOLVER), base);
  }

  public void test_resolveLinks_resolvable() {
    BondFutureTrade base = BondFutureTrade.builder()
        .initialPrice(PRICE_REFERENCE)
        .quantity(QUANTITY)
        .securityLink(FUTURE_SECURITY_LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .build();
    BondFutureTrade expected = BondFutureTrade.builder()
        .initialPrice(PRICE_REFERENCE)
        .quantity(QUANTITY)
        .securityLink(FUTURE_SECURITY_LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .build();
    assertEquals(base.resolveLinks(RESOLVER), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BondFutureTrade test1 = BondFutureTrade.builder()
        .initialPrice(PRICE_REFERENCE)
        .quantity(QUANTITY)
        .securityLink(FUTURE_SECURITY_LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    BondFutureTrade test2 = BondFutureTrade.builder()
        .securityLink(SecurityLink.resolved(UnitSecurity.builder(FUTURE_PRODUCT)
            .standardId(StandardId.of("Ticker", "GOV1-BND-FUT"))
            .build()))
        .quantity(100L)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    BondFutureTrade test = BondFutureTrade.builder()
        .initialPrice(PRICE_REFERENCE)
        .quantity(QUANTITY)
        .securityLink(FUTURE_SECURITY_LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
