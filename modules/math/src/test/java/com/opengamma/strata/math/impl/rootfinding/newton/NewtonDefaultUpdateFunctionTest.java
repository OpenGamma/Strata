/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test.
 */
public class NewtonDefaultUpdateFunctionTest {
  private static final NewtonDefaultUpdateFunction F = new NewtonDefaultUpdateFunction();

  @Test
  public void testNullFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F.getUpdatedMatrix(null, null, null, null, null));
  }

  @Test
  public void testNullVector() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F.getUpdatedMatrix(new Function<DoubleArray, DoubleMatrix>() {

          @Override
          public DoubleMatrix apply(DoubleArray x) {
            return null;
          }
        }, null, null, null, null));
  }

}
