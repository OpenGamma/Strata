/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.CompoundedRateType.CONTINUOUS;
import static com.opengamma.strata.pricer.CompoundedRateType.PERIODIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;

/**
 * Test {@link SimpleDiscountFactors}.
 */
public class SimpleDiscountFactorsTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);
  private static final LocalDate DATE_BEFORE = date(2015, 5, 6);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final CurveMetadata METADATA = Curves.discountFactors(NAME, ACT_365F);

  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(2, 3), INTERPOLATOR);

  private static final double SPREAD = 0.05;
  private static final double TOL = 1.0e-12;
  private static final double TOL_FD = 1.0e-8;
  private static final double EPS = 1.0e-6;
  private static final double TOL_SMALL_YF = 1.0e-9;

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
    assertThat(test.getCurve()).isEqualTo(CURVE);
    assertThat(test.getParameterCount()).isEqualTo(CURVE.getParameterCount());
    assertThat(test.getParameter(0)).isEqualTo(CURVE.getParameter(0));
    assertThat(test.getParameterMetadata(0)).isEqualTo(CURVE.getParameterMetadata(0));
    assertThat(test.withParameter(0, 1d).getCurve()).isEqualTo(CURVE.withParameter(0, 1d));
    assertThat(test.withPerturbation((i, v, m) -> v + 1d).getCurve()).isEqualTo(CURVE.withPerturbation((i, v, m) -> v + 1d));
    assertThat(test.findData(CURVE.getName())).isEqualTo(Optional.of(CURVE));
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
  }

  @Test
  public void test_of_badCurve() {
    InterpolatedNodalCurve notYearFraction = InterpolatedNodalCurve.of(
        Curves.prices(NAME), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    InterpolatedNodalCurve notDiscountFactor = InterpolatedNodalCurve.of(
        Curves.zeroRates(NAME, ACT_365F), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    CurveMetadata noDayCountMetadata = DefaultCurveMetadata.builder()
        .curveName(NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .build();
    InterpolatedNodalCurve notDayCount = InterpolatedNodalCurve.of(
        noDayCountMetadata, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SimpleDiscountFactors.of(GBP, DATE_VAL, notYearFraction));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SimpleDiscountFactors.of(GBP, DATE_VAL, notDiscountFactor));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SimpleDiscountFactors.of(GBP, DATE_VAL, notDayCount));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_discountFactor() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double expected = CURVE.yValue(relativeYearFraction);
    assertThat(test.discountFactor(DATE_AFTER)).isEqualTo(expected);
  }

  @Test
  public void test_discountFactor_beforeValDate() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    assertThat(test.discountFactor(DATE_BEFORE)).isEqualTo(1d);
  }
  
  @Test
  public void test_discountFactorTimeDerivative() {
    DiscountFactors test = DiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double expectedP = test.discountFactor(relativeYearFraction + EPS);
    double expectedM = test.discountFactor(relativeYearFraction - EPS);
    assertThat(test.discountFactorTimeDerivative(relativeYearFraction)).isCloseTo((expectedP - expectedM) / (2 * EPS), offset(TOL_FD));
  }

  @Test
  public void test_discountFactorTimeDerivative_beforeValDate() {
    DiscountFactors test = DiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_BEFORE);
    assertThat(test.discountFactorTimeDerivative(relativeYearFraction)).isEqualTo(0d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_zeroRate() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double discountFactor = test.discountFactor(DATE_AFTER);
    double zeroRate = test.zeroRate(DATE_AFTER);
    assertThat(Math.exp(-zeroRate * relativeYearFraction)).isEqualTo(discountFactor);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_discountFactor_withSpread_continuous() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double expected = CURVE.yValue(relativeYearFraction) * Math.exp(-SPREAD * relativeYearFraction);
    assertThat(test.discountFactorWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0)).isCloseTo(expected, offset(TOL));
  }

  @Test
  public void test_discountFactor_withSpread_continuous_beforeValDate() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_BEFORE);
    double expected = CURVE.yValue(relativeYearFraction) * Math.exp(-SPREAD * relativeYearFraction);
    assertThat(test.discountFactorWithSpread(DATE_BEFORE, SPREAD, CONTINUOUS, 0)).isCloseTo(expected, offset(TOL));
  }

  @Test
  public void test_discountFactor_withSpread_periodic() {
    int periodPerYear = 4;
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double discountFactorBase = test.discountFactor(DATE_AFTER);
    double rate = (Math.pow(discountFactorBase, -1d / periodPerYear / relativeYearFraction) - 1d) * periodPerYear;
    double expected = discountFactorFromPeriodicallyCompoundedRate(rate + SPREAD, periodPerYear, relativeYearFraction);
    assertThat(test.discountFactorWithSpread(DATE_AFTER, SPREAD, PERIODIC, periodPerYear)).isCloseTo(expected, offset(TOL));
  }

  @Test
  public void test_discountFactor_withSpread_periodic_beforeValDate() {
    int periodPerYear = 4;
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_BEFORE);
    double discountFactorBase = test.discountFactor(DATE_BEFORE);
    double rate = (Math.pow(discountFactorBase, -1d / periodPerYear / relativeYearFraction) - 1d) * periodPerYear;
    double expected = discountFactorFromPeriodicallyCompoundedRate(rate + SPREAD, periodPerYear, relativeYearFraction);
    assertThat(test.discountFactorWithSpread(DATE_BEFORE, SPREAD, PERIODIC, periodPerYear)).isCloseTo(expected, offset(TOL));
  }

  @Test
  public void test_discountFactor_withSpread_smallYearFraction() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    assertThat(test.discountFactorWithSpread(DATE_VAL, SPREAD, PERIODIC, 2))
        .isCloseTo(1d, offset(TOL_SMALL_YF));
  }

  private double discountFactorFromPeriodicallyCompoundedRate(double rate, double periodPerYear, double time) {
    return Math.pow(1d + rate / periodPerYear, -periodPerYear * time);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_zeroRatePointSensitivity() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = CURVE.yValue(relativeYearFraction);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, -df * relativeYearFraction);
    assertThat(test.zeroRatePointSensitivity(DATE_AFTER)).isEqualTo(expected);
  }

  @Test
  public void test_zeroRatePointSensitivity_beforeValDate() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_BEFORE);
    double df = CURVE.yValue(relativeYearFraction);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, -df * relativeYearFraction);
    assertThat(test.zeroRatePointSensitivity(DATE_BEFORE)).isEqualTo(expected);
  }

  @Test
  public void test_zeroRatePointSensitivity_sensitivityCurrency() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = CURVE.yValue(relativeYearFraction);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, USD, -df * relativeYearFraction);
    assertThat(test.zeroRatePointSensitivity(DATE_AFTER, USD)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_zeroRatePointSensitivityWithSpread_continuous() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = CURVE.yValue(relativeYearFraction) * Math.exp(-SPREAD * relativeYearFraction);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, -df * relativeYearFraction);
    assertThat(test.zeroRatePointSensitivityWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0)).isEqualTo(expected);
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_continuous_beforeValDate() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_BEFORE);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, 0d);
    assertThat(test.zeroRatePointSensitivityWithSpread(DATE_BEFORE, SPREAD, CONTINUOUS, 0).build()
        .equalWithTolerance(expected.build(), TOL_SMALL_YF));
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_continuous() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = CURVE.yValue(relativeYearFraction) * Math.exp(-SPREAD * relativeYearFraction);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, USD, -df * relativeYearFraction);
    assertThat(test.zeroRatePointSensitivityWithSpread(DATE_AFTER, USD, SPREAD, CONTINUOUS, 1)).isEqualTo(expected);
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_periodic() {
    int periodPerYear = 4;
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = CURVE.yValue(relativeYearFraction);
    double discountFactorUp = df * Math.exp(-EPS * relativeYearFraction);
    double discountFactorDw = df * Math.exp(EPS * relativeYearFraction);
    double rateUp = (Math.pow(discountFactorUp, -1d / periodPerYear / relativeYearFraction) - 1d) * periodPerYear;
    double rateDw = (Math.pow(discountFactorDw, -1d / periodPerYear / relativeYearFraction) - 1d) * periodPerYear;
    double expectedValue = 0.5 / EPS * (
        discountFactorFromPeriodicallyCompoundedRate(rateUp + SPREAD, periodPerYear, relativeYearFraction) -
        discountFactorFromPeriodicallyCompoundedRate(rateDw + SPREAD, periodPerYear, relativeYearFraction));
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(
        DATE_AFTER, SPREAD, PERIODIC, periodPerYear);
    assertThat(computed.getSensitivity()).isCloseTo(expectedValue, offset(EPS));
    assertThat(computed.getCurrency()).isEqualTo(GBP);
    assertThat(computed.getCurveCurrency()).isEqualTo(GBP);
    assertThat(computed.getYearFraction()).isEqualTo(relativeYearFraction);
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_periodic_beforeValDate() {
    int periodPerYear = 4;
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_BEFORE);
    double df = CURVE.yValue(relativeYearFraction);
    double discountFactorUp = df * Math.exp(-EPS * relativeYearFraction);
    double discountFactorDw = df * Math.exp(EPS * relativeYearFraction);
    double rateUp = (Math.pow(discountFactorUp, -1d / periodPerYear / relativeYearFraction) - 1d) * periodPerYear;
    double rateDw = (Math.pow(discountFactorDw, -1d / periodPerYear / relativeYearFraction) - 1d) * periodPerYear;
    double expectedValue = 0.5 / EPS * (
        discountFactorFromPeriodicallyCompoundedRate(rateUp + SPREAD, periodPerYear, relativeYearFraction) -
            discountFactorFromPeriodicallyCompoundedRate(rateDw + SPREAD, periodPerYear, relativeYearFraction));
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(
        DATE_BEFORE, SPREAD, PERIODIC, periodPerYear);
    assertThat(computed.getSensitivity()).isCloseTo(expectedValue, offset(EPS));
    assertThat(computed.getCurrency()).isEqualTo(GBP);
    assertThat(computed.getCurveCurrency()).isEqualTo(GBP);
    assertThat(computed.getYearFraction()).isEqualTo(relativeYearFraction);
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_periodic() {
    int periodPerYear = 4;
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = CURVE.yValue(relativeYearFraction);
    double discountFactorUp = df * Math.exp(-EPS * relativeYearFraction);
    double discountFactorDw = df * Math.exp(EPS * relativeYearFraction);
    double rateUp = (Math.pow(discountFactorUp, -1d / periodPerYear / relativeYearFraction) - 1d) * periodPerYear;
    double rateDw = (Math.pow(discountFactorDw, -1d / periodPerYear / relativeYearFraction) - 1d) * periodPerYear;
    double expectedValue = 0.5 / EPS * (
        discountFactorFromPeriodicallyCompoundedRate(rateUp + SPREAD, periodPerYear, relativeYearFraction) -
        discountFactorFromPeriodicallyCompoundedRate(rateDw + SPREAD, periodPerYear, relativeYearFraction));
    ZeroRateSensitivity computed = test
        .zeroRatePointSensitivityWithSpread(DATE_AFTER, USD, SPREAD, PERIODIC, periodPerYear);
    assertThat(computed.getSensitivity()).isCloseTo(expectedValue, offset(EPS));
    assertThat(computed.getCurrency()).isEqualTo(USD);
    assertThat(computed.getCurveCurrency()).isEqualTo(GBP);
    assertThat(computed.getYearFraction()).isEqualTo(relativeYearFraction);
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_smallYearFraction() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, 0d, 0d);
    assertThat(test.zeroRatePointSensitivityWithSpread(DATE_VAL, SPREAD, CONTINUOUS, 0).build()
        .equalWithTolerance(expected.build(), TOL_SMALL_YF));
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_smallYearFraction() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, 0d, USD, 0d);
    assertThat(test.zeroRatePointSensitivityWithSpread(DATE_VAL, USD, SPREAD, PERIODIC, 2).build()
        .equalWithTolerance(expected.build(), TOL_SMALL_YF));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyParameterSensitivity() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity sens = test.zeroRatePointSensitivity(DATE_AFTER);

    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double discountFactor = CURVE.yValue(relativeYearFraction);
    CurrencyParameterSensitivities expected = CurrencyParameterSensitivities.of(
        CURVE.yValueParameterSensitivity(relativeYearFraction)
            .multipliedBy(-1d / discountFactor / relativeYearFraction)
            .multipliedBy(sens.getCurrency(), sens.getSensitivity()));
    assertThat(test.parameterSensitivity(sens)).isEqualTo(expected);
  }

  @Test
  public void test_currencyParameterSensitivity_beforeValDate() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity sens = test.zeroRatePointSensitivity(DATE_BEFORE);

    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_BEFORE);
    double discountFactor = CURVE.yValue(relativeYearFraction);
    CurrencyParameterSensitivities expected = CurrencyParameterSensitivities.of(
        CURVE.yValueParameterSensitivity(relativeYearFraction)
            .multipliedBy(-1d / discountFactor / relativeYearFraction)
            .multipliedBy(sens.getCurrency(), sens.getSensitivity()));
    assertThat(test.parameterSensitivity(sens)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyParameterSensitivity_val_date() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity sens = test.zeroRatePointSensitivity(DATE_VAL);

    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_VAL);
    double discountFactor = CURVE.yValue(relativeYearFraction);
    CurrencyParameterSensitivities expected = CurrencyParameterSensitivities.of(
        CurrencyParameterSensitivity.of(NAME, GBP, DoubleArray.of(1d, 0d)));
    CurrencyParameterSensitivities tested = test.parameterSensitivity(sens);
    assertThat(test.parameterSensitivity(sens).equalWithTolerance(expected, TOL));
  }

  //-------------------------------------------------------------------------
  // proper end-to-end FD tests are elsewhere
  @Test
  public void test_parameterSensitivity() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity point = ZeroRateSensitivity.of(GBP, 1d, 1d);
    assertThat(test.parameterSensitivity(point).size()).isEqualTo(1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createParameterSensitivity() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15);
    CurrencyParameterSensitivities sens = test.createParameterSensitivity(USD, sensitivities);
    assertThat(sens.getSensitivities().get(0)).isEqualTo(CURVE.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurve() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE).withCurve(CURVE2);
    assertThat(test.getCurve()).isEqualTo(CURVE2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SimpleDiscountFactors test = SimpleDiscountFactors.of(GBP, DATE_VAL, CURVE);
    coverImmutableBean(test);
    SimpleDiscountFactors test2 = SimpleDiscountFactors.of(USD, DATE_VAL.plusDays(1), CURVE2);
    coverBeanEquals(test, test2);
  }

}
