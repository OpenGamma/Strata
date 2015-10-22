/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
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
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Security;
import com.opengamma.strata.finance.SecurityLink;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.UnitSecurity;

/**
 * Test {@link FixedCouponBondTrade}.
 */
@Test
public class FixedCouponBondTradeTest {

  private static final StandardId SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND1");
  private static final LocalDate TRADE_DATE = LocalDate.of(2015, 3, 25);
  private static final LocalDate SETTLEMENT_DATE = LocalDate.of(2015, 3, 30);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .settlementDate(SETTLEMENT_DATE)
      .build();
  private static final long QUANTITY = 10;

  private static final YieldConvention YIELD_CONVENTION = YieldConvention.GERMAN_BONDS;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG-Ticker", "GOVT1");
  private static final double NOTIONAL = 1.0e7;
  private static final double FIXED_RATE = 0.015;
  private static final HolidayCalendar EUR_CALENDAR = HolidayCalendars.EUTA;
  private static final DaysAdjustment DATE_OFFSET = DaysAdjustment.ofBusinessDays(3, EUR_CALENDAR);
  private static final DayCount DAY_COUNT = DayCounts.ACT_365F;
  private static final LocalDate START_DATE = LocalDate.of(2015, 4, 12);
  private static final LocalDate END_DATE = LocalDate.of(2025, 4, 12);
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUR_CALENDAR);
  private static final PeriodicSchedule PERIOD_SCHEDULE = PeriodicSchedule.of(
      START_DATE, END_DATE, Frequency.P6M, BUSINESS_ADJUST, StubConvention.SHORT_INITIAL, false);
  private static final FixedCouponBond PRODUCT = FixedCouponBond.builder()
      .dayCount(DAY_COUNT)
      .fixedRate(FIXED_RATE)
      .legalEntityId(LEGAL_ENTITY)
      .currency(EUR)
      .notional(NOTIONAL)
      .periodicSchedule(PERIOD_SCHEDULE)
      .settlementDateOffset(DATE_OFFSET)
      .yieldConvention(YIELD_CONVENTION)
      .build();
  private static final Security<FixedCouponBond> BOND_SECURITY =
      UnitSecurity.builder(PRODUCT).standardId(SECURITY_ID).build();
  private static final Payment UPFRONT_PAYMENT = Payment.of(
      CurrencyAmount.of(EUR, -NOTIONAL * QUANTITY * 0.99), SETTLEMENT_DATE);

  private static final SecurityLink<FixedCouponBond> SECURITY_LINK_RESOLVED = SecurityLink.resolved(BOND_SECURITY);
  private static final SecurityLink<FixedCouponBond> SECURITY_LINK_RESOLVABLE =
      SecurityLink.resolvable(SECURITY_ID, FixedCouponBond.class);

  private static final LinkResolver RESOLVER = new LinkResolver() {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
      assertEquals(identifier, SECURITY_ID);
      return (T) BOND_SECURITY;
    }
  };

  //-------------------------------------------------------------------------
  public void test_builder_resolved() {
    FixedCouponBondTrade test = FixedCouponBondTrade.builder()
        .securityLink(SECURITY_LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurity(), BOND_SECURITY);
    assertEquals(test.getSecurityLink(), SECURITY_LINK_RESOLVED);
    assertEquals(test.getPayment(), UPFRONT_PAYMENT);
  }

  public void test_builder_resolvable() {
    FixedCouponBondTrade test = FixedCouponBondTrade.builder()
        .securityLink(SECURITY_LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurityLink(), SECURITY_LINK_RESOLVABLE);
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolved() {
    FixedCouponBondTrade base = FixedCouponBondTrade.builder()
        .securityLink(SECURITY_LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
    assertEquals(base.resolveLinks(RESOLVER), base);
  }

  public void test_resolveLinks_resolvable() {
    FixedCouponBondTrade base = FixedCouponBondTrade.builder()
        .securityLink(SECURITY_LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
    FixedCouponBondTrade expected = FixedCouponBondTrade.builder()
        .securityLink(SECURITY_LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
    assertEquals(base.resolveLinks(RESOLVER), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedCouponBondTrade test1 = FixedCouponBondTrade.builder()
        .securityLink(SECURITY_LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
    coverImmutableBean(test1);
    FixedCouponBondTrade test2 = FixedCouponBondTrade.builder()
        .securityLink(SecurityLink.resolved(UnitSecurity.builder(PRODUCT)
            .standardId(StandardId.of("Ticker", "GOV1-BND1"))
            .build()))
        .quantity(100L)
        .payment(Payment.of(CurrencyAmount.of(EUR, -NOTIONAL * QUANTITY * 0.99), SETTLEMENT_DATE))
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FixedCouponBondTrade test = FixedCouponBondTrade.builder()
        .securityLink(SECURITY_LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
    assertSerialization(test);
  }

}
