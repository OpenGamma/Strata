/*
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Test {@link NormalIborCapFloorLegPricer}.
 */
public class NormalIborCapFloorLegPricerTest {

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
  private static final ImmutableRatesProvider RATES =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION.toLocalDate());
  private static final NormalIborCapletFloorletExpiryStrikeVolatilities VOLS = IborCapletFloorletDataSet
      .createNormalVolatilities(VALUATION, EUR_EURIBOR_3M);
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
      .createNormalVolatilities(VALUATION_AFTER, EUR_EURIBOR_3M);
  // valuation at payment of 2nd period
  private static final ZonedDateTime VALUATION_PAY = dateUtc(2011, 9, 19);
  private static final ImmutableRatesProvider RATES_PAY =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION_PAY.toLocalDate(), EUR_EURIBOR_3M, TIME_SERIES);
  private static final NormalIborCapletFloorletExpiryStrikeVolatilities VOLS_PAY = IborCapletFloorletDataSet
      .createNormalVolatilities(VALUATION_PAY, EUR_EURIBOR_3M);

  private static final double TOL = 1.0e-14;
  private static final NormalIborCapFloorLegPricer PRICER = NormalIborCapFloorLegPricer.DEFAULT;
  private static final NormalIborCapletFloorletPeriodPricer PRICER_PERIOD = NormalIborCapletFloorletPeriodPricer.DEFAULT;

  @Test
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
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isEqualTo(capExpected);
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isEqualTo(floorExpected);
  }

  @Test
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
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isCloseTo(capExpected, offset(TOL * NOTIONAL_VALUE));
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isCloseTo(floorExpected, offset(TOL * NOTIONAL_VALUE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueCapletFloorletPeriods() {
    CurrencyAmount capComputed = PRICER.presentValue(CAP, RATES, VOLS);
    CurrencyAmount floorComputed = PRICER.presentValue(FLOOR, RATES, VOLS);
    Map<IborCapletFloorletPeriod, CurrencyAmount> capletsComputed =
        PRICER.presentValueCapletFloorletPeriods(CAP, RATES, VOLS).getAmounts();
    Map<IborCapletFloorletPeriod, CurrencyAmount> floorletsComputed =
        PRICER.presentValueCapletFloorletPeriods(FLOOR, RATES, VOLS).getAmounts();

    Map<IborCapletFloorletPeriod, CurrencyAmount> capletsExpected = MapStream.of(CAP.getCapletFloorletPeriods())
        .mapValues(caplet -> PRICER_PERIOD.presentValue(caplet, RATES, VOLS))
        .toMap();
    CurrencyAmount capletsTotalExpected = capletsExpected.values().stream().reduce(CurrencyAmount::plus).get();
    Map<IborCapletFloorletPeriod, CurrencyAmount> floorletsExpected = MapStream.of(FLOOR.getCapletFloorletPeriods())
        .mapValues(floorlet -> PRICER_PERIOD.presentValue(floorlet, RATES, VOLS))
        .toMap();
    CurrencyAmount floorletsTotalExpected = floorletsExpected.values().stream().reduce(CurrencyAmount::plus).get();

    assertThat(capletsComputed).isEqualTo(capletsExpected);
    assertThat(capComputed).isEqualTo(capletsTotalExpected);
    assertThat(floorletsComputed).isEqualTo(floorletsExpected);
    assertThat(floorComputed).isEqualTo(floorletsTotalExpected);
  }

  @Test
  public void test_presentValueCapletFloorletPeriods_after() {
    CurrencyAmount capComputed = PRICER.presentValue(CAP, RATES_AFTER, VOLS_AFTER);
    CurrencyAmount floorComputed = PRICER.presentValue(FLOOR, RATES_AFTER, VOLS_AFTER);
    Map<IborCapletFloorletPeriod, CurrencyAmount> capletsComputed =
        PRICER.presentValueCapletFloorletPeriods(CAP, RATES_AFTER, VOLS_AFTER).getAmounts();
    Map<IborCapletFloorletPeriod, CurrencyAmount> floorletsComputed =
        PRICER.presentValueCapletFloorletPeriods(FLOOR, RATES_AFTER, VOLS_AFTER).getAmounts();

    Map<IborCapletFloorletPeriod, CurrencyAmount> capletsExpected = MapStream.of(CAP.getCapletFloorletPeriods())
        .mapValues(caplet -> PRICER_PERIOD.presentValue(caplet, RATES_AFTER, VOLS_AFTER))
        .toMap();
    CurrencyAmount capletsTotalExpected = capletsExpected.values().stream().reduce(CurrencyAmount::plus).get();
    Map<IborCapletFloorletPeriod, CurrencyAmount> floorletsExpected = MapStream.of(FLOOR.getCapletFloorletPeriods())
        .mapValues(floorlet -> PRICER_PERIOD.presentValue(floorlet, RATES_AFTER, VOLS_AFTER))
        .toMap();
    CurrencyAmount floorletsTotalExpected = floorletsExpected.values().stream().reduce(CurrencyAmount::plus).get();

    assertThat(capletsComputed).isEqualTo(capletsExpected);
    assertThat(capComputed).isEqualTo(capletsTotalExpected);
    assertThat(floorletsComputed).isEqualTo(floorletsExpected);
    assertThat(floorComputed).isEqualTo(floorletsTotalExpected);
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isCloseTo(capExpected, offset(TOL * NOTIONAL_VALUE));
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isCloseTo(floorExpected, offset(TOL * NOTIONAL_VALUE));
  }

  @Test
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
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isCloseTo(capExpected, offset(TOL * NOTIONAL_VALUE));
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isCloseTo(floorExpected, offset(TOL * NOTIONAL_VALUE));
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isCloseTo(capExpected, offset(TOL * NOTIONAL_VALUE));
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isCloseTo(floorExpected, offset(TOL * NOTIONAL_VALUE));
  }

  @Test
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
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isCloseTo(capExpected, offset(TOL * NOTIONAL_VALUE));
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isCloseTo(floorExpected, offset(TOL * NOTIONAL_VALUE));
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isCloseTo(capExpected, offset(TOL * NOTIONAL_VALUE));
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isCloseTo(floorExpected, offset(TOL * NOTIONAL_VALUE));
  }

  @Test
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
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isCloseTo(capExpected, offset(TOL * NOTIONAL_VALUE));
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isCloseTo(floorExpected, offset(TOL * NOTIONAL_VALUE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity() {
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
    assertThat(capSensiComputed.equalWithTolerance(capSensiExpected, NOTIONAL_VALUE * TOL)).isTrue();
    assertThat(floorSensiComputed.equalWithTolerance(floorSensiExpected, NOTIONAL_VALUE * TOL)).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_after() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityRates(CAP, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityRates(FLOOR, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder capExpected = PointSensitivityBuilder.none();
    IborCapletFloorletPeriod period = FLOOR.getCapletFloorletPeriods().get(1);
    PointSensitivityBuilder floorExpected = RATES_AFTER.discountFactors(EUR)
        .zeroRatePointSensitivity(period.getPaymentDate())
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
    assertThat(capSensiComputed.equalWithTolerance(capSensiExpected, NOTIONAL_VALUE * TOL)).isTrue();
    assertThat(floorSensiComputed.equalWithTolerance(floorSensiExpected, NOTIONAL_VALUE * TOL)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivityVolatility() {
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
    CurrencyParameterSensitivities capSensiComputed =
        VOLS.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed =
        VOLS.parameterSensitivity(floorComputed.build());
    CurrencyParameterSensitivity capSensiExpected = capExpected.getSensitivities().get(0);
    CurrencyParameterSensitivity floorSensiExpected = floorExpected.getSensitivities().get(0);
    assertThat(DoubleArrayMath.fuzzyEquals(capSensiComputed.getSensitivities().get(0).getSensitivity().toArray(),
        capSensiExpected.getSensitivity().toArray(), TOL * NOTIONAL_VALUE)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(floorSensiComputed.getSensitivities().get(0).getSensitivity().toArray(),
        floorSensiExpected.getSensitivity().toArray(), TOL * NOTIONAL_VALUE)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityVolatility_after() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityModelParamsVolatility(CAP, RATES_AFTER, VOLS_AFTER);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityModelParamsVolatility(FLOOR, RATES_AFTER, VOLS_AFTER);
    CurrencyParameterSensitivities capExpected = CurrencyParameterSensitivities.empty();
    CurrencyParameterSensitivities floorExpected = CurrencyParameterSensitivities.empty();
    int nPeriods = CAP.getCapletFloorletPeriods().size();
    for (int i = 3; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(VOLS_AFTER.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsVolatility(CAP.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).build()));
      floorExpected = floorExpected.combinedWith(VOLS_AFTER.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsVolatility(FLOOR.getCapletFloorletPeriods().get(i), RATES_AFTER, VOLS_AFTER).build()));
    }
    CurrencyParameterSensitivities capSensiComputed =
        VOLS_AFTER.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed =
        VOLS_AFTER.parameterSensitivity(floorComputed.build());
    CurrencyParameterSensitivity capSensiExpected = capExpected.getSensitivities().get(0);
    CurrencyParameterSensitivity floorSensiExpected = floorExpected.getSensitivities().get(0);
    assertThat(DoubleArrayMath.fuzzyEquals(capSensiComputed.getSensitivities().get(0).getSensitivity().toArray(),
        capSensiExpected.getSensitivity().toArray(), TOL * NOTIONAL_VALUE)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(floorSensiComputed.getSensitivities().get(0).getSensitivity().toArray(),
        floorSensiExpected.getSensitivity().toArray(), TOL * NOTIONAL_VALUE)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currentCash() {
    CurrencyAmount capComputed = PRICER.currentCash(CAP, RATES, VOLS);
    CurrencyAmount floorComputed = PRICER.currentCash(FLOOR, RATES, VOLS);
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isEqualTo(0d);
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isEqualTo(0d);
  }

  @Test
  public void test_currentCash_pay() {
    CurrencyAmount capComputed = PRICER.currentCash(CAP, RATES_PAY, VOLS_PAY);
    CurrencyAmount floorComputed = PRICER.currentCash(FLOOR, RATES_PAY, VOLS_PAY);
    double capExpected = 0d;
    IborCapletFloorletPeriod period = FLOOR.getCapletFloorletPeriods().get(1);
    double floorExpected = -(STRIKE - OBS_INDEX_2) * NOTIONAL_VALUE * period.getYearFraction();
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isEqualTo(capExpected);
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isCloseTo(floorExpected, offset(TOL * NOTIONAL_VALUE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_impliedVolatility() {
    Map<IborCapletFloorletPeriod, Double> computed =
        PRICER.impliedVolatilities(CAP, RATES, VOLS).getAmounts();
    Map<IborCapletFloorletPeriod, Double> expected = MapStream.of(CAP.getCapletFloorletPeriods())
        .mapValues(caplet -> PRICER_PERIOD.impliedVolatility(caplet, RATES, VOLS))
        .toMap();
    assertThat(computed).isEqualTo(expected);
    computed.forEach((caplet, vol) -> assertThat(vol).isCloseTo(expected.get(caplet), offset(TOL)));
  }

  @Test
  public void test_impliedVolatility_onFix() {
    Map<IborCapletFloorletPeriod, Double> computed =
        PRICER.impliedVolatilities(CAP, RATES_PAY, VOLS_PAY).getAmounts();
    Map<IborCapletFloorletPeriod, Double> expected = MapStream.of(CAP.getCapletFloorletPeriods())
        .filterKeys(caplet -> VOLS_PAY.relativeTime(caplet.getFixingDateTime()) >= 0)
        .mapValues(caplet -> PRICER_PERIOD.impliedVolatility(caplet, RATES_PAY, VOLS_PAY))
        .toMap();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_impliedVolatility_afterFix() {
    Map<IborCapletFloorletPeriod, Double> computed =
        PRICER.impliedVolatilities(CAP, RATES_AFTER, VOLS_AFTER).getAmounts();
    Map<IborCapletFloorletPeriod, Double> expected = MapStream.of(CAP.getCapletFloorletPeriods())
        .filterKeys(caplet -> VOLS_PAY.relativeTime(caplet.getFixingDateTime()) >= 0)
        .mapValues(caplet -> PRICER_PERIOD.impliedVolatility(caplet, RATES_AFTER, VOLS_AFTER))
        .toMap();
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forwardRate() {
    Map<IborCapletFloorletPeriod, Double> computed = PRICER.forwardRates(CAP, RATES).getAmounts();
    Map<IborCapletFloorletPeriod, Double> expected = MapStream.of(CAP.getCapletFloorletPeriods())
        .filterKeys(caplet -> !RATES.getValuationDate().isAfter(caplet.getFixingDate()))
        .mapValues(caplet -> PRICER_PERIOD.forwardRate(caplet, RATES))
        .toMap();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_forwardRate_onFix() {
    Map<IborCapletFloorletPeriod, Double> computed = PRICER.forwardRates(CAP, RATES_PAY).getAmounts();
    Map<IborCapletFloorletPeriod, Double> expected = MapStream.of(CAP.getCapletFloorletPeriods())
        .filterKeys(caplet -> !RATES_PAY.getValuationDate().isAfter(caplet.getFixingDate()))
        .mapValues(caplet -> PRICER_PERIOD.forwardRate(caplet, RATES_PAY))
        .toMap();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_forwardRate_afterFix() {
    Map<IborCapletFloorletPeriod, Double> computed = PRICER.forwardRates(CAP, RATES_AFTER).getAmounts();
    Map<IborCapletFloorletPeriod, Double> expected = MapStream.of(CAP.getCapletFloorletPeriods())
        .filterKeys(caplet -> !RATES_AFTER.getValuationDate().isAfter(caplet.getFixingDate()))
        .mapValues(caplet -> PRICER_PERIOD.forwardRate(caplet, RATES_AFTER))
        .toMap();
    assertThat(computed).isEqualTo(expected);
  }
}
