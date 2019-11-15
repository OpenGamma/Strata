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
import com.opengamma.strata.market.curve.CurveInfoType;
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
 * Test {@link ZeroRatePeriodicDiscountFactors}.
 */
public class ZeroRatePeriodicDiscountFactorsTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_AFTER = date(2016, 7, 21);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final int CMP_PERIOD = 2;
  private static final CurveMetadata META_ZERO_PERIODIC = DefaultCurveMetadata.builder()
      .curveName(NAME)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .dayCount(ACT_365F)
      .addInfo(CurveInfoType.COMPOUNDING_PER_YEAR, CMP_PERIOD)
      .build();

  private static final DoubleArray X = DoubleArray.of(0, 5, 10);
  private static final DoubleArray Y = DoubleArray.of(0.0100, 0.0200, 0.0150);
  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(META_ZERO_PERIODIC, X, Y, INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(META_ZERO_PERIODIC, DoubleArray.of(0, 10), DoubleArray.of(2, 3), INTERPOLATOR);

  private static final double SPREAD = 0.05;
  private static final double TOLERANCE_DF = 1.0e-12;
  private static final double TOLERANCE_DELTA = 1.0e-10;
  private static final double TOLERANCE_DELTA_FD = 1.0e-8;
  private static final double EPS = 1.0e-6;

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
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
    InterpolatedNodalCurve notZeroRate = InterpolatedNodalCurve.of(
        Curves.discountFactors(NAME, ACT_365F), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    CurveMetadata noDayCountMetadata = DefaultCurveMetadata.builder()
        .curveName(NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .addInfo(CurveInfoType.COMPOUNDING_PER_YEAR, 4)
        .build();
    InterpolatedNodalCurve notDayCount = InterpolatedNodalCurve.of(
        noDayCountMetadata, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    CurveMetadata metaNoCompoundPerYear = DefaultCurveMetadata.builder()
        .curveName(NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    InterpolatedNodalCurve notCompoundPerYear = InterpolatedNodalCurve.of(
        metaNoCompoundPerYear, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    CurveMetadata metaNegativeNb = DefaultCurveMetadata.builder()
        .curveName(NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .addInfo(CurveInfoType.COMPOUNDING_PER_YEAR, -1)
        .build();
    InterpolatedNodalCurve curveNegativeNb = InterpolatedNodalCurve.of(
        metaNegativeNb, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notYearFraction));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notZeroRate));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notDayCount));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notCompoundPerYear));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, curveNegativeNb));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_discountFactor() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double expected = Math.pow(1.0d + CURVE.yValue(relativeYearFraction) / CMP_PERIOD,
        -CMP_PERIOD * relativeYearFraction);
    assertThat(test.discountFactor(DATE_AFTER)).isEqualTo(expected);
  }
  
  @Test
  public void test_discountFactorTimeDerivative() {
    DiscountFactors test = DiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double expectedP = test.discountFactor(relativeYearFraction + EPS);
    double expectedM = test.discountFactor(relativeYearFraction - EPS);
    assertThat(test.discountFactorTimeDerivative(relativeYearFraction)).isCloseTo((expectedP - expectedM) / (2 * EPS), offset(TOLERANCE_DELTA_FD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_zeroRate() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double discountFactor = test.discountFactor(DATE_AFTER);
    double zeroRate = test.zeroRate(DATE_AFTER);
    assertThat(Math.exp(-zeroRate * relativeYearFraction)).isEqualTo(discountFactor);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_discountFactorWithSpread_continuous() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    double expected = df * Math.exp(-SPREAD * relativeYearFraction);
    assertThat(test.discountFactorWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0)).isCloseTo(expected, offset(TOLERANCE_DF));
  }

  @Test
  public void test_discountFactorWithSpread_periodic() {
    int periodPerYear = 4;
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double discountFactorBase = test.discountFactor(DATE_AFTER);
    double onePlus = Math.pow(discountFactorBase, -1.0d / (periodPerYear * relativeYearFraction));
    double expected = Math.pow(onePlus + SPREAD / periodPerYear, -periodPerYear * relativeYearFraction);
    assertThat(test.discountFactorWithSpread(DATE_AFTER, SPREAD, PERIODIC, periodPerYear)).isCloseTo(expected, offset(TOLERANCE_DF));
  }

  @Test
  public void test_discountFactorWithSpread_smallYearFraction() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    assertThat(test.discountFactorWithSpread(DATE_VAL, SPREAD, PERIODIC, 1)).isCloseTo(1d, offset(TOLERANCE_DF));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_zeroRatePointSensitivity() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, -df * relativeYearFraction);
    assertThat(test.zeroRatePointSensitivity(DATE_AFTER)).isEqualTo(expected);
  }

  @Test
  public void test_zeroRatePointSensitivity_sensitivityCurrency() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, USD, -df * relativeYearFraction);
    assertThat(test.zeroRatePointSensitivity(DATE_AFTER, USD)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_zeroRatePointSensitivityWithSpread_continous() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactorWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, -df * relativeYearFraction);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0);
    assertThat(computed.compareKey(expected) == 0).isTrue();
    assertThat(computed.getSensitivity()).isCloseTo(expected.getSensitivity(), offset(TOLERANCE_DELTA));
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_continous() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactorWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, USD, -df * relativeYearFraction);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, USD, SPREAD, CONTINUOUS, 0);
    assertThat(computed.compareKey(expected) == 0).isTrue();
    assertThat(computed.getSensitivity()).isCloseTo(expected.getSensitivity(), offset(TOLERANCE_DELTA));
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_periodic() {
    int periodPerYear = 4;
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    double z = -1.0 / relativeYearFraction * Math.log(df);
    double shift = 1.0E-6;
    double zP = z + shift;
    double zM = z - shift;
    double dfSP = Math.pow(
        Math.pow(Math.exp(-zP * relativeYearFraction),
            -1.0 / (relativeYearFraction * periodPerYear)) + SPREAD / periodPerYear,
        -relativeYearFraction * periodPerYear);
    double dfSM = Math.pow(
        Math.pow(Math.exp(-zM * relativeYearFraction),
            -1.0 / (relativeYearFraction * periodPerYear)) + SPREAD / periodPerYear,
        -relativeYearFraction * periodPerYear);
    double ddfSdz = (dfSP - dfSM) / (2 * shift);    
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, ddfSdz);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, SPREAD, PERIODIC, periodPerYear);
    assertThat(computed.compareKey(expected) == 0).isTrue();
    assertThat(computed.getSensitivity()).isCloseTo(expected.getSensitivity(), offset(TOLERANCE_DELTA_FD));
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_periodic() {
    int periodPerYear = 4;
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    double z = -1.0 / relativeYearFraction * Math.log(df);
    double shift = 1.0E-6;
    double zP = z + shift;
    double zM = z - shift;
    double dfSP = Math.pow(
        Math.pow(Math.exp(-zP * relativeYearFraction),
            -1.0 / (relativeYearFraction * periodPerYear)) + SPREAD / periodPerYear,
        -relativeYearFraction * periodPerYear);
    double dfSM = Math.pow(
        Math.pow(Math.exp(-zM * relativeYearFraction),
            -1.0 / (relativeYearFraction * periodPerYear)) + SPREAD / periodPerYear,
        -relativeYearFraction * periodPerYear);
    double ddfSdz = (dfSP - dfSM) / (2 * shift);    
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, USD, ddfSdz);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, USD, SPREAD, PERIODIC, periodPerYear);
    assertThat(computed.compareKey(expected) == 0).isTrue();
    assertThat(computed.getSensitivity()).isCloseTo(expected.getSensitivity(), offset(TOLERANCE_DELTA_FD));
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_smallYearFraction() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, 0d, 0.0d);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_VAL, SPREAD, CONTINUOUS, 0);
    assertThat(computed.compareKey(expected) == 0).isTrue();
    assertThat(computed.getSensitivity()).isCloseTo(expected.getSensitivity(), offset(TOLERANCE_DELTA_FD));
  }

  @Test
  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_smallYearFraction() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, 0d, USD, 0.0d);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_VAL, USD, SPREAD, CONTINUOUS, 0);
    assertThat(computed.compareKey(expected) == 0).isTrue();
    assertThat(computed.getSensitivity()).isCloseTo(expected.getSensitivity(), offset(TOLERANCE_DELTA_FD));
  }

  //-------------------------------------------------------------------------
  //-------------------------------------------------------------------------
  @Test
  public void test_parameterSensitivity() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double sensiValue = 25d;
    ZeroRateSensitivity point = test.zeroRatePointSensitivity(DATE_AFTER);
    point = point.multipliedBy(sensiValue);
    CurrencyParameterSensitivities sensiObject = test.parameterSensitivity(point);
    assertThat(sensiObject.size()).isEqualTo(1);
    CurrencyParameterSensitivity sensi1 = sensiObject.getSensitivities().get(0);
    assertThat(sensi1.getCurrency()).isEqualTo(GBP);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parameterSensitivity_full() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double sensiValue = 25d;
    ZeroRateSensitivity point = test.zeroRatePointSensitivity(DATE_AFTER);
    point = point.multipliedBy(sensiValue);
    CurrencyParameterSensitivities sensiObject = test.parameterSensitivity(point);
    assertThat(sensiObject.getSensitivities()).hasSize(1);
    DoubleArray sensi0 =  sensiObject.getSensitivities().get(0).getSensitivity();
    double shift = 1.0E-6;
    for (int i = 0; i < X.size(); i++) {
      DoubleArray yP = Y.with(i, Y.get(i) + shift);
      InterpolatedNodalCurve curveP =
          InterpolatedNodalCurve.of(META_ZERO_PERIODIC, X, yP, INTERPOLATOR);
      double dfP = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, curveP).discountFactor(DATE_AFTER);
      DoubleArray yM = Y.with(i, Y.get(i) - shift);
      InterpolatedNodalCurve curveM =
          InterpolatedNodalCurve.of(META_ZERO_PERIODIC, X, yM, INTERPOLATOR);
      double dfM = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, curveM).discountFactor(DATE_AFTER);
      assertThat(sensi0.get(i)).isCloseTo(sensiValue * (dfP - dfM) / (2 * shift), offset(TOLERANCE_DELTA_FD));
    }    
  }
  
  @Test
  public void test_parameterSensitivity_withSpread_full() {
    int periodPerYear = 2;
    double spread = 0.0011; // 11 bp
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double sensiValue = 25d;
    ZeroRateSensitivity point = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, spread, PERIODIC, periodPerYear);
    point = point.multipliedBy(sensiValue);
    CurrencyParameterSensitivities sensiObject = test.parameterSensitivity(point);
    assertThat(sensiObject.getSensitivities()).hasSize(1);
    DoubleArray sensi0 =  sensiObject.getSensitivities().get(0).getSensitivity();
    double shift = 1.0E-6;
    for (int i = 0; i < X.size(); i++) {
      DoubleArray yP = Y.with(i, Y.get(i) + shift);
      InterpolatedNodalCurve curveP =
          InterpolatedNodalCurve.of(META_ZERO_PERIODIC, X, yP, INTERPOLATOR);
      double dfP = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, curveP)
          .discountFactorWithSpread(DATE_AFTER, spread, PERIODIC, periodPerYear);
      DoubleArray yM = Y.with(i, Y.get(i) - shift);
      InterpolatedNodalCurve curveM =
          InterpolatedNodalCurve.of(META_ZERO_PERIODIC, X, yM, INTERPOLATOR);
      double dfM = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, curveM)
          .discountFactorWithSpread(DATE_AFTER, spread, PERIODIC, periodPerYear);
      assertThat(sensi0.get(i)).as("With spread - " + i).isCloseTo(sensiValue * (dfP - dfM) / (2 * shift), offset(TOLERANCE_DELTA_FD));
    }    
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createParameterSensitivity() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15, 0.16);
    CurrencyParameterSensitivities sens = test.createParameterSensitivity(USD, sensitivities);
    assertThat(sens.getSensitivities().get(0)).isEqualTo(CURVE.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurve() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE).withCurve(CURVE2);
    assertThat(test.getCurve()).isEqualTo(CURVE2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    coverImmutableBean(test);
    ZeroRatePeriodicDiscountFactors test2 = ZeroRatePeriodicDiscountFactors.of(USD, DATE_VAL.plusDays(1), CURVE2);
    coverBeanEquals(test, test2);
  }

}
