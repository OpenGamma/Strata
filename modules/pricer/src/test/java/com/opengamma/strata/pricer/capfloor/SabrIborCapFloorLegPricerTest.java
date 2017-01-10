/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static com.opengamma.strata.product.common.PutCall.PUT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Test {@link SabrIborCapFloorLegPricer}.
 */
@Test
public class SabrIborCapFloorLegPricerTest {

  private static final double STRIKE = 0.015;
  private static final double NOTIONAL_VALUE = 100_000_000;
  private static final ValueSchedule STRIKE_SCHEDULE = ValueSchedule.of(STRIKE);
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE);
  private static final LocalDate START = LocalDate.of(2011, 3, 17);
  private static final LocalDate END = LocalDate.of(2016, 3, 17);
  private static final ResolvedIborCapFloorLeg CAP =
      IborCapFloorDataSet.createCapFloorLeg(EUR_EURIBOR_3M, START, END, STRIKE_SCHEDULE, NOTIONAL, CALL, RECEIVE);
  private static final ResolvedIborCapFloorLeg FLOOR =
      IborCapFloorDataSet.createCapFloorLeg(EUR_EURIBOR_3M, START, END, STRIKE_SCHEDULE, NOTIONAL, PUT, PAY);
  // valuation before start
  private static final ZonedDateTime VALUATION = dateUtc(2011, 1, 20);
  private static final ImmutableRatesProvider RATES = IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
      VALUATION.toLocalDate(), EUR_EURIBOR_3M, LocalDateDoubleTimeSeries.empty());
  private static final SabrIborCapletFloorletVolatilities VOLS = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(VALUATION, EUR_EURIBOR_3M);
  // valuation between fixing of 3rd period and payment of 2nd period
  private static final double OBS_INDEX_2 = 0.012;
  private static final double OBS_INDEX_3 = 0.0125;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 6, 15), OBS_INDEX_2)
      .put(date(2011, 9, 15), OBS_INDEX_3)
      .build();
  private static final ZonedDateTime VALUATION_AFTER = dateUtc(2011, 9, 16);
  private static final ImmutableRatesProvider RATES_AFTER = IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
      VALUATION_AFTER.toLocalDate(), EUR_EURIBOR_3M, TIME_SERIES);
  private static final SabrIborCapletFloorletVolatilities VOLS_AFTER = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(VALUATION_AFTER, EUR_EURIBOR_3M);
  // valuation at payment of 2nd period
  private static final ZonedDateTime VALUATION_PAY = dateUtc(2011, 9, 19);
  private static final ImmutableRatesProvider RATES_PAY =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION_PAY.toLocalDate(), EUR_EURIBOR_3M, TIME_SERIES);
  private static final SabrIborCapletFloorletVolatilities VOLS_PAY = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(VALUATION_PAY, EUR_EURIBOR_3M);

  private static final double TOL = 1.0e-14;
  private static final SabrIborCapFloorLegPricer PRICER = SabrIborCapFloorLegPricer.DEFAULT;
  private static final SabrIborCapletFloorletPeriodPricer PRICER_PERIOD = SabrIborCapletFloorletPeriodPricer.DEFAULT;

  public void test_presentValue() {
    CurrencyAmount capComputed = PRICER.presentValue(CAP, RATES, VOLS);
    CurrencyAmount floorComputed = PRICER.presentValue(FLOOR, RATES, VOLS);
    double capExpected = 0d;
    double floorExpected = 0d;
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected += PRICER_PERIOD.presentValue(CAP.getCapletFloorletPeriods().get(i), RATES, VOLS).getAmount();
      floorExpected += PRICER_PERIOD.presentValue(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS).getAmount();
    }
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), capExpected);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), floorExpected);
  }

  public void test_presentValue_after() {
    CurrencyAmount capComputed = PRICER.presentValue(CAP, RATES_AFTER, VOLS_AFTER);
    CurrencyAmount floorComputed = PRICER.presentValue(FLOOR, RATES_AFTER, VOLS_AFTER);
    double capExpected = 0d;
    IborCapletFloorletPeriod period = FLOOR.getCapletFloorletPeriods().get(1);
    double floorExpected = -(STRIKE - OBS_INDEX_2) * NOTIONAL_VALUE * period.getYearFraction() *
        RATES_AFTER.discountFactor(EUR, period.getPaymentDate());
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 2; i < nPeriods; ++i) {
      capExpected += PRICER_PERIOD.presentValue(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
      floorExpected += PRICER_PERIOD.presentValue(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
    }
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), capExpected, TOL * NOTIONAL_VALUE);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), floorExpected, TOL * NOTIONAL_VALUE);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueDelta() {
    CurrencyAmount capComputed = PRICER.presentValueDelta(CAP, RATES, VOLS);
    CurrencyAmount floorComputed = PRICER.presentValueDelta(FLOOR, RATES, VOLS);
    double capExpected = 0d;
    double floorExpected = 0d;
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected += PRICER_PERIOD.presentValueDelta(CAP.getCapletFloorletPeriods().get(i), RATES, VOLS).getAmount();
      floorExpected += PRICER_PERIOD.presentValueDelta(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS)
          .getAmount();
    }
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), capExpected, TOL * NOTIONAL_VALUE);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), floorExpected, TOL * NOTIONAL_VALUE);
  }

  public void test_presentValueDelta_after() {
    CurrencyAmount capComputed = PRICER.presentValueDelta(CAP, RATES_AFTER, VOLS_AFTER);
    CurrencyAmount floorComputed = PRICER.presentValueDelta(FLOOR, RATES_AFTER, VOLS_AFTER);
    double capExpected = 0d;
    double floorExpected = 0d;
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 2; i < nPeriods; ++i) {
      capExpected += PRICER_PERIOD.presentValueDelta(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER)
          .getAmount();
      floorExpected += PRICER_PERIOD
          .presentValueDelta(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
    }
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), capExpected, TOL * NOTIONAL_VALUE);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), floorExpected, TOL * NOTIONAL_VALUE);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueGamma() {
    CurrencyAmount capComputed = PRICER.presentValueGamma(CAP, RATES, VOLS);
    CurrencyAmount floorComputed = PRICER.presentValueGamma(FLOOR, RATES, VOLS);
    double capExpected = 0d;
    double floorExpected = 0d;
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected += PRICER_PERIOD.presentValueGamma(CAP.getCapletFloorletPeriods().get(i), RATES, VOLS).getAmount();
      floorExpected += PRICER_PERIOD.presentValueGamma(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS)
          .getAmount();
    }
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), capExpected, TOL * NOTIONAL_VALUE);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), floorExpected, TOL * NOTIONAL_VALUE);
  }

  public void test_presentValueGamma_after() {
    CurrencyAmount capComputed = PRICER.presentValueGamma(CAP, RATES_AFTER, VOLS_AFTER);
    CurrencyAmount floorComputed = PRICER.presentValueGamma(FLOOR, RATES_AFTER, VOLS_AFTER);
    double capExpected = 0d;
    double floorExpected = 0d;
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 2; i < nPeriods; ++i) {
      capExpected += PRICER_PERIOD.presentValueGamma(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER)
          .getAmount();
      floorExpected += PRICER_PERIOD
          .presentValueGamma(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
    }
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), capExpected, TOL * NOTIONAL_VALUE);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), floorExpected, TOL * NOTIONAL_VALUE);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueTheta() {
    CurrencyAmount capComputed = PRICER.presentValueTheta(CAP, RATES, VOLS);
    CurrencyAmount floorComputed = PRICER.presentValueTheta(FLOOR, RATES, VOLS);
    double capExpected = 0d;
    double floorExpected = 0d;
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected += PRICER_PERIOD.presentValueTheta(CAP.getCapletFloorletPeriods().get(i), RATES, VOLS).getAmount();
      floorExpected += PRICER_PERIOD.presentValueTheta(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS)
          .getAmount();
    }
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), capExpected, TOL * NOTIONAL_VALUE);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), floorExpected, TOL * NOTIONAL_VALUE);
  }

  public void test_presentValueTheta_after() {
    CurrencyAmount capComputed = PRICER.presentValueTheta(CAP, RATES_AFTER, VOLS_AFTER);
    CurrencyAmount floorComputed = PRICER.presentValueTheta(FLOOR, RATES_AFTER, VOLS_AFTER);
    double capExpected = 0d;
    double floorExpected = 0d;
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 2; i < nPeriods; ++i) {
      capExpected += PRICER_PERIOD.presentValueTheta(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER)
          .getAmount();
      floorExpected += PRICER_PERIOD
          .presentValueTheta(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
    }
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), capExpected, TOL * NOTIONAL_VALUE);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), floorExpected, TOL * NOTIONAL_VALUE);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityRatesStickyModel() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityRatesStickyModel(CAP, RATES, VOLS);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityRatesStickyModel(FLOOR, RATES, VOLS);
    PointSensitivityBuilder capExpected = PointSensitivityBuilder.none();
    PointSensitivityBuilder floorExpected = PointSensitivityBuilder.none();
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRatesStickyModel(CAP.getCapletFloorletPeriods().get(i), RATES, VOLS));
      floorExpected = floorExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRatesStickyModel(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS));
    }
    CurrencyParameterSensitivities capSensiComputed = RATES_AFTER.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = RATES_AFTER.parameterSensitivity(floorComputed.build());
    CurrencyParameterSensitivities capSensiExpected = RATES_AFTER.parameterSensitivity(capExpected.build());
    CurrencyParameterSensitivities floorSensiExpected = RATES_AFTER.parameterSensitivity(floorExpected.build());
    assertTrue(capSensiComputed.equalWithTolerance(capSensiExpected, NOTIONAL_VALUE * TOL));
    assertTrue(floorSensiComputed.equalWithTolerance(floorSensiExpected, NOTIONAL_VALUE * TOL));
  }

  public void test_presentValueSensitivityRatesStickyModel_after() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityRatesStickyModel(CAP, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityRatesStickyModel(FLOOR, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder capExpected = PointSensitivityBuilder.none();
    IborCapletFloorletPeriod period = FLOOR.getCapletFloorletPeriods().get(1);
    PointSensitivityBuilder floorExpected = RATES_AFTER.discountFactors(EUR).zeroRatePointSensitivity(period.getPaymentDate())
        .multipliedBy(-(STRIKE - OBS_INDEX_2) * NOTIONAL_VALUE * period.getYearFraction());
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 2; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRatesStickyModel(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER));
      floorExpected = floorExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRatesStickyModel(
              FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER));
    }
    CurrencyParameterSensitivities capSensiComputed = RATES_AFTER.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = RATES_AFTER.parameterSensitivity(floorComputed.build());
    CurrencyParameterSensitivities capSensiExpected = RATES_AFTER.parameterSensitivity(capExpected.build());
    CurrencyParameterSensitivities floorSensiExpected = RATES_AFTER.parameterSensitivity(floorExpected.build());
    assertTrue(capSensiComputed.equalWithTolerance(capSensiExpected, NOTIONAL_VALUE * TOL));
    assertTrue(floorSensiComputed.equalWithTolerance(floorSensiExpected, NOTIONAL_VALUE * TOL));
  }

  public void test_presentValueSensitivityRates() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityRates(CAP, RATES, VOLS);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityRates(FLOOR, RATES, VOLS);
    PointSensitivityBuilder capExpected = PointSensitivityBuilder.none();
    PointSensitivityBuilder floorExpected = PointSensitivityBuilder.none();
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRates(CAP.getCapletFloorletPeriods().get(i), RATES, VOLS));
      floorExpected = floorExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRates(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS));
    }
    CurrencyParameterSensitivities capSensiComputed = RATES_AFTER.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = RATES_AFTER.parameterSensitivity(floorComputed.build());
    CurrencyParameterSensitivities capSensiExpected = RATES_AFTER.parameterSensitivity(capExpected.build());
    CurrencyParameterSensitivities floorSensiExpected = RATES_AFTER.parameterSensitivity(floorExpected.build());
    assertTrue(capSensiComputed.equalWithTolerance(capSensiExpected, NOTIONAL_VALUE * TOL));
    assertTrue(floorSensiComputed.equalWithTolerance(floorSensiExpected, NOTIONAL_VALUE * TOL));
  }

  public void test_presentValueSensitivityRates_after() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityRates(CAP, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityRates(FLOOR, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder capExpected = PointSensitivityBuilder.none();
    IborCapletFloorletPeriod period = FLOOR.getCapletFloorletPeriods().get(1);
    PointSensitivityBuilder floorExpected = RATES_AFTER.discountFactors(EUR).zeroRatePointSensitivity(period.getPaymentDate())
        .multipliedBy(-(STRIKE - OBS_INDEX_2) * NOTIONAL_VALUE * period.getYearFraction());
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 2; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRates(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER));
      floorExpected = floorExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRates(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER));
    }
    CurrencyParameterSensitivities capSensiComputed = RATES_AFTER.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = RATES_AFTER.parameterSensitivity(floorComputed.build());
    CurrencyParameterSensitivities capSensiExpected = RATES_AFTER.parameterSensitivity(capExpected.build());
    CurrencyParameterSensitivities floorSensiExpected = RATES_AFTER.parameterSensitivity(floorExpected.build());
    assertTrue(capSensiComputed.equalWithTolerance(capSensiExpected, NOTIONAL_VALUE * TOL));
    assertTrue(floorSensiComputed.equalWithTolerance(floorSensiExpected, NOTIONAL_VALUE * TOL));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityModelParamsSabr() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityModelParamsSabr(CAP, RATES, VOLS);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityModelParamsSabr(FLOOR, RATES, VOLS);
    CurrencyParameterSensitivities capExpected = CurrencyParameterSensitivities.empty();
    CurrencyParameterSensitivities floorExpected = CurrencyParameterSensitivities.empty();
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(VOLS.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsSabr(CAP.getCapletFloorletPeriods().get(i), RATES, VOLS).build()));
      floorExpected = floorExpected.combinedWith(VOLS.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsSabr(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS).build()));
    }
    CurrencyParameterSensitivities capSensiComputed = VOLS.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = VOLS.parameterSensitivity(floorComputed.build());
    assertTrue(capSensiComputed.equalWithTolerance(capExpected, TOL * NOTIONAL_VALUE));
    assertTrue(floorSensiComputed.equalWithTolerance(floorExpected, TOL * NOTIONAL_VALUE));
  }

  public void test_presentValueSensitivityModelParamsSabr_after() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityModelParamsSabr(CAP, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityModelParamsSabr(FLOOR, RATES_AFTER, VOLS_AFTER);
    CurrencyParameterSensitivities capExpected = CurrencyParameterSensitivities.empty();
    CurrencyParameterSensitivities floorExpected = CurrencyParameterSensitivities.empty();
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 3; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(VOLS_AFTER.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsSabr(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).build()));
      floorExpected = floorExpected.combinedWith(VOLS_AFTER.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsSabr(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER)
          .build()));
    }
    CurrencyParameterSensitivities capSensiComputed = VOLS_AFTER.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = VOLS_AFTER.parameterSensitivity(floorComputed.build());
    assertTrue(capSensiComputed.equalWithTolerance(capExpected, TOL * NOTIONAL_VALUE));
    assertTrue(floorSensiComputed.equalWithTolerance(floorExpected, TOL * NOTIONAL_VALUE));
  }

  public void test_presentValueSensitivityModelParamsVolatility() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityModelParamsVolatility(CAP, RATES, VOLS);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityModelParamsVolatility(FLOOR, RATES, VOLS);
    CurrencyParameterSensitivities capExpected = CurrencyParameterSensitivities.empty();
    CurrencyParameterSensitivities floorExpected = CurrencyParameterSensitivities.empty();
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(VOLS.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsVolatility(CAP.getCapletFloorletPeriods().get(i), RATES, VOLS).build()));
      floorExpected = floorExpected.combinedWith(VOLS.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsVolatility(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS).build()));
    }
    CurrencyParameterSensitivities capSensiComputed = VOLS.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = VOLS.parameterSensitivity(floorComputed.build());
    assertTrue(capSensiComputed.equalWithTolerance(capExpected, TOL * NOTIONAL_VALUE));
    assertTrue(floorSensiComputed.equalWithTolerance(floorExpected, TOL * NOTIONAL_VALUE));
  }

  public void test_presentValueSensitivityModelParamsVolatility_after() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityModelParamsVolatility(CAP, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityModelParamsVolatility(FLOOR, RATES_AFTER, VOLS_AFTER);
    CurrencyParameterSensitivities capExpected = CurrencyParameterSensitivities.empty();
    CurrencyParameterSensitivities floorExpected = CurrencyParameterSensitivities.empty();
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 3; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(VOLS_AFTER.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsVolatility(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).build()));
      floorExpected = floorExpected.combinedWith(VOLS_AFTER.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsVolatility(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER)
          .build()));
    }
    CurrencyParameterSensitivities capSensiComputed = VOLS_AFTER.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = VOLS_AFTER.parameterSensitivity(floorComputed.build());
    assertTrue(capSensiComputed.equalWithTolerance(capExpected, TOL * NOTIONAL_VALUE));
    assertTrue(floorSensiComputed.equalWithTolerance(floorExpected, TOL * NOTIONAL_VALUE));
  }

  //-------------------------------------------------------------------------
  public void test_currentCash() {
    CurrencyAmount capComputed = PRICER.currentCash(CAP, RATES, VOLS);
    CurrencyAmount floorComputed = PRICER.currentCash(FLOOR, RATES, VOLS);
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), 0d);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), 0d);
  }

  public void test_currentCash_pay() {
    CurrencyAmount capComputed = PRICER.currentCash(CAP, RATES_PAY, VOLS_PAY);
    CurrencyAmount floorComputed = PRICER.currentCash(FLOOR, RATES_PAY, VOLS_PAY);
    double capExpected = 0d;
    IborCapletFloorletPeriod period = FLOOR.getCapletFloorletPeriods().get(1);
    double floorExpected = -(STRIKE - OBS_INDEX_2) * NOTIONAL_VALUE * period.getYearFraction();
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), capExpected);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), floorExpected, TOL * NOTIONAL_VALUE);
  }

}
