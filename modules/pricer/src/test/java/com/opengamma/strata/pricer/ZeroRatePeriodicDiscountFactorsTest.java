/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.CompoundedRateType.CONTINUOUS;
import static com.opengamma.strata.pricer.CompoundedRateType.PERIODIC;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

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
@Test
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
  public void test_of() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getCurve(), CURVE);
    assertEquals(test.getParameterCount(), CURVE.getParameterCount());
    assertEquals(test.getParameter(0), CURVE.getParameter(0));
    assertEquals(test.getParameterMetadata(0), CURVE.getParameterMetadata(0));
    assertEquals(test.withParameter(0, 1d).getCurve(), CURVE.withParameter(0, 1d));
    assertEquals(test.withPerturbation((i, v, m) -> v + 1d).getCurve(), CURVE.withPerturbation((i, v, m) -> v + 1d));
    assertEquals(test.findData(CURVE.getName()), Optional.of(CURVE));
    assertEquals(test.findData(CurveName.of("Rubbish")), Optional.empty());
  }

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
    assertThrowsIllegalArg(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notYearFraction));
    assertThrowsIllegalArg(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notZeroRate));
    assertThrowsIllegalArg(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notDayCount));
    assertThrowsIllegalArg(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, notCompoundPerYear));
    assertThrowsIllegalArg(() -> ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, curveNegativeNb));
  }

  //-------------------------------------------------------------------------
  public void test_discountFactor() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double expected = Math.pow(1.0d + CURVE.yValue(relativeYearFraction) / CMP_PERIOD,
        -CMP_PERIOD * relativeYearFraction);
    assertEquals(test.discountFactor(DATE_AFTER), expected);
  }
  
  public void test_discountFactorTimeDerivative() {
    DiscountFactors test = DiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double expectedP = test.discountFactor(relativeYearFraction + EPS);
    double expectedM = test.discountFactor(relativeYearFraction - EPS);
    assertEquals(test.discountFactorTimeDerivative(relativeYearFraction), (expectedP - expectedM) / (2 * EPS),
        TOLERANCE_DELTA_FD);
  }

  //-------------------------------------------------------------------------
  public void test_zeroRate() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double discountFactor = test.discountFactor(DATE_AFTER);
    double zeroRate = test.zeroRate(DATE_AFTER);
    assertEquals(Math.exp(-zeroRate * relativeYearFraction), discountFactor);
  }

  //-------------------------------------------------------------------------
  public void test_discountFactorWithSpread_continuous() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    double expected = df * Math.exp(-SPREAD * relativeYearFraction);
    assertEquals(test.discountFactorWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0), expected, TOLERANCE_DF);
  }

  public void test_discountFactorWithSpread_periodic() {
    int periodPerYear = 4;
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double discountFactorBase = test.discountFactor(DATE_AFTER);
    double onePlus = Math.pow(discountFactorBase, -1.0d / (periodPerYear * relativeYearFraction));
    double expected = Math.pow(onePlus + SPREAD / periodPerYear, -periodPerYear * relativeYearFraction);
    assertEquals(test.discountFactorWithSpread(DATE_AFTER, SPREAD, PERIODIC, periodPerYear), expected, TOLERANCE_DF);
  }

  public void test_discountFactorWithSpread_smallYearFraction() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    assertEquals(test.discountFactorWithSpread(DATE_VAL, SPREAD, PERIODIC, 1), 1d, TOLERANCE_DF);
  }

  //-------------------------------------------------------------------------
  public void test_zeroRatePointSensitivity() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, -df * relativeYearFraction);
    assertEquals(test.zeroRatePointSensitivity(DATE_AFTER), expected);
  }

  public void test_zeroRatePointSensitivity_sensitivityCurrency() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, USD, -df * relativeYearFraction);
    assertEquals(test.zeroRatePointSensitivity(DATE_AFTER, USD), expected);
  }

  //-------------------------------------------------------------------------
  public void test_zeroRatePointSensitivityWithSpread_continous() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactorWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, -df * relativeYearFraction);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0);
    assertTrue(computed.compareKey(expected) == 0);
    assertEquals(computed.getSensitivity(), expected.getSensitivity(), TOLERANCE_DELTA);
  }

  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_continous() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactorWithSpread(DATE_AFTER, SPREAD, CONTINUOUS, 0);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, relativeYearFraction, USD, -df * relativeYearFraction);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, USD, SPREAD, CONTINUOUS, 0);
    assertTrue(computed.compareKey(expected) == 0);
    assertEquals(computed.getSensitivity(), expected.getSensitivity(), TOLERANCE_DELTA);
  }

  public void test_zeroRatePointSensitivityWithSpread_periodic() {
    int periodPerYear = 4;
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    double z = -1.0/relativeYearFraction*Math.log(df);
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
    assertTrue(computed.compareKey(expected) == 0);
    assertEquals(computed.getSensitivity(), expected.getSensitivity(), TOLERANCE_DELTA_FD);
  }

  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_periodic() {
    int periodPerYear = 4;
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = test.discountFactor(DATE_AFTER);
    double z = -1.0/relativeYearFraction*Math.log(df);
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
    assertTrue(computed.compareKey(expected) == 0);
    assertEquals(computed.getSensitivity(), expected.getSensitivity(), TOLERANCE_DELTA_FD);
  }

  public void test_zeroRatePointSensitivityWithSpread_smallYearFraction() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, 0d, 0.0d);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_VAL, SPREAD, CONTINUOUS, 0);
    assertTrue(computed.compareKey(expected) == 0);
    assertEquals(computed.getSensitivity(), expected.getSensitivity(), TOLERANCE_DELTA_FD);
  }

  public void test_zeroRatePointSensitivityWithSpread_sensitivityCurrency_smallYearFraction() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, 0d, USD, 0.0d);
    ZeroRateSensitivity computed = test.zeroRatePointSensitivityWithSpread(DATE_VAL, USD, SPREAD, CONTINUOUS, 0);
    assertTrue(computed.compareKey(expected) == 0);
    assertEquals(computed.getSensitivity(), expected.getSensitivity(), TOLERANCE_DELTA_FD);
  }

  //-------------------------------------------------------------------------
  //-------------------------------------------------------------------------
  public void test_parameterSensitivity() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double sensiValue = 25d;
    ZeroRateSensitivity point = test.zeroRatePointSensitivity(DATE_AFTER);
    point = point.multipliedBy(sensiValue);
    CurrencyParameterSensitivities sensiObject = test.parameterSensitivity(point);
    assertEquals(sensiObject.size(), 1);
    CurrencyParameterSensitivity sensi1 = sensiObject.getSensitivities().get(0);
    assertEquals(sensi1.getCurrency(), GBP);
  }

  //-------------------------------------------------------------------------
  public void test_parameterSensitivity_full() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double sensiValue = 25d;
    ZeroRateSensitivity point = test.zeroRatePointSensitivity(DATE_AFTER);
    point = point.multipliedBy(sensiValue);
    CurrencyParameterSensitivities sensiObject = test.parameterSensitivity(point);
    assertEquals(sensiObject.getSensitivities().size(), 1);
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
      assertEquals(sensi0.get(i), sensiValue * (dfP - dfM) / (2 * shift), TOLERANCE_DELTA_FD);
    }    
  }
  
  public void test_parameterSensitivity_withSpread_full() {
    int periodPerYear = 2;
    double spread = 0.0011; // 11 bp
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    double sensiValue = 25d;
    ZeroRateSensitivity point = test.zeroRatePointSensitivityWithSpread(DATE_AFTER, spread, PERIODIC, periodPerYear);
    point = point.multipliedBy(sensiValue);
    CurrencyParameterSensitivities sensiObject = test.parameterSensitivity(point);
    assertEquals(sensiObject.getSensitivities().size(), 1);
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
      assertEquals(sensi0.get(i), sensiValue * (dfP - dfM) / (2 * shift), TOLERANCE_DELTA_FD, "With spread - " + i);
    }    
  }

  //-------------------------------------------------------------------------
  public void test_createParameterSensitivity() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15, 0.16);
    CurrencyParameterSensitivities sens = test.createParameterSensitivity(USD, sensitivities);
    assertEquals(sens.getSensitivities().get(0), CURVE.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  public void test_withCurve() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE).withCurve(CURVE2);
    assertEquals(test.getCurve(), CURVE2);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ZeroRatePeriodicDiscountFactors test = ZeroRatePeriodicDiscountFactors.of(GBP, DATE_VAL, CURVE);
    coverImmutableBean(test);
    ZeroRatePeriodicDiscountFactors test2 = ZeroRatePeriodicDiscountFactors.of(USD, DATE_VAL.plusDays(1), CURVE2);
    coverBeanEquals(test, test2);
  }

}
