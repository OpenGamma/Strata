/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.testng.annotations.Test;

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
}
