/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Test.
 */
@Test
public class ShermanMorrisonMatrixUpdateFunctionTest {
  private static final MatrixAlgebra ALGEBRA = new OGMatrixAlgebra();
  private static final ShermanMorrisonMatrixUpdateFunction UPDATE = new ShermanMorrisonMatrixUpdateFunction(ALGEBRA);
  private static final DoubleMatrix1D V = new DoubleMatrix1D(new double[] {1, 2 });
  private static final DoubleMatrix2D M = new DoubleMatrix2D(new double[][] {new double[] {3, 4 }, new double[] {5, 6 } });
  private static final Function1D<DoubleMatrix1D, DoubleMatrix2D> J = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
    @SuppressWarnings("synthetic-access")
    @Override
    public DoubleMatrix2D evaluate(DoubleMatrix1D x) {
      return ALGEBRA.getOuterProduct(x, x);
    }
  };

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull() {
    new ShermanMorrisonMatrixUpdateFunction(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDeltaX() {
    UPDATE.getUpdatedMatrix(J, V, null, V, M);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDeltaY() {
    UPDATE.getUpdatedMatrix(J, V, V, null, M);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullMatrix() {
    UPDATE.getUpdatedMatrix(J, V, V, V, null);
  }
}
