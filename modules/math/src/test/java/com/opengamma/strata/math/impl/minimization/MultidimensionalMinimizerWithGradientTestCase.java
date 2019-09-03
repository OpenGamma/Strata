/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Abstract test.
 */
public abstract class MultidimensionalMinimizerWithGradientTestCase {

  protected void assertSolvingRosenbrock(
      final MinimizerWithGradient<Function<DoubleArray, Double>, Function<DoubleArray, DoubleArray>, DoubleArray> minimzer,
      final double tol) {

    final DoubleArray start = DoubleArray.of(-1.0, 1.0);
    final DoubleArray solution =
        minimzer.minimize(MinimizationTestFunctions.ROSENBROCK, MinimizationTestFunctions.ROSENBROCK_GRAD, start);
    assertThat(1.0).isCloseTo(solution.get(0), offset(tol));
    assertThat(1.0).isCloseTo(solution.get(1), offset(tol));
  }

  protected void assertSolvingRosenbrockWithoutGradient(
      final MinimizerWithGradient<Function<DoubleArray, Double>, Function<DoubleArray, DoubleArray>, DoubleArray> minimzer,
      final double tol) {

    final DoubleArray start = DoubleArray.of(-1.0, 1.0);
    final DoubleArray solution = minimzer.minimize(MinimizationTestFunctions.ROSENBROCK, start);
    assertThat(1.0).isCloseTo(solution.get(0), offset(tol));
    assertThat(1.0).isCloseTo(solution.get(1), offset(tol));
  }

  protected void assertSolvingCoupledRosenbrock(
      final MinimizerWithGradient<Function<DoubleArray, Double>, Function<DoubleArray, DoubleArray>, DoubleArray> minimzer,
      final double tol) {

    final DoubleArray start = DoubleArray.of(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0, 1.0);
    final DoubleArray solution =
        minimzer.minimize(MinimizationTestFunctions.COUPLED_ROSENBROCK, MinimizationTestFunctions.COUPLED_ROSENBROCK_GRAD, start);
    for (int i = 0; i < solution.size(); i++) {
      assertThat(1.0).isCloseTo(solution.get(i), offset(tol));
    }
  }

  protected void assertSolvingCoupledRosenbrockWithoutGradient(
      final MinimizerWithGradient<Function<DoubleArray, Double>, Function<DoubleArray, DoubleArray>, DoubleArray> minimzer,
      final double tol) {

    final DoubleArray start = DoubleArray.of(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0, 1.0);
    final DoubleArray solution = minimzer.minimize(MinimizationTestFunctions.COUPLED_ROSENBROCK, start);
    for (int i = 0; i < solution.size(); i++) {
      assertThat(1.0).isCloseTo(solution.get(i), offset(tol));
    }
  }
}
