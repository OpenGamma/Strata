/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.pricer.common.PriceType.CLEAN;
import static com.opengamma.strata.pricer.common.PriceType.DIRTY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.SplitCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.PaymentOnDefault;
import com.opengamma.strata.product.credit.ProtectionStartOfDay;
import com.opengamma.strata.product.credit.ResolvedCds;

/**
 * Test {@link IsdaCdsProductPricer}.
 * <p>
 * The numbers in the regression tests are from 2.x.
 */
@Test
public class IsdaCdsProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 1, 3);
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final DaysAdjustment SETTLE_DAY_ADJ = DaysAdjustment.ofBusinessDays(3, CALENDAR);
  private static final DaysAdjustment STEPIN_DAY_ADJ = DaysAdjustment.ofCalendarDays(1);
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");

  private static final DoubleArray TIME_YC = DoubleArray.ofUnsafe(new double[] {0.09041095890410959, 0.16712328767123288,
      0.2547945205479452, 0.5041095890410959, 0.7534246575342466, 1.0054794520547945, 2.0054794520547947, 3.008219178082192,
      4.013698630136987, 5.010958904109589, 6.008219178082192, 7.010958904109589, 8.01095890410959, 9.01095890410959,
      10.016438356164384, 12.013698630136986, 15.021917808219179, 20.01917808219178, 30.024657534246575});
  private static final DoubleArray RATE_YC = DoubleArray.ofUnsafe(new double[] {-0.002078655697855299, -0.001686438401304855,
      -0.0013445486228483379, -4.237819925898475E-4, 2.5142499469348057E-5, 5.935063895780138E-4, -3.247081037469503E-4,
      6.147182786549223E-4, 0.0019060597240545122, 0.0033125742254568815, 0.0047766352312329455, 0.0062374324537341225,
      0.007639664176639106, 0.008971003650150983, 0.010167545380711455, 0.012196853322376243, 0.01441082634734099,
      0.016236611610989507, 0.01652439910865982});
  private static final DefaultCurveMetadata METADATA_YC = DefaultCurveMetadata.builder()
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .curveName("yield")
      .dayCount(ACT_365F)
      .build();
  private static final InterpolatedNodalCurve NODAL_YC = InterpolatedNodalCurve.of(METADATA_YC, TIME_YC, RATE_YC,
      CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
  private static final IsdaCompliantZeroRateDiscountFactors YIELD_CRVE =
      IsdaCompliantZeroRateDiscountFactors.of(USD, VALUATION_DATE, NODAL_YC);

  private static final DoubleArray TIME_CC = DoubleArray.ofUnsafe(new double[] {1.2054794520547945, 1.7095890410958905,
      2.712328767123288, 3.712328767123288, 4.712328767123288, 5.712328767123288, 7.715068493150685, 10.717808219178082});
  private static final DoubleArray RATE_CC = DoubleArray.ofUnsafe(new double[] {0.009950492020354761, 0.01203385973637765,
      0.01418821591480718, 0.01684815168721049, 0.01974873350586718, 0.023084203422383043, 0.02696911931489543,
      0.029605642651816415});
  private static final DefaultCurveMetadata METADATA_CC = DefaultCurveMetadata.builder()
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .curveName("credit")
      .dayCount(ACT_365F)
      .build();
  private static final InterpolatedNodalCurve NODAL_CC = InterpolatedNodalCurve.of(METADATA_CC, TIME_CC, RATE_CC,
      CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
  private static final CreditDiscountFactors CREDIT_CRVE =
      IsdaCompliantZeroRateDiscountFactors.of(USD, VALUATION_DATE, NODAL_CC);
  private static final ConstantRecoveryRates RECOVERY_RATES =
      ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, 0.25);
  private static final ImmutableCreditRatesProvider RATES_PROVIDER = ImmutableCreditRatesProvider.builder()
      .valuationDate(VALUATION_DATE)
      .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, CREDIT_CRVE)))
      .discountCurves(ImmutableMap.of(USD, YIELD_CRVE))
      .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_RATES))
      .build();

  private static final double NOTIONAL = 1.0e7;
  private static final ResolvedCds PRODUCT_NEXTDAY =
      Cds.of(BuySell.BUY, LEGAL_ENTITY, USD, NOTIONAL, LocalDate.of(2014, 1, 4), LocalDate.of(2020, 10, 20), Frequency.P3M,
          BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR), StubConvention.SHORT_INITIAL, 0.05, ACT_360,
          PaymentOnDefault.ACCRUED_PREMIUM, ProtectionStartOfDay.BEGINNING, STEPIN_DAY_ADJ, SETTLE_DAY_ADJ).resolve(REF_DATA);
  private static final ResolvedCds PRODUCT_BEFORE = Cds.of(
      BuySell.SELL, LEGAL_ENTITY, USD, NOTIONAL, LocalDate.of(2013, 12, 20), LocalDate.of(2024, 9, 20), Frequency.P3M,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR), StubConvention.SHORT_INITIAL, 0.05, ACT_360,
      PaymentOnDefault.ACCRUED_PREMIUM, ProtectionStartOfDay.BEGINNING, STEPIN_DAY_ADJ, SETTLE_DAY_ADJ).resolve(REF_DATA);
  private static final ResolvedCds PRODUCT_AFTER = Cds.of(
      BuySell.BUY, LEGAL_ENTITY, USD, NOTIONAL, LocalDate.of(2014, 3, 20), LocalDate.of(2029, 12, 20), Frequency.P3M,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR), StubConvention.SHORT_INITIAL, 0.05, ACT_360,
      PaymentOnDefault.ACCRUED_PREMIUM, ProtectionStartOfDay.BEGINNING, STEPIN_DAY_ADJ, SETTLE_DAY_ADJ).resolve(REF_DATA);

  private static final DaysAdjustment SETTLE_DAY_ADJ_NS = DaysAdjustment.ofBusinessDays(5, CALENDAR);
  private static final DaysAdjustment STEPIN_DAY_ADJ_NS = DaysAdjustment.ofCalendarDays(7);
  private static final ResolvedCds PRODUCT_NS_TODAY = Cds.of(
      BuySell.BUY, LEGAL_ENTITY, USD, NOTIONAL, VALUATION_DATE, LocalDate.of(2021, 4, 25), Frequency.P4M,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR), StubConvention.SHORT_FINAL, 0.05, ACT_360,
      PaymentOnDefault.ACCRUED_PREMIUM, ProtectionStartOfDay.NONE, STEPIN_DAY_ADJ_NS, SETTLE_DAY_ADJ_NS).resolve(REF_DATA);
  private static final ResolvedCds PRODUCT_NS_STEPIN = Cds.of(
      BuySell.SELL, LEGAL_ENTITY, USD, NOTIONAL, STEPIN_DAY_ADJ_NS.adjust(VALUATION_DATE, REF_DATA), LocalDate.of(2019, 1, 26),
      Frequency.P6M, BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR), StubConvention.LONG_INITIAL,
      0.05, ACT_360, PaymentOnDefault.ACCRUED_PREMIUM, ProtectionStartOfDay.NONE, STEPIN_DAY_ADJ_NS, SETTLE_DAY_ADJ_NS)
      .resolve(REF_DATA);
  private static final ResolvedCds PRODUCT_NS_BTW = Cds.of(
      BuySell.BUY, LEGAL_ENTITY, USD, NOTIONAL, VALUATION_DATE.plusDays(4), LocalDate.of(2026, 8, 2), Frequency.P12M,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR), StubConvention.LONG_FINAL, 0.05, ACT_360,
      PaymentOnDefault.ACCRUED_PREMIUM, ProtectionStartOfDay.NONE, STEPIN_DAY_ADJ_NS, SETTLE_DAY_ADJ_NS).resolve(REF_DATA);

  private static final double TOL = 1.0e-14;
  private static final double EPS = 1.0e-6;
  private static final IsdaCdsProductPricer PRICER = IsdaCdsProductPricer.DEFAULT;
  private static final IsdaCdsProductPricer PRICER_FIX = new IsdaCdsProductPricer(AccrualOnDefaultFormula.MARKIT_FIX);
  private static final IsdaCdsProductPricer PRICER_CORRECT = new IsdaCdsProductPricer(AccrualOnDefaultFormula.CORRECT);
  private static final RatesFiniteDifferenceSensitivityCalculator CALC_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  //-------------------------------------------------------------------------
  public void accFormulaTest() {
    assertEquals(PRICER.getAccrualOnDefaultFormula(), AccrualOnDefaultFormula.ORIGINAL_ISDA);
    assertEquals(PRICER_FIX.getAccrualOnDefaultFormula(), AccrualOnDefaultFormula.MARKIT_FIX);
    assertEquals(PRICER_CORRECT.getAccrualOnDefaultFormula(), AccrualOnDefaultFormula.CORRECT);
  }

  public void endedTest() {
    LocalDate valuationDate = PRODUCT_NEXTDAY.getProtectionEndDate().plusDays(1);
    CreditRatesProvider provider = createCreditRatesProvider(valuationDate);
    double price = PRICER.price(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertEquals(price, 0d);
    CurrencyAmount pv = PRICER.presentValue(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertEquals(pv, CurrencyAmount.zero(USD));
    assertThrowsIllegalArg(() -> PRICER.parSpread(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA));
    double protectionLeg = PRICER.protectionLeg(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(protectionLeg, 0d);
    double riskyAnnuity = PRICER.riskyAnnuity(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertEquals(riskyAnnuity, 0d);
    CurrencyAmount rpv01 = PRICER.rpv01(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertEquals(rpv01, CurrencyAmount.zero(USD));
    CurrencyAmount recovery01 = PRICER.recovery01(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(recovery01, CurrencyAmount.zero(USD));
    PointSensitivityBuilder sensi = PRICER.presentValueSensitivity(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(sensi, PointSensitivityBuilder.none());
    PointSensitivityBuilder sensiPrice = PRICER.priceSensitivity(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(sensiPrice, PointSensitivityBuilder.none());
    assertThrowsIllegalArg(() -> PRICER.parSpreadSensitivity(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA));
    SplitCurrencyAmount<StandardId> jtd = PRICER.jumpToDefault(PRODUCT_NEXTDAY, provider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(provider.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(jtd, SplitCurrencyAmount.of(USD, ImmutableMap.of(LEGAL_ENTITY, 0d)));
    CurrencyAmount expectedLoss = PRICER.expectedLoss(PRODUCT_NEXTDAY, provider);
    assertEquals(expectedLoss, CurrencyAmount.zero(USD));
  }

  public void consistencyTest() {
    double price = PRICER.price(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    CurrencyAmount rpv01 = PRICER.rpv01(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    CurrencyAmount recovery01 = PRICER.recovery01(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    double spread = PRICER.parSpread(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    // the following methods are tested by regression tests
    double protPv = PRICER.protectionLeg(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    double annuity = PRICER.riskyAnnuity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertEquals(price, protPv - PRODUCT_NEXTDAY.getFixedRate() * annuity, TOL);
    assertEquals(rpv01.getCurrency(), USD);
    assertEquals(rpv01.getAmount(), annuity * NOTIONAL, NOTIONAL * TOL);
    assertEquals(recovery01.getCurrency(), USD);
    assertEquals(recovery01.getAmount(), -protPv / (1d - RECOVERY_RATES.getRecoveryRate()) * NOTIONAL, NOTIONAL * TOL);
    assertEquals(spread, protPv / annuity, TOL);
  }

  public void withCouponTest() {
    double coupon = 0.15;
    double price = PRICER.price(PRODUCT_NEXTDAY, RATES_PROVIDER, coupon,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    double protPv = PRICER.protectionLeg(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    double annuity = PRICER.riskyAnnuity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertEquals(price, protPv - coupon * annuity, TOL);
  }

  //-------------------------------------------------------------------------
  public void pvSensitivityTest() {
    PointSensitivityBuilder pointNext = PRICER.presentValueSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext.build());
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivityBuilder pointBefore = PRICER.presentValueSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore.build());
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER.presentValue(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivityBuilder pointAfter = PRICER.presentValueSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter.build());
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER.presentValue(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  public void pvSensitivityFixTest() {
    PointSensitivityBuilder pointNext = PRICER_FIX.presentValueSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext.build());
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_FIX.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivityBuilder pointBefore = PRICER_FIX.presentValueSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore.build());
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_FIX.presentValue(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivityBuilder pointAfter = PRICER_FIX.presentValueSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter.build());
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_FIX.presentValue(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  public void pvSensitivityCorrectTest() {
    PointSensitivityBuilder pointNext = PRICER_CORRECT.presentValueSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext.build());
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_CORRECT.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivityBuilder pointBefore = PRICER_CORRECT.presentValueSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore.build());
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_CORRECT.presentValue(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivityBuilder pointAfter = PRICER_CORRECT.presentValueSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter.build());
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> PRICER_CORRECT.presentValue(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  public void priceSensitivityTest() {
    PointSensitivityBuilder pointNext = PRICER.priceSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext.build());
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.price(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivityBuilder pointBefore = PRICER.priceSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore.build());
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.price(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivityBuilder pointAfter = PRICER.priceSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter.build());
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.price(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  public void priceSensitivityFixTest() {
    PointSensitivityBuilder pointNext = PRICER_FIX.priceSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext.build());
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.price(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivityBuilder pointBefore = PRICER_FIX.priceSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore.build());
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.price(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivityBuilder pointAfter = PRICER_FIX.priceSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter.build());
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.price(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  public void priceSensitivityCorrectTest() {
    PointSensitivityBuilder pointNext = PRICER_CORRECT.priceSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext.build());
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.price(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivityBuilder pointBefore = PRICER_CORRECT.priceSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore.build());
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.price(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivityBuilder pointAfter = PRICER_CORRECT.priceSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter.build());
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.price(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  public void parSpreadSensitivityTest() {
    PointSensitivityBuilder pointNext = PRICER.parSpreadSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext.build());
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.parSpread(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivityBuilder pointBefore = PRICER.parSpreadSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore.build());
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.parSpread(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivityBuilder pointAfter = PRICER.parSpreadSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter.build());
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER.parSpread(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  public void parSpreadSensitivityFixTest() {
    PointSensitivityBuilder pointNext = PRICER_FIX.parSpreadSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext.build());
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.parSpread(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivityBuilder pointBefore = PRICER_FIX.parSpreadSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore.build());
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.parSpread(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivityBuilder pointAfter = PRICER_FIX.parSpreadSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter.build());
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_FIX.parSpread(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  public void parSpreadSensitivityCorrectTest() {
    PointSensitivityBuilder pointNext = PRICER_CORRECT.parSpreadSensitivity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resNext = RATES_PROVIDER.parameterSensitivity(pointNext.build());
    CurrencyParameterSensitivities expNext =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.parSpread(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resNext, expNext, NOTIONAL * EPS);

    PointSensitivityBuilder pointBefore = PRICER_CORRECT.parSpreadSensitivity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resBefore = RATES_PROVIDER.parameterSensitivity(pointBefore.build());
    CurrencyParameterSensitivities expBefore =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.parSpread(PRODUCT_BEFORE,
            p, PRODUCT_BEFORE.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resBefore, expBefore, NOTIONAL * EPS);

    PointSensitivityBuilder pointAfter = PRICER_CORRECT.parSpreadSensitivity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities resAfter = RATES_PROVIDER.parameterSensitivity(pointAfter.build());
    CurrencyParameterSensitivities expAfter =
        CALC_FD.sensitivity(RATES_PROVIDER, p -> CurrencyAmount.of(USD, PRICER_CORRECT.parSpread(PRODUCT_AFTER,
            p, PRODUCT_AFTER.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), REF_DATA)));
    equalWithRelativeTolerance(resAfter, expAfter, NOTIONAL * EPS);
  }

  private void equalWithRelativeTolerance(
      CurrencyParameterSensitivities computed,
      CurrencyParameterSensitivities expected,
      double tolerance) {

    List<CurrencyParameterSensitivity> mutable = new ArrayList<>(expected.getSensitivities());
    // for each sensitivity in this instance, find matching in other instance
    for (CurrencyParameterSensitivity sens1 : computed.getSensitivities()) {
      // list is already sorted so binary search is safe
      int index = Collections.binarySearch(mutable, sens1, CurrencyParameterSensitivity::compareKey);
      if (index >= 0) {
        // matched, so must be equal
        CurrencyParameterSensitivity sens2 = mutable.get(index);
        equalZeroWithRelativeTolerance(sens1.getSensitivity(), sens2.getSensitivity(), tolerance);
        mutable.remove(index);
      } else {
        // did not match, so must be zero
        assertTrue(sens1.getSensitivity().equalZeroWithTolerance(tolerance));
      }
    }
    // all that remain from other instance must be zero
    for (CurrencyParameterSensitivity sens2 : mutable) {
      assertTrue(sens2.getSensitivity().equalZeroWithTolerance(tolerance));
    }
  }

  private void equalZeroWithRelativeTolerance(DoubleArray computed, DoubleArray expected, double tolerance) {
    int size = expected.size();
    assertEquals(size, computed.size());
    for (int i = 0; i < size; i++) {
      double ref = Math.max(1d, Math.abs(expected.get(i)));
      assertEquals(computed.get(i), expected.get(i), tolerance * ref);
    }
  }

  //-------------------------------------------------------------------------
  public void cleanPvTest() {
    double resNext = PRICER.presentValue(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertEquals(resNext, -0.20208402732565636 * NOTIONAL, TOL * NOTIONAL);
    double resBefore = PRICER.presentValue(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertEquals(resBefore, -0.26741962741508013 * (-NOTIONAL), TOL * NOTIONAL);
    double resAfter = PRICER.presentValue(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertEquals(resAfter, -0.32651549808776237 * NOTIONAL, TOL * NOTIONAL);
    double resNsToday = PRICER.presentValue(PRODUCT_NS_TODAY, RATES_PROVIDER,
        PRODUCT_NS_TODAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertEquals(resNsToday, -0.2101704800313836 * NOTIONAL, TOL * NOTIONAL);
    double resNsStepin = PRICER.presentValue(PRODUCT_NS_STEPIN, RATES_PROVIDER,
        PRODUCT_NS_STEPIN.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN,
        REF_DATA).getAmount();
    assertEquals(resNsStepin, -0.1691072048424866 * (-NOTIONAL), TOL * NOTIONAL);
    double resNsBtw = PRICER.presentValue(PRODUCT_NS_BTW, RATES_PROVIDER,
        PRODUCT_NS_BTW.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertEquals(resNsBtw, -0.29068253160089597 * NOTIONAL, TOL * NOTIONAL);
  }

  public void cleanPvTruncationTest() {
    CreditRatesProvider ratesAccEndDate = createCreditRatesProvider(LocalDate.of(2014, 3, 22));
    double resAccEndDate = PRICER.presentValue(PRODUCT_BEFORE, ratesAccEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesAccEndDate.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertEquals(resAccEndDate, -0.26418577838510354 * (-NOTIONAL), TOL * NOTIONAL);
    CreditRatesProvider ratesEffectiveEndDate = createCreditRatesProvider(LocalDate.of(2014, 3, 21));
    double resEffectiveEndDate = PRICER.presentValue(PRODUCT_BEFORE, ratesEffectiveEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesEffectiveEndDate.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertEquals(resEffectiveEndDate, -0.26422628099362094 * (-NOTIONAL), TOL * NOTIONAL);
    CreditRatesProvider ratesProtectionEndDateOne = createCreditRatesProvider(LocalDate.of(2024, 9, 19));
    double resProtectionEndDateOne = PRICER.presentValue(PRODUCT_BEFORE, ratesProtectionEndDateOne,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesProtectionEndDateOne.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertEquals(resProtectionEndDateOne, -1.1814923847919301E-4 * (-NOTIONAL), TOL * NOTIONAL);
    CreditRatesProvider ratesProtectionEndDate = createCreditRatesProvider(LocalDate.of(2024, 9, 20));
    double resProtectionEndDate = PRICER.presentValue(PRODUCT_BEFORE, ratesProtectionEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesProtectionEndDate.getValuationDate(), REF_DATA), CLEAN, REF_DATA)
        .getAmount();
    assertEquals(resProtectionEndDate, 0d, TOL * NOTIONAL);
  }

  public void protectionLegRegressionTest() {
    double resNext = PRICER.protectionLeg(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(resNext, 0.11770082424693698, TOL);
    double resBefore = PRICER.protectionLeg(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(resBefore, 0.19621836970171463, TOL);
    double resAfter = PRICER.protectionLeg(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(resAfter, 0.2744043768251808, TOL);
    double resNsToday = PRICER.protectionLeg(PRODUCT_NS_TODAY, RATES_PROVIDER,
        PRODUCT_NS_TODAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(resNsToday, 0.12920042414763938, TOL);
    double resNsStepin = PRICER.protectionLeg(PRODUCT_NS_STEPIN, RATES_PROVIDER,
        PRODUCT_NS_STEPIN.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(resNsStepin, 0.07540932150559641, TOL);
    double resNsBtw = PRICER.protectionLeg(PRODUCT_NS_BTW, RATES_PROVIDER,
        PRODUCT_NS_BTW.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), REF_DATA);
    assertEquals(resNsBtw, 0.22727774070157128, TOL);
  }

  public void premiumLegRegressionTest() {
    double resNext = PRICER.riskyAnnuity(PRODUCT_NEXTDAY, RATES_PROVIDER,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertEquals(resNext, 6.395697031451866, TOL);
    double resBefore = PRICER.riskyAnnuity(PRODUCT_BEFORE, RATES_PROVIDER,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertEquals(resBefore, 9.314426609002561, TOL);
    double resAfter = PRICER.riskyAnnuity(PRODUCT_AFTER, RATES_PROVIDER,
        PRODUCT_AFTER.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertEquals(resAfter, 12.018397498258862, TOL);
    double resNsToday = PRICER.riskyAnnuity(PRODUCT_NS_TODAY, RATES_PROVIDER,
        PRODUCT_NS_TODAY.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertEquals(resNsToday, 6.806862528024904, TOL);
    double resNsStepin = PRICER.riskyAnnuity(PRODUCT_NS_STEPIN, RATES_PROVIDER,
        PRODUCT_NS_STEPIN.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertEquals(resNsStepin, 4.89033052696166, TOL);
    double resNsBtw = PRICER.riskyAnnuity(PRODUCT_NS_BTW, RATES_PROVIDER,
        PRODUCT_NS_BTW.getSettlementDateOffset().adjust(RATES_PROVIDER.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertEquals(resNsBtw, 10.367538779382677, TOL);
  }

  public void truncationRegressionTest() {
    CreditRatesProvider ratesAccEndDate = createCreditRatesProvider(LocalDate.of(2014, 3, 22));
    double resAccEndDate = PRICER.riskyAnnuity(PRODUCT_BEFORE, ratesAccEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesAccEndDate.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertEquals(resAccEndDate, 9.140484282937514, TOL);
    CreditRatesProvider ratesEffectiveEndDate = createCreditRatesProvider(LocalDate.of(2014, 3, 21));
    double resEffectiveEndDate = PRICER.riskyAnnuity(PRODUCT_BEFORE, ratesEffectiveEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesEffectiveEndDate.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertEquals(resEffectiveEndDate, 9.139474456128156, TOL);
    CreditRatesProvider ratesProtectionEndDateOne = createCreditRatesProvider(LocalDate.of(2024, 9, 19));
    double resProtectionEndDateOne = PRICER.riskyAnnuity(PRODUCT_BEFORE, ratesProtectionEndDateOne,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesProtectionEndDateOne.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertEquals(resProtectionEndDateOne, 0.2583274486014851, TOL);
    CreditRatesProvider ratesProtectionEndDate = createCreditRatesProvider(LocalDate.of(2024, 9, 20));
    double resProtectionEndDate = PRICER.riskyAnnuity(PRODUCT_BEFORE, ratesProtectionEndDate,
        PRODUCT_BEFORE.getSettlementDateOffset().adjust(ratesProtectionEndDate.getValuationDate(), REF_DATA), DIRTY, REF_DATA);
    assertEquals(resProtectionEndDate, 0d, TOL);
  }

  public void epsilonTest() {
    // Math.abs(dhrt) < 1e-5 is true
    DoubleArray timeDsc = DoubleArray.of(0.5d, 1d, 3d, 5d, 10d, 20d);
    DoubleArray rateDsc = DoubleArray.of(0.01, 0.03, 0.02, 0.01, 0.005, 0.005);
    DoubleArray timeCrd = DoubleArray.of(0.5866887582723792, 1.352879628192491, 2.3701168800050576, 3.10563128282816,
        11.326702860486112, 27.026366789997947);
    DoubleArray rateCrd = DoubleArray.of(-0.010004517471216213, -0.030006261363849242, -0.020009969610648954,
        -0.010001132580906982, -0.0050024950914495684, -0.0050025781457872075);
    InterpolatedNodalCurve nodalYc = InterpolatedNodalCurve.of(METADATA_YC, timeDsc, rateDsc,
        CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
    IsdaCompliantZeroRateDiscountFactors yc = IsdaCompliantZeroRateDiscountFactors.of(USD, VALUATION_DATE, nodalYc);
    InterpolatedNodalCurve nodalCc = InterpolatedNodalCurve.of(METADATA_CC, timeCrd, rateCrd,
        CurveInterpolators.PRODUCT_LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.PRODUCT_LINEAR);
    CreditDiscountFactors cc = IsdaCompliantZeroRateDiscountFactors.of(USD, VALUATION_DATE, nodalCc);
    CreditRatesProvider ratesProvider = ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, cc)))
        .discountCurves(ImmutableMap.of(USD, yc))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, RECOVERY_RATES))
        .build();
    // pv
    CurrencyAmount pv1 = PRICER.presentValue(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    CurrencyAmount pv2 = PRICER_FIX.presentValue(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    CurrencyAmount pv3 = PRICER_CORRECT.presentValue(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), CLEAN, REF_DATA);
    assertEquals(pv1.getAmount(), -0.3728276314104907 * NOTIONAL, NOTIONAL * TOL);
    assertEquals(pv2.getAmount(), -0.3728585818359114 * NOTIONAL, NOTIONAL * TOL);
    assertEquals(pv3.getAmount(), -0.3728305887124643 * NOTIONAL, NOTIONAL * TOL);
    // sensitivity
    PointSensitivityBuilder point1 = PRICER.presentValueSensitivity(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities res1 = ratesProvider.parameterSensitivity(point1.build());
    CurrencyParameterSensitivities exp1 =
        CALC_FD.sensitivity(ratesProvider, p -> PRICER.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(res1, exp1, NOTIONAL * EPS);
    PointSensitivityBuilder point2 = PRICER_FIX.presentValueSensitivity(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities res2 = ratesProvider.parameterSensitivity(point2.build());
    CurrencyParameterSensitivities exp2 =
        CALC_FD.sensitivity(ratesProvider, p -> PRICER_FIX.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(res2, exp2, NOTIONAL * EPS);
    PointSensitivityBuilder point3 = PRICER_CORRECT.presentValueSensitivity(PRODUCT_NEXTDAY, ratesProvider,
        PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), REF_DATA), REF_DATA);
    CurrencyParameterSensitivities res3 = ratesProvider.parameterSensitivity(point3.build());
    CurrencyParameterSensitivities exp3 =
        CALC_FD.sensitivity(ratesProvider, p -> PRICER_CORRECT.presentValue(PRODUCT_NEXTDAY,
            p, PRODUCT_NEXTDAY.getSettlementDateOffset().adjust(p.getValuationDate(), REF_DATA), CLEAN, REF_DATA));
    equalWithRelativeTolerance(res3, exp3, NOTIONAL * EPS);
  }

  public void accruedInterestTest() {
    double acc = PRODUCT_BEFORE.accruedYearFraction(VALUATION_DATE) * PRODUCT_BEFORE.getFixedRate();
    double accAccEndDate = PRODUCT_BEFORE.accruedYearFraction(LocalDate.of(2014, 3, 22)) * PRODUCT_BEFORE.getFixedRate();
    double accEffectiveEndDateOne = PRODUCT_BEFORE.accruedYearFraction(LocalDate.of(2014, 3, 20)) * PRODUCT_BEFORE.getFixedRate();
    double accEffectiveEndDate = PRODUCT_BEFORE.accruedYearFraction(LocalDate.of(2014, 3, 21)) * PRODUCT_BEFORE.getFixedRate();
    assertEquals(acc, 0.0019444444444444446, TOL);
    assertEquals(accAccEndDate, 2.777777777777778E-4, TOL);
    assertEquals(accEffectiveEndDateOne, 0d, TOL);
    assertEquals(accEffectiveEndDate, 1.388888888888889E-4, TOL);
  }

  //-------------------------------------------------------------------------
  public void jumpToDefaultTest() {
    SplitCurrencyAmount<StandardId> computed = PRICER.jumpToDefault(PRODUCT_BEFORE, RATES_PROVIDER, VALUATION_DATE, REF_DATA);
    LocalDate stepinDate = PRODUCT_BEFORE.getStepinDateOffset().adjust(VALUATION_DATE, REF_DATA);
    double dirtyPv = PRICER.presentValue(PRODUCT_BEFORE, RATES_PROVIDER, VALUATION_DATE, PriceType.DIRTY, REF_DATA).getAmount();
    double accrued = PRODUCT_BEFORE.accruedYearFraction(stepinDate) * PRODUCT_BEFORE.getFixedRate() *
        PRODUCT_BEFORE.getBuySell().normalize(NOTIONAL);
    double protection = PRODUCT_BEFORE.getBuySell().normalize(NOTIONAL) * (1d - RECOVERY_RATES.getRecoveryRate());
    double expected = protection - accrued - dirtyPv;
    assertEquals(computed.getCurrency(), USD);
    assertTrue(computed.getSplitValues().size() == 1);
    assertEquals(computed.getSplitValues().get(LEGAL_ENTITY), expected, NOTIONAL * TOL);
  }

  public void expectedLossTest() {
    CurrencyAmount computed = PRICER.expectedLoss(PRODUCT_BEFORE, RATES_PROVIDER);
    double survivalProb = CREDIT_CRVE.discountFactor(PRODUCT_BEFORE.getProtectionEndDate());
    double expected = NOTIONAL * (1d - RECOVERY_RATES.getRecoveryRate()) * (1d - survivalProb);
    assertEquals(computed.getCurrency(), USD);
    assertEquals(computed.getAmount(), expected, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  private CreditRatesProvider createCreditRatesProvider(LocalDate valuationDate) {
    IsdaCompliantZeroRateDiscountFactors yc = IsdaCompliantZeroRateDiscountFactors.of(USD, valuationDate, NODAL_YC);
    CreditDiscountFactors cc = IsdaCompliantZeroRateDiscountFactors.of(USD, valuationDate, NODAL_CC);
    ConstantRecoveryRates rr = ConstantRecoveryRates.of(LEGAL_ENTITY, valuationDate, 0.25);
    return ImmutableCreditRatesProvider.builder()
        .valuationDate(valuationDate)
        .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, cc)))
        .discountCurves(ImmutableMap.of(USD, yc))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, rr))
        .build();
  }

}
