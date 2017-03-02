/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * 
 */
public class NonLinearTransformFunction {

  private static final MatrixAlgebra MA = new OGMatrixAlgebra();

  private final NonLinearParameterTransforms _transform;
  private final Function<DoubleArray, DoubleArray> _func;
  private final Function<DoubleArray, DoubleMatrix> _jac;

  public NonLinearTransformFunction(
      Function<DoubleArray, DoubleArray> func,
      Function<DoubleArray, DoubleMatrix> jac,
      NonLinearParameterTransforms transform) {

    _transform = transform;

    _func = new Function<DoubleArray, DoubleArray>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleArray apply(DoubleArray yStar) {
        DoubleArray y = _transform.inverseTransform(yStar);
        return func.apply(y);
      }
    };

    _jac = new Function<DoubleArray, DoubleMatrix>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix apply(DoubleArray yStar) {
        DoubleArray y = _transform.inverseTransform(yStar);
        DoubleMatrix h = jac.apply(y);
        DoubleMatrix invJ = _transform.inverseJacobian(yStar);
        return (DoubleMatrix) MA.multiply(h, invJ);
      }
    };

  }

  public Function<DoubleArray, DoubleArray> getFittingFunction() {
    return _func;
  }

  public Function<DoubleArray, DoubleMatrix> getFittingJacobian() {
    return _jac;
  }

}
