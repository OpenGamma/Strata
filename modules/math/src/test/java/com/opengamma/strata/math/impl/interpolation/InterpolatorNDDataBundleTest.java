/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.interpolation.data.InterpolatorNDDataBundle;

/**
 * Test.
 */
@Test
public class InterpolatorNDDataBundleTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData() {
    new InterpolatorNDDataBundle(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyData() {
    final List<Pair<double[], Double>> data = new ArrayList<>();
    new InterpolatorNDDataBundle(data);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyData2() {
    final List<Pair<double[], Double>> data = new ArrayList<>();
    final double[] temp = new double[] {};
    final Pair<double[], Double> pair = Pair.of(temp, 0.0);
    data.add(pair);
    new InterpolatorNDDataBundle(data);
  }

}
