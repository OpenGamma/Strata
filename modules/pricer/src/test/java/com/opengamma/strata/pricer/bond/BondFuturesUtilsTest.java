/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.FixedCouponBondYieldConvention;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBond;

/**
 * Test {@link BondFuturesUtils}.
 */
public class BondFuturesUtilsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  /* Tests */
  private static final double TOLERANCE_CF_EUREX = 1.0E-6;
  private static final double TOLERANCE_PF_ICE = 1.0E-7;
  private static final double TOLERANCE_CF_CME = 1.0E-4;

  /*      =====     Eurex conversion factor - German bond      =====     */
  private static final LegalEntityId ISSUER_ID_DE = LegalEntityId.of("OG-Ticker", "GOVT-DE");
  private static final FixedCouponBondYieldConvention YIELD_CONVENTION_DE = FixedCouponBondYieldConvention.DE_BONDS;
  public static final double NOTIONAL_DE = 100000d;
  private static final HolidayCalendarId CALENDAR_DE = HolidayCalendarIds.EUTA;
  private static final DaysAdjustment SETTLEMENT_DAYS_DE = DaysAdjustment.ofBusinessDays(2, CALENDAR_DE);
  private static final DayCount DAY_COUNT_DE = DayCounts.ACT_ACT_ICMA;
  private static final BusinessDayAdjustment BUSINESS_ADJUST_DE =
      BusinessDayAdjustment.of(FOLLOWING, CALENDAR_DE);
  private static final DaysAdjustment EX_COUPON_DE = DaysAdjustment.NONE;
  private static final int NB_BOND_DE = 5;
  private static final double[] RATE_DE = new double[] {0.00, 0.0170, 0.00, 0.0050, 0.0125};
  private static final LocalDate[] START_DATE_DE = new LocalDate[] {
      LocalDate.of(2021, 8, 15), LocalDate.of(2022, 7, 8), LocalDate.of(2022, 2, 15), LocalDate.of(2018, 2, 15),
      LocalDate.of(2018, 8, 15)};
  private static final LocalDate[] END_DATE_DE = new LocalDate[] {
      LocalDate.of(2031, 8, 15), LocalDate.of(2032, 8, 15), LocalDate.of(2032, 2, 15), LocalDate.of(2028, 2, 15),
      LocalDate.of(2048, 8, 15)};
  private static final StandardId[] BOND_SECURITY_ID_DE = new StandardId[] {
      StandardId.of("OG-Ticker", "DE0001102564"),
      StandardId.of("OG-Ticker", "DE0001102606"),
      StandardId.of("OG-Ticker", "DE0001102580"),
      StandardId.of("OG-Ticker", "DE0001102440"),
      StandardId.of("OG-Ticker", "DE0001102432")};
  public static final ResolvedFixedCouponBond[] BOND_DE = new ResolvedFixedCouponBond[NB_BOND_DE];
  static {
    for (int i = 0; i < NB_BOND_DE; ++i) {
      PeriodicSchedule periodSchedule = PeriodicSchedule.of(
          START_DATE_DE[i], END_DATE_DE[i], Frequency.P12M, BUSINESS_ADJUST_DE, StubConvention.LONG_INITIAL, false);
      FixedCouponBond product = FixedCouponBond.builder()
          .securityId(SecurityId.of(BOND_SECURITY_ID_DE[i]))
          .dayCount(DAY_COUNT_DE)
          .fixedRate(RATE_DE[i])
          .legalEntityId(ISSUER_ID_DE)
          .currency(EUR)
          .notional(NOTIONAL_DE)
          .accrualSchedule(periodSchedule)
          .settlementDateOffset(SETTLEMENT_DAYS_DE)
          .yieldConvention(YIELD_CONVENTION_DE)
          .exCouponPeriod(EX_COUPON_DE)
          .build();
      BOND_DE[i] = product.resolve(REF_DATA);
    }
  }
  private static final LocalDate[] SETTLE_DATE_DE = new LocalDate[] {
      LocalDate.of(2022, 9, 12), LocalDate.of(2022, 9, 12), LocalDate.of(2023, 3, 10), LocalDate.of(2022, 9, 12),
      LocalDate.of(2023, 3, 10)};
  private static final double[] NOTIONAL_COUPON_DE = {0.06, 0.06, 0.06, 0.06, 0.04};
  private static final double[] CONVERSION_FACTOR_EXPECTED_DE =
      new double[] {0.59455, 0.685182, 0.594076, 0.751436, 0.565991}; // Hard-coded values checked with external providers

  @Test
  public void cf_eurex() {
    for (int i = 0; i < NB_BOND_DE; ++i) {
      double cfComputed = BondFuturesUtils.conversionFactorEurexDE(BOND_DE[i], SETTLE_DATE_DE[i], NOTIONAL_COUPON_DE[i]);
      assertThat(cfComputed).isCloseTo(CONVERSION_FACTOR_EXPECTED_DE[i], offset(TOLERANCE_CF_EUREX));
    }
  }

  /*      =====     ICE conversion factor - Gilts     =====      */
  private static final LegalEntityId ISSUER_ID_UK = LegalEntityId.of("OG-Ticker", "GOVT-UK");
  private static final FixedCouponBondYieldConvention YIELD_CONVENTION_UK = FixedCouponBondYieldConvention.GB_BUMP_DMO;
  public static final double NOTIONAL_UK = 100000d;
  private static final HolidayCalendarId CALENDAR_UK = HolidayCalendarIds.GBLO;
  private static final DaysAdjustment SETTLEMENT_DAYS_UK = DaysAdjustment.ofBusinessDays(2, CALENDAR_UK);
  private static final DayCount DAY_COUNT_UK = DayCounts.ACT_ACT_ICMA;
  private static final BusinessDayAdjustment BUSINESS_ADJUST_UK =
      BusinessDayAdjustment.of(FOLLOWING, CALENDAR_UK);
  private static final DaysAdjustment EX_COUPON_UK = DaysAdjustment.ofCalendarDays(-7,
      BusinessDayAdjustment.of(BusinessDayConventions.PRECEDING, SAT_SUN));
  private static final int NB_BOND_UK = 4;
  private static final double[] RATE_UK = new double[] {0.0100, 0.0125, 0.0450, 0.00625};
  private static final LocalDate[] START_DATE_UK = new LocalDate[] {
      LocalDate.of(2021, 4, 22), LocalDate.of(2022, 7, 22), LocalDate.of(2022, 9, 7), LocalDate.of(2020, 10, 22)};
  private static final LocalDate[] END_DATE_UK = new LocalDate[] {
      LocalDate.of(2024, 4, 22), LocalDate.of(2027, 7, 22), LocalDate.of(2034, 9, 7), LocalDate.of(2050, 10, 22)};
  private static final StandardId[] BOND_SECURITY_ID_UK = new StandardId[] {
      StandardId.of("OG-Ticker", "GB00BFWFPL34"),
      StandardId.of("OG-Ticker", "GB00BDRHNP05"),
      StandardId.of("OG-Ticker", "GB00B52WS153"),
      StandardId.of("OG-Ticker", "GB00BMBL1F74")};
  public static final ResolvedFixedCouponBond[] BOND_UK = new ResolvedFixedCouponBond[NB_BOND_UK];
  static {
    for (int i = 0; i < NB_BOND_UK; ++i) {
      PeriodicSchedule periodSchedule = PeriodicSchedule.of(
          START_DATE_UK[i], END_DATE_UK[i], Frequency.P6M, BUSINESS_ADJUST_UK, StubConvention.LONG_INITIAL, false);
      FixedCouponBond product = FixedCouponBond.builder()
          .securityId(SecurityId.of(BOND_SECURITY_ID_UK[i]))
          .dayCount(DAY_COUNT_UK)
          .fixedRate(RATE_UK[i])
          .legalEntityId(ISSUER_ID_UK)
          .currency(GBP)
          .notional(NOTIONAL_UK)
          .accrualSchedule(periodSchedule)
          .settlementDateOffset(SETTLEMENT_DAYS_UK)
          .yieldConvention(YIELD_CONVENTION_UK)
          .exCouponPeriod(EX_COUPON_UK)
          .build();
      BOND_UK[i] = product.resolve(REF_DATA);
    }
  }
  private static final LocalDate[] SETTLE_DATE_UK = new LocalDate[] {
      LocalDate.of(2022, 9, 1), LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 1), LocalDate.of(2022, 9, 1)};
  private static final double[] NOTIONAL_COUPON_UK = {0.03, 0.04, 0.04, 0.04};
  private static final double[] CONVERSION_FACTOR_EXPECTED_UK =
      new double[] {0.9682306, 0.8845462, 1.0465032, 0.4330701}; // Hard-coded values checked with external providers

  @Test
  public void pf_ice() {
    for (int i = 0; i < NB_BOND_UK; ++i) {
      double pfComputed = BondFuturesUtils.priceFactorIceUK(BOND_UK[i], SETTLE_DATE_UK[i], NOTIONAL_COUPON_UK[i]);
      assertThat(pfComputed).isCloseTo(CONVERSION_FACTOR_EXPECTED_UK[i], offset(TOLERANCE_PF_ICE));
    }
  }

  /*      =====     CME conversion factor - US Treasury      =====     */
  private static final LegalEntityId ISSUER_ID_US = LegalEntityId.of("OG-Ticker", "GOVT-US");
  private static final FixedCouponBondYieldConvention YIELD_CONVENTION_US = FixedCouponBondYieldConvention.GB_BUMP_DMO;
  public static final double NOTIONAL_US = 100000d;
  private static final HolidayCalendarId CALENDAR_US = HolidayCalendarIds.USGS;
  private static final DaysAdjustment SETTLEMENT_DAYS_US = DaysAdjustment.ofBusinessDays(2, CALENDAR_US);
  private static final DayCount DAY_COUNT_US = DayCounts.ACT_ACT_ICMA;
  private static final BusinessDayAdjustment BUSINESS_ADJUST_US =
      BusinessDayAdjustment.of(FOLLOWING, CALENDAR_US);
  private static final DaysAdjustment EX_COUPON_US = DaysAdjustment.NONE;
  private static final int NB_BOND_US = 5;
  private static final double[] RATE_US = new double[] {0.0150, 0.01125, 0.0275, 0.0375, 0.0450};
  private static final LocalDate[] START_DATE_US = new LocalDate[] {
      LocalDate.of(2008, 10, 31), LocalDate.of(2009, 1, 15), LocalDate.of(2008, 10, 31), LocalDate.of(2008, 11, 15),
      LocalDate.of(2008, 5, 15)};
  private static final LocalDate[] END_DATE_US = new LocalDate[] {
      LocalDate.of(2010, 10, 31), LocalDate.of(2012, 1, 15), LocalDate.of(2013, 10, 31), LocalDate.of(2018, 11, 15),
      LocalDate.of(2038, 5, 15)};
  private static final StandardId[] BOND_SECURITY_ID_US = new StandardId[] {
      StandardId.of("OG-Ticker", "T-1.50-2010-10-31"),
      StandardId.of("OG-Ticker", "T-1.125-2012-01-15"),
      StandardId.of("OG-Ticker", "T-2.75-2013-10-31"),
      StandardId.of("OG-Ticker", "T-3.75-2018-11-15"),
      StandardId.of("OG-Ticker", "T-4.50-2038-05-15")};
  public static final ResolvedFixedCouponBond[] BOND_US = new ResolvedFixedCouponBond[NB_BOND_US];
  static {
    for (int i = 0; i < NB_BOND_US; ++i) {
      PeriodicSchedule periodSchedule = PeriodicSchedule.of(
          START_DATE_US[i], END_DATE_US[i], Frequency.P6M, BUSINESS_ADJUST_US, StubConvention.LONG_INITIAL, false);
      FixedCouponBond product = FixedCouponBond.builder()
          .securityId(SecurityId.of(BOND_SECURITY_ID_US[i]))
          .dayCount(DAY_COUNT_US)
          .fixedRate(RATE_US[i])
          .legalEntityId(ISSUER_ID_US)
          .currency(GBP)
          .notional(NOTIONAL_US)
          .accrualSchedule(periodSchedule)
          .settlementDateOffset(SETTLEMENT_DAYS_US)
          .yieldConvention(YIELD_CONVENTION_US)
          .exCouponPeriod(EX_COUPON_US)
          .build();
      BOND_US[i] = product.resolve(REF_DATA);
    }
  }
  private static final LocalDate[] SETTLE_DATE_US = new LocalDate[] {
      LocalDate.of(2008, 12, 1), LocalDate.of(2009, 3, 1), LocalDate.of(2008, 12, 1), LocalDate.of(2008, 12, 1),
      LocalDate.of(2008, 12, 1)};
  private static final double[] NOTIONAL_COUPON_US = {0.06, 0.06, 0.06, 0.06, 0.06};
  private static final double[] CONVERSION_FACTOR_EXPECTED_US =
      new double[] {0.9229, 0.8747, 0.8653, 0.8357, 0.7943}; // Hard-coded values checked with external providers

  @Test
  public void cf_cme() {
    for (int i = 0; i < 3; ++i) {
      double cfComputed = BondFuturesUtils.conversionFactorCmeUsShort(BOND_US[i], SETTLE_DATE_US[i], NOTIONAL_COUPON_US[i]);
      assertThat(cfComputed).isCloseTo(CONVERSION_FACTOR_EXPECTED_US[i], offset(TOLERANCE_CF_CME));
    }
    for (int i = 3; i < NB_BOND_US; ++i) {
      double cfComputed = BondFuturesUtils.conversionFactorCmeUsLong(BOND_US[i], SETTLE_DATE_US[i], NOTIONAL_COUPON_US[i]);
      assertThat(cfComputed).isCloseTo(CONVERSION_FACTOR_EXPECTED_US[i], offset(TOLERANCE_CF_CME));
    }
  }

}
