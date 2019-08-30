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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloor;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;

/**
 * Test {@link SabrIborCapFloorProductPricer}.
 */
public class SabrIborCapFloorProductPricerTest {

  private static final double NOTIONAL_VALUE = 1.0e6;
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE);
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2020, 10, 21);
  private static final double STRIKE_VALUE = 0.0105;
  private static final ValueSchedule STRIKE = ValueSchedule.of(STRIKE_VALUE);
  private static final ResolvedIborCapFloorLeg CAP_LEG =
      IborCapFloorDataSet.createCapFloorLeg(EUR_EURIBOR_3M, START, END, STRIKE, NOTIONAL, CALL, RECEIVE);
  private static final ResolvedSwapLeg PAY_LEG =
      IborCapFloorDataSet.createFixedPayLeg(EUR_EURIBOR_3M, START, END, 0.0015, NOTIONAL_VALUE, PAY);
  private static final ResolvedIborCapFloor CAP_TWO_LEGS = ResolvedIborCapFloor.of(CAP_LEG, PAY_LEG);
  private static final ResolvedIborCapFloor CAP_ONE_LEG = ResolvedIborCapFloor.of(CAP_LEG);

  // valuation before start
  private static final ZonedDateTime VALUATION = dateUtc(2015, 8, 20);
  private static final ImmutableRatesProvider RATES = IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
      VALUATION.toLocalDate(), EUR_EURIBOR_3M, LocalDateDoubleTimeSeries.empty());
  private static final SabrIborCapletFloorletVolatilities VOLS = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(VALUATION, EUR_EURIBOR_3M);
  // valuation at payment of 1st period
  private static final double OBS_INDEX_1 = 0.012;
  private static final double OBS_INDEX_2 = 0.0125;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2015, 10, 19), OBS_INDEX_1)
      .put(date(2016, 1, 19), OBS_INDEX_2)
      .build();
  private static final ZonedDateTime VALUATION_PAY = dateUtc(2016, 1, 21);
  private static final ImmutableRatesProvider RATES_PAY =
      IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(VALUATION_PAY.toLocalDate(), EUR_EURIBOR_3M, TIME_SERIES);
  private static final SabrIborCapletFloorletVolatilities VOLS_PAY =
      IborCapletFloorletSabrRateVolatilityDataSet.getVolatilities(VALUATION_PAY, EUR_EURIBOR_3M);

  private static final double TOL = 1.0e-13;
  private static final SabrIborCapFloorProductPricer PRICER = SabrIborCapFloorProductPricer.DEFAULT;
  private static final SabrIborCapFloorLegPricer PRICER_CAP_LEG = SabrIborCapFloorLegPricer.DEFAULT;
  private static final DiscountingSwapLegPricer PRICER_PAY_LEG = DiscountingSwapLegPricer.DEFAULT;

  @Test
  public void test_presentValue() {
    MultiCurrencyAmount computed1 = PRICER.presentValue(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.presentValue(CAP_TWO_LEGS, RATES, VOLS);
    CurrencyAmount cap = PRICER_CAP_LEG.presentValue(CAP_LEG, RATES, VOLS);
    CurrencyAmount pay = PRICER_PAY_LEG.presentValue(PAY_LEG, RATES);
    assertThat(computed1).isEqualTo(MultiCurrencyAmount.of(cap));
    assertThat(computed2).isEqualTo(MultiCurrencyAmount.of(cap.plus(pay)));
  }

  @Test
  public void test_presentValueDelta() {
    MultiCurrencyAmount computed1 = PRICER.presentValueDelta(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.presentValueDelta(CAP_TWO_LEGS, RATES, VOLS);
    CurrencyAmount cap = PRICER_CAP_LEG.presentValueDelta(CAP_LEG, RATES, VOLS);
    assertThat(computed1).isEqualTo(MultiCurrencyAmount.of(cap));
    assertThat(computed2).isEqualTo(MultiCurrencyAmount.of(cap));
  }

  @Test
  public void test_presentValueGamma() {
    MultiCurrencyAmount computed1 = PRICER.presentValueGamma(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.presentValueGamma(CAP_TWO_LEGS, RATES, VOLS);
    CurrencyAmount cap = PRICER_CAP_LEG.presentValueGamma(CAP_LEG, RATES, VOLS);
    assertThat(computed1).isEqualTo(MultiCurrencyAmount.of(cap));
    assertThat(computed2).isEqualTo(MultiCurrencyAmount.of(cap));
  }

  @Test
  public void test_presentValueTheta() {
    MultiCurrencyAmount computed1 = PRICER.presentValueTheta(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.presentValueTheta(CAP_TWO_LEGS, RATES, VOLS);
    CurrencyAmount cap = PRICER_CAP_LEG.presentValueTheta(CAP_LEG, RATES, VOLS);
    assertThat(computed1).isEqualTo(MultiCurrencyAmount.of(cap));
    assertThat(computed2).isEqualTo(MultiCurrencyAmount.of(cap));
  }

  @Test
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder computed1 = PRICER.presentValueSensitivityRates(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder computed2 = PRICER.presentValueSensitivityRates(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder cap = PRICER_CAP_LEG.presentValueSensitivityRates(CAP_LEG, RATES, VOLS);
    PointSensitivityBuilder pay = PRICER_PAY_LEG.presentValueSensitivity(PAY_LEG, RATES);
    assertThat(computed1).isEqualTo(cap);
    assertThat(computed2).isEqualTo(cap.combinedWith(pay));
  }

  @Test
  public void test_presentValueSensitivityRatesStickyModel() {
    PointSensitivityBuilder computed1 = PRICER.presentValueSensitivityRatesStickyModel(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder computed2 = PRICER.presentValueSensitivityRatesStickyModel(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder cap = PRICER_CAP_LEG.presentValueSensitivityRatesStickyModel(CAP_LEG, RATES, VOLS);
    PointSensitivityBuilder pay = PRICER_PAY_LEG.presentValueSensitivity(PAY_LEG, RATES);
    assertThat(computed1).isEqualTo(cap);
    assertThat(computed2).isEqualTo(cap.combinedWith(pay));
  }

  @Test
  public void test_presentValueSensitivityVolatility() {
    PointSensitivityBuilder computed1 = PRICER.presentValueSensitivityModelParamsVolatility(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder computed2 = PRICER.presentValueSensitivityModelParamsVolatility(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder cap = PRICER_CAP_LEG.presentValueSensitivityModelParamsVolatility(CAP_LEG, RATES, VOLS);
    assertThat(computed1).isEqualTo(cap);
    assertThat(computed2).isEqualTo(cap);
  }

  @Test
  public void test_presentValueSensitivityModelParamsSabr() {
    PointSensitivityBuilder computed1 = PRICER.presentValueSensitivityModelParamsSabr(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder computed2 = PRICER.presentValueSensitivityModelParamsSabr(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder cap = PRICER_CAP_LEG.presentValueSensitivityModelParamsSabr(CAP_LEG, RATES, VOLS);
    assertThat(computed1).isEqualTo(cap);
    assertThat(computed2).isEqualTo(cap);
  }

  @Test
  public void test_currencyExposure() {
    MultiCurrencyAmount computed1 = PRICER.currencyExposure(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.currencyExposure(CAP_TWO_LEGS, RATES, VOLS);
    MultiCurrencyAmount pv1 = PRICER.presentValue(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount pv2 = PRICER.presentValue(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder point1 = PRICER.presentValueSensitivityRates(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder point2 = PRICER.presentValueSensitivityRates(CAP_TWO_LEGS, RATES, VOLS);
    MultiCurrencyAmount expected1 = RATES.currencyExposure(point1.build()).plus(pv1);
    MultiCurrencyAmount expected2 = RATES.currencyExposure(point2.build()).plus(pv2);
    assertThat(computed1.getAmount(EUR).getAmount()).isCloseTo(expected1.getAmount(EUR).getAmount(), offset(NOTIONAL_VALUE * TOL));
    assertThat(computed2.getAmount(EUR).getAmount()).isCloseTo(expected2.getAmount(EUR).getAmount(), offset(NOTIONAL_VALUE * TOL));
  }

  @Test
  public void test_currentCash() {
    MultiCurrencyAmount cc1 = PRICER.currentCash(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount cc2 = PRICER.currentCash(CAP_TWO_LEGS, RATES, VOLS);
    assertThat(cc1).isEqualTo(MultiCurrencyAmount.of(CurrencyAmount.zero(EUR)));
    assertThat(cc2).isEqualTo(MultiCurrencyAmount.of(CurrencyAmount.zero(EUR)));
  }

  @Test
  public void test_currentCash_onPay() {
    MultiCurrencyAmount cc1 = PRICER.currentCash(CAP_ONE_LEG, RATES_PAY, VOLS_PAY);
    MultiCurrencyAmount cc2 = PRICER.currentCash(CAP_TWO_LEGS, RATES_PAY, VOLS_PAY);
    CurrencyAmount ccCap = PRICER_CAP_LEG.currentCash(CAP_LEG, RATES_PAY, VOLS_PAY);
    CurrencyAmount ccPay = PRICER_PAY_LEG.currentCash(PAY_LEG, RATES_PAY);
    assertThat(cc1).isEqualTo(MultiCurrencyAmount.of(ccCap));
    assertThat(cc2).isEqualTo(MultiCurrencyAmount.of(ccCap).plus(ccPay));
  }

}
