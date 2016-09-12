/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.VectorFieldSecondOrderDifferentiator;
import com.opengamma.strata.math.impl.function.ParameterizedFunction;
import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionFactory;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionResult;
import com.opengamma.strata.math.impl.linearalgebra.SVDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.SVDecompositionResult;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory;

/**
 *
 */
public class NonLinearLeastSquare {

  private static final Logger LOGGER = LoggerFactory.getLogger(NonLinearLeastSquare.class);
  private static final int MAX_ATTEMPTS = 10000;
  private static final Function<DoubleArray, Boolean> UNCONSTRAINED = new Function<DoubleArray, Boolean>() {
    @Override
    public Boolean apply(DoubleArray x) {
      return true;
    }
  };

  private final double _eps;
  private final Decomposition<?> _decomposition;
  private final MatrixAlgebra _algebra;

  public NonLinearLeastSquare() {
    this(DecompositionFactory.SV_COMMONS, MatrixAlgebraFactory.OG_ALGEBRA, 1e-8);
  }

  public NonLinearLeastSquare(Decomposition<?> decomposition, MatrixAlgebra algebra, double eps) {
    _decomposition = decomposition;
    _algebra = algebra;
    _eps = eps;
  }

  //-------------------------------------------------------------------------
  /**
   * Use this when the model is in the ParameterizedFunction form and analytic parameter sensitivity is not available
   * @param x Set of measurement points
   * @param y Set of measurement values
   * @param func The model in ParameterizedFunction form (i.e. takes measurement points and a set of parameters and
   * returns a model value)
   * @param startPos Initial value of the parameters
   * @return A LeastSquareResults object
   */
  public LeastSquareResults solve(
      DoubleArray x,
      DoubleArray y,
      ParameterizedFunction<Double, DoubleArray, Double> func,
      DoubleArray startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    int n = x.size();
    ArgChecker.isTrue(y.size() == n, "y wrong length");
    return solve(x, y, DoubleArray.filled(n, 1), func, startPos);
  }

  /**
   * Use this when the model is in the ParameterizedFunction form and analytic parameter sensitivity is not available
   * but a measurement error is.
   * @param x Set of measurement points
   * @param y Set of measurement values
   * @param sigma y Set of measurement errors
   * @param func The model in ParameterizedFunction form (i.e. takes measurement points and a set of parameters and
   * returns a model value)
   * @param startPos Initial value of the parameters
   * @return A LeastSquareResults object
   */
  public LeastSquareResults solve(
      DoubleArray x,
      DoubleArray y,
      double sigma,
      ParameterizedFunction<Double, DoubleArray, Double> func,
      DoubleArray startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    ArgChecker.notNull(sigma, "sigma");
    int n = x.size();
    ArgChecker.isTrue(y.size() == n, "y wrong length");
    return solve(x, y, DoubleArray.filled(n, sigma), func, startPos);

  }

  /**
   * Use this when the model is in the ParameterizedFunction form and analytic parameter sensitivity is not available
   * but an array of measurements errors is.
   * @param x Set of measurement points
   * @param y Set of measurement values
   * @param sigma Set of measurement errors
   * @param func The model in ParameterizedFunction form (i.e. takes measurement points and a set of parameters and
   * returns a model value)
   * @param startPos Initial value of the parameters
   * @return A LeastSquareResults object
   */
  public LeastSquareResults solve(
      DoubleArray x,
      DoubleArray y,
      DoubleArray sigma,
      ParameterizedFunction<Double, DoubleArray, Double> func,
      DoubleArray startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    ArgChecker.notNull(sigma, "sigma");

    int n = x.size();
    ArgChecker.isTrue(y.size() == n, "y wrong length");
    ArgChecker.isTrue(sigma.size() == n, "sigma wrong length");

    Function<DoubleArray, DoubleArray> func1D = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray theta) {
        return DoubleArray.of(x.size(), i -> func.evaluate(x.get(i), theta));
      }
    };

    return solve(y, sigma, func1D, startPos, null);
  }

  /**
   * Use this when the model is in the ParameterizedFunction form and analytic parameter sensitivity
   * @param x Set of measurement points
   * @param y Set of measurement values
   * @param func The model in ParameterizedFunction form (i.e. takes a measurement points and a set of parameters and
   * returns a model value)
   * @param grad The model parameter sensitivities in ParameterizedFunction form (i.e. takes a measurement points and a
   * set of parameters and returns a model parameter sensitivities)
   * @param startPos Initial value of the parameters
   * @return value of the fitted parameters
   */
  public LeastSquareResults solve(
      DoubleArray x,
      DoubleArray y,
      ParameterizedFunction<Double, DoubleArray, Double> func,
      ParameterizedFunction<Double, DoubleArray, DoubleArray> grad,
      DoubleArray startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    ArgChecker.notNull(x, "sigma");
    int n = x.size();
    ArgChecker.isTrue(y.size() == n, "y wrong length");
    // emcleod 31-1-2011 arbitrary value 1 for now
    return solve(x, y, DoubleArray.filled(n, 1), func, grad, startPos);
  }

  /**
   * Use this when the model is in the ParameterizedFunction form and analytic parameter sensitivity and a single
   * measurement error are available
   * @param x Set of measurement points
   * @param y Set of measurement values
   * @param sigma Measurement errors
   * @param func The model in ParameterizedFunction form (i.e. takes a measurement points and a set of parameters and
   * returns a model value)
   * @param grad The model parameter sensitivities in ParameterizedFunction form (i.e. takes a measurement points and a
   * set of parameters and returns a model parameter sensitivities)
   * @param startPos Initial value of the parameters
   * @return value of the fitted parameters
   */
  public LeastSquareResults solve(
      DoubleArray x,
      DoubleArray y,
      double sigma,
      ParameterizedFunction<Double, DoubleArray, Double> func,
      ParameterizedFunction<Double, DoubleArray, DoubleArray> grad,
      DoubleArray startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    int n = x.size();
    ArgChecker.isTrue(y.size() == n, "y wrong length");
    return solve(x, y, DoubleArray.filled(n, sigma), func, grad, startPos);
  }

  /**
   * Use this when the model is in the ParameterizedFunction form and analytic parameter sensitivity and measurement
   * errors are available
   * @param x Set of measurement points
   * @param y Set of measurement values
   * @param sigma Set of measurement errors
   * @param func The model in ParameterizedFunction form (i.e. takes a measurement points and a set of parameters and
   * returns a model value)
   * @param grad The model parameter sensitivities in ParameterizedFunction form (i.e. takes a measurement points and a
   * set of parameters and returns a model parameter sensitivities)
   * @param startPos Initial value of the parameters
   * @return value of the fitted parameters
   */
  public LeastSquareResults solve(
      DoubleArray x,
      DoubleArray y,
      DoubleArray sigma,
      ParameterizedFunction<Double, DoubleArray, Double> func,
      ParameterizedFunction<Double, DoubleArray, DoubleArray> grad,
      DoubleArray startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    ArgChecker.notNull(x, "sigma");

    int n = x.size();
    ArgChecker.isTrue(y.size() == n, "y wrong length");
    ArgChecker.isTrue(sigma.size() == n, "sigma wrong length");

    Function<DoubleArray, DoubleArray> func1D = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray theta) {
        return DoubleArray.of(x.size(), i -> func.evaluate(x.get(i), theta));
      }
    };

    Function<DoubleArray, DoubleMatrix> jac = new Function<DoubleArray, DoubleMatrix>() {
      @Override
      public DoubleMatrix apply(DoubleArray theta) {
        int m = x.size();
        double[][] res = new double[m][];
        for (int i = 0; i < m; i++) {
          DoubleArray temp = grad.evaluate(x.get(i), theta);
          res[i] = temp.toArray();
        }
        return DoubleMatrix.copyOf(res);
      }
    };

    return solve(y, sigma, func1D, jac, startPos, null);
  }

  /**
   * Use this when the model is given as a function of its parameters only (i.e. a function that takes a set of
   * parameters and return a set of model values,
   * so the measurement points are already known to the function), and analytic parameter sensitivity is not available
   * @param observedValues Set of measurement values
   * @param func The model as a function of its parameters only
   * @param startPos Initial value of the parameters
   * @return value of the fitted parameters
   */
  public LeastSquareResults solve(
      DoubleArray observedValues,
      Function<DoubleArray, DoubleArray> func,
      DoubleArray startPos) {

    int n = observedValues.size();
    VectorFieldFirstOrderDifferentiator jac = new VectorFieldFirstOrderDifferentiator();
    return solve(observedValues, DoubleArray.filled(n, 1.0), func, jac.differentiate(func), startPos, null);
  }

  /**
   * Use this when the model is given as a function of its parameters only (i.e. a function that takes a set of
   * parameters and return a set of model values,
   * so the measurement points are already known to the function), and analytic parameter sensitivity is not available
   * @param observedValues Set of measurement values
   * @param sigma Set of measurement errors
   * @param func The model as a function of its parameters only
   * @param startPos Initial value of the parameters
   * @return value of the fitted parameters
   */
  public LeastSquareResults solve(
      DoubleArray observedValues,
      DoubleArray sigma,
      Function<DoubleArray, DoubleArray> func,
      DoubleArray startPos) {

    VectorFieldFirstOrderDifferentiator jac = new VectorFieldFirstOrderDifferentiator();
    return solve(observedValues, sigma, func, jac.differentiate(func), startPos, null);
  }

  /**
   * Use this when the model is given as a function of its parameters only (i.e. a function that takes a set of
   * parameters and return a set of model values,
   * so the measurement points are already known to the function), and analytic parameter sensitivity is not available
   * @param observedValues Set of measurement values
   * @param sigma Set of measurement errors
   * @param func The model as a function of its parameters only
   * @param startPos Initial value of the parameters
   * @param maxJumps A vector containing the maximum absolute allowed step in a particular direction in each iteration.
   * Can be null, in which case no constant
   * on the step size is applied.
   * @return value of the fitted parameters
   */
  public LeastSquareResults solve(
      DoubleArray observedValues,
      DoubleArray sigma,
      Function<DoubleArray, DoubleArray> func,
      DoubleArray startPos,
      DoubleArray maxJumps) {

    VectorFieldFirstOrderDifferentiator jac = new VectorFieldFirstOrderDifferentiator();
    return solve(observedValues, sigma, func, jac.differentiate(func), startPos, maxJumps);
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
   * @return value of the fitted parameters
   */
  public LeastSquareResults solve(
      DoubleArray observedValues,
      DoubleArray sigma,
      Function<DoubleArray, DoubleArray> func,
      Function<DoubleArray, DoubleMatrix> jac, DoubleArray startPos) {

    return solve(observedValues, sigma, func, jac, startPos, UNCONSTRAINED, null);
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
   * @param maxJumps A vector containing the maximum absolute allowed step in a particular direction in each iteration.
   * Can be null, in which case on constant
   * on the step size is applied.
   * @return value of the fitted parameters
   */
  public LeastSquareResults solve(
      DoubleArray observedValues,
      DoubleArray sigma,
      Function<DoubleArray, DoubleArray> func,
      Function<DoubleArray, DoubleMatrix> jac,
      DoubleArray startPos,
      DoubleArray maxJumps) {

    return solve(observedValues, sigma, func, jac, startPos, UNCONSTRAINED, maxJumps);
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
   * @param constraints A function that returns true if the trial point is within the constraints of the model
   * @param maxJumps A vector containing the maximum absolute allowed step in a particular direction in each iteration.
   * Can be null, in which case on constant
   * on the step size is applied.
   * @return value of the fitted parameters
   */
  public LeastSquareResults solve(
      DoubleArray observedValues,
      DoubleArray sigma,
      Function<DoubleArray, DoubleArray> func,
      Function<DoubleArray, DoubleMatrix> jac,
      DoubleArray startPos,
      Function<DoubleArray, Boolean> constraints,
      DoubleArray maxJumps) {

    ArgChecker.notNull(observedValues, "observedValues");
    ArgChecker.notNull(sigma, " sigma");
    ArgChecker.notNull(func, " func");
    ArgChecker.notNull(jac, " jac");
    ArgChecker.notNull(startPos, "startPos");
    int nObs = observedValues.size();
    int nParms = startPos.size();
    ArgChecker.isTrue(nObs == sigma.size(), "observedValues and sigma must be same length");
    ArgChecker.isTrue(nObs >= nParms,
        "must have data points greater or equal to number of parameters. #date points = {}, #parameters = {}", nObs, nParms);
    ArgChecker.isTrue(constraints.apply(startPos),
        "The inital value of the parameters (startPos) is {} - this is not an allowed value", startPos);
    DoubleMatrix alpha;
    DecompositionResult decmp;
    DoubleArray theta = startPos;

    double lambda = 0.0; // TODO debug if the model is linear, it will be solved in 1 step
    double newChiSqr, oldChiSqr;
    DoubleArray error = getError(func, observedValues, sigma, theta);

    DoubleArray newError;
    DoubleMatrix jacobian = getJacobian(jac, sigma, theta);
    oldChiSqr = getChiSqr(error);

    // If we start at the solution we are done
    if (oldChiSqr == 0.0) {
      return finish(oldChiSqr, jacobian, theta, sigma);
    }

    DoubleArray beta = getChiSqrGrad(error, jacobian);

    for (int count = 0; count < MAX_ATTEMPTS; count++) {
      alpha = getModifiedCurvatureMatrix(jacobian, lambda);

      DoubleArray deltaTheta;
      try {
        decmp = _decomposition.apply(alpha);
        deltaTheta = decmp.solve(beta);
      } catch (Exception e) {
        throw new MathException(e);
      }

      DoubleArray trialTheta = (DoubleArray) _algebra.add(theta, deltaTheta);

      // acceptable step is found
      if (!constraints.apply(trialTheta) || !allowJump(deltaTheta, maxJumps)) {
        lambda = increaseLambda(lambda);
        continue;
      }

      newError = getError(func, observedValues, sigma, trialTheta);
      newChiSqr = getChiSqr(newError);

      // Check for convergence when no improvement in chiSqr occurs
      if (Math.abs(newChiSqr - oldChiSqr) / (1 + oldChiSqr) < _eps) {

        DoubleMatrix alpha0 = lambda == 0.0 ? alpha : getModifiedCurvatureMatrix(jacobian, 0.0);

        // if the model is an exact fit to the data, then no more improvement is possible
        if (newChiSqr < _eps) {
          if (lambda > 0.0) {
            decmp = _decomposition.apply(alpha0);
          }
          return finish(alpha0, decmp, newChiSqr, jacobian, trialTheta, sigma);
        }

        SVDecompositionCommons svd = (SVDecompositionCommons) DecompositionFactory.SV_COMMONS;

        // add the second derivative information to the Hessian matrix to check we are not at a local maximum or saddle
        // point
        VectorFieldSecondOrderDifferentiator diff = new VectorFieldSecondOrderDifferentiator();
        Function<DoubleArray, DoubleMatrix[]> secDivFunc = diff.differentiate(func, constraints);
        DoubleMatrix[] secDiv = secDivFunc.apply(trialTheta);
        double[][] temp = new double[nParms][nParms];
        for (int i = 0; i < nObs; i++) {
          for (int j = 0; j < nParms; j++) {
            for (int k = 0; k < nParms; k++) {
              temp[j][k] -= newError.get(i) * secDiv[i].get(j, k) / sigma.get(i);
            }
          }
        }
        DoubleMatrix newAlpha = (DoubleMatrix) _algebra.add(alpha0, DoubleMatrix.copyOf(temp));

        SVDecompositionResult svdRes = svd.apply(newAlpha);
        double[] w = svdRes.getSingularValues();
        DoubleMatrix u = svdRes.getU();
        DoubleMatrix v = svdRes.getV();

        double[] p = new double[nParms];
        boolean saddle = false;

        double sum = 0.0;
        for (int i = 0; i < nParms; i++) {
          double a = 0.0;
          for (int j = 0; j < nParms; j++) {
            a += u.get(j, i) * v.get(j, i);
          }
          int sign = a > 0.0 ? 1 : -1;
          if (w[i] * sign < 0.0) {
            sum += w[i];
            w[i] = -w[i];
            saddle = true;
          }
        }

        // if a local maximum or saddle point is found (as indicated by negative eigenvalues), move in a direction that
        // is a weighted
        // sum of the eigenvectors corresponding to the negative eigenvalues
        if (saddle) {
          lambda = increaseLambda(lambda);
          for (int i = 0; i < nParms; i++) {
            if (w[i] < 0.0) {
              double scale = 0.5 * Math.sqrt(-oldChiSqr * w[i]) / sum;
              for (int j = 0; j < nParms; j++) {
                p[j] += scale * u.get(j, i);
              }
            }
          }
          DoubleArray direction = DoubleArray.copyOf(p);
          deltaTheta = direction;
          trialTheta = (DoubleArray) _algebra.add(theta, deltaTheta);
          int i = 0;
          double scale = 1.0;
          while (!constraints.apply(trialTheta)) {
            scale *= -0.5;
            deltaTheta = (DoubleArray) _algebra.scale(direction, scale);
            trialTheta = (DoubleArray) _algebra.add(theta, deltaTheta);
            i++;
            if (i > 10) {
              throw new MathException("Could not satify constraint");
            }
          }

          newError = getError(func, observedValues, sigma, trialTheta);
          newChiSqr = getChiSqr(newError);

          int counter = 0;
          while (newChiSqr > oldChiSqr) {
            // if even a tiny move along the negative eigenvalue cannot improve chiSqr, then exit
            if (counter > 10 || Math.abs(newChiSqr - oldChiSqr) / (1 + oldChiSqr) < _eps) {
              LOGGER.warn("Saddle point detected, but no improvement to chi^2 possible by moving away. " +
                  "It is recommended that a different starting point is used.");
              return finish(newAlpha, decmp, oldChiSqr, jacobian, theta, sigma);
            }
            scale /= 2.0;
            deltaTheta = (DoubleArray) _algebra.scale(direction, scale);
            trialTheta = (DoubleArray) _algebra.add(theta, deltaTheta);
            newError = getError(func, observedValues, sigma, trialTheta);
            newChiSqr = getChiSqr(newError);
            counter++;
          }
        } else {
          // this should be the normal finish - i.e. no improvement in chiSqr and at a true minimum (although there is
          // no guarantee it is not a local minimum)
          return finish(newAlpha, decmp, newChiSqr, jacobian, trialTheta, sigma);
        }
      }

      if (newChiSqr < oldChiSqr) {
        lambda = decreaseLambda(lambda);
        theta = trialTheta;
        error = newError;
        jacobian = getJacobian(jac, sigma, trialTheta);
        beta = getChiSqrGrad(error, jacobian);
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

  private boolean allowJump(DoubleArray deltaTheta, DoubleArray maxJumps) {
    if (maxJumps == null) {
      return true;
    }
    int n = deltaTheta.size();
    for (int i = 0; i < n; i++) {
      if (Math.abs(deltaTheta.get(i)) > maxJumps.get(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * 
   * the inverse-Jacobian where the i-j entry is the sensitivity of the ith (fitted) parameter (a_i) to the jth data
   * point (y_j).
   * @param sigma Set of measurement errors
   * @param func The model as a function of its parameters only
   * @param jac The model sensitivity to its parameters (i.e. the Jacobian matrix) as a function of its parameters only
   * @param originalSolution The value of the parameters at a converged solution
   * @return inverse-Jacobian
   */
  public DoubleMatrix calInverseJacobian(
      DoubleArray sigma,
      Function<DoubleArray, DoubleArray> func,
      Function<DoubleArray, DoubleMatrix> jac,
      DoubleArray originalSolution) {

    DoubleMatrix jacobian = getJacobian(jac, sigma, originalSolution);
    DoubleMatrix a = getModifiedCurvatureMatrix(jacobian, 0.0);
    DoubleMatrix bT = getBTranspose(jacobian, sigma);
    DecompositionResult decRes = _decomposition.apply(a);
    return decRes.solve(bT);
  }

  private LeastSquareResults finish(
      double newChiSqr,
      DoubleMatrix jacobian,
      DoubleArray newTheta,
      DoubleArray sigma) {

    DoubleMatrix alpha = getModifiedCurvatureMatrix(jacobian, 0.0);
    DecompositionResult decmp = _decomposition.apply(alpha);
    return finish(alpha, decmp, newChiSqr, jacobian, newTheta, sigma);
  }

  private LeastSquareResults finish(
      DoubleMatrix alpha,
      DecompositionResult decmp,
      double newChiSqr,
      DoubleMatrix jacobian,
      DoubleArray newTheta,
      DoubleArray sigma) {

    DoubleMatrix covariance = decmp.solve(DoubleMatrix.identity(alpha.rowCount()));
    DoubleMatrix bT = getBTranspose(jacobian, sigma);
    DoubleMatrix inverseJacobian = decmp.solve(bT);
    return new LeastSquareResults(newChiSqr, newTheta, covariance, inverseJacobian);
  }

  private DoubleArray getError(final Function<DoubleArray, DoubleArray> func, final DoubleArray observedValues, final DoubleArray sigma, final DoubleArray theta) {
    int n = observedValues.size();
    DoubleArray modelValues = func.apply(theta);
    ArgChecker.isTrue(n == modelValues.size(), "Number of data points different between model (" + modelValues.size() + ") and observed (" + n + ")");
    return DoubleArray.of(n, i -> (observedValues.get(i) - modelValues.get(i)) / sigma.get(i));
  }

  private DoubleMatrix getBTranspose(DoubleMatrix jacobian, DoubleArray sigma) {
    int n = jacobian.rowCount();
    int m = jacobian.columnCount();

    double[][] res = new double[m][n];

    for (int i = 0; i < n; i++) {
      double sigmaInv = 1.0 / sigma.get(i);
      for (int k = 0; k < m; k++) {
        res[k][i] = jacobian.get(i, k) * sigmaInv;
      }
    }
    return DoubleMatrix.copyOf(res);
  }

  private DoubleMatrix getJacobian(final Function<DoubleArray, DoubleMatrix> jac, final DoubleArray sigma, final DoubleArray theta) {
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

  @SuppressWarnings("unused")
  private DoubleArray getDiagonalCurvatureMatrix(DoubleMatrix jacobian) {
    int n = jacobian.rowCount();
    int m = jacobian.columnCount();

    double[] alpha = new double[m];

    for (int i = 0; i < m; i++) {
      double sum = 0.0;
      for (int k = 0; k < n; k++) {
        sum += FunctionUtils.square(jacobian.get(k, i));
      }
      alpha[i] = sum;
    }
    return DoubleArray.copyOf(alpha);
  }

  private DoubleMatrix getModifiedCurvatureMatrix(DoubleMatrix jacobian, double lambda) {

    int m = jacobian.columnCount();
    double onePLambda = 1.0 + lambda;
    DoubleMatrix alpha = _algebra.matrixTransposeMultiplyMatrix(jacobian);
    // scale the diagonal
    double[][] data = alpha.toArray();
    for (int i = 0; i < m; i++) {
      data[i][i] *= onePLambda;
    }
    return DoubleMatrix.ofUnsafe(data);
  }

}
