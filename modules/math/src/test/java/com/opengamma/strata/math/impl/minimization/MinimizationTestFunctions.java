/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * 
 */
public abstract class MinimizationTestFunctions {
  public static final Function1D<DoubleMatrix1D, Double> ROSENBROCK = new Function1D<DoubleMatrix1D, Double>() {

    @Override
    public Double evaluate(DoubleMatrix1D x) {
      return FunctionUtils.square(1 - x.getEntry(0)) + 100 * FunctionUtils.square(x.getEntry(1) - FunctionUtils.square(x.getEntry(0)));
    }
  };

  public static final Function1D<DoubleMatrix1D, DoubleMatrix1D> ROSENBROCK_GRAD = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

    @Override
    public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
      double[] temp = new double[2];
      temp[0] = 2 * (x.getEntry(0) - 1) + 400 * x.getEntry(0) * (FunctionUtils.square(x.getEntry(0)) - x.getEntry(1));
      temp[1] = 200 * (x.getEntry(1) - FunctionUtils.square(x.getEntry(0)));
      return new DoubleMatrix1D(temp);
    }
  };

  public static final Function1D<DoubleMatrix1D, Double> UNCOUPLED_ROSENBROCK = new Function1D<DoubleMatrix1D, Double>() {

    @Override
    public Double evaluate(final DoubleMatrix1D x) {
      final int n = x.getNumberOfElements();
      if (n % 2 != 0) {
        throw new IllegalArgumentException("vector length must be even");
      }
      double sum = 0;
      for (int i = 0; i < n / 2; i++) {
        sum += FunctionUtils.square(1 - x.getEntry(2 * i)) + 100 * FunctionUtils.square(x.getEntry(2 * i + 1) - FunctionUtils.square(x.getEntry(2 * i)));
      }
      return sum;
    }
  };

  public static final Function1D<DoubleMatrix1D, Double> COUPLED_ROSENBROCK = new Function1D<DoubleMatrix1D, Double>() {

    @Override
    public Double evaluate(DoubleMatrix1D x) {
      int n = x.getNumberOfElements();

      double sum = 0;
      for (int i = 0; i < n - 1; i++) {
        sum += FunctionUtils.square(1 - x.getEntry(i)) + 100 * FunctionUtils.square(x.getEntry(i + 1) - FunctionUtils.square(x.getEntry(i)));
      }
      return sum;
    }
  };

  public static final Function1D<DoubleMatrix1D, DoubleMatrix1D> COUPLED_ROSENBROCK_GRAD = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

    @Override
    public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
      int n = x.getNumberOfElements();

      double[] res = new double[n];
      res[0] = 2 * (x.getEntry(0) - 1) + 400 * x.getEntry(0) * (FunctionUtils.square(x.getEntry(0)) - x.getEntry(1));
      res[n - 1] = 200 * (x.getEntry(n - 1) - FunctionUtils.square(x.getEntry(n - 2)));
      for (int i = 1; i < n - 1; i++) {
        res[i] = 2 * (x.getEntry(i) - 1) + 400 * x.getEntry(i) * (FunctionUtils.square(x.getEntry(i)) - x.getEntry(i + 1)) + 200
            * (x.getEntry(i) - FunctionUtils.square(x.getEntry(i - 1)));
      }
      return new DoubleMatrix1D(res);
    }
  };
}
