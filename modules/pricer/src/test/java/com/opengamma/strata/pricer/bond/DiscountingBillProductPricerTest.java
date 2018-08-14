/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.bond.Bill;
import com.opengamma.strata.product.bond.BillYieldConvention;
import com.opengamma.strata.product.bond.ResolvedBill;

/**
 * Test {@link DiscountingBillProductPricer}
 */
@Test
public class DiscountingBillProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2018, 6, 20);

  // Bill
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Ticker", "GOVT1-BOND1");
  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG-Ticker", "GOVT1");
  private static final BillYieldConvention YIELD_CONVENTION = BillYieldConvention.INTEREST_AT_MATURITY;
  
  private static final HolidayCalendarId EUR_CALENDAR = HolidayCalendarIds.EUTA;
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUR_CALENDAR);
  private static final double NOTIONAL_AMOUNT = 1_000_000;
  private static final LocalDate MATURITY_DATE = LocalDate.of(2018, 12, 12);
  private static final AdjustableDate MATURITY_DATE_ADJ = AdjustableDate.of(MATURITY_DATE, BUSINESS_ADJUST);
  private static final AdjustablePayment NOTIONAL = 
      AdjustablePayment.of(CurrencyAmount.of(EUR, NOTIONAL_AMOUNT), MATURITY_DATE_ADJ);
  
  private static final DaysAdjustment DATE_OFFSET = DaysAdjustment.ofBusinessDays(2, EUR_CALENDAR);
  private static final DayCount DAY_COUNT = DayCounts.ACT_360;
  private static final ResolvedBill BILL = Bill.builder()
      .dayCount(DAY_COUNT)
      .legalEntityId(ISSUER_ID)
      .notional(NOTIONAL)
      .securityId(SECURITY_ID)
      .settlementDateOffset(DATE_OFFSET)
      .yieldConvention(YIELD_CONVENTION).build().resolve(REF_DATA);
  private static final ResolvedBill BILL_PAST = 
      BILL.toBuilder().notional(BILL.getNotional().toBuilder().date(VAL_DATE.minusDays(1)).build()).build();

  // rates provider
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME_REPO = CurveName.of("TestRepoCurve");
  private static final CurveMetadata METADATA_REPO = Curves.zeroRates(NAME_REPO, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_REPO = InterpolatedNodalCurve.of(
      METADATA_REPO, DoubleArray.of(0.1, 2.0, 10.0), DoubleArray.of(0.05, 0.06, 0.09), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_REPO = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_REPO);
  private static final RepoGroup GROUP_REPO = RepoGroup.of("GOVT1 BOND1");
  private static final CurveName NAME_ISSUER = CurveName.of("TestIssuerCurve");
  private static final CurveMetadata METADATA_ISSUER = Curves.zeroRates(NAME_ISSUER, ACT_365F);
  private static final InterpolatedNodalCurve CURVE_ISSUER = InterpolatedNodalCurve.of(
      METADATA_ISSUER, DoubleArray.of(0.2, 9.0, 15.0), DoubleArray.of(0.03, 0.05, 0.13), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS_ISSUER = ZeroRateDiscountFactors.of(EUR, VAL_DATE, CURVE_ISSUER);
  private static final LegalEntityGroup GROUP_ISSUER = LegalEntityGroup.of("GOVT1");
  private static final LegalEntityDiscountingProvider PROVIDER = ImmutableLegalEntityDiscountingProvider.builder()
      .issuerCurves(ImmutableMap.of(Pair.of(GROUP_ISSUER, EUR), DSC_FACTORS_ISSUER))
      .issuerCurveGroups(ImmutableMap.of(ISSUER_ID, GROUP_ISSUER))
      .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO, EUR), DSC_FACTORS_REPO))
      .repoCurveSecurityGroups(ImmutableMap.of(SECURITY_ID, GROUP_REPO))
      .valuationDate(VAL_DATE)
      .build();
  
  // pricers
  private static final DiscountingBillProductPricer PRICER = DiscountingBillProductPricer.DEFAULT;
  private static final double EPS = 1.0e-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALC = new RatesFiniteDifferenceSensitivityCalculator(EPS);

  private static final double Z_SPREAD = 0.035;
  private static final double TOLERANCE_PV = 1.0e-6;
  private static final double TOLERANCE_PRICE = 1.0e-10;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount pvComputed = PRICER.presentValue(BILL, PROVIDER);
    double pvExpected = DSC_FACTORS_ISSUER.discountFactor(MATURITY_DATE) * NOTIONAL.getAmount();
    assertEquals(pvComputed.getCurrency(), EUR);
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }
  
  public void test_presentValue_aftermaturity() {
    CurrencyAmount pvComputed = PRICER.presentValue(BILL_PAST, PROVIDER);
    assertEquals(pvComputed.getCurrency(), EUR);
    assertEquals(pvComputed.getAmount(), 0.0d, TOLERANCE_PV);
  }
  public void test_presentValue_zspread() {
    CurrencyAmount pvComputed = 
        PRICER.presentValueWithZSpread(BILL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    double pvExpected = DSC_FACTORS_ISSUER.discountFactor(MATURITY_DATE) * NOTIONAL.getAmount() *
        Math.exp(-Z_SPREAD * DSC_FACTORS_ISSUER.relativeYearFraction(MATURITY_DATE));
    assertEquals(pvComputed.getCurrency(), EUR);
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }

  public void test_presentValue_zspread_aftermaturity() {
    CurrencyAmount pvComputed =
        PRICER.presentValueWithZSpread(BILL_PAST, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    assertEquals(pvComputed.getCurrency(), EUR);
    assertEquals(pvComputed.getAmount(), 0.0d, TOLERANCE_PV);
  }
  
  //-------------------------------------------------------------------------
  public void presentValueSensitivity() {
    PointSensitivities sensiComputed = PRICER.presentValueSensitivity(BILL, PROVIDER);
    PointSensitivities sensiExpected = IssuerCurveDiscountFactors.of(DSC_FACTORS_ISSUER, GROUP_ISSUER)
        .zeroRatePointSensitivity(MATURITY_DATE)
        .multipliedBy(NOTIONAL.getAmount()).build();
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PV));
    CurrencyParameterSensitivities paramSensiComputed = PROVIDER.parameterSensitivity(sensiComputed);
    CurrencyParameterSensitivities paramSensiExpected = FD_CALC.sensitivity(PROVIDER, p -> PRICER.presentValue(BILL, p));
    assertTrue(paramSensiComputed.equalWithTolerance(paramSensiExpected, EPS * NOTIONAL_AMOUNT));
  }
  
  public void presentValueSensitivity_aftermaturity() {
    PointSensitivities sensiComputed = PRICER.presentValueSensitivity(BILL_PAST, PROVIDER);
    PointSensitivities sensiExpected = PointSensitivities.empty();
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PV));
  }
  
  public void presentValueSensitivity_zspread() {
    PointSensitivities sensiComputed = 
        PRICER.presentValueSensitivityWithZSpread(BILL, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    PointSensitivities sensiExpected = IssuerCurveZeroRateSensitivity.of(
        DSC_FACTORS_ISSUER.zeroRatePointSensitivityWithSpread(MATURITY_DATE, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0),
        GROUP_ISSUER)
        .multipliedBy(NOTIONAL.getAmount())
        .build();
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PV));
    CurrencyParameterSensitivities paramSensiComputed = PROVIDER.parameterSensitivity(sensiComputed);
    CurrencyParameterSensitivities paramSensiExpected = FD_CALC.sensitivity(
        PROVIDER, p -> PRICER.presentValueWithZSpread(BILL, p, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0));
    assertTrue(paramSensiComputed.equalWithTolerance(paramSensiExpected, EPS * NOTIONAL_AMOUNT));
  }
  
  public void presentValueSensitivity_zspread_aftermaturity() {
    PointSensitivities sensiComputed = 
        PRICER.presentValueSensitivityWithZSpread(BILL_PAST, PROVIDER, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    PointSensitivities sensiExpected = PointSensitivities.empty();
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PV));
  }
  
  //-------------------------------------------------------------------------
  public void priceFromCurves() {
    LocalDate settlementDate = VAL_DATE.plusDays(1);
    double priceComputed = PRICER.priceFromCurves(BILL, PROVIDER, settlementDate);
    double dfMaturity = DSC_FACTORS_ISSUER.discountFactor(MATURITY_DATE);
    double dfSettle = DSC_FACTORS_REPO.discountFactor(settlementDate);
    double priceExpected = dfMaturity/dfSettle;
    assertEquals(priceComputed, priceExpected, TOLERANCE_PRICE);
  }
  
  public void price_settle_date_after_maturity_error() {
    assertThrows(() -> PRICER.priceFromCurves(BILL, PROVIDER, MATURITY_DATE), IllegalArgumentException.class);
  }
  
  public void price_settle_date_before_valuation_error() {
    assertThrows(() -> PRICER.priceFromCurves(BILL, PROVIDER, VAL_DATE.minusDays(1)), IllegalArgumentException.class);
  }

  public void priceFromCurves_zspread() {
    LocalDate settlementDate = VAL_DATE.plusDays(1);
    double priceComputed =
        PRICER.priceFromCurvesWithZSpread(BILL, PROVIDER, settlementDate, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    double dfMaturity = DSC_FACTORS_ISSUER.discountFactor(MATURITY_DATE);
    double dfSettle = DSC_FACTORS_REPO.discountFactor(settlementDate);
    double priceExpected = dfMaturity * Math.exp(-Z_SPREAD * DSC_FACTORS_ISSUER.relativeYearFraction(MATURITY_DATE)) / dfSettle;
    assertEquals(priceComputed, priceExpected, TOLERANCE_PRICE);
  }
  
  public void price_zspread_settle_date_after_maturity_error() {
    assertThrows(
        () -> PRICER.priceFromCurvesWithZSpread(BILL, PROVIDER, MATURITY_DATE, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0),
        IllegalArgumentException.class);
  }
  
  public void price_zspread_settle_date_before_valuation_error() {
    assertThrows(
        () -> PRICER.priceFromCurvesWithZSpread(BILL, PROVIDER, VAL_DATE.minusDays(1), Z_SPREAD, CompoundedRateType.CONTINUOUS, 0), 
        IllegalArgumentException.class);
  }
  
  //-------------------------------------------------------------------------
  public void yieldFromCurves() {
    LocalDate settlementDate = VAL_DATE.plusDays(1);
    double yieldComputed = PRICER.yieldFromCurves(BILL, PROVIDER, settlementDate);
    double dfMaturity = DSC_FACTORS_ISSUER.discountFactor(MATURITY_DATE);
    double dfSettle = DSC_FACTORS_REPO.discountFactor(settlementDate);
    double priceExpected = dfMaturity/dfSettle;
    double yieldExpected = BILL.yieldFromPrice(priceExpected, settlementDate);
    assertEquals(yieldComputed, yieldExpected, TOLERANCE_PRICE);
  }
  
  public void yield_settle_date_after_maturity_error() {
    assertThrows(() -> PRICER.yieldFromCurves(BILL, PROVIDER, MATURITY_DATE), IllegalArgumentException.class);
  }
  
  public void yield_settle_date_before_valuation_error() {
    assertThrows(() -> PRICER.yieldFromCurves(BILL, PROVIDER, VAL_DATE.minusDays(1)), IllegalArgumentException.class);
  }
  
  public void yieldFromCurves_zspread() {
    LocalDate settlementDate = VAL_DATE.plusDays(1);
    double yieldComputed = 
        PRICER.yieldFromCurvesWithZSpread(BILL, PROVIDER, settlementDate, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    double priceExpected = 
        PRICER.priceFromCurvesWithZSpread(BILL, PROVIDER, settlementDate, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0);
    double yieldExpected = BILL.yieldFromPrice(priceExpected, settlementDate);
    assertEquals(yieldComputed, yieldExpected, TOLERANCE_PRICE);
  }
  
  public void yield_zspread_settle_date_after_maturity_error() {
    assertThrows(
        () -> PRICER.yieldFromCurvesWithZSpread(BILL, PROVIDER, MATURITY_DATE, Z_SPREAD, CompoundedRateType.CONTINUOUS, 0), 
        IllegalArgumentException.class);
  }
  
  public void yield_zspread_settle_date_before_valuation_error() {
    assertThrows(
        () -> PRICER.yieldFromCurvesWithZSpread(BILL, PROVIDER, VAL_DATE.minusDays(1), Z_SPREAD, CompoundedRateType.CONTINUOUS, 0), 
        IllegalArgumentException.class);
  }

}
