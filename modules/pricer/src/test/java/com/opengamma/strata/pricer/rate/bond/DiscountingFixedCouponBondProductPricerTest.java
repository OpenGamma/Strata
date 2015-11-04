/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.value.CompoundedRateType.CONTINUOUS;
import static com.opengamma.strata.market.value.CompoundedRateType.PERIODIC;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.BondGroup;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.IssuerCurveDiscountFactors;
import com.opengamma.strata.market.value.LegalEntityGroup;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.impl.bond.DiscountingFixedCouponBondPaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.rate.bond.ExpandedFixedCouponBond;
import com.opengamma.strata.product.rate.bond.FixedCouponBond;
import com.opengamma.strata.product.rate.bond.FixedCouponBondPaymentPeriod;
import com.opengamma.strata.product.rate.bond.YieldConvention;

/**
 * Test
 */
@Test
public class DiscountingFixedCouponBondProductPricerTest {
  // fixed coupon bond
  private static final StandardId SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND1");
  private static final StandardId ISSUER_ID = StandardId.of("OG-Ticker", "GOVT1");
  private static final LocalDate VALUATION = date(2016, 4, 25);
  private static final YieldConvention YIELD_CONVENTION = YieldConvention.GERMAN_BONDS;
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
  private static final DaysAdjustment EX_COUPON = DaysAdjustment.ofBusinessDays(-5, EUR_CALENDAR, BUSINESS_ADJUST);
  /** nonzero ex-coupon period */
  private static final FixedCouponBond PRODUCT = FixedCouponBond.builder()
      .dayCount(DAY_COUNT)
      .fixedRate(FIXED_RATE)
      .legalEntityId(ISSUER_ID)
      .currency(EUR)
      .notional(NOTIONAL)
      .periodicSchedule(PERIOD_SCHEDULE)
      .settlementDateOffset(DATE_OFFSET)
      .yieldConvention(YIELD_CONVENTION)
      .exCouponPeriod(EX_COUPON)
      .build();
  private static final Security<FixedCouponBond> BOND_SECURITY =
      UnitSecurity.builder(PRODUCT).standardId(SECURITY_ID).build();
  /** no ex-coupon period */
  private static final FixedCouponBond PRODUCT_NO_EXCOUPON = FixedCouponBond.builder()
      .dayCount(DAY_COUNT)
      .fixedRate(FIXED_RATE)
      .legalEntityId(ISSUER_ID)
      .currency(EUR)
      .notional(NOTIONAL)
      .periodicSchedule(PERIOD_SCHEDULE)
      .settlementDateOffset(DATE_OFFSET)
      .yieldConvention(YIELD_CONVENTION)
      .build();

  // rates provider
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME_REPO = CurveName.of("TestRepoCurve");
  private static final CurveMetadata METADATA_REPO = Curves.zeroRates(NAME_REPO, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_REPO = InterpolatedNodalCurve.of(
      METADATA_REPO, DoubleArray.of(0.1, 2.0, 10.0), DoubleArray.of(0.05, 0.06, 0.09), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_REPO = ZeroRateDiscountFactors.of(EUR, VALUATION, CURVE_REPO);
  private static final BondGroup GROUP_REPO = BondGroup.of("GOVT1 BOND1");
  private static final CurveName NAME_ISSUER = CurveName.of("TestIssuerCurve");
  private static final CurveMetadata METADATA_ISSUER = Curves.zeroRates(NAME_ISSUER, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_ISSUER = InterpolatedNodalCurve.of(
      METADATA_ISSUER, DoubleArray.of(0.2, 9.0, 15.0), DoubleArray.of(0.03, 0.05, 0.13), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_ISSUER = ZeroRateDiscountFactors.of(EUR, VALUATION, CURVE_ISSUER);
  private static final LegalEntityGroup GROUP_ISSUER = LegalEntityGroup.of("GOVT1");
  private static final LegalEntityDiscountingProvider PROVIDER = LegalEntityDiscountingProvider.builder()
      .issuerCurves(ImmutableMap.<Pair<LegalEntityGroup, Currency>, DiscountFactors>of(
          Pair.<LegalEntityGroup, Currency>of(GROUP_ISSUER, EUR), DSC_FACTORS_ISSUER))
      .legalEntityMap(ImmutableMap.<StandardId, LegalEntityGroup>of(ISSUER_ID, GROUP_ISSUER))
      .repoCurves(ImmutableMap.<Pair<BondGroup, Currency>, DiscountFactors>of(
          Pair.<BondGroup, Currency>of(GROUP_REPO, EUR), DSC_FACTORS_REPO))
      .bondMap(ImmutableMap.<StandardId, BondGroup>of(SECURITY_ID, GROUP_REPO))
      .valuationDate(VALUATION)
      .build();

  private static final double Z_SPREAD = 0.035;
  private static final int PERIOD_PER_YEAR = 4;
  private static final double TOL = 1.0e-12;
  private static final double EPS = 1.0e-6;
  
  // pricers
  private static final DiscountingFixedCouponBondProductPricer PRICER = DiscountingFixedCouponBondProductPricer.DEFAULT;
  private static final DiscountingPaymentPricer PRICER_NOMINAL = DiscountingPaymentPricer.DEFAULT;
  private static final DiscountingFixedCouponBondPaymentPeriodPricer PRICER_COUPON =
      DiscountingFixedCouponBondPaymentPeriodPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL = new RatesFiniteDifferenceSensitivityCalculator(EPS);

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount computed = PRICER.presentValue(PRODUCT, PROVIDER);
    ExpandedFixedCouponBond expanded = PRODUCT.expand();
    CurrencyAmount expected = PRICER_NOMINAL.presentValue(expanded.getNominalPayment(), DSC_FACTORS_ISSUER);
    int size = expanded.getPeriodicPayments().size();
    double pvCupon = 0d;
    for (int i = 2; i < size; ++i) {
      FixedCouponBondPaymentPeriod payment = expanded.getPeriodicPayments().get(i);
      pvCupon += PRICER_COUPON.presentValue(payment, IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER));
    }
    expected = expected.plus(pvCupon);
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValueWithZSpread_continuous() {
    CurrencyAmount computed = PRICER.presentValueWithZSpread(PRODUCT, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    ExpandedFixedCouponBond expanded = PRODUCT.expand();
    CurrencyAmount expected = PRICER_NOMINAL.presentValue(
        expanded.getNominalPayment(), DSC_FACTORS_ISSUER, Z_SPREAD, CONTINUOUS, 0);
    int size = expanded.getPeriodicPayments().size();
    double pvcCupon = 0d;
    for (int i = 2; i < size; ++i) {
      FixedCouponBondPaymentPeriod payment = expanded.getPeriodicPayments().get(i);
      pvcCupon += PRICER_COUPON.presentValueWithSpread(payment, 
          IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER), Z_SPREAD, CONTINUOUS, 0);
    }
    expected = expected.plus(pvcCupon);
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValueWithZSpread_periodic() {
    CurrencyAmount computed = PRICER.presentValueWithZSpread(
        PRODUCT, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    ExpandedFixedCouponBond expanded = PRODUCT.expand();
    CurrencyAmount expected = PRICER_NOMINAL.presentValue(
        expanded.getNominalPayment(), DSC_FACTORS_ISSUER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    int size = expanded.getPeriodicPayments().size();
    double pvcCupon = 0d;
    for (int i = 2; i < size; ++i) {
      FixedCouponBondPaymentPeriod payment = expanded.getPeriodicPayments().get(i);
      pvcCupon += PRICER_COUPON.presentValueWithSpread(payment,
          IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    }
    expected = expected.plus(pvcCupon);
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValue_noExcoupon() {
    CurrencyAmount computed = PRICER.presentValue(PRODUCT_NO_EXCOUPON, PROVIDER);
    ExpandedFixedCouponBond expanded = PRODUCT.expand();
    CurrencyAmount expected = PRICER_NOMINAL.presentValue(expanded.getNominalPayment(), DSC_FACTORS_ISSUER);
    int size = expanded.getPeriodicPayments().size();
    double pvcCupon = 0d;
    for (int i = 2; i < size; ++i) {
      FixedCouponBondPaymentPeriod payment = expanded.getPeriodicPayments().get(i);
      pvcCupon += PRICER_COUPON.presentValue(payment, IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER));
    }
    expected = expected.plus(pvcCupon);
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValueWithZSpread_continuous_noExcoupon() {
    CurrencyAmount computed = PRICER.presentValueWithZSpread(
        PRODUCT_NO_EXCOUPON, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    ExpandedFixedCouponBond expanded = PRODUCT.expand();
    CurrencyAmount expected = PRICER_NOMINAL.presentValue(
        expanded.getNominalPayment(), DSC_FACTORS_ISSUER, Z_SPREAD, CONTINUOUS, 0);
    int size = expanded.getPeriodicPayments().size();
    double pvcCupon = 0d;
    for (int i = 2; i < size; ++i) {
      FixedCouponBondPaymentPeriod payment = expanded.getPeriodicPayments().get(i);
      pvcCupon += PRICER_COUPON.presentValueWithSpread(payment,
          IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER), Z_SPREAD, CONTINUOUS, 0);
    }
    expected = expected.plus(pvcCupon);
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValueWithZSpread_periodic_noExcoupon() {
    CurrencyAmount computed = PRICER.presentValueWithZSpread(
        PRODUCT_NO_EXCOUPON, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    ExpandedFixedCouponBond expanded = PRODUCT.expand();
    CurrencyAmount expected = PRICER_NOMINAL.presentValue(
        expanded.getNominalPayment(), DSC_FACTORS_ISSUER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    int size = expanded.getPeriodicPayments().size();
    double pvcCupon = 0d;
    for (int i = 2; i < size; ++i) {
      FixedCouponBondPaymentPeriod payment = expanded.getPeriodicPayments().get(i);
      pvcCupon += PRICER_COUPON.presentValueWithSpread(payment, 
          IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    }
    expected = expected.plus(pvcCupon);
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected.getAmount(), NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_dirtyPriceFromCurves() {
    double computed = PRICER.dirtyPriceFromCurves(BOND_SECURITY, PROVIDER);
    CurrencyAmount pv = PRICER.presentValue(PRODUCT, PROVIDER);
    LocalDate settlement = DATE_OFFSET.adjust(VALUATION);
    double df = DSC_FACTORS_REPO.discountFactor(settlement);
    assertEquals(computed, pv.getAmount() / df / NOTIONAL);
  }

  public void test_dirtyPriceFromCurvesWithZSpread_continuous() {
    double computed = PRICER.dirtyPriceFromCurvesWithZSpread(
        BOND_SECURITY, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    CurrencyAmount pv = PRICER.presentValueWithZSpread(PRODUCT, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    LocalDate settlement = DATE_OFFSET.adjust(VALUATION);
    double df = DSC_FACTORS_REPO.discountFactor(settlement);
    assertEquals(computed, pv.getAmount() / df / NOTIONAL);
  }

  public void test_dirtyPriceFromCurvesWithZSpread_periodic() {
    double computed = PRICER.dirtyPriceFromCurvesWithZSpread(
        BOND_SECURITY, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount pv = PRICER.presentValueWithZSpread(
        PRODUCT, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    LocalDate settlement = DATE_OFFSET.adjust(VALUATION);
    double df = DSC_FACTORS_REPO.discountFactor(settlement);
    assertEquals(computed, pv.getAmount() / df / NOTIONAL);
  }

  public void test_dirtyPriceFromCleanPrice_cleanPriceFromDirtyPrice() {
    double dirtyPrice = PRICER.dirtyPriceFromCurves(BOND_SECURITY, PROVIDER);
    LocalDate settlement = DATE_OFFSET.adjust(VALUATION);
    double cleanPrice = PRICER.cleanPriceFromDirtyPrice(PRODUCT, settlement, dirtyPrice);
    double accruedInterest = PRICER.accruedInterest(PRODUCT, settlement);
    assertEquals(cleanPrice, dirtyPrice - accruedInterest / NOTIONAL, NOTIONAL * TOL);
    double dirtyPriceRe = PRICER.dirtyPriceFromCleanPrice(PRODUCT, settlement, cleanPrice);
    assertEquals(dirtyPriceRe, dirtyPrice, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_zSpreadFromCurvesAndPV_continuous() {
    double dirtyPrice = PRICER.dirtyPriceFromCurvesWithZSpread(
        BOND_SECURITY, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    double computed = PRICER.zSpreadFromCurvesAndDirtyPrice(
        BOND_SECURITY, PROVIDER, dirtyPrice, CONTINUOUS, 0);
    assertEquals(computed, Z_SPREAD, TOL);
  }

  public void test_zSpreadFromCurvesAndPV_periodic() {
    double dirtyPrice = PRICER.dirtyPriceFromCurvesWithZSpread(
        BOND_SECURITY, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double computed = PRICER.zSpreadFromCurvesAndDirtyPrice(
        BOND_SECURITY, PROVIDER, dirtyPrice, PERIODIC, PERIOD_PER_YEAR);
    assertEquals(computed, Z_SPREAD, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivity(PRODUCT, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(PROVIDER, (p) -> PRICER.presentValue(PRODUCT, (p)));
    assertTrue(computed.equalWithTolerance(expected, 30d * NOTIONAL * EPS));
  }

  public void test_presentValueSensitivityWithZSpread_continuous() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivityWithZSpread(PRODUCT, PROVIDER, Z_SPREAD, CONTINUOUS,
        0);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(
        PROVIDER, (p) -> PRICER.presentValueWithZSpread(PRODUCT, (p), Z_SPREAD, CONTINUOUS, 0));
    assertTrue(computed.equalWithTolerance(expected, 20d * NOTIONAL * EPS));
  }

  public void test_presentValueSensitivityWithZSpread_periodic() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivityWithZSpread(
        PRODUCT, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(PROVIDER, 
        (p) -> PRICER.presentValueWithZSpread(PRODUCT, (p), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR));
    assertTrue(computed.equalWithTolerance(expected, 20d * NOTIONAL * EPS));
  }

  public void test_presentValueProductSensitivity_noExcoupon() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivity(PRODUCT_NO_EXCOUPON, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(
        PROVIDER, (p) -> PRICER.presentValue(PRODUCT_NO_EXCOUPON, (p)));
    assertTrue(computed.equalWithTolerance(expected, 30d * NOTIONAL * EPS));
  }

  public void test_presentValueSensitivityWithZSpread_continuous_noExcoupon() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivityWithZSpread(
        PRODUCT_NO_EXCOUPON, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(PROVIDER, 
        (p) -> PRICER.presentValueWithZSpread(PRODUCT_NO_EXCOUPON, (p), Z_SPREAD, CONTINUOUS, 0));
    assertTrue(computed.equalWithTolerance(expected, 20d * NOTIONAL * EPS));
  }

  public void test_presentValueSensitivityWithZSpread_periodic_noExcoupon() {
    PointSensitivityBuilder point = PRICER.presentValueSensitivityWithZSpread(
        PRODUCT_NO_EXCOUPON, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(PROVIDER, (p) -> 
        PRICER.presentValueWithZSpread(PRODUCT_NO_EXCOUPON, (p), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR));
    assertTrue(computed.equalWithTolerance(expected, 20d * NOTIONAL * EPS));
  }

  public void test_dirtyPriceSensitivity() {
    PointSensitivityBuilder point = PRICER.dirtyPriceSensitivity(BOND_SECURITY, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(
        PROVIDER, (p) -> CurrencyAmount.of(EUR, PRICER.dirtyPriceFromCurves(BOND_SECURITY, (p))));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * EPS));
  }

  public void test_dirtyPriceSensitivityWithZspread_continuous() {
    PointSensitivityBuilder point =
        PRICER.dirtyPriceSensitivityWithZspread(BOND_SECURITY, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(PROVIDER, (p) ->
        CurrencyAmount.of(EUR, PRICER.dirtyPriceFromCurvesWithZSpread(BOND_SECURITY, (p), Z_SPREAD, CONTINUOUS, 0)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * EPS));
  }

  public void test_dirtyPriceSensitivityWithZspread_periodic() {
    PointSensitivityBuilder point = PRICER.dirtyPriceSensitivityWithZspread(
        BOND_SECURITY, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point.build());
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(PROVIDER, (p) -> CurrencyAmount.of(EUR, PRICER
        .dirtyPriceFromCurvesWithZSpread(BOND_SECURITY, (p), Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * EPS));
  }

  //-------------------------------------------------------------------------
  public void test_accruedInterest() {
    // settle before start
    LocalDate settleDate1 = START_DATE.minusDays(5);
    double accruedInterest1 = PRICER.accruedInterest(PRODUCT, settleDate1);
    assertEquals(accruedInterest1, 0d);
    // settle between endDate and endDate -lag
    LocalDate settleDate2 = date(2015, 10, 8);
    double accruedInterest2 = PRICER.accruedInterest(PRODUCT, settleDate2);
    assertEquals(accruedInterest2, -4.0 / 365.0 * FIXED_RATE * NOTIONAL, EPS);
    // normal
    LocalDate settleDate3 = date(2015, 4, 18); // not adjusted
    FixedCouponBond product = FixedCouponBond.builder()
        .dayCount(DAY_COUNT)
        .fixedRate(FIXED_RATE)
        .legalEntityId(ISSUER_ID)
        .currency(EUR)
        .notional(NOTIONAL)
        .periodicSchedule(PERIOD_SCHEDULE)
        .settlementDateOffset(DATE_OFFSET)
        .yieldConvention(YIELD_CONVENTION)
        .exCouponPeriod(DaysAdjustment.NONE)
        .build();
    double accruedInterest3 = PRICER.accruedInterest(product, settleDate3);
    assertEquals(accruedInterest3, 6.0 / 365.0 * FIXED_RATE * NOTIONAL, EPS);
  }

  //-------------------------------------------------------------------------
  /* US Street convention */
  private static final LocalDate START_US = date(2006, 11, 15);
  private static final LocalDate END_US = START_US.plusYears(10);
  private static final PeriodicSchedule SCHEDULE_US = PeriodicSchedule.of(START_US, END_US, Frequency.P6M,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN),
      StubConvention.SHORT_INITIAL, false);
  private static final FixedCouponBond PRODUCT_US = FixedCouponBond.builder()
      .dayCount(DayCounts.ACT_ACT_ICMA)
      .fixedRate(0.04625)
      .legalEntityId(ISSUER_ID)
      .currency(Currency.USD)
      .notional(100)
      .periodicSchedule(SCHEDULE_US)
      .settlementDateOffset(DaysAdjustment.ofBusinessDays(3, HolidayCalendars.SAT_SUN))
      .yieldConvention(YieldConvention.US_STREET)
      .exCouponPeriod(DaysAdjustment.NONE)
      .build();
  private static final LocalDate VALUATION_US = date(2011, 8, 18);
  private static final LocalDate SETTLEMENT_US = PRODUCT_US.getSettlementDateOffset().adjust(VALUATION_US);
  private static final LocalDate VALUATION_LAST_US = date(2016, 6, 3);
  private static final LocalDate SETTLEMENT_LAST_US = PRODUCT_US.getSettlementDateOffset().adjust(VALUATION_LAST_US);
  private static final double YIELD_US = 0.04;

  public void dirtyPriceFromYieldUS() {
    double dirtyPrice = PRICER.dirtyPriceFromYield(PRODUCT_US, SETTLEMENT_US, YIELD_US);
    assertEquals(dirtyPrice, 1.0417352500524246, TOL); // 2.x.
    double yield = PRICER.yieldFromDirtyPrice(PRODUCT_US, SETTLEMENT_US, dirtyPrice);
    assertEquals(yield, YIELD_US, TOL);
  }

  public void dirtyPriceFromYieldUSLastPeriod() {
    double dirtyPrice = PRICER.dirtyPriceFromYield(PRODUCT_US, SETTLEMENT_LAST_US, YIELD_US);
    assertEquals(dirtyPrice, 1.005635683760684, TOL); // 2.x.
    double yield = PRICER.yieldFromDirtyPrice(PRODUCT_US, SETTLEMENT_LAST_US, dirtyPrice);
    assertEquals(yield, YIELD_US, TOL);
  }

  public void modifiedDurationFromYieldUS() {
    double computed = PRICER.modifiedDurationFromYield(PRODUCT_US, SETTLEMENT_US, YIELD_US);
    double price = PRICER.dirtyPriceFromYield(PRODUCT_US, SETTLEMENT_US, YIELD_US);
    double priceUp = PRICER.dirtyPriceFromYield(PRODUCT_US, SETTLEMENT_US, YIELD_US + EPS);
    double priceDw = PRICER.dirtyPriceFromYield(PRODUCT_US, SETTLEMENT_US, YIELD_US - EPS);
    double expected = 0.5 * (priceDw - priceUp) / price / EPS;
    assertEquals(computed, expected, EPS);
  }

  public void modifiedDurationFromYieldUSLastPeriod() {
    double computed = PRICER.modifiedDurationFromYield(PRODUCT_US, SETTLEMENT_LAST_US, YIELD_US);
    double price = PRICER.dirtyPriceFromYield(PRODUCT_US, SETTLEMENT_LAST_US, YIELD_US);
    double priceUp = PRICER.dirtyPriceFromYield(PRODUCT_US, SETTLEMENT_LAST_US, YIELD_US + EPS);
    double priceDw = PRICER.dirtyPriceFromYield(PRODUCT_US, SETTLEMENT_LAST_US, YIELD_US - EPS);
    double expected = 0.5 * (priceDw - priceUp) / price / EPS;
    assertEquals(computed, expected, EPS);
  }

  public void convexityFromYieldUS() {
    double computed = PRICER.convexityFromYield(PRODUCT_US, SETTLEMENT_US, YIELD_US);
    double duration = PRICER.modifiedDurationFromYield(PRODUCT_US, SETTLEMENT_US, YIELD_US);
    double durationUp = PRICER.modifiedDurationFromYield(PRODUCT_US, SETTLEMENT_US, YIELD_US + EPS);
    double durationDw = PRICER.modifiedDurationFromYield(PRODUCT_US, SETTLEMENT_US, YIELD_US - EPS);
    double expected = 0.5 * (durationDw - durationUp) / EPS + duration * duration;
    assertEquals(computed, expected, EPS);
  }

  public void convexityFromYieldUSLastPeriod() {
    double computed = PRICER.convexityFromYield(PRODUCT_US, SETTLEMENT_LAST_US, YIELD_US);
    double duration = PRICER.modifiedDurationFromYield(PRODUCT_US, SETTLEMENT_LAST_US, YIELD_US);
    double durationUp = PRICER.modifiedDurationFromYield(PRODUCT_US, SETTLEMENT_LAST_US, YIELD_US + EPS);
    double durationDw = PRICER.modifiedDurationFromYield(PRODUCT_US, SETTLEMENT_LAST_US, YIELD_US - EPS);
    double expected = 0.5 * (durationDw - durationUp) / EPS + duration * duration;
    assertEquals(computed, expected, EPS);
  }

  public void macaulayDurationFromYieldUS() {
    double duration = PRICER.macaulayDurationFromYield(PRODUCT_US, SETTLEMENT_US, YIELD_US);
    assertEquals(duration, 4.6575232098896215, TOL); // 2.x.
  }

  public void macaulayDurationFromYieldUSLastPeriod() {
    double duration = PRICER.macaulayDurationFromYield(PRODUCT_US, SETTLEMENT_LAST_US, YIELD_US);
    assertEquals(duration, 0.43478260869565216, TOL); // 2.x.
  }

  /* UK BUMP/DMO convention */
  private static final LocalDate START_UK = date(2002, 9, 7);
  private static final LocalDate END_UK = START_UK.plusYears(12);
  private static final PeriodicSchedule SCHEDULE_UK = PeriodicSchedule.of(START_UK, END_UK, Frequency.P6M,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN),
      StubConvention.SHORT_INITIAL, false);
  private static final FixedCouponBond PRODUCT_UK = FixedCouponBond.builder()
      .dayCount(DayCounts.ACT_ACT_ICMA)
      .fixedRate(0.05)
      .legalEntityId(ISSUER_ID)
      .currency(Currency.GBP)
      .notional(100)
      .periodicSchedule(SCHEDULE_UK)
      .settlementDateOffset(DaysAdjustment.ofBusinessDays(1, HolidayCalendars.SAT_SUN))
      .yieldConvention(YieldConvention.UK_BUMP_DMO)
      .exCouponPeriod(DaysAdjustment.ofCalendarDays(-7,
          BusinessDayAdjustment.of(BusinessDayConventions.PRECEDING, HolidayCalendars.SAT_SUN)))
      .build();
  private static final LocalDate VALUATION_UK = date(2011, 9, 2);
  private static final LocalDate SETTLEMENT_UK = PRODUCT_UK.getSettlementDateOffset().adjust(VALUATION_UK);
  private static final LocalDate VALUATION_LAST_UK = date(2014, 6, 3);
  private static final LocalDate SETTLEMENT_LAST_UK = PRODUCT_UK.getSettlementDateOffset().adjust(VALUATION_LAST_UK);
  private static final double YIELD_UK = 0.04;

  public void dirtyPriceFromYieldUK() {
    double dirtyPrice = PRICER.dirtyPriceFromYield(PRODUCT_UK, SETTLEMENT_UK, YIELD_UK);
    assertEquals(dirtyPrice, 1.0277859038905428, TOL); // 2.x.
    double yield = PRICER.yieldFromDirtyPrice(PRODUCT_UK, SETTLEMENT_UK, dirtyPrice);
    assertEquals(yield, YIELD_UK, TOL);
  }

  public void dirtyPriceFromYieldUKLastPeriod() {
    double dirtyPrice = PRICER.dirtyPriceFromYield(PRODUCT_UK, SETTLEMENT_LAST_UK, YIELD_UK);
    assertEquals(dirtyPrice, 1.0145736043763598, TOL); // 2.x.
    double yield = PRICER.yieldFromDirtyPrice(PRODUCT_UK, SETTLEMENT_LAST_UK, dirtyPrice);
    assertEquals(yield, YIELD_UK, TOL);
  }

  public void modifiedDurationFromYieldUK() {
    double computed = PRICER.modifiedDurationFromYield(PRODUCT_UK, SETTLEMENT_UK, YIELD_UK);
    double price = PRICER.dirtyPriceFromYield(PRODUCT_UK, SETTLEMENT_UK, YIELD_UK);
    double priceUp = PRICER.dirtyPriceFromYield(PRODUCT_UK, SETTLEMENT_UK, YIELD_UK + EPS);
    double priceDw = PRICER.dirtyPriceFromYield(PRODUCT_UK, SETTLEMENT_UK, YIELD_UK - EPS);
    double expected = 0.5 * (priceDw - priceUp) / price / EPS;
    assertEquals(computed, expected, EPS);
  }

  public void modifiedDurationFromYieldUKLastPeriod() {
    double computed = PRICER.modifiedDurationFromYield(PRODUCT_UK, SETTLEMENT_LAST_UK, YIELD_UK);
    double price = PRICER.dirtyPriceFromYield(PRODUCT_UK, SETTLEMENT_LAST_UK, YIELD_UK);
    double priceUp = PRICER.dirtyPriceFromYield(PRODUCT_UK, SETTLEMENT_LAST_UK, YIELD_UK + EPS);
    double priceDw = PRICER.dirtyPriceFromYield(PRODUCT_UK, SETTLEMENT_LAST_UK, YIELD_UK - EPS);
    double expected = 0.5 * (priceDw - priceUp) / price / EPS;
    assertEquals(computed, expected, EPS);
  }

  public void convexityFromYieldUK() {
    double computed = PRICER.convexityFromYield(PRODUCT_UK, SETTLEMENT_UK, YIELD_UK);
    double duration = PRICER.modifiedDurationFromYield(PRODUCT_UK, SETTLEMENT_UK, YIELD_UK);
    double durationUp = PRICER.modifiedDurationFromYield(PRODUCT_UK, SETTLEMENT_UK, YIELD_UK + EPS);
    double durationDw = PRICER.modifiedDurationFromYield(PRODUCT_UK, SETTLEMENT_UK, YIELD_UK - EPS);
    double expected = 0.5 * (durationDw - durationUp) / EPS + duration * duration;
    assertEquals(computed, expected, EPS);
  }

  public void convexityFromYieldUKLastPeriod() {
    double computed = PRICER.convexityFromYield(PRODUCT_UK, SETTLEMENT_LAST_UK, YIELD_UK);
    double duration = PRICER.modifiedDurationFromYield(PRODUCT_UK, SETTLEMENT_LAST_UK, YIELD_UK);
    double durationUp = PRICER.modifiedDurationFromYield(PRODUCT_UK, SETTLEMENT_LAST_UK, YIELD_UK + EPS);
    double durationDw = PRICER.modifiedDurationFromYield(PRODUCT_UK, SETTLEMENT_LAST_UK, YIELD_UK - EPS);
    double expected = 0.5 * (durationDw - durationUp) / EPS + duration * duration;
    assertEquals(computed, expected, EPS);
  }

  public void macaulayDurationFromYieldUK() {
    double duration = PRICER.macaulayDurationFromYield(PRODUCT_UK, SETTLEMENT_UK, YIELD_UK);
    assertEquals(duration, 2.8312260658609163, TOL); // 2.x.
  }

  public void macaulayDurationFromYieldUKLastPeriod() {
    double duration = PRICER.macaulayDurationFromYield(PRODUCT_UK, SETTLEMENT_LAST_UK, YIELD_UK);
    assertEquals(duration, 0.25815217391304346, TOL); // 2.x.
  }

  /* German bond convention */
  private static final LocalDate START_GER = date(2002, 9, 7);
  private static final LocalDate END_GER = START_GER.plusYears(12);
  private static final PeriodicSchedule SCHEDULE_GER = PeriodicSchedule.of(START_GER, END_GER, Frequency.P12M,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN),
      StubConvention.SHORT_INITIAL, false);
  private static final FixedCouponBond PRODUCT_GER = FixedCouponBond.builder()
      .dayCount(DayCounts.ACT_ACT_ICMA)
      .fixedRate(0.05)
      .legalEntityId(ISSUER_ID)
      .currency(Currency.EUR)
      .notional(100)
      .periodicSchedule(SCHEDULE_GER)
      .settlementDateOffset(DaysAdjustment.ofBusinessDays(3, HolidayCalendars.SAT_SUN))
      .yieldConvention(YieldConvention.GERMAN_BONDS)
      .exCouponPeriod(DaysAdjustment.NONE)
      .build();
  private static final LocalDate VALUATION_GER = date(2011, 9, 2);
  private static final LocalDate SETTLEMENT_GER = PRODUCT_GER.getSettlementDateOffset().adjust(VALUATION_GER);
  private static final LocalDate VALUATION_LAST_GER = date(2014, 6, 3);
  private static final LocalDate SETTLEMENT_LAST_GER = PRODUCT_GER.getSettlementDateOffset().adjust(VALUATION_LAST_GER);
  private static final double YIELD_GER = 0.04;

  public void dirtyPriceFromYieldGerman() {
    double dirtyPrice = PRICER.dirtyPriceFromYield(PRODUCT_GER, SETTLEMENT_GER, YIELD_GER);
    assertEquals(dirtyPrice, 1.027750910332271, TOL); // 2.x.
    double yield = PRICER.yieldFromDirtyPrice(PRODUCT_GER, SETTLEMENT_GER, dirtyPrice);
    assertEquals(yield, YIELD_GER, TOL);
  }

  public void dirtyPriceFromYieldGermanLastPeriod() {
    double dirtyPrice = PRICER.dirtyPriceFromYield(PRODUCT_GER, SETTLEMENT_LAST_GER, YIELD_GER);
    assertEquals(dirtyPrice, 1.039406595790844, TOL); // 2.x.
    double yield = PRICER.yieldFromDirtyPrice(PRODUCT_GER, SETTLEMENT_LAST_GER, dirtyPrice);
    assertEquals(yield, YIELD_GER, TOL);
  }

  public void modifiedDurationFromYieldGER() {
    double computed = PRICER.modifiedDurationFromYield(PRODUCT_GER, SETTLEMENT_GER, YIELD_GER);
    double price = PRICER.dirtyPriceFromYield(PRODUCT_GER, SETTLEMENT_GER, YIELD_GER);
    double priceUp = PRICER.dirtyPriceFromYield(PRODUCT_GER, SETTLEMENT_GER, YIELD_GER + EPS);
    double priceDw = PRICER.dirtyPriceFromYield(PRODUCT_GER, SETTLEMENT_GER, YIELD_GER - EPS);
    double expected = 0.5 * (priceDw - priceUp) / price / EPS;
    assertEquals(computed, expected, EPS);
  }

  public void modifiedDurationFromYieldGERLastPeriod() {
    double computed = PRICER.modifiedDurationFromYield(PRODUCT_GER, SETTLEMENT_LAST_GER, YIELD_GER);
    double price = PRICER.dirtyPriceFromYield(PRODUCT_GER, SETTLEMENT_LAST_GER, YIELD_GER);
    double priceUp = PRICER.dirtyPriceFromYield(PRODUCT_GER, SETTLEMENT_LAST_GER, YIELD_GER + EPS);
    double priceDw = PRICER.dirtyPriceFromYield(PRODUCT_GER, SETTLEMENT_LAST_GER, YIELD_GER - EPS);
    double expected = 0.5 * (priceDw - priceUp) / price / EPS;
    assertEquals(computed, expected, EPS);
  }

  public void convexityFromYieldGER() {
    double computed = PRICER.convexityFromYield(PRODUCT_GER, SETTLEMENT_GER, YIELD_GER);
    double duration = PRICER.modifiedDurationFromYield(PRODUCT_GER, SETTLEMENT_GER, YIELD_GER);
    double durationUp = PRICER.modifiedDurationFromYield(PRODUCT_GER, SETTLEMENT_GER, YIELD_GER + EPS);
    double durationDw = PRICER.modifiedDurationFromYield(PRODUCT_GER, SETTLEMENT_GER, YIELD_GER - EPS);
    double expected = 0.5 * (durationDw - durationUp) / EPS + duration * duration;
    assertEquals(computed, expected, EPS);
  }

  public void convexityFromYieldGERLastPeriod() {
    double computed = PRICER.convexityFromYield(PRODUCT_GER, SETTLEMENT_LAST_GER, YIELD_GER);
    double duration = PRICER.modifiedDurationFromYield(PRODUCT_GER, SETTLEMENT_LAST_GER, YIELD_GER);
    double durationUp = PRICER.modifiedDurationFromYield(PRODUCT_GER, SETTLEMENT_LAST_GER, YIELD_GER + EPS);
    double durationDw = PRICER.modifiedDurationFromYield(PRODUCT_GER, SETTLEMENT_LAST_GER, YIELD_GER - EPS);
    double expected = 0.5 * (durationDw - durationUp) / EPS + duration * duration;
    assertEquals(computed, expected, EPS);
  }

  public void macaulayDurationFromYieldGER() {
    double duration = PRICER.macaulayDurationFromYield(PRODUCT_GER, SETTLEMENT_GER, YIELD_GER);
    assertEquals(duration, 2.861462874541554, TOL); // 2.x.
  }

  public void macaulayDurationFromYieldGERLastPeriod() {
    double duration = PRICER.macaulayDurationFromYield(PRODUCT_GER, SETTLEMENT_LAST_GER, YIELD_GER);
    assertEquals(duration, 0.26231286613148186, TOL); // 2.x.
  }

  /* Japan simple convention */
  private static final LocalDate START_JP = date(2015, 9, 20);
  private static final LocalDate END_JP = START_JP.plusYears(10);
  private static final PeriodicSchedule SCHEDULE_JP = PeriodicSchedule.of(START_JP, END_JP, Frequency.P6M,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.JPTO),
      StubConvention.SHORT_INITIAL, false);
  private static final double RATE_JP = 0.004;
  private static final FixedCouponBond PRODUCT_JP = FixedCouponBond.builder()
      .dayCount(DayCounts.NL_365)
      .fixedRate(RATE_JP)
      .legalEntityId(ISSUER_ID)
      .currency(Currency.JPY)
      .notional(100)
      .periodicSchedule(SCHEDULE_JP)
      .settlementDateOffset(DaysAdjustment.ofBusinessDays(3, HolidayCalendars.JPTO))
      .yieldConvention(YieldConvention.JAPAN_SIMPLE)
      .exCouponPeriod(DaysAdjustment.NONE)
      .build();
  private static final LocalDate VALUATION_JP = date(2015, 9, 24);
  private static final LocalDate SETTLEMENT_JP = PRODUCT_JP.getSettlementDateOffset().adjust(VALUATION_JP);
  private static final LocalDate VALUATION_LAST_JP = date(2025, 6, 3);
  private static final LocalDate SETTLEMENT_LAST_JP = PRODUCT_JP.getSettlementDateOffset().adjust(VALUATION_LAST_JP);
  private static final LocalDate VALUATION_ENDED_JP = date(2026, 8, 3);
  private static final LocalDate SETTLEMENT_ENDED_JP = PRODUCT_JP.getSettlementDateOffset().adjust(VALUATION_ENDED_JP);
  private static final double YIELD_JP = 0.00321;

  public void dirtyPriceFromYieldJP() {
    double computed = PRICER.dirtyPriceFromYield(PRODUCT_JP, SETTLEMENT_JP, YIELD_JP);
    double maturity = DayCounts.NL_365.relativeYearFraction(SETTLEMENT_JP, END_JP);
    double expected = PRICER.dirtyPriceFromCleanPrice(
        PRODUCT_JP, SETTLEMENT_JP, (1d + RATE_JP * maturity) / (1d + YIELD_JP * maturity));
    assertEquals(computed, expected, TOL);
    double yield = PRICER.yieldFromDirtyPrice(PRODUCT_JP, SETTLEMENT_JP, computed);
    assertEquals(yield, YIELD_JP, TOL);
  }

  public void dirtyPriceFromYieldJPLastPeriod() {
    double computed = PRICER.dirtyPriceFromYield(PRODUCT_JP, SETTLEMENT_LAST_JP, YIELD_JP);
    double maturity = DayCounts.NL_365.relativeYearFraction(SETTLEMENT_LAST_JP, END_JP);
    double expected = PRICER.dirtyPriceFromCleanPrice(
        PRODUCT_JP, SETTLEMENT_LAST_JP, (1d + RATE_JP * maturity) / (1d + YIELD_JP * maturity));
    assertEquals(computed, expected, TOL);
    double yield = PRICER.yieldFromDirtyPrice(PRODUCT_JP, SETTLEMENT_LAST_JP, computed);
    assertEquals(yield, YIELD_JP, TOL);
  }

  public void dirtyPriceFromYieldJPEnded() {
    double computed = PRICER.dirtyPriceFromYield(PRODUCT_JP, SETTLEMENT_ENDED_JP, YIELD_JP);
    assertEquals(computed, 0d, TOL);
  }

  public void modifiedDurationFromYielddJP() {
    double computed = PRICER.modifiedDurationFromYield(PRODUCT_JP, SETTLEMENT_JP, YIELD_JP);
    double price = PRICER.dirtyPriceFromYield(PRODUCT_JP, SETTLEMENT_JP, YIELD_JP);
    double priceUp = PRICER.dirtyPriceFromYield(PRODUCT_JP, SETTLEMENT_JP, YIELD_JP + EPS);
    double priceDw = PRICER.dirtyPriceFromYield(PRODUCT_JP, SETTLEMENT_JP, YIELD_JP - EPS);
    double expected = 0.5 * (priceDw - priceUp) / price / EPS;
    assertEquals(computed, expected, EPS);
  }

  public void modifiedDurationFromYieldJPLastPeriod() {
    double computed = PRICER.modifiedDurationFromYield(PRODUCT_JP, SETTLEMENT_LAST_JP, YIELD_JP);
    double price = PRICER.dirtyPriceFromYield(PRODUCT_JP, SETTLEMENT_LAST_JP, YIELD_JP);
    double priceUp = PRICER.dirtyPriceFromYield(PRODUCT_JP, SETTLEMENT_LAST_JP, YIELD_JP + EPS);
    double priceDw = PRICER.dirtyPriceFromYield(PRODUCT_JP, SETTLEMENT_LAST_JP, YIELD_JP - EPS);
    double expected = 0.5 * (priceDw - priceUp) / price / EPS;
    assertEquals(computed, expected, EPS);
  }

  public void modifiedDurationFromYielddJPEnded() {
    double computed = PRICER.modifiedDurationFromYield(PRODUCT_JP, SETTLEMENT_ENDED_JP, YIELD_JP);
    assertEquals(computed, 0d, EPS);
  }

  public void convexityFromYieldJP() {
    double computed = PRICER.convexityFromYield(PRODUCT_JP, SETTLEMENT_JP, YIELD_JP);
    double duration = PRICER.modifiedDurationFromYield(PRODUCT_JP, SETTLEMENT_JP, YIELD_JP);
    double durationUp = PRICER.modifiedDurationFromYield(PRODUCT_JP, SETTLEMENT_JP, YIELD_JP + EPS);
    double durationDw = PRICER.modifiedDurationFromYield(PRODUCT_JP, SETTLEMENT_JP, YIELD_JP - EPS);
    double expected = 0.5 * (durationDw - durationUp) / EPS + duration * duration;
    assertEquals(computed, expected, EPS);
  }

  public void convexityFromYieldJPLastPeriod() {
    double computed = PRICER.convexityFromYield(PRODUCT_JP, SETTLEMENT_LAST_JP, YIELD_JP);
    double duration = PRICER.modifiedDurationFromYield(PRODUCT_JP, SETTLEMENT_LAST_JP, YIELD_JP);
    double durationUp = PRICER.modifiedDurationFromYield(PRODUCT_JP, SETTLEMENT_LAST_JP, YIELD_JP + EPS);
    double durationDw = PRICER.modifiedDurationFromYield(PRODUCT_JP, SETTLEMENT_LAST_JP, YIELD_JP - EPS);
    double expected = 0.5 * (durationDw - durationUp) / EPS + duration * duration;
    assertEquals(computed, expected, EPS);
  }

  public void convexityFromYieldJPEnded() {
    double computed = PRICER.convexityFromYield(PRODUCT_JP, SETTLEMENT_ENDED_JP, YIELD_JP);
    assertEquals(computed, 0d, EPS);
  }

  public void macaulayDurationFromYieldYieldJP() {
    assertThrows(() -> PRICER.macaulayDurationFromYield(PRODUCT_JP, SETTLEMENT_JP, YIELD_JP),
        UnsupportedOperationException.class, "The convention JAPAN_SIMPLE is not supported.");
  }

}
