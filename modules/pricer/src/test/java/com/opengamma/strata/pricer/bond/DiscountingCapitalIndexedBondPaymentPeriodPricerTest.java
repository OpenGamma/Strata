/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.pricer.CompoundedRateType.CONTINUOUS;
import static com.opengamma.strata.pricer.CompoundedRateType.PERIODIC;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.bond.CapitalIndexedBondPaymentPeriod;
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationEndMonthRateComputation;

/**
 * Test {@link DiscountingCapitalIndexedBondPaymentPeriodPricer}.
 */
@Test
public class DiscountingCapitalIndexedBondPaymentPeriodPricerTest {
  // periods
  private static final LocalDate START_UNADJ = LocalDate.of(2008, 1, 13);
  private static final LocalDate END_UNADJ = LocalDate.of(2008, 7, 13);
  private static final LocalDate START = LocalDate.of(2008, 1, 14);
  private static final LocalDate END = LocalDate.of(2008, 7, 14);
  private static final YearMonth REF_END = YearMonth.of(2008, 4);
  private static final PriceIndexObservation OBS = PriceIndexObservation.of(US_CPI_U, REF_END);
  private static final PriceIndexObservation OBS_PLUS1 = PriceIndexObservation.of(US_CPI_U, REF_END.plusMonths(1));
  private static final double NOTIONAL = 10_000_000d;
  private static final double REAL_COUPON = 0.01d;
  private static final LocalDate DETACHMENT = LocalDate.of(2008, 1, 11);
  private static final double START_INDEX = 198.475;
  private static final double WEIGHT = 0.6;
  private static final InflationEndInterpolatedRateComputation COMPUTE_INTERP =
      InflationEndInterpolatedRateComputation.of(US_CPI_U, START_INDEX, REF_END, WEIGHT);
  private static final InflationEndMonthRateComputation COMPUTE_MONTH =
      InflationEndMonthRateComputation.of(US_CPI_U, START_INDEX, REF_END);
  private static final CapitalIndexedBondPaymentPeriod PERIOD_INTERP = CapitalIndexedBondPaymentPeriod.builder()
      .currency(USD)
      .notional(NOTIONAL)
      .detachmentDate(DETACHMENT)
      .startDate(START)
      .endDate(END)
      .unadjustedStartDate(START_UNADJ)
      .unadjustedEndDate(END_UNADJ)
      .rateComputation(COMPUTE_INTERP)
      .realCoupon(REAL_COUPON)
      .build();
  private static final CapitalIndexedBondPaymentPeriod PERIOD_MONTHLY = CapitalIndexedBondPaymentPeriod.builder()
      .currency(USD)
      .notional(NOTIONAL)
      .detachmentDate(DETACHMENT)
      .startDate(START)
      .endDate(END)
      .unadjustedStartDate(START_UNADJ)
      .unadjustedEndDate(END_UNADJ)
      .rateComputation(COMPUTE_MONTH)
      .realCoupon(REAL_COUPON)
      .build();
  // rates providers
  private static final LocalDate VALUATION_BEFORE_START = LocalDate.of(2007, 10, 9);
  private static final LocalDate VALUATION_ON_FIX = LocalDate.of(2008, 4, 29);
  private static final LocalDate VALUATION_AFTER_FIX = LocalDate.of(2008, 5, 20);
  private static final LocalDate VALUATION_AFTER_PAY = LocalDate.of(2008, 8, 9);
  private static final double Z_SPREAD = 0.005;
  private static final int PERIOD_PER_YEAR = 4;
  private static final IssuerCurveDiscountFactors ICDF_BEFORE_START =
      CapitalIndexedBondCurveDataSet.getIssuerCurveDiscountFactors(VALUATION_BEFORE_START);
  private static final IssuerCurveDiscountFactors ICDF_ON_FIX =
      CapitalIndexedBondCurveDataSet.getIssuerCurveDiscountFactors(VALUATION_ON_FIX);
  private static final IssuerCurveDiscountFactors ICDF_AFTER_FIX =
      CapitalIndexedBondCurveDataSet.getIssuerCurveDiscountFactors(VALUATION_AFTER_FIX);
  private static final IssuerCurveDiscountFactors ICDF_AFTER_PAY =
      CapitalIndexedBondCurveDataSet.getIssuerCurveDiscountFactors(VALUATION_AFTER_PAY);
  private static final LegalEntityDiscountingProvider LEDP_BEFORE_START =
      CapitalIndexedBondCurveDataSet.getLegalEntityDiscountingProvider(VALUATION_BEFORE_START);
  private static final LegalEntityDiscountingProvider LEDP_ON_FIX =
      CapitalIndexedBondCurveDataSet.getLegalEntityDiscountingProvider(VALUATION_ON_FIX);
  private static final LegalEntityDiscountingProvider LEDP_AFTER_FIX =
      CapitalIndexedBondCurveDataSet.getLegalEntityDiscountingProvider(VALUATION_AFTER_FIX);
  private static final LocalDateDoubleTimeSeries TIME_SERIES_OLD =
      LocalDateDoubleTimeSeries.of(LocalDate.of(2007, 9, 30), 200.0);
  private static final double INDEX_END_1 = 210.5;
  private static final double INDEX_END_2 = 215.5;
  private static final LocalDateDoubleTimeSeries TIME_SERIES_ONE =
      LocalDateDoubleTimeSeries.of(REF_END.atEndOfMonth(), INDEX_END_1);
  private static final LocalDateDoubleTimeSeries TIME_SERIES_TWO = LocalDateDoubleTimeSeries.builder()
      .put(REF_END.atEndOfMonth(), INDEX_END_1)
      .put(REF_END.plusMonths(1).atEndOfMonth(), INDEX_END_2)
      .build();
  private static final ImmutableRatesProvider IRP_BEFORE_START =
      CapitalIndexedBondCurveDataSet.getRatesProvider(VALUATION_BEFORE_START, TIME_SERIES_OLD);
  private static final ImmutableRatesProvider IRP_ON_FIX =
      CapitalIndexedBondCurveDataSet.getRatesProvider(VALUATION_ON_FIX, TIME_SERIES_ONE);
  private static final ImmutableRatesProvider IRP_AFTER_FIX =
      CapitalIndexedBondCurveDataSet.getRatesProvider(VALUATION_AFTER_FIX, TIME_SERIES_TWO);
  private static final ImmutableRatesProvider IRP_AFTER_PAY =
      CapitalIndexedBondCurveDataSet.getRatesProvider(VALUATION_AFTER_PAY, TIME_SERIES_TWO);
  // calculators
  private static final double TOL = 1.0e-13;
  private static final double FD_EPS = 1.0e-6;
  private static final DiscountingCapitalIndexedBondPaymentPeriodPricer PRICER =
      DiscountingCapitalIndexedBondPaymentPeriodPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  //-------------------------------------------------------------------------
  public void test_getter() {
    assertEquals(PRICER.getRateComputationFn(), RateComputationFn.standard());
  }

  //-------------------------------------------------------------------------
  public void test_presentValue_beforeStart() {
    double computedInterp = PRICER.presentValue(PERIOD_INTERP, IRP_BEFORE_START, ICDF_BEFORE_START);
    double computedMonthly = PRICER.presentValue(PERIOD_MONTHLY, IRP_BEFORE_START, ICDF_BEFORE_START);
    double computedFvInterp = PRICER.forecastValue(PERIOD_INTERP, IRP_BEFORE_START);
    double computedFvMonthly = PRICER.forecastValue(PERIOD_MONTHLY, IRP_BEFORE_START);
    double index1 = IRP_BEFORE_START.priceIndexValues(US_CPI_U).value(OBS);
    double index2 = IRP_BEFORE_START.priceIndexValues(US_CPI_U).value(OBS_PLUS1);
    double df = ICDF_BEFORE_START.discountFactor(END);
    double expectedFvInterp = (index1 * WEIGHT + (1d - WEIGHT) * index2) / START_INDEX * REAL_COUPON * NOTIONAL;
    double expectedFvMonthly = index1 / START_INDEX * REAL_COUPON * NOTIONAL;
    assertEquals(computedFvInterp, expectedFvInterp, TOL * expectedFvInterp);
    assertEquals(computedFvMonthly, expectedFvMonthly, TOL * expectedFvMonthly);
    assertEquals(computedInterp, expectedFvInterp * df, TOL * expectedFvInterp * df);
    assertEquals(computedMonthly, expectedFvMonthly * df, TOL * expectedFvMonthly * df);
  }

  public void test_presentValue_onFix() {
    double computedInterp = PRICER.presentValue(PERIOD_INTERP, IRP_ON_FIX, ICDF_ON_FIX);
    double computedMonthly = PRICER.presentValue(PERIOD_MONTHLY, IRP_ON_FIX, ICDF_ON_FIX);
    double index2 = IRP_ON_FIX.priceIndexValues(US_CPI_U).value(OBS_PLUS1);
    double df = ICDF_ON_FIX.discountFactor(END);
    double expectedInterp = (INDEX_END_1 * WEIGHT + (1d - WEIGHT) * index2) / START_INDEX * REAL_COUPON * NOTIONAL * df;
    double expectedMonthly = INDEX_END_1 / START_INDEX * REAL_COUPON * NOTIONAL * df;
    assertEquals(computedInterp, expectedInterp, TOL * expectedInterp);
    assertEquals(computedMonthly, expectedMonthly, TOL * expectedMonthly);
  }

  public void test_presentValue_afterFix() {
    double computedInterp = PRICER.presentValue(PERIOD_INTERP, IRP_AFTER_FIX, ICDF_AFTER_FIX);
    double computedMonthly = PRICER.presentValue(PERIOD_MONTHLY, IRP_AFTER_FIX, ICDF_AFTER_FIX);
    double df = ICDF_AFTER_FIX.discountFactor(END);
    double expectedInterp = (INDEX_END_1 * WEIGHT + (1d - WEIGHT) * INDEX_END_2) / START_INDEX * REAL_COUPON * NOTIONAL * df;
    double expectedMonthly = INDEX_END_1 / START_INDEX * REAL_COUPON * NOTIONAL * df;
    assertEquals(computedInterp, expectedInterp, TOL * expectedInterp);
    assertEquals(computedMonthly, expectedMonthly, TOL * expectedMonthly);
  }

  public void test_presentValue_afterPay() {
    double computedInterp = PRICER.presentValue(PERIOD_INTERP, IRP_AFTER_PAY, ICDF_AFTER_PAY);
    double computedMonthly = PRICER.presentValue(PERIOD_MONTHLY, IRP_AFTER_PAY, ICDF_AFTER_PAY);
    assertEquals(computedInterp, 0d, TOL);
    assertEquals(computedMonthly, 0d, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueWithZSpread_beforeStart() {
    double computedInterp = PRICER.presentValueWithZSpread(
        PERIOD_INTERP, IRP_BEFORE_START, ICDF_BEFORE_START, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double computedMonthly = PRICER.presentValueWithZSpread(
        PERIOD_MONTHLY, IRP_BEFORE_START, ICDF_BEFORE_START, Z_SPREAD, CONTINUOUS, 0);
    double index1 = IRP_BEFORE_START.priceIndexValues(US_CPI_U).value(OBS);
    double index2 = IRP_BEFORE_START.priceIndexValues(US_CPI_U).value(OBS_PLUS1);
    double expectedInterp = (index1 * WEIGHT + (1d - WEIGHT) * index2) / START_INDEX * REAL_COUPON * NOTIONAL *
        ICDF_BEFORE_START.getDiscountFactors().discountFactorWithSpread(END, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double expectedMonthly = index1 / START_INDEX * REAL_COUPON * NOTIONAL *
        ICDF_BEFORE_START.getDiscountFactors().discountFactorWithSpread(END, Z_SPREAD, CONTINUOUS, 0);
    assertEquals(computedInterp, expectedInterp, TOL * expectedInterp);
    assertEquals(computedMonthly, expectedMonthly, TOL * expectedMonthly);
  }

  public void test_presentValueWithZSpread_onFix() {
    double computedInterp = PRICER.presentValueWithZSpread(
        PERIOD_INTERP, IRP_ON_FIX, ICDF_ON_FIX, Z_SPREAD, CONTINUOUS, 0);
    double computedMonthly = PRICER.presentValueWithZSpread(
        PERIOD_MONTHLY, IRP_ON_FIX, ICDF_ON_FIX, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double index2 = IRP_ON_FIX.priceIndexValues(US_CPI_U).value(OBS_PLUS1);
    double expectedInterp = (INDEX_END_1 * WEIGHT + (1d - WEIGHT) * index2) / START_INDEX * REAL_COUPON * NOTIONAL *
        ICDF_ON_FIX.getDiscountFactors().discountFactorWithSpread(END, Z_SPREAD, CONTINUOUS, 0);
    double expectedMonthly = INDEX_END_1 / START_INDEX * REAL_COUPON * NOTIONAL *
        ICDF_ON_FIX.getDiscountFactors().discountFactorWithSpread(END, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertEquals(computedInterp, expectedInterp, TOL * expectedInterp);
    assertEquals(computedMonthly, expectedMonthly, TOL * expectedMonthly);
  }

  public void test_presentValueWithZSpread_afterFix() {
    double computedInterp = PRICER.presentValueWithZSpread(
        PERIOD_INTERP, IRP_AFTER_FIX, ICDF_AFTER_FIX, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double computedMonthly = PRICER.presentValueWithZSpread(
        PERIOD_MONTHLY, IRP_AFTER_FIX, ICDF_AFTER_FIX, Z_SPREAD, CONTINUOUS, 0);
    double expectedInterp = (INDEX_END_1 * WEIGHT + (1d - WEIGHT) * INDEX_END_2) / START_INDEX * REAL_COUPON * NOTIONAL *
        ICDF_AFTER_FIX.getDiscountFactors().discountFactorWithSpread(END, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double expectedMonthly = INDEX_END_1 / START_INDEX * REAL_COUPON * NOTIONAL *
        ICDF_AFTER_FIX.getDiscountFactors().discountFactorWithSpread(END, Z_SPREAD, CONTINUOUS, 0);
    assertEquals(computedInterp, expectedInterp, TOL * expectedInterp);
    assertEquals(computedMonthly, expectedMonthly, TOL * expectedMonthly);
  }

  public void test_presentValueWithZSpread_afterPay() {
    double computedInterp = PRICER.presentValueWithZSpread(
        PERIOD_INTERP, IRP_AFTER_PAY, ICDF_AFTER_PAY, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double computedMonthly = PRICER.presentValueWithZSpread(
        PERIOD_MONTHLY, IRP_AFTER_PAY, ICDF_AFTER_PAY, Z_SPREAD, CONTINUOUS, 0);
    assertEquals(computedInterp, 0d, TOL);
    assertEquals(computedMonthly, 0d, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity_beforeStart() {
    PointSensitivityBuilder pointInterp = 
        PRICER.presentValueSensitivity(PERIOD_INTERP, IRP_BEFORE_START, ICDF_BEFORE_START);
    CurrencyParameterSensitivities computedInterp1 =
        LEDP_BEFORE_START.parameterSensitivity(pointInterp.build());
    CurrencyParameterSensitivities computedInterp2 =
        IRP_BEFORE_START.parameterSensitivity(pointInterp.build());
    PointSensitivityBuilder pointMonthly =
        PRICER.presentValueSensitivity(PERIOD_MONTHLY, IRP_BEFORE_START, ICDF_BEFORE_START);
    CurrencyParameterSensitivities computedMonthly1 =
        LEDP_BEFORE_START.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities computedMonthly2 =
        IRP_BEFORE_START.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities expectedInterp =
        fdSensitivity(PERIOD_INTERP, IRP_BEFORE_START, LEDP_BEFORE_START);
    CurrencyParameterSensitivities expectedMonthly =
        fdSensitivity(PERIOD_MONTHLY, IRP_BEFORE_START, LEDP_BEFORE_START);
    assertTrue(computedInterp1.combinedWith(computedInterp2).equalWithTolerance(expectedInterp, NOTIONAL * FD_EPS));
    assertTrue(computedMonthly1.combinedWith(computedMonthly2).equalWithTolerance(expectedMonthly, NOTIONAL * FD_EPS));
  }

  public void test_presentValueSensitivity_onFix() {
    PointSensitivityBuilder pointInterp =
        PRICER.presentValueSensitivity(PERIOD_INTERP, IRP_ON_FIX, ICDF_ON_FIX);
    CurrencyParameterSensitivities computedInterp1 =
        LEDP_ON_FIX.parameterSensitivity(pointInterp.build());
    CurrencyParameterSensitivities computedInterp2 =
        IRP_ON_FIX.parameterSensitivity(pointInterp.build());
    PointSensitivityBuilder pointMonthly =
        PRICER.presentValueSensitivity(PERIOD_MONTHLY, IRP_ON_FIX, ICDF_ON_FIX);
    CurrencyParameterSensitivities computedMonthly1 =
        LEDP_ON_FIX.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities computedMonthly2 =
        IRP_ON_FIX.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities expectedInterp =
        fdSensitivity(PERIOD_INTERP, IRP_ON_FIX, LEDP_ON_FIX);
    CurrencyParameterSensitivities expectedMonthly =
        fdSensitivity(PERIOD_MONTHLY, IRP_ON_FIX, LEDP_ON_FIX);
    assertTrue(computedInterp1.combinedWith(computedInterp2).equalWithTolerance(expectedInterp, NOTIONAL * FD_EPS));
    assertTrue(computedMonthly1.combinedWith(computedMonthly2).equalWithTolerance(expectedMonthly, NOTIONAL * FD_EPS));
  }

  public void test_presentValueSensitivity_afterFix() {
    PointSensitivityBuilder pointInterp =
        PRICER.presentValueSensitivity(PERIOD_INTERP, IRP_AFTER_FIX, ICDF_AFTER_FIX);
    CurrencyParameterSensitivities computedInterp1 =
        LEDP_AFTER_FIX.parameterSensitivity(pointInterp.build());
    CurrencyParameterSensitivities computedInterp2 =
        IRP_AFTER_FIX.parameterSensitivity(pointInterp.build());
    PointSensitivityBuilder pointMonthly =
        PRICER.presentValueSensitivity(PERIOD_MONTHLY, IRP_AFTER_FIX, ICDF_AFTER_FIX);
    CurrencyParameterSensitivities computedMonthly1 =
        LEDP_AFTER_FIX.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities computedMonthly2 =
        IRP_AFTER_FIX.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities expectedInterp =
        fdSensitivity(PERIOD_INTERP, IRP_AFTER_FIX, LEDP_AFTER_FIX);
    CurrencyParameterSensitivities expectedMonthly =
        fdSensitivity(PERIOD_MONTHLY, IRP_AFTER_FIX, LEDP_AFTER_FIX);
    assertTrue(computedInterp1.combinedWith(computedInterp2).equalWithTolerance(expectedInterp, NOTIONAL * FD_EPS));
    assertTrue(computedMonthly1.combinedWith(computedMonthly2).equalWithTolerance(expectedMonthly, NOTIONAL * FD_EPS));
  }

  public void test_presentValueSensitivity_afterPay() {
    PointSensitivityBuilder computedInterp =
        PRICER.presentValueSensitivity(PERIOD_INTERP, IRP_AFTER_PAY, ICDF_AFTER_PAY);
    PointSensitivityBuilder computedMonthly =
        PRICER.presentValueSensitivity(PERIOD_MONTHLY, IRP_AFTER_PAY, ICDF_AFTER_PAY);
    assertEquals(computedInterp, PointSensitivityBuilder.none());
    assertEquals(computedMonthly, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityWithZSpread_beforeStart() {
    PointSensitivityBuilder pointInterp = PRICER.presentValueSensitivityWithZSpread(
        PERIOD_INTERP, IRP_BEFORE_START, ICDF_BEFORE_START, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyParameterSensitivities computedInterp1 =
        LEDP_BEFORE_START.parameterSensitivity(pointInterp.build());
    CurrencyParameterSensitivities computedInterp2 =
        IRP_BEFORE_START.parameterSensitivity(pointInterp.build());
    PointSensitivityBuilder pointMonthly = PRICER.presentValueSensitivityWithZSpread(
        PERIOD_MONTHLY, IRP_BEFORE_START, ICDF_BEFORE_START, Z_SPREAD, CONTINUOUS, 0);
    CurrencyParameterSensitivities computedMonthly1 =
        LEDP_BEFORE_START.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities computedMonthly2 =
        IRP_BEFORE_START.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities expectedInterp = fdSensitivityWithZSpread(
        PERIOD_INTERP, IRP_BEFORE_START, LEDP_BEFORE_START, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyParameterSensitivities expectedMonthly =
        fdSensitivityWithZSpread(PERIOD_MONTHLY, IRP_BEFORE_START, LEDP_BEFORE_START, Z_SPREAD, CONTINUOUS, 0);
    assertTrue(computedInterp1.combinedWith(computedInterp2).equalWithTolerance(expectedInterp, NOTIONAL * FD_EPS));
    assertTrue(computedMonthly1.combinedWith(computedMonthly2).equalWithTolerance(expectedMonthly, NOTIONAL * FD_EPS));
  }

  public void test_presentValueSensitivityWithZSpread_onFix() {
    PointSensitivityBuilder pointInterp =
        PRICER.presentValueSensitivityWithZSpread(PERIOD_INTERP, IRP_ON_FIX, ICDF_ON_FIX, Z_SPREAD, CONTINUOUS, 0);
    CurrencyParameterSensitivities computedInterp1 =
        LEDP_ON_FIX.parameterSensitivity(pointInterp.build());
    CurrencyParameterSensitivities computedInterp2 =
        IRP_ON_FIX.parameterSensitivity(pointInterp.build());
    PointSensitivityBuilder pointMonthly = PRICER.presentValueSensitivityWithZSpread(
        PERIOD_MONTHLY, IRP_ON_FIX, ICDF_ON_FIX, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyParameterSensitivities computedMonthly1 =
        LEDP_ON_FIX.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities computedMonthly2 =
        IRP_ON_FIX.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities expectedInterp =
        fdSensitivityWithZSpread(PERIOD_INTERP, IRP_ON_FIX, LEDP_ON_FIX, Z_SPREAD, CONTINUOUS, 0);
    CurrencyParameterSensitivities expectedMonthly =
        fdSensitivityWithZSpread(PERIOD_MONTHLY, IRP_ON_FIX, LEDP_ON_FIX, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertTrue(computedInterp1.combinedWith(computedInterp2).equalWithTolerance(expectedInterp, NOTIONAL * FD_EPS));
    assertTrue(computedMonthly1.combinedWith(computedMonthly2).equalWithTolerance(expectedMonthly, NOTIONAL * FD_EPS));
  }

  public void test_presentValueSensitivityWithZSpread_afterFix() {
    PointSensitivityBuilder pointInterp = PRICER.presentValueSensitivityWithZSpread(
        PERIOD_INTERP, IRP_AFTER_FIX, ICDF_AFTER_FIX, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyParameterSensitivities computedInterp1 =
        LEDP_AFTER_FIX.parameterSensitivity(pointInterp.build());
    CurrencyParameterSensitivities computedInterp2 =
        IRP_AFTER_FIX.parameterSensitivity(pointInterp.build());
    PointSensitivityBuilder pointMonthly = PRICER.presentValueSensitivityWithZSpread(
        PERIOD_MONTHLY, IRP_AFTER_FIX, ICDF_AFTER_FIX, Z_SPREAD, CONTINUOUS, 0);
    CurrencyParameterSensitivities computedMonthly1 =
        LEDP_AFTER_FIX.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities computedMonthly2 =
        IRP_AFTER_FIX.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities expectedInterp =
        fdSensitivityWithZSpread(PERIOD_INTERP, IRP_AFTER_FIX, LEDP_AFTER_FIX, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyParameterSensitivities expectedMonthly =
        fdSensitivityWithZSpread(PERIOD_MONTHLY, IRP_AFTER_FIX, LEDP_AFTER_FIX, Z_SPREAD, CONTINUOUS, 0);
    assertTrue(computedInterp1.combinedWith(computedInterp2).equalWithTolerance(expectedInterp, NOTIONAL * FD_EPS));
    assertTrue(computedMonthly1.combinedWith(computedMonthly2).equalWithTolerance(expectedMonthly, NOTIONAL * FD_EPS));
  }

  public void test_presentValueSensitivityWithZSpread_afterPay() {
    PointSensitivityBuilder computedInterp = PRICER.presentValueSensitivityWithZSpread(
        PERIOD_INTERP, IRP_AFTER_PAY, ICDF_AFTER_PAY, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    PointSensitivityBuilder computedMonthly = PRICER.presentValueSensitivityWithZSpread(
        PERIOD_MONTHLY, IRP_AFTER_PAY, ICDF_AFTER_PAY, Z_SPREAD, CONTINUOUS, 0);
    assertEquals(computedInterp, PointSensitivityBuilder.none());
    assertEquals(computedMonthly, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_forecastValueSensitivity_beforeStart() {
    PointSensitivityBuilder pointInterp =
        PRICER.forecastValueSensitivity(PERIOD_INTERP, IRP_BEFORE_START);
    CurrencyParameterSensitivities computedInterp =
        IRP_BEFORE_START.parameterSensitivity(pointInterp.build());
    PointSensitivityBuilder pointMonthly =
        PRICER.forecastValueSensitivity(PERIOD_MONTHLY, IRP_BEFORE_START);
    CurrencyParameterSensitivities computedMonthly =
        IRP_BEFORE_START.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities expectedInterp =
        FD_CAL.sensitivity(IRP_BEFORE_START, p -> CurrencyAmount.of(USD, PRICER.forecastValue(PERIOD_INTERP, p)));
    CurrencyParameterSensitivities expectedMonthly =
        FD_CAL.sensitivity(IRP_BEFORE_START, p -> CurrencyAmount.of(USD, PRICER.forecastValue(PERIOD_MONTHLY, p)));
    assertTrue(computedInterp.equalWithTolerance(expectedInterp, NOTIONAL * FD_EPS));
    assertTrue(computedMonthly.equalWithTolerance(expectedMonthly, NOTIONAL * FD_EPS));
  }

  public void test_forecastValueSensitivity_onFix() {
    PointSensitivityBuilder pointInterp =
        PRICER.forecastValueSensitivity(PERIOD_INTERP, IRP_ON_FIX);
    CurrencyParameterSensitivities computedInterp =
        IRP_ON_FIX.parameterSensitivity(pointInterp.build());
    PointSensitivityBuilder pointMonthly =
        PRICER.forecastValueSensitivity(PERIOD_MONTHLY, IRP_ON_FIX);
    CurrencyParameterSensitivities computedMonthly =
        IRP_ON_FIX.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities expectedInterp =
        FD_CAL.sensitivity(IRP_ON_FIX, p -> CurrencyAmount.of(USD, PRICER.forecastValue(PERIOD_INTERP, p)));
    CurrencyParameterSensitivities expectedMonthly =
        FD_CAL.sensitivity(IRP_ON_FIX, p -> CurrencyAmount.of(USD, PRICER.forecastValue(PERIOD_MONTHLY, p)));
    assertTrue(computedInterp.equalWithTolerance(expectedInterp, NOTIONAL * FD_EPS));
    assertTrue(computedMonthly.equalWithTolerance(expectedMonthly, NOTIONAL * FD_EPS));
  }

  public void test_forecastValueSensitivity_afterFix() {
    PointSensitivityBuilder pointInterp =
        PRICER.forecastValueSensitivity(PERIOD_INTERP, IRP_AFTER_FIX);
    CurrencyParameterSensitivities computedInterp =
        IRP_AFTER_FIX.parameterSensitivity(pointInterp.build());
    PointSensitivityBuilder pointMonthly =
        PRICER.forecastValueSensitivity(PERIOD_MONTHLY, IRP_AFTER_FIX);
    CurrencyParameterSensitivities computedMonthly =
        IRP_AFTER_FIX.parameterSensitivity(pointMonthly.build());
    CurrencyParameterSensitivities expectedInterp =
        FD_CAL.sensitivity(IRP_AFTER_FIX, p -> CurrencyAmount.of(USD, PRICER.forecastValue(PERIOD_INTERP, p)));
    CurrencyParameterSensitivities expectedMonthly =
        FD_CAL.sensitivity(IRP_AFTER_FIX, p -> CurrencyAmount.of(USD, PRICER.forecastValue(PERIOD_MONTHLY, p)));
    assertTrue(computedInterp.equalWithTolerance(expectedInterp, NOTIONAL * FD_EPS));
    assertTrue(computedMonthly.equalWithTolerance(expectedMonthly, NOTIONAL * FD_EPS));
  }

  public void test_forecastValueSensitivity_afterPay() {
    PointSensitivityBuilder computedInterp =
        PRICER.forecastValueSensitivity(PERIOD_INTERP, IRP_AFTER_PAY);
    PointSensitivityBuilder computedMonthly =
        PRICER.forecastValueSensitivity(PERIOD_MONTHLY, IRP_AFTER_PAY);
    assertEquals(computedInterp, PointSensitivityBuilder.none());
    assertEquals(computedMonthly, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_explainPresentValue() {
    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValue(PERIOD_INTERP, IRP_BEFORE_START, ICDF_BEFORE_START, builder);
    ExplainMap explain = builder.build();
    assertEquals(explain.get(ExplainKey.ENTRY_TYPE).get(), "CapitalIndexedBondPaymentPeriod");
    assertEquals(explain.get(ExplainKey.PAYMENT_DATE).get(), PERIOD_INTERP.getPaymentDate());
    assertEquals(explain.get(ExplainKey.PAYMENT_CURRENCY).get(), PERIOD_INTERP.getCurrency());
    assertEquals(explain.get(ExplainKey.START_DATE).get(), START);
    assertEquals(explain.get(ExplainKey.UNADJUSTED_START_DATE).get(), START_UNADJ);
    assertEquals(explain.get(ExplainKey.END_DATE).get(), END);
    assertEquals(explain.get(ExplainKey.UNADJUSTED_END_DATE).get(), END_UNADJ);
    assertEquals(explain.get(ExplainKey.DAYS).get().intValue(), (int) DAYS.between(START_UNADJ, END_UNADJ));
    assertEquals(explain.get(ExplainKey.DISCOUNT_FACTOR).get(), ICDF_BEFORE_START.discountFactor(END));
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount(),
        PRICER.forecastValue(PERIOD_INTERP, IRP_BEFORE_START), NOTIONAL * TOL);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount(),
        PRICER.presentValue(PERIOD_INTERP, IRP_BEFORE_START, ICDF_BEFORE_START), NOTIONAL * TOL);
  }

  public void test_explainPresentValue_past() {
    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValue(PERIOD_INTERP, IRP_AFTER_PAY, ICDF_AFTER_PAY, builder);
    ExplainMap explain = builder.build();
    assertEquals(explain.get(ExplainKey.ENTRY_TYPE).get(), "CapitalIndexedBondPaymentPeriod");
    assertEquals(explain.get(ExplainKey.PAYMENT_DATE).get(), PERIOD_INTERP.getPaymentDate());
    assertEquals(explain.get(ExplainKey.PAYMENT_CURRENCY).get(), PERIOD_INTERP.getCurrency());
    assertEquals(explain.get(ExplainKey.START_DATE).get(), START);
    assertEquals(explain.get(ExplainKey.UNADJUSTED_START_DATE).get(), START_UNADJ);
    assertEquals(explain.get(ExplainKey.END_DATE).get(), END);
    assertEquals(explain.get(ExplainKey.UNADJUSTED_END_DATE).get(), END_UNADJ);
    assertEquals(explain.get(ExplainKey.DAYS).get().intValue(), (int) DAYS.between(START_UNADJ, END_UNADJ));
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_explainPresentValueWithZSpread() {
    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValueWithZSpread(
        PERIOD_INTERP, IRP_BEFORE_START, ICDF_BEFORE_START, builder, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    ExplainMap explain = builder.build();
    assertEquals(explain.get(ExplainKey.ENTRY_TYPE).get(), "CapitalIndexedBondPaymentPeriod");
    assertEquals(explain.get(ExplainKey.PAYMENT_DATE).get(), PERIOD_INTERP.getPaymentDate());
    assertEquals(explain.get(ExplainKey.PAYMENT_CURRENCY).get(), PERIOD_INTERP.getCurrency());
    assertEquals(explain.get(ExplainKey.START_DATE).get(), START);
    assertEquals(explain.get(ExplainKey.UNADJUSTED_START_DATE).get(), START_UNADJ);
    assertEquals(explain.get(ExplainKey.END_DATE).get(), END);
    assertEquals(explain.get(ExplainKey.UNADJUSTED_END_DATE).get(), END_UNADJ);
    assertEquals(explain.get(ExplainKey.DAYS).get().intValue(), (int) DAYS.between(START_UNADJ, END_UNADJ));
    assertEquals(explain.get(ExplainKey.DISCOUNT_FACTOR).get(), ICDF_BEFORE_START.discountFactor(END));
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount(),
        PRICER.forecastValue(PERIOD_INTERP, IRP_BEFORE_START), NOTIONAL * TOL);
    assertEquals(
        explain.get(ExplainKey.PRESENT_VALUE).get().getAmount(),
        PRICER.presentValueWithZSpread(
            PERIOD_INTERP, IRP_BEFORE_START, ICDF_BEFORE_START, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR),
        NOTIONAL * TOL);
  }

  public void test_explainPresentValueWithZSpread_past() {
    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValueWithZSpread(
        PERIOD_INTERP, IRP_AFTER_PAY, ICDF_AFTER_PAY, builder, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    ExplainMap explain = builder.build();
    assertEquals(explain.get(ExplainKey.ENTRY_TYPE).get(), "CapitalIndexedBondPaymentPeriod");
    assertEquals(explain.get(ExplainKey.PAYMENT_DATE).get(), PERIOD_INTERP.getPaymentDate());
    assertEquals(explain.get(ExplainKey.PAYMENT_CURRENCY).get(), PERIOD_INTERP.getCurrency());
    assertEquals(explain.get(ExplainKey.START_DATE).get(), START);
    assertEquals(explain.get(ExplainKey.UNADJUSTED_START_DATE).get(), START_UNADJ);
    assertEquals(explain.get(ExplainKey.END_DATE).get(), END);
    assertEquals(explain.get(ExplainKey.UNADJUSTED_END_DATE).get(), END_UNADJ);
    assertEquals(explain.get(ExplainKey.DAYS).get().intValue(), (int) DAYS.between(START_UNADJ, END_UNADJ));
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  // computes sensitivity with finite difference approximation
  private CurrencyParameterSensitivities fdSensitivity(
      CapitalIndexedBondPaymentPeriod period,
      ImmutableRatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerRatesProvider) {

    CurrencyParameterSensitivities sensi1 = FD_CAL.sensitivity(
        issuerRatesProvider,
        p -> CurrencyAmount.of(
            USD,
            PRICER.presentValue(
                period,
                ratesProvider,
                p.issuerCurveDiscountFactors(CapitalIndexedBondCurveDataSet.getIssuerId(), USD))));
    CurrencyParameterSensitivities sensi2 = FD_CAL.sensitivity(
        ratesProvider,
        p -> CurrencyAmount.of(
            USD,
            PRICER.presentValue(
                period,
                p,
                issuerRatesProvider.issuerCurveDiscountFactors(CapitalIndexedBondCurveDataSet.getIssuerId(), USD))));
    return sensi1.combinedWith(sensi2);
  }

  // computes sensitivity with finite difference approximation
  private CurrencyParameterSensitivities fdSensitivityWithZSpread(
      CapitalIndexedBondPaymentPeriod period,
      ImmutableRatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerRatesProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    CurrencyParameterSensitivities sensi1 = FD_CAL.sensitivity(
        issuerRatesProvider,
        p -> CurrencyAmount.of(USD,
            PRICER.presentValueWithZSpread(
                period,
                ratesProvider,
                p.issuerCurveDiscountFactors(CapitalIndexedBondCurveDataSet.getIssuerId(), USD),
                zSpread,
                compoundedRateType,
                periodsPerYear)));
    CurrencyParameterSensitivities sensi2 = FD_CAL.sensitivity(
        ratesProvider,
        p -> CurrencyAmount.of(
            USD,
            PRICER.presentValueWithZSpread(
                period,
                p,
                issuerRatesProvider.issuerCurveDiscountFactors(CapitalIndexedBondCurveDataSet.getIssuerId(), USD),
                zSpread,
                compoundedRateType,
                periodsPerYear)));
    return sensi1.combinedWith(sensi2);
  }

}
