/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static com.opengamma.strata.math.impl.minimization.MinimizationTestFunctions.COUPLED_ROSENBROCK;
import static com.opengamma.strata.math.impl.minimization.MinimizationTestFunctions.ROSENBROCK;
import static com.opengamma.strata.math.impl.minimization.MinimizationTestFunctions.UNCOUPLED_ROSENBROCK;
import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Abstract test.
 */
@Test
public abstract class MultidimensionalMinimizerTestCase {

  private static final Function<DoubleArray, Double> F_2D = new Function<DoubleArray, Double>() {
    @Override
    public Double apply(final DoubleArray x) {
      return (x.get(0) + 3.4) * (x.get(0) + 3.4) + (x.get(1) - 1) * (x.get(1) - 1);
    }
  };

  protected void assertInputs(final Minimizer<Function<DoubleArray, Double>, DoubleArray> minimizer) {
    try {
      minimizer.minimize(null, DoubleArray.of(2d, 3d));
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    try {
      minimizer.minimize(F_2D, null);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
  }

  protected void assertMinimizer(final Minimizer<Function<DoubleArray, Double>, DoubleArray> minimizer, final double tol) {
    DoubleArray r = minimizer.minimize(F_2D, DoubleArray.of(10d, 10d));
    assertEquals(r.get(0), -3.4, tol);
    assertEquals(r.get(1), 1, tol);
    r = (minimizer.minimize(ROSENBROCK, DoubleArray.of(10d, -5d)));
    assertEquals(r.get(0), 1, tol);
    assertEquals(r.get(1), 1, tol);
  }

  protected void assertSolvingRosenbrock(final Minimizer<Function<DoubleArray, Double>, DoubleArray> minimizer, final double tol) {
    final DoubleArray start = DoubleArray.of(-1d, 1d);
    final DoubleArray solution = minimizer.minimize(ROSENBROCK, start);
    assertEquals(1.0, solution.get(0), tol);
    assertEquals(1.0, solution.get(1), tol);
  }

  protected void assertSolvingUncoupledRosenbrock(final Minimizer<Function<DoubleArray, Double>, DoubleArray> minimizer, final double tol) {
    final DoubleArray start = DoubleArray.of(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0);
    final DoubleArray solution = minimizer.minimize(UNCOUPLED_ROSENBROCK, start);
    for (int i = 0; i < solution.size(); i++) {
      assertEquals(1.0, solution.get(i), tol);
    }
  }

  protected void assertSolvingCoupledRosenbrock(final Minimizer<Function<DoubleArray, Double>, DoubleArray> minimizer, final double tol) {
    final DoubleArray start = DoubleArray.of(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0, 1.0);
    final DoubleArray solution = minimizer.minimize(COUPLED_ROSENBROCK, start);
    for (int i = 0; i < solution.size(); i++) {
      assertEquals(1.0, solution.get(i), tol);
    }
  }

}
