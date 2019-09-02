/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

/**
 * Abstract test.
 */
public abstract class Minimizer1DTestCase {
  private static final double EPS = 1e-5;
  private static final Function<Double, Double> QUADRATIC = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return x * x + 7 * x + 12;
    }

  };
  private static final Function<Double, Double> QUINTIC = new Function<Double, Double>() {
    @Override
    public Double apply(final Double x) {
      return 1 + x * (-3 + x * (-9 + x * (-1 + x * (4 + x))));
    }
  };

  public void assertInputs(final ScalarMinimizer minimizer) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> minimizer.minimize(null, 0.0, 2., 3.));
  }

  public void assertMinimizer(final ScalarMinimizer minimizer) {
    double result = minimizer.minimize(QUADRATIC, 0.0, -10., 10.);
    assertThat(-3.5).isCloseTo(result, offset(EPS));
    result = minimizer.minimize(QUINTIC, 0.0, 0.5, 2.);
    assertThat(1.06154).isCloseTo(result, offset(EPS));
  }
}
