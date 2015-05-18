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
import com.opengamma.strata.basics.interpolator.OneDimensionalInterpolator;

@Test
public class OneDimensionalInterpolatorsTest {

  /**
   * Test that the constants correctly resolve to interpolator instances.
   */
  public void constants() {
    OneDimensionalInterpolator linear = OneDimensionalInterpolators.LINEAR;
    assertThat(linear).isInstanceOf(LinearInterpolator1D.class);

    OneDimensionalInterpolator exponential = OneDimensionalInterpolators.EXPONENTIAL;
    assertThat(exponential).isInstanceOf(ExponentialInterpolator1D.class);
  }
}
