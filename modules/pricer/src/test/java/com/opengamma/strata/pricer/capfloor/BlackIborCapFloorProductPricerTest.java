/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.capfloor.IborCapletFloorletDataSet;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.product.capfloor.IborCapFloor;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Test {@link BlackIborCapFloorProductPricer}.
 */
@Test
public class BlackIborCapFloorProductPricerTest {

  private static final double NOTIONAL_VALUE = 1.0e6;
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE);
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2020, 10, 21);
  private static final double STRIKE_VALUE = 0.0105;
  private static final ValueSchedule STRIKE = ValueSchedule.of(STRIKE_VALUE);
  private static final IborCapFloorLeg CAP_LEG =
      IborCapFloorDataSet.createCapFloorLeg(EUR_EURIBOR_3M, START, END, STRIKE, NOTIONAL, CALL, RECEIVE);
  private static final SwapLeg PAY_LEG =
      IborCapFloorDataSet.createFixedPayLeg(EUR_EURIBOR_3M, START, END, 0.0015, NOTIONAL_VALUE, PAY);
  private static final IborCapFloor CAP_TWO_LEGS = IborCapFloor.of(CAP_LEG, PAY_LEG);
  private static final IborCapFloor CAP_ONE_LEG = IborCapFloor.of(CAP_LEG);

  // valuation before start
  private static final ZonedDateTime VALUATION = dateUtc(2015, 8, 20);
  private static final ImmutableRatesProvider RATES =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION.toLocalDate());
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities VOLS = IborCapletFloorletDataSet
      .createBlackVolatilitiesProvider(VALUATION, EUR_EURIBOR_3M);
  // valuation at payment of 1st period
  private static final double OBS_INDEX_1 = 0.012;
  private static final double OBS_INDEX_2 = 0.0125;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2015, 10, 19), OBS_INDEX_1)
      .put(date(2016, 1, 19), OBS_INDEX_2)
      .build();
  private static final ZonedDateTime VALUATION_PAY = dateUtc(2016, 1, 21);
  private static final ImmutableRatesProvider RATES_PAY =
      IborCapletFloorletDataSet.createRatesProvider(VALUATION_PAY.toLocalDate(), EUR_EURIBOR_3M, TIME_SERIES);
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities VOLS_PAY = IborCapletFloorletDataSet
      .createBlackVolatilitiesProvider(VALUATION_PAY, EUR_EURIBOR_3M);

  private static final double TOL = 1.0e-13;
  private static final BlackIborCapFloorProductPricer PRICER = BlackIborCapFloorProductPricer.DEFAULT;
  private static final BlackIborCapFloorLegPricer PRICER_CAP_LEG = BlackIborCapFloorLegPricer.DEFAULT;
  private static final DiscountingSwapLegPricer PRICER_PAY_LEG = DiscountingSwapLegPricer.DEFAULT;

  public void test_presentValue() {
    MultiCurrencyAmount computed1 = PRICER.presentValue(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.presentValue(CAP_TWO_LEGS, RATES, VOLS);
    CurrencyAmount cap = PRICER_CAP_LEG.presentValue(CAP_LEG.expand(), RATES, VOLS);
    CurrencyAmount pay = PRICER_PAY_LEG.presentValue(PAY_LEG, RATES);
    assertEquals(computed1, MultiCurrencyAmount.of(cap));
    assertEquals(computed2, MultiCurrencyAmount.of(cap.plus(pay)));
  }

  public void test_presentValueDelta() {
    MultiCurrencyAmount computed1 = PRICER.presentValueDelta(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.presentValueDelta(CAP_TWO_LEGS, RATES, VOLS);
    CurrencyAmount cap = PRICER_CAP_LEG.presentValueDelta(CAP_LEG.expand(), RATES, VOLS);
    assertEquals(computed1, MultiCurrencyAmount.of(cap));
    assertEquals(computed2, MultiCurrencyAmount.of(cap));
  }

  public void test_presentValueGamma() {
    MultiCurrencyAmount computed1 = PRICER.presentValueGamma(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.presentValueGamma(CAP_TWO_LEGS, RATES, VOLS);
    CurrencyAmount cap = PRICER_CAP_LEG.presentValueGamma(CAP_LEG.expand(), RATES, VOLS);
    assertEquals(computed1, MultiCurrencyAmount.of(cap));
    assertEquals(computed2, MultiCurrencyAmount.of(cap));
  }

  public void test_presentValueTheta() {
    MultiCurrencyAmount computed1 = PRICER.presentValueTheta(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.presentValueTheta(CAP_TWO_LEGS, RATES, VOLS);
    CurrencyAmount cap = PRICER_CAP_LEG.presentValueTheta(CAP_LEG.expand(), RATES, VOLS);
    assertEquals(computed1, MultiCurrencyAmount.of(cap));
    assertEquals(computed2, MultiCurrencyAmount.of(cap));
  }

  public void test_presentValueSensitivity() {
    PointSensitivityBuilder computed1 = PRICER.presentValueSensitivity(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder computed2 = PRICER.presentValueSensitivity(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder cap = PRICER_CAP_LEG.presentValueSensitivity(CAP_LEG.expand(), RATES, VOLS);
    PointSensitivityBuilder pay = PRICER_PAY_LEG.presentValueSensitivity(PAY_LEG, RATES);
    assertEquals(computed1, cap);
    assertEquals(computed2, cap.combinedWith(pay));
  }

  public void test_presentValueSensitivityVolatility() {
    PointSensitivityBuilder computed1 = PRICER.presentValueSensitivityVolatility(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder computed2 = PRICER.presentValueSensitivityVolatility(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder cap = PRICER_CAP_LEG.presentValueSensitivityVolatility(CAP_LEG.expand(), RATES, VOLS);
    assertEquals(computed1, cap);
    assertEquals(computed2, cap);
  }

  public void test_currencyExposure() {
    MultiCurrencyAmount computed1 = PRICER.currencyExposure(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount computed2 = PRICER.currencyExposure(CAP_TWO_LEGS, RATES, VOLS);
    MultiCurrencyAmount pv1 = PRICER.presentValue(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount pv2 = PRICER.presentValue(CAP_TWO_LEGS, RATES, VOLS);
    PointSensitivityBuilder point1 = PRICER.presentValueSensitivity(CAP_ONE_LEG, RATES, VOLS);
    PointSensitivityBuilder point2 = PRICER.presentValueSensitivity(CAP_TWO_LEGS, RATES, VOLS);
    MultiCurrencyAmount expected1 = RATES.currencyExposure(point1.build()).plus(pv1);
    MultiCurrencyAmount expected2 = RATES.currencyExposure(point2.build()).plus(pv2);
    assertEquals(computed1.getAmount(EUR).getAmount(), expected1.getAmount(EUR).getAmount(), NOTIONAL_VALUE * TOL);
    assertEquals(computed2.getAmount(EUR).getAmount(), expected2.getAmount(EUR).getAmount(), NOTIONAL_VALUE * TOL);
  }

  public void test_currentCash() {
    MultiCurrencyAmount cc1 = PRICER.currentCash(CAP_ONE_LEG, RATES, VOLS);
    MultiCurrencyAmount cc2 = PRICER.currentCash(CAP_TWO_LEGS, RATES, VOLS);
    assertEquals(cc1, MultiCurrencyAmount.of(CurrencyAmount.zero(EUR)));
    assertEquals(cc2, MultiCurrencyAmount.of(CurrencyAmount.zero(EUR)));
  }

  public void test_currentCash_onPay() {
    MultiCurrencyAmount cc1 = PRICER.currentCash(CAP_ONE_LEG, RATES_PAY, VOLS_PAY);
    MultiCurrencyAmount cc2 = PRICER.currentCash(CAP_TWO_LEGS, RATES_PAY, VOLS_PAY);
    CurrencyAmount ccCap = PRICER_CAP_LEG.currentCash(CAP_LEG.expand(), RATES_PAY, VOLS_PAY);
    CurrencyAmount ccPay = PRICER_PAY_LEG.currentCash(PAY_LEG, RATES_PAY);
    assertEquals(cc1, MultiCurrencyAmount.of(ccCap));
    assertEquals(cc2, MultiCurrencyAmount.of(ccCap).plus(ccPay));
  }

}
