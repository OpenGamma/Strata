/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.interpolation.data.InterpolatorNDDataBundle;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;

/**
 * Abstract test.
 */
@Test
public abstract class InterpolatorNDTestCase {
  protected static final List<Pair<double[], Double>> FLAT_DATA = new ArrayList<>();
  protected static final List<Pair<double[], Double>> COS_EXP_DATA = new ArrayList<>();
  protected static final List<Pair<double[], Double>> SWAPTION_ATM_VOL_DATA = new ArrayList<>();
  protected static final double VALUE = 0.3;

  protected static final Function<double[], Double> COS_EXP_FUNCTION = new Function<double[], Double>() {

    @Override
    public Double apply(final double[] x) {
      return Math.sin(Math.PI * x[0] / 10.0) * Math.exp(-x[1] / 5.);
    }
  };

  static {
    final RandomEngine random = new MersenneTwister64(MersenneTwister.DEFAULT_SEED);
    double x, y, z;
    double[] temp;
    for (int i = 0; i < 200; i++) {
      x = 10 * random.nextDouble();
      y = 10 * random.nextDouble();
      z = 10 * random.nextDouble();
      FLAT_DATA.add(Pair.of(new double[] {x, y, z }, VALUE));
      temp = new double[] {x, y };
      COS_EXP_DATA.add(Pair.of(temp, COS_EXP_FUNCTION.apply(temp)));
    }

    SWAPTION_ATM_VOL_DATA.add(Pair.of(new double[] {1, 1 }, 0.7332));
    SWAPTION_ATM_VOL_DATA.add(Pair.of(new double[] {1, 5 }, 0.36995));
    SWAPTION_ATM_VOL_DATA.add(Pair.of(new double[] {5, 5 }, 0.23845));
    SWAPTION_ATM_VOL_DATA.add(Pair.of(new double[] {5, 10 }, 0.2177));
    SWAPTION_ATM_VOL_DATA.add(Pair.of(new double[] {10, 20 }, 0.1697));
    SWAPTION_ATM_VOL_DATA.add(Pair.of(new double[] {15, 15 }, 0.162));
  }

  protected void assertFlat(final InterpolatorND interpolator, final double tol) {
    double x1, x2, x3;
    double[] x;
    final InterpolatorNDDataBundle dataBundle = interpolator.getDataBundle(FLAT_DATA);
    for (int i = 0; i < 10; i++) {
      x1 = 10 * getRandom().nextDouble();
      x2 = 10 * getRandom().nextDouble();
      x3 = 10 * getRandom().nextDouble();
      x = new double[] {x1, x2, x3 };
      final double fit = interpolator.interpolate(dataBundle, x);
      assertEquals(VALUE, fit, tol);
    }

  }

  protected void assertCosExp(final InterpolatorND interpolator, final double tol) {
    double x1, x2;
    double[] x;
    final InterpolatorNDDataBundle dataBundle = interpolator.getDataBundle(COS_EXP_DATA);
    for (int i = 0; i < 10; i++) {
      x1 = 10 * getRandom().nextDouble();
      x2 = 10 * getRandom().nextDouble();
      x = new double[] {x1, x2 };
      final double fit = interpolator.interpolate(dataBundle, x);
      assertEquals(COS_EXP_FUNCTION.apply(x), fit, tol);
    }

    //check the input points are recovered exactly
    for (int i = 0; i < 10; i++) {
      final Pair<double[], Double> t = COS_EXP_DATA.get(i);
      assertEquals(t.getSecond(), interpolator.interpolate(dataBundle, t.getFirst()), 1e-9);
    }
  }

  protected abstract RandomEngine getRandom();
}
