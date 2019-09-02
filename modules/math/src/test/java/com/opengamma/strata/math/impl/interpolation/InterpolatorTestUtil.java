/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * 
 */
public abstract class InterpolatorTestUtil {

  /**
   * Test double array with relative tolerance
   * @param message The message
   * @param expected The expected values
   * @param obtained The obtained values
   * @param relativeTol The relative tolerance
   */
  public static void assertArrayRelative(String message, double[] expected, double[] obtained, double relativeTol) {
    int nData = expected.length;
    assertThat(nData).as(message).isEqualTo(obtained.length);
    for (int i = 0; i < nData; ++i) {
      assertRelative(message, expected[i], obtained[i], relativeTol);
    }
  }

  /**
   * Test double with relative tolerance
   * @param message The message
   * @param expected The expected value
   * @param obtained The obtained value
   * @param relativeTol The relative tolerance
   */
  public static void assertRelative(String message, double expected, double obtained, double relativeTol) {
    double ref = Math.max(Math.abs(expected), 1.0);
    assertThat(obtained).as(message).isCloseTo(expected, offset(ref * relativeTol));
  }
}
