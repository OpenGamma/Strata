/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.rate.bond.BondFuture;
import com.opengamma.strata.product.rate.bond.BondFutureTrade;
import com.opengamma.strata.product.rate.bond.FixedCouponBond;
import com.opengamma.strata.product.rate.bond.YieldConvention;

/**
 * Bond and bond future data sets for testing. 
 */
public final class BondDataSets {

  //      =====     Fixed coupon bonds, bond future, USD   =====      
  // Fixed coupon bonds
  private static final StandardId ISSUER_ID = StandardId.of("OG-Ticker", "GOVT1");
  private static final YieldConvention YIELD_CONVENTION = YieldConvention.US_STREET;
  /** Notional of underlying bond */
  public static final double NOTIONAL = 100000.0;
  private static final HolidayCalendar CALENDAR = HolidayCalendars.SAT_SUN;
  private static final DaysAdjustment SETTLEMENT_DAYS = DaysAdjustment.ofBusinessDays(1, CALENDAR);
  private static final DayCount DAY_COUNT = DayCounts.ACT_ACT_ICMA;
  private static final BusinessDayAdjustment BUSINESS_ADJUST = BusinessDayAdjustment.of(FOLLOWING, CALENDAR);
  private static final DaysAdjustment EX_COUPON = DaysAdjustment.NONE;
  private static final int NB_BOND = 7;
  private static final double[] RATE = new double[] {0.01375, 0.02125, 0.0200, 0.02125, 0.0225, 0.0200, 0.0175};
  private static final LocalDate[] START_DATE = new LocalDate[] {LocalDate.of(2010, 11, 30),
    LocalDate.of(2010, 12, 31), LocalDate.of(2011, 1, 31), LocalDate.of(2008, 2, 29), LocalDate.of(2011, 3, 31),
    LocalDate.of(2011, 4, 30), LocalDate.of(2011, 5, 31) };
  private static final Period[] BOND_TENOR = new Period[] {Period.ofYears(5), Period.ofYears(5), Period.ofYears(5),
    Period.ofYears(8), Period.ofYears(5), Period.ofYears(5), Period.ofYears(5) };
  private static final StandardId[] BOND_SECURITY_ID = new StandardId[] {StandardId.of("OG-Ticker", "GOVT1-BOND1"),
    StandardId.of("OG-Ticker", "GOVT1-BOND2"), StandardId.of("OG-Ticker", "GOVT1-BOND3"),
    StandardId.of("OG-Ticker", "GOVT1-BOND4"), StandardId.of("OG-Ticker", "GOVT1-BOND5"),
    StandardId.of("OG-Ticker", "GOVT1-BOND6"), StandardId.of("OG-Ticker", "GOVT1-BOND7") };
  /** Security link of underlying bond */
  @SuppressWarnings("unchecked")
  public static final SecurityLink<FixedCouponBond>[] BOND_SECURITY_LINK = new SecurityLink[NB_BOND];
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
      Security<FixedCouponBond> bondSecurity = UnitSecurity.builder(product).standardId(BOND_SECURITY_ID[i]).build();
      BOND_SECURITY_LINK[i] = SecurityLink.resolved(bondSecurity);
    }
  }

  // Bond future 
  /** Conversion factors */
  public static final Double[] CONVERSION_FACTOR = new Double[] {.8317, .8565, .8493, .8516, .8540, .8417, .8292};
  private static final LocalDate LAST_TRADING_DATE = LocalDate.of(2011, 9, 30);
  private static final LocalDate FIRST_NOTICE_DATE = LocalDate.of(2011, 8, 31);
  private static final LocalDate LAST_NOTICE_DATE = LocalDate.of(2011, 10, 4);
  /** Bond future product */
  public static final BondFuture FUTURE_PRODUCT = BondFuture.builder()
      .conversionFactor(CONVERSION_FACTOR)
      .deliveryBasket(BOND_SECURITY_LINK)
      .firstNoticeDate(FIRST_NOTICE_DATE)
      .lastNoticeDate(LAST_NOTICE_DATE)
      .lastTradeDate(LAST_TRADING_DATE)
      .build();
  /** trade date */
  public static final LocalDate TRADE_DATE = LocalDate.of(2011, 6, 20);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(TRADE_DATE).build();
  private static final StandardId FUTURE_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND-FUT");
  private static final Security<BondFuture> FUTURE_SECURITY = UnitSecurity.builder(FUTURE_PRODUCT).standardId(FUTURE_SECURITY_ID).build();
  private static final SecurityLink<BondFuture> FUTURE_SECURITY_LINK = SecurityLink.resolved(FUTURE_SECURITY);
  /** Quantity of bond future trade */
  public static final long QUANTITY = 1234l;
  /** Bond future trade */
  public static final BondFutureTrade FUTURE_TRADE = BondFutureTrade.builder()
      .initialPrice(1.1d)
      .quantity(QUANTITY)
      .securityLink(FUTURE_SECURITY_LINK)
      .tradeInfo(TRADE_INFO)
      .build();
  /** Reference price */
  public static final double REFERENCE_PRICE = 1.2345d;

}
