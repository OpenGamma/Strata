/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link BiLinearSimpleBoundInterpolator}.
 */
public class BiLinearSimpleBoundInterpolatorTest {

  private static final double TOLERANCE = 1.0E-8;
  
  @Test
  void interpolation() {

    double[] xValues = new double[] {0.1, 0.3, 0.5};
    double[] yValues = new double[] {1.0, 2.0, 4.0, 10.0};
    double[][] zValues = new double[][] {
        {1.0, 2.0, 4.0, 10.0},
        {2.0, 2.0, 2.0, 2.0},
        {3.0, 4.0, 5.0, 7.0}
    };

    BiLinearSimpleBoundInterpolator interpolator =
        new BiLinearSimpleBoundInterpolator(xValues, yValues, zValues);

    double[] xValuesTest = new double[] {0.3, 0.3, 0.3, 0.3, 0.4, 0.4};
    double[] yValuesTest = new double[] {1.0, 2.0, 4.0, 10.0, 1.0, 5.0};
    double[] zExpected = new double[] {2.0, 2.0, 2.0, 2.0, 2.5, 3.666666666666667};
    double[] zComputed = new double[xValuesTest.length];
    for (int i = 0; i < xValuesTest.length; i++) {
      zComputed[i] = interpolator.interpolate(xValuesTest[i], yValuesTest[i]);
      assertThat(zComputed[i]).isCloseTo(zExpected[i], offset(TOLERANCE));
      //System.out.println(zComputed[i]);
    }
  }

}
