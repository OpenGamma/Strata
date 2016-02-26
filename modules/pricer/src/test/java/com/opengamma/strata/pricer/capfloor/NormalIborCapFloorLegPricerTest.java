/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.PutCall.PUT;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.pricer.impl.capfloor.IborCapletFloorletDataSet;
import com.opengamma.strata.pricer.impl.capfloor.NormalIborCapletFloorletPeriodPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.capfloor.ExpandedIborCapFloorLeg;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;

/**
 * Test {@link NormalIborCapFloorLegPricer}.
 */
@Test
public class NormalIborCapFloorLegPricerTest {

  private static final double STRIKE = 0.015;
  private static final double NOTIONAL_VALUE = 100_000_000;
  private static final ValueSchedule STRIKE_SCHEDULE = ValueSchedule.of(STRIKE);
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE);
  private static final LocalDate START = LocalDate.of(2011, 3, 17);
  private static final LocalDate END = LocalDate.of(2016, 3, 17);
  private static final ExpandedIborCapFloorLeg CAP =
      IborCapFloorDataSet.createCapFloorLeg(EUR_EURIBOR_3M, START, END, STRIKE_SCHEDULE, NOTIONAL, CALL, RECEIVE).expand();
  private static final ExpandedIborCapFloorLeg FLOOR =
      IborCapFloorDataSet.createCapFloorLeg(EUR_EURIBOR_3M, START, END, STRIKE_SCHEDULE, NOTIONAL, PUT, PAY).expand();

  // valuation before start
  private static final ZonedDateTime VALUATION = dateUtc(2011, 1, 20);
  private static final ImmutableRatesProvider RATES =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION.toLocalDate());
  private static final NormalIborCapletFloorletExpiryStrikeVolatilities VOLS = IborCapletFloorletDataSet
      .createNormalVolatilitiesProvider(VALUATION, EUR_EURIBOR_3M);
  // valuation between fixing of 3rd period and payment of 2nd period
  private static final double OBS_INDEX_2 = 0.012;
  private static final double OBS_INDEX_3 = 0.0125;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 6, 15), OBS_INDEX_2)
      .put(date(2011, 9, 15), OBS_INDEX_3)
      .build();
  private static final ZonedDateTime VALUATION_AFTER = dateUtc(2011, 9, 16);
  private static final ImmutableRatesProvider RATES_AFTER =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION_AFTER.toLocalDate(), EUR_EURIBOR_3M, TIME_SERIES);
  private static final NormalIborCapletFloorletExpiryStrikeVolatilities VOLS_AFTER = IborCapletFloorletDataSet
      .createNormalVolatilitiesProvider(VALUATION_AFTER, EUR_EURIBOR_3M);
  // valuation at payment of 2nd period
  private static final ZonedDateTime VALUATION_PAY = dateUtc(2011, 9, 19);
  private static final ImmutableRatesProvider RATES_PAY =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION_PAY.toLocalDate(), EUR_EURIBOR_3M, TIME_SERIES);
  private static final NormalIborCapletFloorletExpiryStrikeVolatilities VOLS_PAY = IborCapletFloorletDataSet
      .createNormalVolatilitiesProvider(VALUATION_PAY, EUR_EURIBOR_3M);

  private static final double TOL = 1.0e-14;
  private static final NormalIborCapFloorLegPricer PRICER = NormalIborCapFloorLegPricer.DEFAULT;
  private static final NormalIborCapletFloorletPeriodPricer PRICER_PERIOD = NormalIborCapletFloorletPeriodPricer.DEFAULT;

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
    double floorExpected = -(STRIKE - OBS_INDEX_2) * NOTIONAL_VALUE * period.getYearFraction()
        * RATES_AFTER.discountFactor(EUR, period.getPaymentDate());
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
      floorExpected += PRICER_PERIOD.presentValueDelta(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS).getAmount();
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
      capExpected +=
          PRICER_PERIOD.presentValueDelta(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
      floorExpected +=
          PRICER_PERIOD.presentValueDelta(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
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
      floorExpected += PRICER_PERIOD.presentValueGamma(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS).getAmount();
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
      capExpected +=
          PRICER_PERIOD.presentValueGamma(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
      floorExpected +=
          PRICER_PERIOD.presentValueGamma(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
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
      floorExpected += PRICER_PERIOD.presentValueTheta(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS).getAmount();
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
      capExpected +=
          PRICER_PERIOD.presentValueTheta(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
      floorExpected += PRICER_PERIOD
          .presentValueTheta(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).getAmount();
    }
    assertEquals(capComputed.getCurrency(), EUR);
    assertEquals(capComputed.getAmount(), capExpected, TOL * NOTIONAL_VALUE);
    assertEquals(floorComputed.getCurrency(), EUR);
    assertEquals(floorComputed.getAmount(), floorExpected, TOL * NOTIONAL_VALUE);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivity(CAP, RATES, VOLS);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivity(FLOOR, RATES, VOLS);
    PointSensitivityBuilder capExpected = PointSensitivityBuilder.none();
    PointSensitivityBuilder floorExpected = PointSensitivityBuilder.none();
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivity(CAP.getCapletFloorletPeriods().get(i), RATES, VOLS));
      floorExpected = floorExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivity(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS));
    }
    CurveCurrencyParameterSensitivities capSensiComputed = RATES_AFTER.curveParameterSensitivity(capComputed.build());
    CurveCurrencyParameterSensitivities floorSensiComputed = RATES_AFTER.curveParameterSensitivity(floorComputed.build());
    CurveCurrencyParameterSensitivities capSensiExpected = RATES_AFTER.curveParameterSensitivity(capExpected.build());
    CurveCurrencyParameterSensitivities floorSensiExpected = RATES_AFTER.curveParameterSensitivity(floorExpected.build());
    assertTrue(capSensiComputed.equalWithTolerance(capSensiExpected, NOTIONAL_VALUE * TOL));
    assertTrue(floorSensiComputed.equalWithTolerance(floorSensiExpected, NOTIONAL_VALUE * TOL));
  }

  public void test_presentValueSensitivity_after() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivity(CAP, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivity(FLOOR, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder capExpected = PointSensitivityBuilder.none();
    IborCapletFloorletPeriod period = FLOOR.getCapletFloorletPeriods().get(1);
    PointSensitivityBuilder floorExpected = RATES_AFTER.discountFactors(EUR)
        .zeroRatePointSensitivity(period.getPaymentDate())
        .multipliedBy(-(STRIKE - OBS_INDEX_2) * NOTIONAL_VALUE * period.getYearFraction());
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 2; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivity(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER));
      floorExpected = floorExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivity(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER));
    }
    CurveCurrencyParameterSensitivities capSensiComputed = RATES_AFTER.curveParameterSensitivity(capComputed.build());
    CurveCurrencyParameterSensitivities floorSensiComputed = RATES_AFTER.curveParameterSensitivity(floorComputed.build());
    CurveCurrencyParameterSensitivities capSensiExpected = RATES_AFTER.curveParameterSensitivity(capExpected.build());
    CurveCurrencyParameterSensitivities floorSensiExpected = RATES_AFTER.curveParameterSensitivity(floorExpected.build());
    assertTrue(capSensiComputed.equalWithTolerance(capSensiExpected, NOTIONAL_VALUE * TOL));
    assertTrue(floorSensiComputed.equalWithTolerance(floorSensiExpected, NOTIONAL_VALUE * TOL));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityVolatility() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityVolatility(CAP, RATES, VOLS);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityVolatility(FLOOR, RATES, VOLS);
    SurfaceCurrencyParameterSensitivities capExpected = SurfaceCurrencyParameterSensitivities.empty();
    SurfaceCurrencyParameterSensitivities floorExpected = SurfaceCurrencyParameterSensitivities.empty();
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(VOLS.surfaceCurrencyParameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityVolatility(CAP.getCapletFloorletPeriods().get(i), RATES, VOLS).build()));
      floorExpected = floorExpected.combinedWith(VOLS.surfaceCurrencyParameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityVolatility(FLOOR.getCapletFloorletPeriods().get(i), RATES, VOLS).build()));
    }
    SurfaceCurrencyParameterSensitivities capSensiComputed =
        VOLS.surfaceCurrencyParameterSensitivity(capComputed.build());
    SurfaceCurrencyParameterSensitivities floorSensiComputed =
        VOLS.surfaceCurrencyParameterSensitivity(floorComputed.build());
    SurfaceCurrencyParameterSensitivity capSensiExpected = capExpected.getSensitivities().get(0);
    SurfaceCurrencyParameterSensitivity floorSensiExpected = floorExpected.getSensitivities().get(0);
    assertTrue(DoubleArrayMath.fuzzyEquals(capSensiComputed.getSensitivities().get(0).getSensitivity().toArray(),
        capSensiExpected.getSensitivity().toArray(), TOL * NOTIONAL_VALUE));
    assertTrue(DoubleArrayMath.fuzzyEquals(floorSensiComputed.getSensitivities().get(0).getSensitivity().toArray(),
        floorSensiExpected.getSensitivity().toArray(), TOL * NOTIONAL_VALUE));
  }

  public void test_presentValueSensitivityVolatility_after() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityVolatility(CAP, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityVolatility(FLOOR, RATES_AFTER, VOLS_AFTER);
    SurfaceCurrencyParameterSensitivities capExpected = SurfaceCurrencyParameterSensitivities.empty();
    SurfaceCurrencyParameterSensitivities floorExpected = SurfaceCurrencyParameterSensitivities.empty();
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 3; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(VOLS_AFTER.surfaceCurrencyParameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityVolatility(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).build()));
      floorExpected = floorExpected.combinedWith(VOLS_AFTER.surfaceCurrencyParameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityVolatility(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).build()));
    }
    SurfaceCurrencyParameterSensitivities capSensiComputed =
        VOLS_AFTER.surfaceCurrencyParameterSensitivity(capComputed.build());
    SurfaceCurrencyParameterSensitivities floorSensiComputed =
        VOLS_AFTER.surfaceCurrencyParameterSensitivity(floorComputed.build());
    SurfaceCurrencyParameterSensitivity capSensiExpected = capExpected.getSensitivities().get(0);
    SurfaceCurrencyParameterSensitivity floorSensiExpected = floorExpected.getSensitivities().get(0);
    assertTrue(DoubleArrayMath.fuzzyEquals(capSensiComputed.getSensitivities().get(0).getSensitivity().toArray(),
        capSensiExpected.getSensitivity().toArray(), TOL * NOTIONAL_VALUE));
    assertTrue(DoubleArrayMath.fuzzyEquals(floorSensiComputed.getSensitivities().get(0).getSensitivity().toArray(),
        floorSensiExpected.getSensitivity().toArray(), TOL * NOTIONAL_VALUE));
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
