/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.Tenor.TENOR_18M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_5Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_6M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_7Y;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.ValueType.YEAR_FRACTION;
import static com.opengamma.strata.market.ValueType.ZERO_RATE;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_CUBIC_SPLINE;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PCHIP;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * Test {@link CombinedCurve}.
 */
@Test
public class CombinedCurveTest {

  private static final String BASE_NAME = "BaseCurve";
  private static final String SPREAD_NAME = "SpreadCurve";
  private static final DoubleArray XVALUES_BASE = DoubleArray.of(1.0, 3.0, 5.0);
  private static final DoubleArray YVALUES_BASE = DoubleArray.of(0.05, 0.1, 0.01);
  private static final DoubleArray XVALUES_SPREAD = DoubleArray.of(0.5, 1.5, 5.0, 7.0);
  private static final DoubleArray YVALUES_SPREAD = DoubleArray.of(0.01, 0.1, 0.03, 0.15);
  private static final List<ParameterMetadata> PARAM_METADATA_BASE = new ArrayList<>();
  private static final List<ParameterMetadata> PARAM_METADATA_SPREAD = new ArrayList<>();
  static {
    PARAM_METADATA_BASE.add(TenorParameterMetadata.of(TENOR_1Y, BASE_NAME + TENOR_1Y.toString()));
    PARAM_METADATA_BASE.add(TenorParameterMetadata.of(TENOR_3Y, BASE_NAME + TENOR_3Y.toString()));
    PARAM_METADATA_BASE.add(TenorParameterMetadata.of(TENOR_5Y, BASE_NAME + TENOR_5Y.toString()));
    PARAM_METADATA_SPREAD
        .add(TenorParameterMetadata.of(TENOR_6M, SPREAD_NAME + TENOR_6M.toString()));
    PARAM_METADATA_SPREAD
        .add(TenorParameterMetadata.of(TENOR_18M, SPREAD_NAME + TENOR_18M.toString()));
    PARAM_METADATA_SPREAD
        .add(TenorParameterMetadata.of(TENOR_5Y, SPREAD_NAME + TENOR_5Y.toString()));
    PARAM_METADATA_SPREAD
        .add(TenorParameterMetadata.of(TENOR_7Y, SPREAD_NAME + TENOR_7Y.toString()));
  }
  private static final CurveMetadata METADATA_BASE = DefaultCurveMetadata.builder()
      .curveName(BASE_NAME)
      .xValueType(YEAR_FRACTION)
      .yValueType(ZERO_RATE)
      .dayCount(ACT_365F)
      .parameterMetadata(PARAM_METADATA_BASE)
      .build();
  private static final CurveMetadata METADATA_SPREAD = DefaultCurveMetadata.builder()
      .curveName(SPREAD_NAME)
      .xValueType(YEAR_FRACTION)
      .yValueType(ZERO_RATE)
      .dayCount(ACT_365F)
      .parameterMetadata(PARAM_METADATA_SPREAD)
      .build();
  private static final InterpolatedNodalCurve BASE_CURVE = InterpolatedNodalCurve.of(
      METADATA_BASE, XVALUES_BASE, YVALUES_BASE, PCHIP, LINEAR, LINEAR);
  private static final InterpolatedNodalCurve SPREAD_CURVE = InterpolatedNodalCurve.of(
      METADATA_SPREAD, XVALUES_SPREAD, YVALUES_SPREAD, PCHIP, LINEAR, LINEAR);
  private static final CombinedCurve COMBINED_CURVE = CombinedCurve.of(BASE_CURVE, SPREAD_CURVE);
  private static final DoubleArray X_SAMPLES = DoubleArray.of(0.25, 0.75, 1.5, 4.0, 10.0d);
  private static final int NUM_SAMPLES = X_SAMPLES.size();
  private static final double TOL = 1.0e-14;

  public void test_of_noMetadata() {
    List<ParameterMetadata> combinedParamMeta = new ArrayList<>();
    combinedParamMeta.addAll(PARAM_METADATA_BASE);
    combinedParamMeta.addAll(PARAM_METADATA_SPREAD);
    CurveMetadata expectedMetadata = DefaultCurveMetadata.builder()
        .curveName(BASE_NAME + "+" + SPREAD_NAME)
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(combinedParamMeta)
        .build();
    assertEquals(COMBINED_CURVE.getBaseCurve(), BASE_CURVE);
    assertEquals(COMBINED_CURVE.getSpreadCurve(), SPREAD_CURVE);
    assertEquals(COMBINED_CURVE.getMetadata(), expectedMetadata);
    assertEquals(COMBINED_CURVE.getName(), expectedMetadata.getCurveName());
    assertEquals(
        COMBINED_CURVE.getParameterCount(),
        BASE_CURVE.getParameterCount() + SPREAD_CURVE.getParameterCount());
    assertEquals(COMBINED_CURVE.getParameter(1), BASE_CURVE.getParameter(1));
    assertEquals(COMBINED_CURVE.getParameter(6), SPREAD_CURVE.getParameter(3));
    assertEquals(COMBINED_CURVE.getParameterMetadata(0), BASE_CURVE.getParameterMetadata(0));
    assertEquals(COMBINED_CURVE.getParameterMetadata(4), SPREAD_CURVE.getParameterMetadata(1));
  }

  public void test_of() {
    CurveMetadata baseMetadata = DefaultCurveMetadata.builder()
        .curveName(BASE_NAME)
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    CurveMetadata spreadMetadata = DefaultCurveMetadata.builder()
        .curveName(SPREAD_NAME)
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    InterpolatedNodalCurve baseCurve = InterpolatedNodalCurve.of(
        baseMetadata, XVALUES_BASE, YVALUES_BASE, NATURAL_CUBIC_SPLINE, LINEAR, LINEAR);
    InterpolatedNodalCurve spreadCurve = InterpolatedNodalCurve.of(
        spreadMetadata, XVALUES_SPREAD, YVALUES_SPREAD, PCHIP, LINEAR, LINEAR);
    CurveMetadata combinedMetadata = DefaultCurveMetadata.builder()
        .curveName("CombinedCurve")
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    CombinedCurve test = CombinedCurve.of(baseCurve, spreadCurve, combinedMetadata);
    assertEquals(test.getBaseCurve(), baseCurve);
    assertEquals(test.getSpreadCurve(), spreadCurve);
    assertEquals(test.getMetadata(), combinedMetadata);
    assertEquals(test.getName(), combinedMetadata.getCurveName());
    assertEquals(
        test.getParameterCount(),
        baseCurve.getParameterCount() + spreadCurve.getParameterCount());
    assertEquals(test.getParameter(1), baseCurve.getParameter(1));
    assertEquals(test.getParameter(6), spreadCurve.getParameter(3));
    assertEquals(test.getParameterMetadata(2), baseCurve.getParameterMetadata(2));
    assertEquals(test.getParameterMetadata(5), spreadCurve.getParameterMetadata(2));
  }

  //-------------------------------------------------------------------------
  public void test_withMetadata() {
    List<ParameterMetadata> combinedParamMeta = new ArrayList<>();
    combinedParamMeta.addAll(PARAM_METADATA_BASE);
    combinedParamMeta.addAll(PARAM_METADATA_SPREAD);
    CurveMetadata newMetadata = DefaultCurveMetadata.builder()
        .curveName("newName")
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(combinedParamMeta)
        .build();
    CombinedCurve computed = COMBINED_CURVE.withMetadata(newMetadata);
    CombinedCurve expected = CombinedCurve.of(BASE_CURVE, SPREAD_CURVE, newMetadata);
    assertEquals(computed, expected);
  }

  public void test_withParameter() {
    CombinedCurve computed1 = COMBINED_CURVE.withParameter(1, 12.5);
    CombinedCurve expected1 = CombinedCurve.of(BASE_CURVE.withParameter(1, 12.5), SPREAD_CURVE);
    assertEquals(computed1, expected1);
    CombinedCurve computed2 = COMBINED_CURVE.withParameter(5, 7.5);
    CombinedCurve expected2 = CombinedCurve.of(
        BASE_CURVE, SPREAD_CURVE.withParameter(5 - BASE_CURVE.getParameterCount(), 7.5));
    assertEquals(computed2, expected2);
  }

  public void test_withPerturbation() {
    CombinedCurve computed = COMBINED_CURVE.withPerturbation((i, v, m) -> 2d * v * i);
    CombinedCurve expected = CombinedCurve.of(
        BASE_CURVE.withPerturbation((i, v, m) -> 2d * v * i),
        SPREAD_CURVE.withPerturbation((i, v, m) -> 2d * v * (i + BASE_CURVE.getParameterCount())));
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void test_yValue() {
    for (int i = 0; i < NUM_SAMPLES; ++i) {
      double computed = COMBINED_CURVE.yValue(X_SAMPLES.get(i));
      double expected = BASE_CURVE.yValue(X_SAMPLES.get(i)) + SPREAD_CURVE.yValue(X_SAMPLES.get(i));
      assertEquals(computed, expected, TOL);
    }
  }

  public void test_firstDerivative() {
    for (int i = 0; i < NUM_SAMPLES; ++i) {
      double computed = COMBINED_CURVE.firstDerivative(X_SAMPLES.get(i));
      double expected = BASE_CURVE.firstDerivative(X_SAMPLES.get(i)) +
          SPREAD_CURVE.firstDerivative(X_SAMPLES.get(i));
      assertEquals(computed, expected, TOL);
    }
  }

  public void test_yValueParameterSensitivity() {
    for (int i = 0; i < NUM_SAMPLES; ++i) {
      UnitParameterSensitivity computed =
          COMBINED_CURVE.yValueParameterSensitivity(X_SAMPLES.get(i));
      UnitParameterSensitivity baseSens = BASE_CURVE.yValueParameterSensitivity(X_SAMPLES.get(i));
      UnitParameterSensitivity spreadSens = SPREAD_CURVE.yValueParameterSensitivity(
          X_SAMPLES.get(i));
      assertEquals(computed.split(), ImmutableList.of(baseSens, spreadSens));
    }
  }

  public void test_createParameterSensitivity() {
    DoubleArray values = DoubleArray.of(3d, 4d, 6d, 1d, 2d, 5d, 8d);
    DoubleArray valuesBase = DoubleArray.of(3d, 4d, 6d);
    DoubleArray valuesSpread = DoubleArray.of(1d, 2d, 5d, 8d);
    UnitParameterSensitivity computed = COMBINED_CURVE.createParameterSensitivity(values);
    UnitParameterSensitivity baseSens = BASE_CURVE.createParameterSensitivity(valuesBase);
    UnitParameterSensitivity spreadSens = SPREAD_CURVE.createParameterSensitivity(valuesSpread);
    assertEquals(computed.split(), ImmutableList.of(baseSens, spreadSens));
  }

  public void test_createParameterSensitivityWithCurrency() {
    Currency ccy = Currency.USD;
    DoubleArray values = DoubleArray.of(3d, 4d, 6d, 1d, 2d, 5d, 8d);
    DoubleArray valuesBase = DoubleArray.of(3d, 4d, 6d);
    DoubleArray valuesSpread = DoubleArray.of(1d, 2d, 5d, 8d);
    CurrencyParameterSensitivity computed = COMBINED_CURVE.createParameterSensitivity(ccy, values);
    CurrencyParameterSensitivity baseSens = BASE_CURVE.createParameterSensitivity(ccy, valuesBase);
    CurrencyParameterSensitivity spreadSens =
        SPREAD_CURVE.createParameterSensitivity(ccy, valuesSpread);
    assertEquals(computed.split(), ImmutableList.of(baseSens, spreadSens));
  }

  //-------------------------------------------------------------------------
  public void test_xValueType() {
    CurveMetadata baseMetadata = DefaultCurveMetadata.builder()
        .curveName(BASE_NAME)
        .xValueType(ValueType.UNKNOWN)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    CurveMetadata spreadMetadata = DefaultCurveMetadata.builder()
        .curveName(SPREAD_NAME)
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    InterpolatedNodalCurve baseCurve = InterpolatedNodalCurve.of(
        baseMetadata, XVALUES_BASE, YVALUES_BASE, NATURAL_CUBIC_SPLINE, LINEAR, LINEAR);
    InterpolatedNodalCurve spreadCurve = InterpolatedNodalCurve.of(
        spreadMetadata, XVALUES_SPREAD, YVALUES_SPREAD, PCHIP, LINEAR, LINEAR);
    CurveMetadata combinedMetadata1 = DefaultCurveMetadata.builder()
        .curveName("CombinedCurve")
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    assertThrowsIllegalArg(() -> CombinedCurve.of(baseCurve, spreadCurve, combinedMetadata1));
    CurveMetadata combinedMetadata2 = DefaultCurveMetadata.builder()
        .curveName("CombinedCurve")
        .xValueType(ValueType.UNKNOWN)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    assertThrowsIllegalArg(() -> CombinedCurve.of(baseCurve, spreadCurve, combinedMetadata2));
  }

  public void test_yValueType() {
    CurveMetadata baseMetadata = DefaultCurveMetadata.builder()
        .curveName(BASE_NAME)
        .xValueType(YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .dayCount(ACT_365F)
        .build();
    CurveMetadata spreadMetadata = DefaultCurveMetadata.builder()
        .curveName(SPREAD_NAME)
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    InterpolatedNodalCurve baseCurve = InterpolatedNodalCurve.of(
        baseMetadata, XVALUES_BASE, YVALUES_BASE, NATURAL_CUBIC_SPLINE, LINEAR, LINEAR);
    InterpolatedNodalCurve spreadCurve = InterpolatedNodalCurve.of(
        spreadMetadata, XVALUES_SPREAD, YVALUES_SPREAD, PCHIP, LINEAR, LINEAR);
    CurveMetadata combinedMetadata1 = DefaultCurveMetadata.builder()
        .curveName("CombinedCurve")
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    assertThrowsIllegalArg(() -> CombinedCurve.of(baseCurve, spreadCurve, combinedMetadata1));
    CurveMetadata combinedMetadata2 = DefaultCurveMetadata.builder()
        .curveName("CombinedCurve")
        .xValueType(YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .dayCount(ACT_365F)
        .build();
    assertThrowsIllegalArg(() -> CombinedCurve.of(baseCurve, spreadCurve, combinedMetadata2));
  }

  public void test_dayCount() {
    CurveMetadata baseMetadata = DefaultCurveMetadata.builder()
        .curveName(BASE_NAME)
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    CurveMetadata spreadMetadata = DefaultCurveMetadata.builder()
        .curveName(SPREAD_NAME)
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(DayCounts.ACT_360)
        .build();
    InterpolatedNodalCurve baseCurve = InterpolatedNodalCurve.of(
        baseMetadata, XVALUES_BASE, YVALUES_BASE, NATURAL_CUBIC_SPLINE, LINEAR, LINEAR);
    InterpolatedNodalCurve spreadCurve = InterpolatedNodalCurve.of(
        spreadMetadata, XVALUES_SPREAD, YVALUES_SPREAD, PCHIP, LINEAR, LINEAR);
    CurveMetadata combinedMetadata1 = DefaultCurveMetadata.builder()
        .curveName("CombinedCurve")
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(DayCounts.ACT_360)
        .build();
    assertThrowsIllegalArg(() -> CombinedCurve.of(baseCurve, spreadCurve, combinedMetadata1));
    CurveMetadata combinedMetadata2 = DefaultCurveMetadata.builder()
        .curveName("CombinedCurve")
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .build();
    assertThrowsIllegalArg(() -> CombinedCurve.of(baseCurve, spreadCurve, combinedMetadata2));
  }

  //-------------------------------------------------------------------------
  public void test_underlyingCurves() {
    CurveMetadata metadata = DefaultCurveMetadata.builder()
        .curveName("newCurve")
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(PARAM_METADATA_SPREAD)
        .build();
    InterpolatedNodalCurve newCurve = InterpolatedNodalCurve.of(
        metadata, XVALUES_SPREAD, YVALUES_SPREAD, NATURAL_CUBIC_SPLINE, LINEAR, LINEAR);
    assertEquals(
        COMBINED_CURVE.withUnderlyingCurve(0, newCurve),
        CombinedCurve.of(newCurve, SPREAD_CURVE, COMBINED_CURVE.getMetadata()));
    assertEquals(
        COMBINED_CURVE.withUnderlyingCurve(1, newCurve),
        CombinedCurve.of(BASE_CURVE, newCurve, COMBINED_CURVE.getMetadata()));
    assertEquals(COMBINED_CURVE.split(), ImmutableList.of(BASE_CURVE, SPREAD_CURVE));
    assertThrowsIllegalArg(() -> COMBINED_CURVE.withUnderlyingCurve(2, newCurve));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(COMBINED_CURVE);
    CombinedCurve test = CombinedCurve.of(
        ConstantCurve.of(
            DefaultCurveMetadata.builder()
                .curveName("name1")
                .xValueType(YEAR_FRACTION)
                .yValueType(ZERO_RATE)
                .build(),
            1d),
        ConstantCurve.of(
            DefaultCurveMetadata.builder()
                .curveName("name2")
                .xValueType(YEAR_FRACTION)
                .yValueType(ZERO_RATE)
                .build(),
            2d));
    coverBeanEquals(COMBINED_CURVE, test);
  }

}
