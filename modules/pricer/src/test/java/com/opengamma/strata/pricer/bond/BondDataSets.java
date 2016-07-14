/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.product.common.PutCall.CALL;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.bond.BondFuture;
import com.opengamma.strata.product.bond.BondFutureOption;
import com.opengamma.strata.product.bond.BondFutureOptionTrade;
import com.opengamma.strata.product.bond.BondFutureTrade;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.FixedCouponBondYieldConvention;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * Data sets of bond, bond future and bond future option.
 */
public final class BondDataSets {

  //      =====     Fixed coupon bonds, bond future, USD   =====      
  // Fixed coupon bonds
  private static final StandardId ISSUER_ID_USD = StandardId.of("OG-Ticker", "GOVT1");
  private static final FixedCouponBondYieldConvention YIELD_CONVENTION_USD = FixedCouponBondYieldConvention.US_STREET;
  /** Notional of underlying bond */
  public static final double NOTIONAL_USD = 100000.0;
  private static final HolidayCalendarId CALENDAR_USD = HolidayCalendarIds.SAT_SUN;
  private static final DaysAdjustment SETTLEMENT_DAYS_USD = DaysAdjustment.ofBusinessDays(1, CALENDAR_USD);
  private static final DayCount DAY_COUNT_USD = DayCounts.ACT_ACT_ICMA;
  private static final BusinessDayAdjustment BUSINESS_ADJUST_USD =
      BusinessDayAdjustment.of(FOLLOWING, CALENDAR_USD);
  private static final DaysAdjustment EX_COUPON_USD = DaysAdjustment.NONE;
  private static final int NB_BOND_USD = 7;
  private static final double[] RATE_USD = new double[] {0.01375, 0.02125, 0.0200, 0.02125, 0.0225, 0.0200, 0.0175 };
  private static final LocalDate[] START_DATE_USD = new LocalDate[] {LocalDate.of(2010, 11, 30),
    LocalDate.of(2010, 12, 31), LocalDate.of(2011, 1, 31), LocalDate.of(2008, 2, 29), LocalDate.of(2011, 3, 31),
    LocalDate.of(2011, 4, 30), LocalDate.of(2011, 5, 31) };
  private static final Period[] BOND_TENOR_USD = new Period[] {Period.ofYears(5), Period.ofYears(5), Period.ofYears(5),
    Period.ofYears(8), Period.ofYears(5), Period.ofYears(5), Period.ofYears(5) };
  private static final StandardId[] BOND_SECURITY_ID = new StandardId[] {StandardId.of("OG-Ticker", "GOVT1-BOND1"),
    StandardId.of("OG-Ticker", "GOVT1-BOND2"), StandardId.of("OG-Ticker", "GOVT1-BOND3"),
    StandardId.of("OG-Ticker", "GOVT1-BOND4"), StandardId.of("OG-Ticker", "GOVT1-BOND5"),
    StandardId.of("OG-Ticker", "GOVT1-BOND6"), StandardId.of("OG-Ticker", "GOVT1-BOND7") };
  /** Security link of underlying bond */
  public static final FixedCouponBond[] BOND_USD = new FixedCouponBond[NB_BOND_USD];
  static {
    for (int i = 0; i < NB_BOND_USD; ++i) {
      LocalDate endDate = START_DATE_USD[i].plus(BOND_TENOR_USD[i]);
      PeriodicSchedule periodSchedule = PeriodicSchedule.of(
          START_DATE_USD[i], endDate, Frequency.P6M, BUSINESS_ADJUST_USD, StubConvention.SHORT_INITIAL, false);
      FixedCouponBond product = FixedCouponBond.builder()
          .securityId(SecurityId.of(BOND_SECURITY_ID[i]))
          .dayCount(DAY_COUNT_USD)
          .fixedRate(RATE_USD[i])
          .legalEntityId(ISSUER_ID_USD)
          .currency(USD)
          .notional(NOTIONAL_USD)
          .accrualSchedule(periodSchedule)
          .settlementDateOffset(SETTLEMENT_DAYS_USD)
          .yieldConvention(YIELD_CONVENTION_USD)
          .exCouponPeriod(EX_COUPON_USD)
          .build();
      BOND_USD[i] = product;
    }
  }

  // Bond future 
  /** Conversion factors */
  public static final Double[] CONVERSION_FACTOR_USD = new Double[] {.8317, .8565, .8493, .8516, .8540, .8417, .8292 };
  private static final LocalDate LAST_TRADING_DATE_USD = LocalDate.of(2011, 9, 30);
  private static final LocalDate FIRST_NOTICE_DATE_USD = LocalDate.of(2011, 8, 31);
  private static final LocalDate LAST_NOTICE_DATE_USD = LocalDate.of(2011, 10, 4);
  /** Bond future product */
  private static final SecurityId FUTURE_SECURITY_ID_USD = SecurityId.of("OG-Ticker", "GOVT1-BOND-FUT");
  public static final BondFuture FUTURE_PRODUCT_USD = BondFuture.builder()
      .securityId(FUTURE_SECURITY_ID_USD)
      .deliveryBasket(BOND_USD)
      .conversionFactors(CONVERSION_FACTOR_USD)
      .firstNoticeDate(FIRST_NOTICE_DATE_USD)
      .lastNoticeDate(LAST_NOTICE_DATE_USD)
      .lastTradeDate(LAST_TRADING_DATE_USD)
      .build();
  /** trade date */
  public static final LocalDate TRADE_DATE_USD = LocalDate.of(2011, 6, 19);
  private static final TradeInfo TRADE_INFO_USD = TradeInfo.builder().tradeDate(TRADE_DATE_USD).build();
  /** Quantity of bond future trade */
  public static final long QUANTITY_USD = 1234l;
  /** Bond future trade */
  public static final BondFutureTrade FUTURE_TRADE_USD = BondFutureTrade.builder()
      .info(TRADE_INFO_USD)
      .product(FUTURE_PRODUCT_USD)
      .quantity(QUANTITY_USD)
      .price(1.1d)
      .build();
  /** Reference price */
  public static final double SETTLE_PRICE_USD = 1.2345d;

  //      =====     Fixed coupon bonds, bond future, EUR   =====      
  // bond basket
  private static final StandardId ISSUER_ID_EUR = StandardId.of("OG-Ticker", "GOVT2");
  private static final FixedCouponBondYieldConvention YIELD_CONVENTION_EUR = FixedCouponBondYieldConvention.DE_BONDS;
  /** Notional of underlying bond */
  public static final double NOTIONAL_EUR = 100000d;
  private static final HolidayCalendarId CALENDAR_EUR = HolidayCalendarIds.EUTA;
  private static final DaysAdjustment SETTLEMENT_DAYS_EUR = DaysAdjustment.ofBusinessDays(3, CALENDAR_EUR);
  private static final DayCount DAY_COUNT_EUR = DayCounts.ACT_ACT_ICMA;
  private static final BusinessDayAdjustment BUSINESS_ADJUST_EUR =
      BusinessDayAdjustment.of(FOLLOWING, CALENDAR_EUR);
  private static final DaysAdjustment EX_COUPON_EUR = DaysAdjustment.NONE;
  private static final int NB_BOND_EUR = 3;
  private static final double[] RATE_EUR = new double[] {0.0375, 0.0350, 0.0100 };
  private static final LocalDate[] START_DATE_EUR = new LocalDate[] {
    LocalDate.of(2013, 1, 4), LocalDate.of(2013, 7, 4), LocalDate.of(2013, 2, 22) };
  private static final Period[] BOND_TENOR_EUR = new Period[] {Period.ofYears(6), Period.ofYears(6), Period.ofYears(6) };
  private static final StandardId[] BOND_SECURITY_ID_EUR = new StandardId[] {StandardId.of("OG-Ticker", "GOVT2-BOND1"),
    StandardId.of("OG-Ticker", "GOVT2-BOND2"), StandardId.of("OG-Ticker", "GOVT2-BOND3") };
  public static final FixedCouponBond[] BOND_EUR = new FixedCouponBond[NB_BOND_EUR];
  static {
    for (int i = 0; i < NB_BOND_EUR; ++i) {
      LocalDate endDate = START_DATE_EUR[i].plus(BOND_TENOR_EUR[i]);
      PeriodicSchedule periodSchedule = PeriodicSchedule.of(
          START_DATE_EUR[i], endDate, Frequency.P12M, BUSINESS_ADJUST_EUR, StubConvention.SHORT_INITIAL, false);
      FixedCouponBond product = FixedCouponBond.builder()
          .securityId(SecurityId.of(BOND_SECURITY_ID_EUR[i]))
          .dayCount(DAY_COUNT_EUR)
          .fixedRate(RATE_EUR[i])
          .legalEntityId(ISSUER_ID_EUR)
          .currency(EUR)
          .notional(NOTIONAL_EUR)
          .accrualSchedule(periodSchedule)
          .settlementDateOffset(SETTLEMENT_DAYS_EUR)
          .yieldConvention(YIELD_CONVENTION_EUR)
          .exCouponPeriod(EX_COUPON_EUR)
          .build();
      BOND_EUR[i] = product;
    }
  }
  // Underlying future
  private static final Double[] CONVERSION_FACTOR_EUR = new Double[] {0.912067, 0.893437, 0.800111 };
  private static final LocalDate LAST_TRADING_DATE_EUR = LocalDate.of(2014, 6, 6);
  private static final LocalDate FIRST_NOTICE_DATE_EUR = LocalDate.of(2014, 6, 6);
  private static final LocalDate LAST_NOTICE_DATE_EUR = LocalDate.of(2014, 6, 6);
  public static final SecurityId FUTURE_SECURITY_ID_EUR = SecurityId.of("OG-Ticker", "GOVT2-BOND-FUT");
  private static final BondFuture FUTURE_PRODUCT_EUR = BondFuture.builder()
      .securityId(FUTURE_SECURITY_ID_EUR)
      .deliveryBasket(BOND_EUR)
      .conversionFactors(CONVERSION_FACTOR_EUR)
      .firstNoticeDate(FIRST_NOTICE_DATE_EUR)
      .lastNoticeDate(LAST_NOTICE_DATE_EUR)
      .lastTradeDate(LAST_TRADING_DATE_EUR)
      .build();
  // future option
  private static final LocalDate EXPIRY_DATE_EUR = LocalDate.of(2014, 6, 5);
  private static final LocalTime EXPIRY_TIME_EUR = LocalTime.of(0, 0);
  private static final ZoneId EXPIRY_ZONE_EUR = ZoneId.of("Z");
  private static final double STRIKE_PRICE_116 = 1.16;
  /** Bond future option product, strike = 1.16 */
  private static final SecurityId OPTION_SECURITY_ID_116 = SecurityId.of("OG-Ticker", "GOVT1-BOND-FUT-OPT-116");
  public static final BondFutureOption FUTURE_OPTION_PRODUCT_EUR_116 = BondFutureOption.builder()
      .securityId(OPTION_SECURITY_ID_116)
      .putCall(CALL)
      .strikePrice(STRIKE_PRICE_116)
      .expiryDate(EXPIRY_DATE_EUR)
      .expiryTime(EXPIRY_TIME_EUR)
      .expiryZone(EXPIRY_ZONE_EUR)
      .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
      .underlyingFuture(FUTURE_PRODUCT_EUR)
      .build();
  private static final double STRIKE_PRICE_115 = 1.15;
  /** Bond future option product, strike = 1.15 */
  private static final SecurityId OPTION_SECURITY_ID_115 = SecurityId.of("OG-Ticker", "GOVT1-BOND-FUT-OPT-115");
  public static final BondFutureOption FUTURE_OPTION_PRODUCT_EUR_115 = BondFutureOption.builder()
      .securityId(OPTION_SECURITY_ID_115)
      .putCall(CALL)
      .strikePrice(STRIKE_PRICE_115)
      .expiryDate(EXPIRY_DATE_EUR)
      .expiryTime(EXPIRY_TIME_EUR)
      .expiryZone(EXPIRY_ZONE_EUR)
      .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
      .underlyingFuture(FUTURE_PRODUCT_EUR)
      .build();
  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 3, 31);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(TRADE_DATE).build();
  /** Quantity of bond future trade */
  public static final long QUANTITY_EUR = 1234L;
  /** Bond future option trade */
  public static final BondFutureOptionTrade FUTURE_OPTION_TRADE_EUR = BondFutureOptionTrade.builder()
      .info(TRADE_INFO)
      .product(FUTURE_OPTION_PRODUCT_EUR_115)
      .quantity(QUANTITY_EUR)
      .price(0.01)
      .build();

}
