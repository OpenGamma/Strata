/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * 
 */
public class NonLinearTransformFunction {

  private static final MatrixAlgebra MA = new OGMatrixAlgebra();

  private final NonLinearParameterTransforms _transform;
  private final Function1D<DoubleArray, DoubleArray> _func;
  private final Function1D<DoubleArray, DoubleMatrix> _jac;

  public NonLinearTransformFunction(
      Function1D<DoubleArray, DoubleArray> func,
      Function1D<DoubleArray, DoubleMatrix> jac,
      NonLinearParameterTransforms transform) {

    _transform = transform;

    _func = new Function1D<DoubleArray, DoubleArray>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleArray evaluate(DoubleArray yStar) {
        DoubleArray y = _transform.inverseTransform(yStar);
        return func.evaluate(y);
      }
    };

    _jac = new Function1D<DoubleArray, DoubleMatrix>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix evaluate(DoubleArray yStar) {
        DoubleArray y = _transform.inverseTransform(yStar);
        DoubleMatrix h = jac.evaluate(y);
        DoubleMatrix invJ = _transform.inverseJacobian(yStar);
        return (DoubleMatrix) MA.multiply(h, invJ);
      }
    };

  }

  public Function1D<DoubleArray, DoubleArray> getFittingFunction() {
    return _func;
  }

  public Function1D<DoubleArray, DoubleMatrix> getFittingJacobian() {
    return _jac;
  }

}
