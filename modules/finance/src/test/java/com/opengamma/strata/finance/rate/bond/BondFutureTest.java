/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
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
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Security;
import com.opengamma.strata.finance.SecurityLink;
import com.opengamma.strata.finance.UnitSecurity;

/**
 * Test {@link BondFuture}.
 */
@SuppressWarnings("unchecked")
@Test
public class BondFutureTest {

  // Underlying bonds
  private static final StandardId SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BONDS"); // same repo curve for all bonds
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
      Security<FixedCouponBond> bondSecurity = UnitSecurity.builder(product).standardId(SECURITY_ID).build();
      SECURITY_LINK[i] = SecurityLink.resolved(bondSecurity);
    }
  }

  // future specification
  private static final Double[] CONVERSION_FACTOR = new Double[] {.8317, .8565, .8493, .8516, .8540, .8417, .8292};
  private static final LocalDate LAST_TRADING_DATE = LocalDate.of(2011, 9, 30);
  private static final LocalDate FIRST_NOTICE_DATE = LocalDate.of(2011, 8, 31);
  private static final LocalDate LAST_NOTICE_DATE = LocalDate.of(2011, 10, 4);
  private static final LocalDate FIRST_DELIVERY_DATE = SETTLEMENT_DAYS.adjust(FIRST_NOTICE_DATE);
  private static final LocalDate LAST_DELIVERY_DATE = SETTLEMENT_DAYS.adjust(LAST_NOTICE_DATE);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(3);

  public void test_builder_full() {
    BondFuture test = BondFuture.builder()
        .conversionFactor(CONVERSION_FACTOR)
        .deliveryBasket(SECURITY_LINK)
        .firstNoticeDate(FIRST_NOTICE_DATE)
        .firstDeliveryDate(FIRST_DELIVERY_DATE)
        .lastNoticeDate(LAST_NOTICE_DATE)
        .lastDeliveryDate(LAST_DELIVERY_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build();
    assertEquals(test.getBondProductBasket(), ImmutableList.copyOf(BOND_PRODUCT));
    assertEquals(test.getConversionFactor(), ImmutableList.copyOf(CONVERSION_FACTOR));
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getDeliveryBasket(), ImmutableList.copyOf(SECURITY_LINK));
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getFirstNoticeDate(), FIRST_NOTICE_DATE);
    assertEquals(test.getFirstDeliveryDate(), FIRST_DELIVERY_DATE);
    assertEquals(test.getLastNoticeDate(), LAST_NOTICE_DATE);
    assertEquals(test.getLastDeliveryDate(), LAST_DELIVERY_DATE);
    assertEquals(test.getLastTradeDate(), LAST_TRADING_DATE);
    assertEquals(test.getRounding(), ROUNDING);
  }

  public void test_builder_noDeliveryDate() {
    BondFuture test = BondFuture.builder()
        .conversionFactor(CONVERSION_FACTOR)
        .deliveryBasket(SECURITY_LINK)
        .firstNoticeDate(FIRST_NOTICE_DATE)
        .lastNoticeDate(LAST_NOTICE_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build();
    assertEquals(test.getBondProductBasket(), ImmutableList.copyOf(BOND_PRODUCT));
    assertEquals(test.getConversionFactor(), ImmutableList.copyOf(CONVERSION_FACTOR));
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getDeliveryBasket(), ImmutableList.copyOf(SECURITY_LINK));
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getFirstNoticeDate(), FIRST_NOTICE_DATE);
    assertEquals(test.getFirstDeliveryDate(), FIRST_DELIVERY_DATE);
    assertEquals(test.getLastNoticeDate(), LAST_NOTICE_DATE);
    assertEquals(test.getLastDeliveryDate(), LAST_DELIVERY_DATE);
    assertEquals(test.getLastTradeDate(), LAST_TRADING_DATE);
    assertEquals(test.getRounding(), ROUNDING);

  }

  public void test_builder_fail() {
    // wrong size
    assertThrowsIllegalArg(() -> BondFuture.builder()
        .conversionFactor(CONVERSION_FACTOR)
        .deliveryBasket(SECURITY_LINK[0])
        .firstNoticeDate(FIRST_NOTICE_DATE)
        .lastNoticeDate(LAST_NOTICE_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build());
    // first notice date missing
    assertThrowsIllegalArg(() -> BondFuture.builder()
        .conversionFactor(CONVERSION_FACTOR)
        .deliveryBasket(SECURITY_LINK)
        .lastNoticeDate(LAST_NOTICE_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build());
    // last notice date missing
    assertThrowsIllegalArg(() -> BondFuture.builder()
        .conversionFactor(CONVERSION_FACTOR)
        .deliveryBasket(SECURITY_LINK)
        .firstNoticeDate(FIRST_NOTICE_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build());
    // basket list empty
    assertThrowsIllegalArg(() -> BondFuture.builder()
        .conversionFactor(CONVERSION_FACTOR)
        .firstNoticeDate(FIRST_NOTICE_DATE)
        .lastNoticeDate(LAST_NOTICE_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build());
    // notional mismatch
    LocalDate endDate = START_DATE[1].plus(BOND_TENOR[1]);
    PeriodicSchedule periodSchedule = PeriodicSchedule.of(
        START_DATE[1], endDate, Frequency.P6M, BUSINESS_ADJUST, StubConvention.SHORT_INITIAL, false);
    FixedCouponBond productNotional = FixedCouponBond.builder()
        .dayCount(DAY_COUNT)
        .fixedRate(RATE[1])
        .legalEntityId(ISSUER_ID)
        .currency(USD)
        .notional(1.0e4)
        .periodicSchedule(periodSchedule)
        .settlementDateOffset(SETTLEMENT_DAYS)
        .yieldConvention(YIELD_CONVENTION)
        .exCouponPeriod(EX_COUPON)
        .build();
    Security<FixedCouponBond> securityNotional = UnitSecurity.builder(productNotional).standardId(SECURITY_ID).build();
    SecurityLink<FixedCouponBond> securityLinkNotional = SecurityLink.resolved(securityNotional);
    assertThrowsIllegalArg(() -> BondFuture.builder()
        .conversionFactor(CONVERSION_FACTOR[0], CONVERSION_FACTOR[1])
        .deliveryBasket(SECURITY_LINK[0], securityLinkNotional)
        .firstNoticeDate(FIRST_NOTICE_DATE)
        .lastNoticeDate(LAST_NOTICE_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build());
    // currency mismatch
    FixedCouponBond productCurrency = FixedCouponBond.builder()
        .dayCount(DAY_COUNT)
        .fixedRate(RATE[1])
        .legalEntityId(ISSUER_ID)
        .currency(Currency.EUR)
        .notional(NOTIONAL)
        .periodicSchedule(periodSchedule)
        .settlementDateOffset(SETTLEMENT_DAYS)
        .yieldConvention(YIELD_CONVENTION)
        .exCouponPeriod(EX_COUPON)
        .build();
    Security<FixedCouponBond> securityCurrency = UnitSecurity.builder(productCurrency).standardId(SECURITY_ID).build();
    SecurityLink<FixedCouponBond> securityLinkCurrency = SecurityLink.resolved(securityCurrency);
    assertThrowsIllegalArg(() -> BondFuture.builder()
        .conversionFactor(CONVERSION_FACTOR[0], CONVERSION_FACTOR[1])
        .deliveryBasket(SECURITY_LINK[0], securityLinkCurrency)
        .firstNoticeDate(FIRST_NOTICE_DATE)
        .lastNoticeDate(LAST_NOTICE_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build());
  }

  public void test_getBondSecurityBasket() {
    BondFuture test = BondFuture.builder()
        .conversionFactor(ImmutableList.copyOf(CONVERSION_FACTOR))
        .deliveryBasket(SECURITY_LINK)
        .firstNoticeDate(FIRST_NOTICE_DATE)
        .lastNoticeDate(LAST_NOTICE_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build();
    ImmutableList<Security<FixedCouponBond>> trades = test.getBondSecurityBasket();
    assertEquals(trades.size(), NB_BOND);
    for (int i = 0; i < NB_BOND; ++i) {
      assertEquals(trades.get(i), SECURITY_LINK[i].resolvedTarget());
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BondFuture test1 = BondFuture.builder()
        .conversionFactor(ImmutableList.copyOf(CONVERSION_FACTOR))
        .deliveryBasket(SECURITY_LINK)
        .firstNoticeDate(FIRST_NOTICE_DATE)
        .firstDeliveryDate(FIRST_DELIVERY_DATE)
        .lastNoticeDate(LAST_NOTICE_DATE)
        .lastDeliveryDate(LAST_DELIVERY_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build();
    coverImmutableBean(test1);
    BondFuture test2 = BondFuture.builder()
        .conversionFactor(0.9187)
        .deliveryBasket(SECURITY_LINK[3])
        .firstNoticeDate(FIRST_NOTICE_DATE.plusDays(7))
        .lastNoticeDate(LAST_NOTICE_DATE.plusDays(7))
        .lastTradeDate(LAST_TRADING_DATE.plusDays(7))
        .build();
    coverBeanEquals(test1, test2);
  }

  public void serialization() {
    BondFuture test = BondFuture.builder()
        .conversionFactor(ImmutableList.copyOf(CONVERSION_FACTOR))
        .deliveryBasket(SECURITY_LINK)
        .firstNoticeDate(FIRST_NOTICE_DATE)
        .firstDeliveryDate(FIRST_DELIVERY_DATE)
        .lastNoticeDate(LAST_NOTICE_DATE)
        .lastDeliveryDate(LAST_DELIVERY_DATE)
        .lastTradeDate(LAST_TRADING_DATE)
        .rounding(ROUNDING)
        .build();
    assertSerialization(test);
  }

}
