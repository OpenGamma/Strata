/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * 
 */
public class NonLinearTransformFunction {

  private static final MatrixAlgebra MA = new OGMatrixAlgebra();

  private final NonLinearParameterTransforms _transform;
  private final Function1D<DoubleMatrix1D, DoubleMatrix1D> _func;
  private final Function1D<DoubleMatrix1D, DoubleMatrix2D> _jac;

  public NonLinearTransformFunction(final Function1D<DoubleMatrix1D, DoubleMatrix1D> func, final Function1D<DoubleMatrix1D, DoubleMatrix2D> jac,
      final NonLinearParameterTransforms transform) {

    _transform = transform;

    _func = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix1D evaluate(final DoubleMatrix1D yStar) {
        final DoubleMatrix1D y = _transform.inverseTransform(yStar);
        return func.evaluate(y);
      }
    };

    _jac = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix2D evaluate(final DoubleMatrix1D yStar) {
        final DoubleMatrix1D y = _transform.inverseTransform(yStar);
        final DoubleMatrix2D h = jac.evaluate(y);
        final DoubleMatrix2D invJ = _transform.inverseJacobian(yStar);
        return (DoubleMatrix2D) MA.multiply(h, invJ);
      }
    };

  }

  public Function1D<DoubleMatrix1D, DoubleMatrix1D> getFittingFunction() {
    return _func;
  }

  public Function1D<DoubleMatrix1D, DoubleMatrix2D> getFittingJacobian() {
    return _jac;
  }

}
