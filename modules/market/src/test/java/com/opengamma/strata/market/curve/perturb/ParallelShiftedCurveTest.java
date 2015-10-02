/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.perturb;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivity;
import com.opengamma.strata.math.impl.interpolation.Interpolator1DFactory;

@Test
public class ParallelShiftedCurveTest {

  private static final CurveMetadata METADATA = DefaultCurveMetadata.of("Test");
  private static final Curve CONSTANT_CURVE = ConstantNodalCurve.of(METADATA, 3);
  private static final Curve CONSTANT_CURVE2 = ConstantNodalCurve.of(METADATA, 5);

  //-------------------------------------------------------------------------
  public void absolute() {
    ParallelShiftedCurve test = ParallelShiftedCurve.absolute(CONSTANT_CURVE, 1);
    assertThat(test.getUnderlyingCurve()).isEqualTo(CONSTANT_CURVE);
    assertThat(test.getShiftType()).isEqualTo(ShiftType.ABSOLUTE);
    assertThat(test.getShiftAmount()).isEqualTo(1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getName()).isEqualTo(METADATA.getCurveName());
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.yValue(0)).isEqualTo(4d);
    assertThat(test.yValue(1)).isEqualTo(4d);
  }

  public void relative() {
    ParallelShiftedCurve test = ParallelShiftedCurve.relative(CONSTANT_CURVE, 0.1);
    assertThat(test.getUnderlyingCurve()).isEqualTo(CONSTANT_CURVE);
    assertThat(test.getShiftType()).isEqualTo(ShiftType.RELATIVE);
    assertThat(test.getShiftAmount()).isEqualTo(0.1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getName()).isEqualTo(METADATA.getCurveName());
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.yValue(0)).isEqualTo(3.3, offset(1e-10));
    assertThat(test.yValue(1)).isEqualTo(3.3, offset(1e-10));
  }

  public void test_of() {
    Curve test = ParallelShiftedCurve.of(CONSTANT_CURVE, ShiftType.RELATIVE, 0.1);
    assertThat(test.yValue(0)).isEqualTo(3.3, offset(1e-10));
    assertThat(test.yValue(1)).isEqualTo(3.3, offset(1e-10));
    assertThat(test.getName()).isEqualTo(METADATA.getCurveName());
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
  }

  //-------------------------------------------------------------------------
  public void test_yValueParameterSensitivity() {
    InterpolatedNodalCurve curve = InterpolatedNodalCurve.of(
        METADATA,
        new double[] {0, 1},
        new double[] {2, 2.5},
        Interpolator1DFactory.LINEAR_INSTANCE); // TODO Use CurveInterpolators.LINEAR when #261 is fixed

    Curve absoluteShiftedCurve = ParallelShiftedCurve.absolute(curve, 1);
    Curve relativeShiftedCurve = ParallelShiftedCurve.relative(curve, 0.2);

    CurveUnitParameterSensitivity expected = curve.yValueParameterSensitivity(0.1);
    assertThat(absoluteShiftedCurve.yValueParameterSensitivity(0.1)).isEqualTo(expected);
    assertThat(relativeShiftedCurve.yValueParameterSensitivity(0.1)).isEqualTo(expected);
  }

  public void test_firstDerivative() {
    InterpolatedNodalCurve curve = InterpolatedNodalCurve.of(
        METADATA,
        new double[] {0, 1},
        new double[] {2, 2.5},
        Interpolator1DFactory.LINEAR_INSTANCE); // TODO Use CurveInterpolators.LINEAR when #261 is fixed

    Curve absoluteShiftedCurve = ParallelShiftedCurve.absolute(curve, 1);
    Curve relativeShiftedCurve = ParallelShiftedCurve.relative(curve, 0.2);

    assertThat(curve.firstDerivative(0.1)).isEqualTo(0.5);
    assertThat(absoluteShiftedCurve.firstDerivative(0.1)).isEqualTo(0.5);
    assertThat(relativeShiftedCurve.firstDerivative(0.1)).isEqualTo(0.5 * 1.2);
  }

  //-------------------------------------------------------------------------
  public void test_toNodalCurve() {
    ParallelShiftedCurve base = ParallelShiftedCurve.absolute(CONSTANT_CURVE, 1);
    NodalCurve test = base.toNodalCurve();
    assertThat(test).isEqualTo(ConstantNodalCurve.of(METADATA, 4));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ParallelShiftedCurve test = ParallelShiftedCurve.absolute(CONSTANT_CURVE, 1);
    coverImmutableBean(test);
    ParallelShiftedCurve test2 = ParallelShiftedCurve.relative(CONSTANT_CURVE2, 0.2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ParallelShiftedCurve test = ParallelShiftedCurve.absolute(CONSTANT_CURVE, 1);
    assertSerialization(test);
  }

}
