/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static com.opengamma.strata.math.impl.minimization.MinimizationTestFunctions.COUPLED_ROSENBROCK;
import static com.opengamma.strata.math.impl.minimization.MinimizationTestFunctions.ROSENBROCK;
import static com.opengamma.strata.math.impl.minimization.MinimizationTestFunctions.UNCOUPLED_ROSENBROCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Abstract test.
 */
public abstract class MultidimensionalMinimizerTestCase {

  private static final Function<DoubleArray, Double> F_2D = new Function<DoubleArray, Double>() {
    @Override
    public Double apply(DoubleArray x) {
      return (x.get(0) + 3.4) * (x.get(0) + 3.4) + (x.get(1) - 1) * (x.get(1) - 1);
    }
  };

  protected void assertInputs(final Minimizer<Function<DoubleArray, Double>, DoubleArray> minimizer) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> minimizer.minimize(null, DoubleArray.of(2d, 3d)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> minimizer.minimize(F_2D, null));
  }

  protected void assertMinimizer(Minimizer<Function<DoubleArray, Double>, DoubleArray> minimizer, double tol) {
    DoubleArray r = minimizer.minimize(F_2D, DoubleArray.of(10d, 10d));
    assertThat(r.get(0)).isCloseTo(-3.4, offset(tol));
    assertThat(r.get(1)).isCloseTo(1, offset(tol));
    r = (minimizer.minimize(ROSENBROCK, DoubleArray.of(10d, -5d)));
    assertThat(r.get(0)).isCloseTo(1, offset(tol));
    assertThat(r.get(1)).isCloseTo(1, offset(tol));
  }

  protected void assertSolvingRosenbrock(
      Minimizer<Function<DoubleArray, Double>, DoubleArray> minimizer,
      double tol) {

    DoubleArray start = DoubleArray.of(-1d, 1d);
    DoubleArray solution = minimizer.minimize(ROSENBROCK, start);
    assertThat(1.0).isCloseTo(solution.get(0), offset(tol));
    assertThat(1.0).isCloseTo(solution.get(1), offset(tol));
  }

  protected void assertSolvingUncoupledRosenbrock(
      Minimizer<Function<DoubleArray, Double>, DoubleArray> minimizer,
      double tol) {

    DoubleArray start = DoubleArray.of(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0);
    DoubleArray solution = minimizer.minimize(UNCOUPLED_ROSENBROCK, start);
    for (int i = 0; i < solution.size(); i++) {
      assertThat(1.0).isCloseTo(solution.get(i), offset(tol));
    }
  }

  protected void assertSolvingCoupledRosenbrock(
      Minimizer<Function<DoubleArray, Double>, DoubleArray> minimizer,
      double tol) {

    DoubleArray start = DoubleArray.of(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0, 1.0);
    DoubleArray solution = minimizer.minimize(COUPLED_ROSENBROCK, start);
    for (int i = 0; i < solution.size(); i++) {
      assertThat(1.0).isCloseTo(solution.get(i), offset(tol));
    }
  }

}
