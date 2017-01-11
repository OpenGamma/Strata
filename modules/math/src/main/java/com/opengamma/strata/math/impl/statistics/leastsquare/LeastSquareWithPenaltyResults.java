/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Hold for results of {@link NonLinearLeastSquareWithPenalty}
 */
public class LeastSquareWithPenaltyResults extends LeastSquareResults {

  private final double _penalty;

  /**
   * Holder for the results of minimising $\sum_{i=1}^N (y_i - f_i(\mathbf{x}))^2 + \mathbf{x}^T\mathbf{P}\mathbf{x}$
   * WRT $\mathbf{x}$  (the vector of model parameters). 
   * @param chiSqr The value of the first term (the chi-squared)- the sum of squares between the 'observed' values $y_i$ and the model values 
   * $f_i(\mathbf{x})$ 
   * @param penalty The value of the second term (the penalty) 
   * @param parameters The value of  $\mathbf{x}$ 
   * @param covariance The covariance matrix for  $\mathbf{x}$ 
   */
  public LeastSquareWithPenaltyResults(double chiSqr, double penalty, DoubleArray parameters,
      DoubleMatrix covariance) {
    super(chiSqr, parameters, covariance);
    _penalty = penalty;
  }

  /**
   * Holder for the results of minimising $\sum_{i=1}^N (y_i - f_i(\mathbf{x}))^2 + \mathbf{x}^T\mathbf{P}\mathbf{x}$
   * WRT $\mathbf{x}$  (the vector of model parameters). 
   * @param chiSqr The value of the first term (the chi-squared)- the sum of squares between the 'observed' values $y_i$ and the model values 
   * $f_i(\mathbf{x})$ 
   * @param penalty The value of the second term (the penalty) 
   * @param parameters The value of  $\mathbf{x}$ 
   * @param covariance The covariance matrix for  $\mathbf{x}$ 
   * @param inverseJacobian The inverse Jacobian - this is the sensitivities of the model parameters to the 'observed' values 
   */
  public LeastSquareWithPenaltyResults(double chiSqr, double penalty, DoubleArray parameters,
      DoubleMatrix covariance, DoubleMatrix inverseJacobian) {
    super(chiSqr, parameters, covariance, inverseJacobian);
    _penalty = penalty;
  }

  /**
   * get the value of the penalty 
   * @return the penalty 
   */
  public double getPenalty() {
    return _penalty;
  }

}
