/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * Test {@link AddFixedCurve}.
 */
@Test
public class AddFixedCurveTest {

  private static final String NAME_FIXED = "FixedCurve";
  private static final String NAME_SPREAD = "SpreadCurve";
  private static final CurveName FIXED_CURVE_NAME = CurveName.of(NAME_FIXED);
  private static final CurveName SPREAD_CURVE_NAME = CurveName.of(NAME_SPREAD);
  private static final CurveMetadata METADATA_FIXED = Curves.zeroRates(FIXED_CURVE_NAME, ACT_365F);
  private static final String LABEL_1 = "Node1";
  private static final String LABEL_2 = "Node2";
  private static final String LABEL_3 = "Node3";
  private static final List<ParameterMetadata> PARAM_METADATA_SPREAD = new ArrayList<>();
  static {
    PARAM_METADATA_SPREAD.add(LabelDateParameterMetadata.of(LocalDate.of(2015, 1, 1), LABEL_1));
    PARAM_METADATA_SPREAD.add(LabelDateParameterMetadata.of(LocalDate.of(2015, 2, 1), LABEL_2));
    PARAM_METADATA_SPREAD.add(LabelDateParameterMetadata.of(LocalDate.of(2015, 3, 1), LABEL_3));
  }
  private static final CurveMetadata METADATA_SPREAD = DefaultCurveMetadata.builder()
      .curveName(SPREAD_CURVE_NAME)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .dayCount(ACT_365F)
      .parameterMetadata(PARAM_METADATA_SPREAD)
      .build();

  private static final DoubleArray XVALUES_FIXED = DoubleArray.of(1d, 2d, 3d, 4d);
  private static final DoubleArray YVALUES_FIXED = DoubleArray.of(0.05d, 0.07d, 0.08d, 0.09d);
  private static final DoubleArray XVALUES_SPREAD = DoubleArray.of(1.5d, 2.5d, 4.5d);
  private static final DoubleArray YVALUES_SPREAD = DoubleArray.of(0.04d, 0.045d, 0.05d);
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final double[] X_SAMPLE = {0.5d, 1.0d, 1.5d, 1.75d, 10.0d};
  private static final int NB_X_SAMPLE = X_SAMPLE.length;

  private static final InterpolatedNodalCurve FIXED_CURVE =
      InterpolatedNodalCurve.of(METADATA_FIXED, XVALUES_FIXED, YVALUES_FIXED, INTERPOLATOR);
  private static final InterpolatedNodalCurve SPREAD_CURVE =
      InterpolatedNodalCurve.of(METADATA_SPREAD, XVALUES_SPREAD, YVALUES_SPREAD, INTERPOLATOR);

  private static final AddFixedCurve ADD_FIXED_CURVE = AddFixedCurve.of(FIXED_CURVE, SPREAD_CURVE);

  private static final double TOLERANCE_Y = 1.0E-10;

  public void test_invalid() {
    // null fixed
    assertThrowsIllegalArg(() -> AddFixedCurve.of(null, SPREAD_CURVE));
    // null spread
    assertThrowsIllegalArg(() -> AddFixedCurve.of(FIXED_CURVE, null));
  }

  public void getter() {
    assertEquals(ADD_FIXED_CURVE.getMetadata(), METADATA_SPREAD);
    assertEquals(ADD_FIXED_CURVE.getParameterCount(), XVALUES_SPREAD.size());
    assertEquals(ADD_FIXED_CURVE.getParameter(0), ADD_FIXED_CURVE.getSpreadCurve().getParameter(0));
    assertEquals(ADD_FIXED_CURVE.getParameterMetadata(0), ADD_FIXED_CURVE.getSpreadCurve().getParameterMetadata(0));
    assertEquals(ADD_FIXED_CURVE.withParameter(0, 9d), AddFixedCurve.of(FIXED_CURVE, SPREAD_CURVE.withParameter(0, 9d)));
    assertEquals(ADD_FIXED_CURVE.withPerturbation((i, v, m) -> v + 1d),
        AddFixedCurve.of(FIXED_CURVE, SPREAD_CURVE.withPerturbation((i, v, m) -> v + 1d)));
    assertEquals(ADD_FIXED_CURVE.withMetadata(METADATA_FIXED),
        AddFixedCurve.of(FIXED_CURVE, SPREAD_CURVE.withMetadata(METADATA_FIXED)));
  }

  public void yValue() {
    for (int i = 0; i < NB_X_SAMPLE; i++) {
      double yComputed = ADD_FIXED_CURVE.yValue(X_SAMPLE[i]);
      double yExpected = FIXED_CURVE.yValue(X_SAMPLE[i]) + SPREAD_CURVE.yValue(X_SAMPLE[i]);
      assertEquals(yComputed, yExpected, TOLERANCE_Y);
    }
  }

  public void firstDerivative() {
    for (int i = 0; i < NB_X_SAMPLE; i++) {
      double dComputed = ADD_FIXED_CURVE.firstDerivative(X_SAMPLE[i]);
      double dExpected = FIXED_CURVE.firstDerivative(X_SAMPLE[i]) + SPREAD_CURVE.firstDerivative(X_SAMPLE[i]);
      assertEquals(dComputed, dExpected, TOLERANCE_Y);
    }
  }

  public void yParameterSensitivity() {
    for (int i = 0; i < X_SAMPLE.length; i++) {
      UnitParameterSensitivity dComputed = ADD_FIXED_CURVE.yValueParameterSensitivity(X_SAMPLE[i]);
      UnitParameterSensitivity dExpected = SPREAD_CURVE.yValueParameterSensitivity(X_SAMPLE[i]);
      assertTrue(dComputed.compareKey(dExpected) == 0);
      assertTrue(dComputed.getSensitivity().equalWithTolerance(dExpected.getSensitivity(), TOLERANCE_Y));
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(ADD_FIXED_CURVE);
    coverBeanEquals(ADD_FIXED_CURVE, AddFixedCurve.of(SPREAD_CURVE, FIXED_CURVE));
  }

}
