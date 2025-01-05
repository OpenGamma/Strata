/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.CAP_LEG;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.FLOOR_LEG;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.NOTIONAL_VALUE;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.RATES;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.RATES_AFTER;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.RATES_PAY;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.VOLS;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.VOLS_AFTER;
import static com.opengamma.strata.pricer.capfloor.SabrOvernightInArrearsCapFloorTestData.VOLS_PAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletPeriod;

/**
 * Test {@link SabrOvernightInArrearsCapFloorLegPricer}.
 */
public class SabrOvernightInArrearsCapFloorLegPricerTest {

  private static final SabrOvernightInArrearsCapFloorLegPricer PRICER = SabrOvernightInArrearsCapFloorLegPricer.DEFAULT;
  private static final SabrOvernightInArrearsCapletFloorletPeriodPricer PRICER_PERIOD = PRICER.getPeriodPricer();
  private static final double TOL = 1.0e-13;
  
  @Test
  public void test_presentValue() {
    CurrencyAmount capComputed = PRICER.presentValue(CAP_LEG, RATES, VOLS);
    CurrencyAmount floorComputed = PRICER.presentValue(FLOOR_LEG, RATES, VOLS);
    double capExpected = 0d;
    double floorExpected = 0d;
    int nPeriods = CAP_LEG.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected += PRICER_PERIOD.presentValue(CAP_LEG.getCapletFloorletPeriods().get(i), RATES, VOLS).getAmount();
      floorExpected += PRICER_PERIOD.presentValue(FLOOR_LEG.getCapletFloorletPeriods().get(i), RATES, VOLS).getAmount();
    }
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isEqualTo(capExpected);
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isEqualTo(floorExpected);
  }

  @Test
  public void test_presentValue_after() {
    CurrencyAmount capComputed = PRICER.presentValue(CAP_LEG, RATES_AFTER, VOLS_AFTER);
    CurrencyAmount floorComputed = PRICER.presentValue(FLOOR_LEG, RATES_AFTER, VOLS_AFTER);
    double capExpected = 0d;
    double floorExpected = 0d;
    int nPeriods = CAP_LEG.getCapletFloorletPeriods().size();
    for (int i = 1; i < nPeriods; ++i) {
      capExpected += PRICER_PERIOD.presentValue(
          CAP_LEG.getCapletFloorletPeriods().get(i),
          RATES_AFTER,
          VOLS_AFTER).getAmount();
      floorExpected += PRICER_PERIOD.presentValue(
          FLOOR_LEG.getCapletFloorletPeriods().get(i),
          RATES_AFTER,
          VOLS_AFTER).getAmount();
    }
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isCloseTo(capExpected, offset(TOL * NOTIONAL_VALUE));
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isCloseTo(floorExpected, offset(TOL * NOTIONAL_VALUE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueCapletFloorletPeriods() {
    CurrencyAmount capComputed = PRICER.presentValue(CAP_LEG, RATES, VOLS);
    CurrencyAmount floorComputed = PRICER.presentValue(FLOOR_LEG, RATES, VOLS);
    Map<OvernightInArrearsCapletFloorletPeriod, CurrencyAmount> capletsComputed =
        PRICER.presentValueCapletFloorletPeriods(CAP_LEG, RATES, VOLS).getAmounts();
    Map<OvernightInArrearsCapletFloorletPeriod, CurrencyAmount> floorletsComputed =
        PRICER.presentValueCapletFloorletPeriods(FLOOR_LEG, RATES, VOLS).getAmounts();

    Map<OvernightInArrearsCapletFloorletPeriod, CurrencyAmount> capletsExpected =
        MapStream.of(CAP_LEG.getCapletFloorletPeriods())
            .mapValues(caplet -> PRICER_PERIOD.presentValue(caplet, RATES, VOLS))
            .toMap();
    CurrencyAmount capletsTotalExpected = capletsExpected.values().stream().reduce(CurrencyAmount::plus).get();
    Map<OvernightInArrearsCapletFloorletPeriod, CurrencyAmount> floorletsExpected =
        MapStream.of(FLOOR_LEG.getCapletFloorletPeriods())
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
    CurrencyAmount capComputed = PRICER.presentValue(CAP_LEG, RATES_AFTER, VOLS_AFTER);
    CurrencyAmount floorComputed = PRICER.presentValue(FLOOR_LEG, RATES_AFTER, VOLS_AFTER);
    Map<OvernightInArrearsCapletFloorletPeriod, CurrencyAmount> capletsComputed =
        PRICER.presentValueCapletFloorletPeriods(CAP_LEG, RATES_AFTER, VOLS_AFTER).getAmounts();
    Map<OvernightInArrearsCapletFloorletPeriod, CurrencyAmount> floorletsComputed =
        PRICER.presentValueCapletFloorletPeriods(FLOOR_LEG, RATES_AFTER, VOLS_AFTER).getAmounts();

    Map<OvernightInArrearsCapletFloorletPeriod, CurrencyAmount> capletsExpected =
        MapStream.of(CAP_LEG.getCapletFloorletPeriods())
            .mapValues(caplet -> PRICER_PERIOD.presentValue(caplet, RATES_AFTER, VOLS_AFTER))
            .toMap();
    CurrencyAmount capletsTotalExpected = capletsExpected.values().stream().reduce(CurrencyAmount::plus).get();
    Map<OvernightInArrearsCapletFloorletPeriod, CurrencyAmount> floorletsExpected =
        MapStream.of(FLOOR_LEG.getCapletFloorletPeriods())
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
  public void test_presentValueSensitivityRatesStickyModel() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityRatesStickyModel(CAP_LEG, RATES, VOLS);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityRatesStickyModel(FLOOR_LEG, RATES, VOLS);
    PointSensitivityBuilder capExpected = PointSensitivityBuilder.none();
    PointSensitivityBuilder floorExpected = PointSensitivityBuilder.none();
    int nPeriods = CAP_LEG.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRatesStickyModel(
              CAP_LEG.getCapletFloorletPeriods().get(i),
              RATES,
              VOLS));
      floorExpected = floorExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRatesStickyModel(
              FLOOR_LEG.getCapletFloorletPeriods().get(i),
              RATES,
              VOLS));
    }
    CurrencyParameterSensitivities capSensiComputed = RATES_AFTER.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = RATES_AFTER.parameterSensitivity(floorComputed.build());
    CurrencyParameterSensitivities capSensiExpected = RATES_AFTER.parameterSensitivity(capExpected.build());
    CurrencyParameterSensitivities floorSensiExpected = RATES_AFTER.parameterSensitivity(floorExpected.build());
    assertThat(capSensiComputed.equalWithTolerance(capSensiExpected, NOTIONAL_VALUE * TOL)).isTrue();
    assertThat(floorSensiComputed.equalWithTolerance(floorSensiExpected, NOTIONAL_VALUE * TOL)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityRatesStickyModel_after() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityRatesStickyModel(
        CAP_LEG,
        RATES_AFTER,
        VOLS_AFTER);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityRatesStickyModel(
        FLOOR_LEG,
        RATES_AFTER,
        VOLS_AFTER);
    PointSensitivityBuilder capExpected = PointSensitivityBuilder.none();
    PointSensitivityBuilder floorExpected = PointSensitivityBuilder.none();
    int nPeriods = CAP_LEG.getCapletFloorletPeriods().size();
    for (int i = 1; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRatesStickyModel(
              CAP_LEG.getCapletFloorletPeriods().get(i),
              RATES_AFTER,
              VOLS_AFTER));
      floorExpected = floorExpected.combinedWith(
          PRICER_PERIOD.presentValueSensitivityRatesStickyModel(
              FLOOR_LEG.getCapletFloorletPeriods().get(i),
              RATES_AFTER,
              VOLS_AFTER));
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
  public void test_presentValueSensitivityModelParamsSabr() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityModelParamsSabr(CAP_LEG, RATES, VOLS);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityModelParamsSabr(FLOOR_LEG, RATES, VOLS);
    CurrencyParameterSensitivities capExpected = CurrencyParameterSensitivities.empty();
    CurrencyParameterSensitivities floorExpected = CurrencyParameterSensitivities.empty();
    int nPeriods = CAP_LEG.getCapletFloorletPeriods().size();
    for (int i = 0; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(VOLS.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsSabr(CAP_LEG.getCapletFloorletPeriods().get(i), RATES, VOLS).build()));
      floorExpected = floorExpected.combinedWith(VOLS.parameterSensitivity(PRICER_PERIOD
          .presentValueSensitivityModelParamsSabr(FLOOR_LEG.getCapletFloorletPeriods().get(i), RATES, VOLS).build()));
    }
    CurrencyParameterSensitivities capSensiComputed = VOLS.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = VOLS.parameterSensitivity(floorComputed.build());
    assertThat(capSensiComputed.equalWithTolerance(capExpected, TOL * NOTIONAL_VALUE)).isTrue();
    assertThat(floorSensiComputed.equalWithTolerance(floorExpected, TOL * NOTIONAL_VALUE)).isTrue();
  }

  @Test
  public void test_presentValueSensitivityModelParamsSabr_after() {
    PointSensitivityBuilder capComputed = PRICER.presentValueSensitivityModelParamsSabr(
        CAP_LEG,
        RATES_AFTER,
        VOLS_AFTER);
    PointSensitivityBuilder floorComputed = PRICER.presentValueSensitivityModelParamsSabr(
        FLOOR_LEG,
        RATES_AFTER,
        VOLS_AFTER);
    CurrencyParameterSensitivities capExpected = CurrencyParameterSensitivities.empty();
    CurrencyParameterSensitivities floorExpected = CurrencyParameterSensitivities.empty();
    int nPeriods = CAP_LEG.getCapletFloorletPeriods().size();
    for (int i = 1; i < nPeriods; ++i) {
      capExpected = capExpected.combinedWith(
          VOLS_AFTER.parameterSensitivity(PRICER_PERIOD.presentValueSensitivityModelParamsSabr(
              CAP_LEG.getCapletFloorletPeriods().get(i),
              RATES_AFTER,
              VOLS_AFTER).build()));
      floorExpected = floorExpected.combinedWith(
          VOLS_AFTER.parameterSensitivity(PRICER_PERIOD.presentValueSensitivityModelParamsSabr(
              FLOOR_LEG.getCapletFloorletPeriods().get(i),
              RATES_AFTER,
              VOLS_AFTER).build()));
    }
    CurrencyParameterSensitivities capSensiComputed = VOLS_AFTER.parameterSensitivity(capComputed.build());
    CurrencyParameterSensitivities floorSensiComputed = VOLS_AFTER.parameterSensitivity(floorComputed.build());
    assertThat(capSensiComputed.equalWithTolerance(capExpected, TOL * NOTIONAL_VALUE)).isTrue();
    assertThat(floorSensiComputed.equalWithTolerance(floorExpected, TOL * NOTIONAL_VALUE)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currentCash() {
    CurrencyAmount capComputed = PRICER.currentCash(CAP_LEG, RATES, VOLS);
    CurrencyAmount floorComputed = PRICER.currentCash(FLOOR_LEG, RATES, VOLS);
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isEqualTo(0d);
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isEqualTo(0d);
  }

  @Test
  public void test_currentCash_pay() {
    CurrencyAmount capComputed = PRICER.currentCash(CAP_LEG, RATES_PAY, VOLS_PAY);
    CurrencyAmount floorComputed = PRICER.currentCash(FLOOR_LEG, RATES_PAY, VOLS_PAY);
    double capExpected = 0d;
    OvernightInArrearsCapletFloorletPeriod period = FLOOR_LEG.getCapletFloorletPeriods().get(1);
    double floorExpected = PRICER_PERIOD.presentValue(period, RATES_PAY, VOLS_PAY).getAmount();
    assertThat(capComputed.getCurrency()).isEqualTo(EUR);
    assertThat(capComputed.getAmount()).isEqualTo(capExpected);
    assertThat(floorComputed.getCurrency()).isEqualTo(EUR);
    assertThat(floorComputed.getAmount()).isCloseTo(floorExpected, offset(TOL * NOTIONAL_VALUE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_impliedVolatility() {
    Map<OvernightInArrearsCapletFloorletPeriod, Double> computed =
        PRICER.impliedVolatilities(CAP_LEG, RATES, VOLS).getAmounts();
    Map<OvernightInArrearsCapletFloorletPeriod, Double> expected = MapStream.of(CAP_LEG.getCapletFloorletPeriods())
        .mapValues(caplet -> PRICER_PERIOD.impliedVolatility(caplet, RATES, VOLS))
        .toMap();
    assertThat(computed).isEqualTo(expected);
    computed.forEach((caplet, vol) -> assertThat(vol).isCloseTo(expected.get(caplet), offset(TOL)));
  }

  @Test
  public void test_impliedVolatility_onFix() {
    Map<OvernightInArrearsCapletFloorletPeriod, Double> computed =
        PRICER.impliedVolatilities(CAP_LEG, RATES_PAY, VOLS_PAY).getAmounts();
    Map<OvernightInArrearsCapletFloorletPeriod, Double> expected = MapStream.of(CAP_LEG.getCapletFloorletPeriods())
        .filterKeys(caplet -> VOLS_PAY.relativeTime(caplet.getEndDate().atStartOfDay(ZoneOffset.UTC)) >= 0)
        .mapValues(caplet -> PRICER_PERIOD.impliedVolatility(caplet, RATES_PAY, VOLS_PAY))
        .toMap();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_impliedVolatility_afterFix() {
    Map<OvernightInArrearsCapletFloorletPeriod, Double> computed =
        PRICER.impliedVolatilities(CAP_LEG, RATES_AFTER, VOLS_AFTER).getAmounts();
    Map<OvernightInArrearsCapletFloorletPeriod, Double> expected = MapStream.of(CAP_LEG.getCapletFloorletPeriods())
        .filterKeys(caplet -> VOLS_AFTER.relativeTime(caplet.getEndDate().atStartOfDay(ZoneOffset.UTC)) >= 0)
        .mapValues(caplet -> PRICER_PERIOD.impliedVolatility(caplet, RATES_AFTER, VOLS_AFTER))
        .toMap();
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forwardRate() {
    Map<OvernightInArrearsCapletFloorletPeriod, Double> computed = PRICER.forwardRates(CAP_LEG, RATES).getAmounts();
    Map<OvernightInArrearsCapletFloorletPeriod, Double> expected = MapStream.of(CAP_LEG.getCapletFloorletPeriods())
        .mapValues(caplet -> PRICER_PERIOD.forwardRate(caplet, RATES))
        .toMap();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_forwardRate_onFix() {
    Map<OvernightInArrearsCapletFloorletPeriod, Double> computed = PRICER.forwardRates(CAP_LEG, RATES_PAY).getAmounts();
    Map<OvernightInArrearsCapletFloorletPeriod, Double> expected = MapStream.of(CAP_LEG.getCapletFloorletPeriods())
        .filterKeys(caplet -> !RATES_PAY.getValuationDate().isAfter(caplet.getEndDate()))
        .mapValues(caplet -> PRICER_PERIOD.forwardRate(caplet, RATES_PAY))
        .toMap();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_forwardRate_afterFix() {
    Map<OvernightInArrearsCapletFloorletPeriod, Double> computed =
        PRICER.forwardRates(CAP_LEG, RATES_AFTER).getAmounts();
    Map<OvernightInArrearsCapletFloorletPeriod, Double> expected = MapStream.of(CAP_LEG.getCapletFloorletPeriods())
        .filterKeys(caplet -> !RATES_AFTER.getValuationDate().isAfter(caplet.getEndDate()))
        .mapValues(caplet -> PRICER_PERIOD.forwardRate(caplet, RATES_AFTER))
        .toMap();
    assertThat(computed).isEqualTo(expected);
  }

}
