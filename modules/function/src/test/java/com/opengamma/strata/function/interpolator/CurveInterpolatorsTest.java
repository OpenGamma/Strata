/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.interpolator;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.analytics.math.interpolation.ExponentialInterpolator1D;
import com.opengamma.analytics.math.interpolation.LinearInterpolator1D;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;

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
}
