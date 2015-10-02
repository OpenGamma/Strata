/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.interpolator;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.math.impl.interpolation.ExponentialInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;

/**
 * Test {@link CurveInterpolators}.
 */
@Test
public class CurveInterpolatorsTest {

  /**
   * Test that the constants correctly resolve to interpolator instances.
   */
  public void constants() {
    CurveInterpolator linear = CurveInterpolators.LINEAR;
    assertThat(linear).isInstanceOf(LinearInterpolator1D.class);

    CurveInterpolator exponential = CurveInterpolators.EXPONENTIAL;
    assertThat(exponential).isInstanceOf(ExponentialInterpolator1D.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(CurveInterpolators.class);
    coverPrivateConstructor(StandardCurveInterpolators.class);
  }

}
