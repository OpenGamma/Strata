/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.math.impl.function.Function;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class GridInterpolator2DSensitivityTest {
  private static final Interpolator1D LINEAR_1D = new LinearInterpolator1D();
  private static final GridInterpolator2D INTERPOLATOR;
  private static final Map<DoublesPair, Double> DATA;
  private static final Map<Double, Interpolator1DDataBundle> DATA_BUNDLE;
  protected static final Function<Double, Double> COS_EXP_FUNCTION = new Function<Double, Double>() {

    @Override
    public Double evaluate(final Double... x) {
      return Math.sin(Math.PI * x[0] / 10.0) * Math.exp(-x[1] / 5.);
    }
  };

  static {
    DATA = new HashMap<>();
    for (int i = 0; i < 11; i++) {
      for (int j = 0; j < 11; j++) {
        final double x = i;
        final double y = j;
        DATA.put(DoublesPair.of(x, y), COS_EXP_FUNCTION.evaluate(x, y));
      }
    }

    INTERPOLATOR = new GridInterpolator2D(LINEAR_1D, LINEAR_1D);
    DATA_BUNDLE = INTERPOLATOR.getDataBundle(DATA);
  }

  @Test
  public void test() {
    final Map<DoublesPair, Double> res = INTERPOLATOR.getNodeSensitivitiesForValue(DATA_BUNDLE, DoublesPair.of(4.5, 7.25));
    assertEquals(0.375, res.get(DoublesPair.of(4.0, 7.0)), 0.0);
    assertEquals(0.375, res.get(DoublesPair.of(5.0, 7.0)), 0.0);
    assertEquals(0.125, res.get(DoublesPair.of(4.0, 8.0)), 0.0);
    assertEquals(0.125, res.get(DoublesPair.of(5.0, 8.0)), 0.0);
    double sum = 0.0;
    for (final Map.Entry<DoublesPair, Double> entry : res.entrySet()) {
      sum += entry.getValue();
    }
    assertEquals(1.0, sum, 0.0);
  }

}
