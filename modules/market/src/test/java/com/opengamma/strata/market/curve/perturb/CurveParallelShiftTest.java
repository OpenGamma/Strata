/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.perturb;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;

/**
 * Test {@link CurveParallelShift}.
 */
@Test
public class CurveParallelShiftTest {

  public void absolute() {
    CurveParallelShift shift = CurveParallelShift.absolute(0.1);
    Curve shiftedCurve = shift.applyTo(ConstantNodalCurve.of("curveName", 2d));
    checkCurveValues(shiftedCurve, 2.1);
  }

  public void relative() {
    CurveParallelShift shift = CurveParallelShift.relative(0.1);
    Curve shiftedCurve = shift.applyTo(ConstantNodalCurve.of("curveName", 2d));
    checkCurveValues(shiftedCurve, 2.2);
  }

  // It's not possible to do an equality test on the curves because shifting them wraps them in a different type
  private void checkCurveValues(Curve curve, double expectedValue) {
    for (int i = 0; i < 10; i++) {
      assertThat(curve.yValue((double) i)).isEqualTo(expectedValue);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveParallelShift test = CurveParallelShift.absolute(0.1);
    coverImmutableBean(test);
    CurveParallelShift test2 = CurveParallelShift.relative(0.1);
    coverBeanEquals(test, test2);
  }

}
