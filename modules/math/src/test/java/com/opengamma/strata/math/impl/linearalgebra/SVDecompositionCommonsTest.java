/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.linearalgebra.Decomposition;

/**
 * Test.
 */
public class SVDecompositionCommonsTest extends SVDecompositionCalculationTestCase {
  private static final MatrixAlgebra ALGEBRA = new CommonsMatrixAlgebra();
  private static final Decomposition<SVDecompositionResult> SVD = new SVDecompositionCommons();

  @Override
  protected MatrixAlgebra getAlgebra() {
    return ALGEBRA;
  }

  @Override
  protected Decomposition<SVDecompositionResult> getSVD() {
    return SVD;
  }
}
