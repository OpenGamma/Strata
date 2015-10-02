/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static com.opengamma.strata.math.impl.minimization.MinimizationTestFunctions.COUPLED_ROSENBROCK;
import static com.opengamma.strata.math.impl.minimization.MinimizationTestFunctions.ROSENBROCK;
import static com.opengamma.strata.math.impl.minimization.MinimizationTestFunctions.UNCOUPLED_ROSENBROCK;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * Abstract test.
 */
@Test
public abstract class MultidimensionalMinimizerTestCase {

  private static final Function1D<DoubleMatrix1D, Double> F_2D = new Function1D<DoubleMatrix1D, Double>() {
    @Override
    public Double evaluate(final DoubleMatrix1D x) {
      return (x.getEntry(0) + 3.4) * (x.getEntry(0) + 3.4) + (x.getEntry(1) - 1) * (x.getEntry(1) - 1);
    }
  };

  protected void assertInputs(final Minimizer<Function1D<DoubleMatrix1D, Double>, DoubleMatrix1D> minimizer) {
    try {
      minimizer.minimize(null, new DoubleMatrix1D(new double[] {2., 3. }));
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

  protected void assertMinimizer(final Minimizer<Function1D<DoubleMatrix1D, Double>, DoubleMatrix1D> minimizer, final double tol) {
    DoubleMatrix1D r = minimizer.minimize(F_2D, new DoubleMatrix1D(new double[] {10., 10. }));
    assertEquals(r.getEntry(0), -3.4, tol);
    assertEquals(r.getEntry(1), 1, tol);
    r = (minimizer.minimize(ROSENBROCK, new DoubleMatrix1D(new double[] {10, -5 })));
    assertEquals(r.getEntry(0), 1, tol);
    assertEquals(r.getEntry(1), 1, tol);
  }

  protected void assertSolvingRosenbrock(final Minimizer<Function1D<DoubleMatrix1D, Double>, DoubleMatrix1D> minimizer, final double tol) {
    final DoubleMatrix1D start = new DoubleMatrix1D(new double[] {-1.0, 1.0 });
    final DoubleMatrix1D solution = minimizer.minimize(ROSENBROCK, start);
    assertEquals(1.0, solution.getEntry(0), tol);
    assertEquals(1.0, solution.getEntry(1), tol);
  }

  protected void assertSolvingUncoupledRosenbrock(final Minimizer<Function1D<DoubleMatrix1D, Double>, DoubleMatrix1D> minimizer, final double tol) {
    final DoubleMatrix1D start = new DoubleMatrix1D(new double[] {-1.0, 1.0, -1.0, 1.0, -1.0, 1.0 });
    final DoubleMatrix1D solution = minimizer.minimize(UNCOUPLED_ROSENBROCK, start);
    for (int i = 0; i < solution.getNumberOfElements(); i++) {
      assertEquals(1.0, solution.getEntry(i), tol);
    }
  }

  protected void assertSolvingCoupledRosenbrock(final Minimizer<Function1D<DoubleMatrix1D, Double>, DoubleMatrix1D> minimizer, final double tol) {
    final DoubleMatrix1D start = new DoubleMatrix1D(new double[] {-1.0, 1.0, -1.0, 1.0, -1.0, 1.0, 1.0 });
    final DoubleMatrix1D solution = minimizer.minimize(COUPLED_ROSENBROCK, start);
    for (int i = 0; i < solution.getNumberOfElements(); i++) {
      assertEquals(1.0, solution.getEntry(i), tol);
    }
  }

}
