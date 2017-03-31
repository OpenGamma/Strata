/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionFactory;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionResult;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Modification to NonLinearLeastSquare to use a penalty function add to the normal chi^2 term of the form $a^TPa$ where
 * $a$ is the vector of model parameters sort and P is some matrix. The idea is to extend the p-spline concept to
 * non-linear models of the form $\hat{y}_j = H\left(\sum_{i=0}^{M-1} w_i b_i (x_j)\right)$ where $H(\cdot)$ is
 * some non-linear function, $b_i(\cdot)$ are a set of basis functions and $w_i$ are the weights (to be found). As with
 * (linear) p-splines, smoothness of the function is obtained by having a penalty on the nth order difference of the
 * weights. The modified chi-squared is written as
 * $\chi^2 = \sum_{i=0}^{N-1} \left(\frac{y_i-H\left(\sum_{k=0}^{M-1} w_k b_k (x_i)\right)}{\sigma_i} \right)^2 +
 * \sum_{i,j=0}^{M-1}P_{i,j}x_ix_j$
 */
public class NonLinearLeastSquareWithPenalty {

  private static final int MAX_ATTEMPTS = 100000;

  // Review should we use Cholesky as default
  private static final Decomposition<?> DEFAULT_DECOMP = DecompositionFactory.SV_COMMONS;
  private static final OGMatrixAlgebra MA = new OGMatrixAlgebra();
  private static final double EPS = 1e-8; // Default convergence tolerance on the relative change in chi2

  /**
   * Unconstrained allowed function - always returns true
   */
  public static final Function<DoubleArray, Boolean> UNCONSTRAINED = new Function<DoubleArray, Boolean>() {
    @Override
    public Boolean apply(DoubleArray x) {
      return true;
    }
  };

  private final double _eps;
  private final Decomposition<?> _decomposition;
  private final MatrixAlgebra _algebra;

  /**
   * Default constructor. This uses SVD, {@link OGMatrixAlgebra} and a convergence tolerance of 1e-8
   */
  public NonLinearLeastSquareWithPenalty() {
    this(DEFAULT_DECOMP, MA, EPS);
  }

  /**
   * Constructor allowing matrix decomposition to be set.
   * This uses {@link OGMatrixAlgebra} and a convergence tolerance of 1e-8.
   * 
   * @param decomposition Matrix decomposition (see {@link DecompositionFactory} for list)
   */
  public NonLinearLeastSquareWithPenalty(Decomposition<?> decomposition) {
    this(decomposition, MA, EPS);
  }

  /**
   * Constructor allowing convergence tolerance to be set.
   * This uses SVD and {@link OGMatrixAlgebra}.
   * 
   * @param eps Convergence tolerance
   */
  public NonLinearLeastSquareWithPenalty(double eps) {
    this(DEFAULT_DECOMP, MA, eps);
  }

  /**
   * Constructor allowing matrix decomposition and convergence tolerance to be set.
   * This uses {@link OGMatrixAlgebra}.
   * 
   * @param decomposition Matrix decomposition (see {@link DecompositionFactory} for list)
   * @param eps Convergence tolerance
   */
  public NonLinearLeastSquareWithPenalty(Decomposition<?> decomposition, double eps) {
    this(decomposition, MA, eps);
  }

  /**
   * General constructor.
   * 
   * @param decomposition Matrix decomposition (see {@link DecompositionFactory} for list)
   * @param algebra  The matrix algebra (see {@link MatrixAlgebraFactory} for list)
   * @param eps Convergence tolerance
   */
  public NonLinearLeastSquareWithPenalty(Decomposition<?> decomposition, MatrixAlgebra algebra, double eps) {
    ArgChecker.notNull(decomposition, "decomposition");
    ArgChecker.notNull(algebra, "algebra");
    ArgChecker.isTrue(eps > 0, "must have positive eps");
    _decomposition = decomposition;
    _algebra = algebra;
    _eps = eps;
  }

  /**
   * Use this when the model is given as a function of its parameters only (i.e. a function that takes a set of
   * parameters and return a set of model values,
   * so the measurement points are already known to the function), and analytic parameter sensitivity is not available.
   * 
   * @param observedValues Set of measurement values
   * @param func The model as a function of its parameters only
   * @param startPos Initial value of the parameters
   * @param penalty Penalty matrix
   * @return value of the fitted parameters
   */
  public LeastSquareWithPenaltyResults solve(
      DoubleArray observedValues,
      Function<DoubleArray, DoubleArray> func,
      DoubleArray startPos,
      DoubleMatrix penalty) {

    int n = observedValues.size();
    VectorFieldFirstOrderDifferentiator jac = new VectorFieldFirstOrderDifferentiator();
    return solve(observedValues, DoubleArray.filled(n, 1.0), func, jac.differentiate(func), startPos, penalty);
  }

  /**
   * Use this when the model is given as a function of its parameters only (i.e. a function that takes a set of
   * parameters and return a set of model values,
   * so the measurement points are already known to the function), and analytic parameter sensitivity is not available
   * @param observedValues Set of measurement values
   * @param sigma Set of measurement errors
   * @param func The model as a function of its parameters only
   * @param startPos Initial value of the parameters
   * @param penalty Penalty matrix
   * @return value of the fitted parameters
   */
  public LeastSquareWithPenaltyResults solve(
      DoubleArray observedValues,
      DoubleArray sigma,
      Function<DoubleArray, DoubleArray> func,
      DoubleArray startPos,
      DoubleMatrix penalty) {

    VectorFieldFirstOrderDifferentiator jac = new VectorFieldFirstOrderDifferentiator();
    return solve(observedValues, sigma, func, jac.differentiate(func), startPos, penalty);
  }

  /**
   * Use this when the model is given as a function of its parameters only (i.e. a function that takes a set of
   * parameters and return a set of model values,
   * so the measurement points are already known to the function), and analytic parameter sensitivity is not available
   * @param observedValues Set of measurement values
   * @param sigma Set of measurement errors
   * @param func The model as a function of its parameters only
   * @param startPos Initial value of the parameters
   * @param penalty Penalty matrix
   * @param allowedValue a function which returned true if the new trial position is allowed by the model. An example
   *   would be to enforce positive parameters
   *   without resorting to a non-linear parameter transform. In some circumstances this approach will lead to slow
   *   convergence.
   * @return value of the fitted parameters
   */
  public LeastSquareWithPenaltyResults solve(
      DoubleArray observedValues,
      DoubleArray sigma,
      Function<DoubleArray, DoubleArray> func,
      DoubleArray startPos,
      DoubleMatrix penalty,
      Function<DoubleArray, Boolean> allowedValue) {

    VectorFieldFirstOrderDifferentiator jac = new VectorFieldFirstOrderDifferentiator();
    return solve(observedValues, sigma, func, jac.differentiate(func), startPos, penalty, allowedValue);
  }

  /**
   * Use this when the model is given as a function of its parameters only (i.e. a function that takes a set of
   * parameters and return a set of model values,
   * so the measurement points are already known to the function), and analytic parameter sensitivity is available
   * @param observedValues Set of measurement values
   * @param sigma Set of measurement errors
   * @param func The model as a function of its parameters only
   * @param jac The model sensitivity to its parameters (i.e. the Jacobian matrix) as a function of its parameters only
   * @param startPos Initial value of the parameters
   * @param penalty Penalty matrix
   * @return the least-square results
   */
  public LeastSquareWithPenaltyResults solve(
      DoubleArray observedValues,
      DoubleArray sigma,
      Function<DoubleArray, DoubleArray> func,
      Function<DoubleArray, DoubleMatrix> jac,
      DoubleArray startPos, DoubleMatrix penalty) {

    return solve(observedValues, sigma, func, jac, startPos, penalty, UNCONSTRAINED);
  }

  /**
   * Use this when the model is given as a function of its parameters only (i.e. a function that takes a set of
   * parameters and return a set of model values,
   * so the measurement points are already known to the function), and analytic parameter sensitivity is available
   * @param observedValues Set of measurement values
   * @param sigma Set of measurement errors
   * @param func The model as a function of its parameters only
   * @param jac The model sensitivity to its parameters (i.e. the Jacobian matrix) as a function of its parameters only
   * @param startPos Initial value of the parameters
   * @param penalty Penalty matrix (must be positive semi-definite)
   * @param allowedValue a function which returned true if the new trial position is allowed by the model. An example
   *   would be to enforce positive parameters
   *   without resorting to a non-linear parameter transform. In some circumstances this approach will lead to slow
   *   convergence.
   * @return the least-square results
   */
  public LeastSquareWithPenaltyResults solve(
      DoubleArray observedValues,
      DoubleArray sigma,
      Function<DoubleArray,
      DoubleArray> func,
      Function<DoubleArray, DoubleMatrix> jac,
      DoubleArray startPos,
      DoubleMatrix penalty,
      Function<DoubleArray, Boolean> allowedValue) {

    ArgChecker.notNull(observedValues, "observedValues");
    ArgChecker.notNull(sigma, " sigma");
    ArgChecker.notNull(func, " func");
    ArgChecker.notNull(jac, " jac");
    ArgChecker.notNull(startPos, "startPos");
    int nObs = observedValues.size();
    ArgChecker.isTrue(nObs == sigma.size(), "observedValues and sigma must be same length");
    ArgChecker.isTrue(allowedValue.apply(startPos),
        "The start position {} is not valid for this model. Please choose a valid start position", startPos);

    DoubleMatrix alpha;
    DecompositionResult decmp;
    DoubleArray theta = startPos;

    double lambda = 0.0; // TODO debug if the model is linear, it will be solved in 1 step
    double newChiSqr, oldChiSqr;
    DoubleArray error = getError(func, observedValues, sigma, theta);

    DoubleArray newError;
    DoubleMatrix jacobian = getJacobian(jac, sigma, theta);

    oldChiSqr = getChiSqr(error);
    double p = getANorm(penalty, theta);
    oldChiSqr += p;

    DoubleArray beta = getChiSqrGrad(error, jacobian);
    DoubleArray temp = (DoubleArray) _algebra.multiply(penalty, theta);
    beta = (DoubleArray) _algebra.subtract(beta, temp);

    for (int count = 0; count < MAX_ATTEMPTS; count++) {

      alpha = getModifiedCurvatureMatrix(jacobian, lambda, penalty);
      DoubleArray deltaTheta;

      try {
        decmp = _decomposition.apply(alpha);
        deltaTheta = decmp.solve(beta);
      } catch (Exception e) {
        throw new MathException(e);
      }

      DoubleArray trialTheta = (DoubleArray) _algebra.add(theta, deltaTheta);

      if (!allowedValue.apply(trialTheta)) {
        lambda = increaseLambda(lambda);
        continue;
      }

      newError = getError(func, observedValues, sigma, trialTheta);
      p = getANorm(penalty, trialTheta);
      newChiSqr = getChiSqr(newError);
      newChiSqr += p;

      // Check for convergence when no improvement in chiSqr occurs
      if (Math.abs(newChiSqr - oldChiSqr) / (1 + oldChiSqr) < _eps) {

        DoubleMatrix alpha0 = lambda == 0.0 ? alpha : getModifiedCurvatureMatrix(jacobian, 0.0, penalty);

        if (lambda > 0.0) {
          decmp = _decomposition.apply(alpha0);
        }
        return finish(alpha0, decmp, newChiSqr - p, p, jacobian, trialTheta, sigma);
      }

      if (newChiSqr < oldChiSqr) {
        lambda = decreaseLambda(lambda);
        theta = trialTheta;
        error = newError;
        jacobian = getJacobian(jac, sigma, trialTheta);
        beta = getChiSqrGrad(error, jacobian);
        temp = (DoubleArray) _algebra.multiply(penalty, theta);
        beta = (DoubleArray) _algebra.subtract(beta, temp);

        oldChiSqr = newChiSqr;
      } else {
        lambda = increaseLambda(lambda);
      }
    }
    throw new MathException("Could not converge in " + MAX_ATTEMPTS + " attempts");
  }

  private double decreaseLambda(double lambda) {
    return lambda / 10;
  }

  private double increaseLambda(double lambda) {
    if (lambda == 0.0) { // this will happen the first time a full quadratic step fails
      return 0.1;
    }
    return lambda * 10;
  }

  private LeastSquareWithPenaltyResults finish(
      DoubleMatrix alpha,
      DecompositionResult decmp,
      double chiSqr,
      double penalty,
      DoubleMatrix jacobian,
      DoubleArray newTheta,
      DoubleArray sigma) {

    DoubleMatrix covariance = decmp.solve(DoubleMatrix.identity(alpha.rowCount()));
    DoubleMatrix bT = getBTranspose(jacobian, sigma);
    DoubleMatrix inverseJacobian = decmp.solve(bT);
    return new LeastSquareWithPenaltyResults(chiSqr, penalty, newTheta, covariance, inverseJacobian);
  }

  private DoubleArray getError(
      Function<DoubleArray, DoubleArray> func,
      DoubleArray observedValues,
      DoubleArray sigma,
      DoubleArray theta) {

    int n = observedValues.size();
    DoubleArray modelValues = func.apply(theta);
    ArgChecker.isTrue(n == modelValues.size(),
        "Number of data points different between model (" + modelValues.size() + ") and observed (" + n + ")");
    return DoubleArray.of(n, i -> (observedValues.get(i) - modelValues.get(i)) / sigma.get(i));
  }

  private DoubleMatrix getBTranspose(DoubleMatrix jacobian, DoubleArray sigma) {
    int n = jacobian.rowCount();
    int m = jacobian.columnCount();

    DoubleMatrix res = DoubleMatrix.filled(m, n);
    double[][] data = res.toArray();
    for (int i = 0; i < n; i++) {
      double sigmaInv = 1.0 / sigma.get(i);
      for (int k = 0; k < m; k++) {
        data[k][i] = jacobian.get(i, k) * sigmaInv;
      }
    }
    return DoubleMatrix.ofUnsafe(data);
  }

  private DoubleMatrix getJacobian(Function<DoubleArray, DoubleMatrix> jac, DoubleArray sigma, DoubleArray theta) {
    DoubleMatrix res = jac.apply(theta);
    double[][] data = res.toArray();
    int n = res.rowCount();
    int m = res.columnCount();
    ArgChecker.isTrue(theta.size() == m, "Jacobian is wrong size");
    ArgChecker.isTrue(sigma.size() == n, "Jacobian is wrong size");

    for (int i = 0; i < n; i++) {
      double sigmaInv = 1.0 / sigma.get(i);
      for (int j = 0; j < m; j++) {
        data[i][j] *= sigmaInv;
      }
    }
    return DoubleMatrix.ofUnsafe(data);
  }

  private double getChiSqr(DoubleArray error) {
    return _algebra.getInnerProduct(error, error);
  }

  private DoubleArray getChiSqrGrad(DoubleArray error, DoubleMatrix jacobian) {
    return (DoubleArray) _algebra.multiply(error, jacobian);
  }

  private DoubleMatrix getModifiedCurvatureMatrix(DoubleMatrix jacobian, double lambda, DoubleMatrix penalty) {
    double onePLambda = 1.0 + lambda;
    int m = jacobian.columnCount();
    DoubleMatrix alpha = (DoubleMatrix) MA.add(MA.matrixTransposeMultiplyMatrix(jacobian), penalty);
    // scale the diagonal
    double[][] data = alpha.toArray();
    for (int i = 0; i < m; i++) {
      data[i][i] *= onePLambda;
    }
    return DoubleMatrix.ofUnsafe(data);
  }

  private double getANorm(DoubleMatrix a, DoubleArray x) {
    int n = x.size();
    double sum = 0.0;
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        sum += a.get(i, j) * x.get(i) * x.get(j);
      }
    }
    return sum;
  }

}
