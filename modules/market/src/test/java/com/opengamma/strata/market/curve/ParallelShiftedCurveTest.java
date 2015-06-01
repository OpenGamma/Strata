/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.testng.annotations.Test;

import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;

@Test
public class ParallelShiftedCurveTest {

  public void absolute() {
    ConstantNodalCurve curve = ConstantNodalCurve.of("curveName", 3);
    Curve shiftedCurve = ParallelShiftedCurve.absolute(curve, 1);
    assertThat(shiftedCurve.yValue(0)).isEqualTo(4d);
    assertThat(shiftedCurve.yValue(1)).isEqualTo(4d);
  }

  public void relative() {
    ConstantNodalCurve curve = ConstantNodalCurve.of("curveName", 3);
    Curve shiftedCurve = ParallelShiftedCurve.relative(curve, 0.1);
    assertThat(shiftedCurve.yValue(0)).isEqualTo(3.3, offset(1e-10));
    assertThat(shiftedCurve.yValue(1)).isEqualTo(3.3, offset(1e-10));
  }

  public void firstDerivative() {
    InterpolatedNodalCurve curve = InterpolatedNodalCurve.of(
        "curve",
        new double[]{0, 1},
        new double[]{2, 2.5},
        Interpolator1DFactory.LINEAR_INSTANCE); // TODO Use CurveInterpolators.LINEAR when #261 is fixed

    Curve absoluteShiftedCurve = ParallelShiftedCurve.absolute(curve, 1);
    Curve relativeShiftedCurve = ParallelShiftedCurve.relative(curve, 0.2);

    assertThat(curve.firstDerivative(0.1)).isEqualTo(0.5);
    assertThat(absoluteShiftedCurve.firstDerivative(0.1)).isEqualTo(0.5);
    assertThat(relativeShiftedCurve.firstDerivative(0.1)).isEqualTo(0.5 * 1.2);
  }
}
