/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.interpolator;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.interpolator.CurveExtrapolator;
import com.opengamma.strata.math.impl.interpolation.LinearExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.LogLinearExtrapolator1D;

/**
 * Test {@link CurveExtrapolators}.
 */
@Test
public class CurveExtrapolatorsTest {

  /**
   * Test that the constants correctly resolve to interpolator instances.
   */
  public void constants() {
    CurveExtrapolator linear = CurveExtrapolators.LINEAR;
    assertThat(linear).isInstanceOf(LinearExtrapolator1D.class);

    CurveExtrapolator logLinear = CurveExtrapolators.LOG_LINEAR;
    assertThat(logLinear).isInstanceOf(LogLinearExtrapolator1D.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(CurveExtrapolators.class);
    coverPrivateConstructor(StandardCurveExtrapolators.class);
  }

}
