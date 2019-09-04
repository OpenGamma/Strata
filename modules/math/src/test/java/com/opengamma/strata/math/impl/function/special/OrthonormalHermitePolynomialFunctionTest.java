/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.impl.function.DoubleFunction1D;

/**
 * Test.
 */
public class OrthonormalHermitePolynomialFunctionTest {
  private static final HermitePolynomialFunction HERMITE = new HermitePolynomialFunction();
  private static final OrthonormalHermitePolynomialFunction ORTHONORMAL = new OrthonormalHermitePolynomialFunction();
  private static final double EPS = 1e-9;

  @Test
  public void testBadN() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ORTHONORMAL.getPolynomials(-3));
  }

  @Test
  public void test() {
    final int n = 15;
    final DoubleFunction1D[] f1 = HERMITE.getPolynomials(n);
    final DoubleFunction1D[] f2 = ORTHONORMAL.getPolynomials(n);
    final double x = 3.4;
    for (int i = 0; i < f1.length; i++) {
      assertThat(f1[i].applyAsDouble(x) / Math.sqrt(CombinatoricsUtils.factorialDouble(i) * Math.pow(2, i) * Math.sqrt(Math.PI)))
          .isCloseTo(f2[i].applyAsDouble(x), offset(EPS));
    }
  }

}
