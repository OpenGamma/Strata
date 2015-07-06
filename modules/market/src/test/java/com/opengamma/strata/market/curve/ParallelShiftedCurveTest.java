/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import org.testng.annotations.Test;

import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;

@Test
public class ParallelShiftedCurveTest {

  private static final CurveMetadata METADATA = DefaultCurveMetadata.of("Test");
  private static final Curve CONSTANT_CURVE = ConstantNodalCurve.of(METADATA, 3);

  public void absolute() {
    Curve shiftedCurve = ParallelShiftedCurve.absolute(CONSTANT_CURVE, 1);
    assertThat(shiftedCurve.yValue(0)).isEqualTo(4d);
    assertThat(shiftedCurve.yValue(1)).isEqualTo(4d);
  }

  public void relative() {
    Curve shiftedCurve = ParallelShiftedCurve.relative(CONSTANT_CURVE, 0.1);
    assertThat(shiftedCurve.yValue(0)).isEqualTo(3.3, offset(1e-10));
    assertThat(shiftedCurve.yValue(1)).isEqualTo(3.3, offset(1e-10));
  }

  public void firstDerivative() {
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
  public void coverage() {
    ParallelShiftedCurve test = ParallelShiftedCurve.absolute(CONSTANT_CURVE, 1);
    coverImmutableBean(test);
    ParallelShiftedCurve test2 = ParallelShiftedCurve.relative(CONSTANT_CURVE, 0.2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ParallelShiftedCurve test = ParallelShiftedCurve.absolute(CONSTANT_CURVE, 1);
    assertSerialization(test);
  }

}
