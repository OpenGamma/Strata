/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.VectorFieldSecondOrderDifferentiator;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.function.ParameterizedFunction;
import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionFactory;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionResult;
import com.opengamma.strata.math.impl.linearalgebra.SVDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.SVDecompositionResult;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrixUtils;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory;

/**
 *
 */
public class NonLinearLeastSquare {

  private static final Logger LOGGER = LoggerFactory.getLogger(NonLinearLeastSquare.class);
  private static final int MAX_ATTEMPTS = 10000;
  private static final Function1D<DoubleMatrix1D, Boolean> UNCONSTRAINED = new Function1D<DoubleMatrix1D, Boolean>() {
    @Override
    public Boolean evaluate(DoubleMatrix1D x) {
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
      DoubleMatrix1D x,
      DoubleMatrix1D y,
      ParameterizedFunction<Double, DoubleMatrix1D, Double> func,
      DoubleMatrix1D startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    int n = x.getNumberOfElements();
    ArgChecker.isTrue(y.getNumberOfElements() == n, "y wrong length");
    double[] sigmas = new double[n];
    Arrays.fill(sigmas, 1);
    return solve(x, y, new DoubleMatrix1D(sigmas), func, startPos);
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
      DoubleMatrix1D x,
      DoubleMatrix1D y,
      double sigma,
      ParameterizedFunction<Double, DoubleMatrix1D, Double> func,
      DoubleMatrix1D startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    ArgChecker.notNull(sigma, "sigma");
    int n = x.getNumberOfElements();
    ArgChecker.isTrue(y.getNumberOfElements() == n, "y wrong length");
    double[] sigmas = new double[n];
    Arrays.fill(sigmas, sigma);
    return solve(x, y, new DoubleMatrix1D(sigmas), func, startPos);

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
      DoubleMatrix1D x,
      DoubleMatrix1D y,
      DoubleMatrix1D sigma,
      ParameterizedFunction<Double, DoubleMatrix1D, Double> func,
      DoubleMatrix1D startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    ArgChecker.notNull(sigma, "sigma");

    int n = x.getNumberOfElements();
    ArgChecker.isTrue(y.getNumberOfElements() == n, "y wrong length");
    ArgChecker.isTrue(sigma.getNumberOfElements() == n, "sigma wrong length");

    Function1D<DoubleMatrix1D, DoubleMatrix1D> func1D = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

      @Override
      public DoubleMatrix1D evaluate(DoubleMatrix1D theta) {
        int m = x.getNumberOfElements();
        double[] res = new double[m];
        for (int i = 0; i < m; i++) {
          res[i] = func.evaluate(x.getEntry(i), theta);
        }
        return new DoubleMatrix1D(res);
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
      DoubleMatrix1D x,
      DoubleMatrix1D y,
      ParameterizedFunction<Double, DoubleMatrix1D, Double> func,
      ParameterizedFunction<Double, DoubleMatrix1D, DoubleMatrix1D> grad,
      DoubleMatrix1D startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    ArgChecker.notNull(x, "sigma");
    int n = x.getNumberOfElements();
    ArgChecker.isTrue(y.getNumberOfElements() == n, "y wrong length");
    double[] sigmas = new double[n];
    Arrays.fill(sigmas, 1); // emcleod 31-1-2011 arbitrary value for now
    return solve(x, y, new DoubleMatrix1D(sigmas), func, grad, startPos);
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
      DoubleMatrix1D x,
      DoubleMatrix1D y,
      double sigma,
      ParameterizedFunction<Double, DoubleMatrix1D, Double> func,
      ParameterizedFunction<Double, DoubleMatrix1D, DoubleMatrix1D> grad,
      DoubleMatrix1D startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    int n = x.getNumberOfElements();
    ArgChecker.isTrue(y.getNumberOfElements() == n, "y wrong length");
    double[] sigmas = new double[n];
    Arrays.fill(sigmas, sigma);
    return solve(x, y, new DoubleMatrix1D(sigmas), func, grad, startPos);
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
      DoubleMatrix1D x,
      DoubleMatrix1D y,
      DoubleMatrix1D sigma,
      ParameterizedFunction<Double, DoubleMatrix1D, Double> func,
      ParameterizedFunction<Double, DoubleMatrix1D, DoubleMatrix1D> grad,
      DoubleMatrix1D startPos) {

    ArgChecker.notNull(x, "x");
    ArgChecker.notNull(y, "y");
    ArgChecker.notNull(x, "sigma");

    int n = x.getNumberOfElements();
    ArgChecker.isTrue(y.getNumberOfElements() == n, "y wrong length");
    ArgChecker.isTrue(sigma.getNumberOfElements() == n, "sigma wrong length");

    Function1D<DoubleMatrix1D, DoubleMatrix1D> func1D = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {
      @Override
      public DoubleMatrix1D evaluate(DoubleMatrix1D theta) {
        int m = x.getNumberOfElements();
        double[] res = new double[m];
        for (int i = 0; i < m; i++) {
          res[i] = func.evaluate(x.getEntry(i), theta);
        }
        return new DoubleMatrix1D(res);
      }
    };

    Function1D<DoubleMatrix1D, DoubleMatrix2D> jac = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
      @Override
      public DoubleMatrix2D evaluate(DoubleMatrix1D theta) {
        int m = x.getNumberOfElements();
        double[][] res = new double[m][];
        for (int i = 0; i < m; i++) {
          DoubleMatrix1D temp = grad.evaluate(x.getEntry(i), theta);
          res[i] = temp.getData();
        }
        return new DoubleMatrix2D(res);
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
      DoubleMatrix1D observedValues,
      Function1D<DoubleMatrix1D, DoubleMatrix1D> func,
      DoubleMatrix1D startPos) {

    int n = observedValues.getNumberOfElements();
    VectorFieldFirstOrderDifferentiator jac = new VectorFieldFirstOrderDifferentiator();
    return solve(observedValues, new DoubleMatrix1D(n, 1.0), func, jac.differentiate(func), startPos, null);
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
      DoubleMatrix1D observedValues,
      DoubleMatrix1D sigma,
      Function1D<DoubleMatrix1D, DoubleMatrix1D> func,
      DoubleMatrix1D startPos) {

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
      DoubleMatrix1D observedValues,
      DoubleMatrix1D sigma,
      Function1D<DoubleMatrix1D, DoubleMatrix1D> func,
      DoubleMatrix1D startPos,
      DoubleMatrix1D maxJumps) {

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
      DoubleMatrix1D observedValues,
      DoubleMatrix1D sigma,
      Function1D<DoubleMatrix1D, DoubleMatrix1D> func,
      Function1D<DoubleMatrix1D, DoubleMatrix2D> jac, DoubleMatrix1D startPos) {

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
      DoubleMatrix1D observedValues,
      DoubleMatrix1D sigma,
      Function1D<DoubleMatrix1D, DoubleMatrix1D> func,
      Function1D<DoubleMatrix1D, DoubleMatrix2D> jac,
      DoubleMatrix1D startPos,
      DoubleMatrix1D maxJumps) {

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
      DoubleMatrix1D observedValues,
      DoubleMatrix1D sigma,
      Function1D<DoubleMatrix1D, DoubleMatrix1D> func,
      Function1D<DoubleMatrix1D, DoubleMatrix2D> jac,
      DoubleMatrix1D startPos,
      Function1D<DoubleMatrix1D, Boolean> constraints,
      DoubleMatrix1D maxJumps) {

    ArgChecker.notNull(observedValues, "observedValues");
    ArgChecker.notNull(sigma, " sigma");
    ArgChecker.notNull(func, " func");
    ArgChecker.notNull(jac, " jac");
    ArgChecker.notNull(startPos, "startPos");
    int nObs = observedValues.getNumberOfElements();
    int nParms = startPos.getNumberOfElements();
    ArgChecker.isTrue(nObs == sigma.getNumberOfElements(), "observedValues and sigma must be same length");
    ArgChecker.isTrue(nObs >= nParms,
        "must have data points greater or equal to number of parameters. #date points = {}, #parameters = {}", nObs, nParms);
    ArgChecker.isTrue(constraints.evaluate(startPos),
        "The inital value of the parameters (startPos) is {} - this is not an allowed value", startPos);
    DoubleMatrix2D alpha;
    DecompositionResult decmp;
    DoubleMatrix1D theta = startPos;

    double lambda = 0.0; // TODO debug if the model is linear, it will be solved in 1 step
    double newChiSqr, oldChiSqr;
    DoubleMatrix1D error = getError(func, observedValues, sigma, theta);

    DoubleMatrix1D newError;
    DoubleMatrix2D jacobian = getJacobian(jac, sigma, theta);
    oldChiSqr = getChiSqr(error);

    // If we start at the solution we are done
    if (oldChiSqr == 0.0) {
      return finish(oldChiSqr, jacobian, theta, sigma);
    }

    DoubleMatrix1D beta = getChiSqrGrad(error, jacobian);

    for (int count = 0; count < MAX_ATTEMPTS; count++) {
      alpha = getModifiedCurvatureMatrix(jacobian, lambda);

      DoubleMatrix1D deltaTheta;
      try {
        decmp = _decomposition.evaluate(alpha);
        deltaTheta = decmp.solve(beta);
      } catch (Exception e) {
        throw new MathException(e);
      }

      DoubleMatrix1D trialTheta = (DoubleMatrix1D) _algebra.add(theta, deltaTheta);

      // acceptable step is found
      if (!constraints.evaluate(trialTheta) || !allowJump(deltaTheta, maxJumps)) {
        lambda = increaseLambda(lambda);
        continue;
      }

      newError = getError(func, observedValues, sigma, trialTheta);
      newChiSqr = getChiSqr(newError);

      // Check for convergence when no improvement in chiSqr occurs
      if (Math.abs(newChiSqr - oldChiSqr) / (1 + oldChiSqr) < _eps) {

        DoubleMatrix2D alpha0 = lambda == 0.0 ? alpha : getModifiedCurvatureMatrix(jacobian, 0.0);

        // if the model is an exact fit to the data, then no more improvement is possible
        if (newChiSqr < _eps) {
          if (lambda > 0.0) {
            decmp = _decomposition.evaluate(alpha0);
          }
          return finish(alpha0, decmp, newChiSqr, jacobian, trialTheta, sigma);
        }

        SVDecompositionCommons svd = (SVDecompositionCommons) DecompositionFactory.SV_COMMONS;

        // add the second derivative information to the Hessian matrix to check we are not at a local maximum or saddle
        // point
        VectorFieldSecondOrderDifferentiator diff = new VectorFieldSecondOrderDifferentiator();
        Function1D<DoubleMatrix1D, DoubleMatrix2D[]> secDivFunc = diff.differentiate(func, constraints);
        DoubleMatrix2D[] secDiv = secDivFunc.evaluate(trialTheta);
        double[][] temp = new double[nParms][nParms];
        for (int i = 0; i < nObs; i++) {
          for (int j = 0; j < nParms; j++) {
            for (int k = 0; k < nParms; k++) {
              temp[j][k] -= newError.getEntry(i) * secDiv[i].getEntry(j, k) / sigma.getEntry(i);
            }
          }
        }
        DoubleMatrix2D newAlpha = (DoubleMatrix2D) _algebra.add(alpha0, new DoubleMatrix2D(temp));

        SVDecompositionResult svdRes = svd.evaluate(newAlpha);
        double[] w = svdRes.getSingularValues();
        DoubleMatrix2D u = svdRes.getU();
        DoubleMatrix2D v = svdRes.getV();

        double[] p = new double[nParms];
        boolean saddle = false;

        double sum = 0.0;
        for (int i = 0; i < nParms; i++) {
          double a = 0.0;
          for (int j = 0; j < nParms; j++) {
            a += u.getEntry(j, i) * v.getEntry(j, i);
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
                p[j] += scale * u.getEntry(j, i);
              }
            }
          }
          DoubleMatrix1D direction = new DoubleMatrix1D(p);
          deltaTheta = direction;
          trialTheta = (DoubleMatrix1D) _algebra.add(theta, deltaTheta);
          int i = 0;
          double scale = 1.0;
          while (!constraints.evaluate(trialTheta)) {
            scale *= -0.5;
            deltaTheta = (DoubleMatrix1D) _algebra.scale(direction, scale);
            trialTheta = (DoubleMatrix1D) _algebra.add(theta, deltaTheta);
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
              LOGGER
                  .warn("Saddle point detected, but no improvement to chi^2 possible by moving away. It is recommended that a different starting point is used.");
              return finish(newAlpha, decmp, oldChiSqr, jacobian, theta, sigma);
            }
            scale /= 2.0;
            deltaTheta = (DoubleMatrix1D) _algebra.scale(direction, scale);
            trialTheta = (DoubleMatrix1D) _algebra.add(theta, deltaTheta);
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

  private boolean allowJump(DoubleMatrix1D deltaTheta, DoubleMatrix1D maxJumps) {
    if (maxJumps == null) {
      return true;
    }
    int n = deltaTheta.getNumberOfElements();
    for (int i = 0; i < n; i++) {
      if (Math.abs(deltaTheta.getEntry(i)) > maxJumps.getEntry(i)) {
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
  public DoubleMatrix2D calInverseJacobian(
      DoubleMatrix1D sigma,
      Function1D<DoubleMatrix1D, DoubleMatrix1D> func,
      Function1D<DoubleMatrix1D, DoubleMatrix2D> jac,
      DoubleMatrix1D originalSolution) {

    DoubleMatrix2D jacobian = getJacobian(jac, sigma, originalSolution);
    DoubleMatrix2D a = getModifiedCurvatureMatrix(jacobian, 0.0);
    DoubleMatrix2D bT = getBTranspose(jacobian, sigma);
    DecompositionResult decRes = _decomposition.evaluate(a);
    return decRes.solve(bT);
  }

  private LeastSquareResults finish(
      double newChiSqr,
      DoubleMatrix2D jacobian,
      DoubleMatrix1D newTheta,
      DoubleMatrix1D sigma) {

    DoubleMatrix2D alpha = getModifiedCurvatureMatrix(jacobian, 0.0);
    DecompositionResult decmp = _decomposition.evaluate(alpha);
    return finish(alpha, decmp, newChiSqr, jacobian, newTheta, sigma);
  }

  private LeastSquareResults finish(
      DoubleMatrix2D alpha,
      DecompositionResult decmp,
      double newChiSqr,
      DoubleMatrix2D jacobian,
      DoubleMatrix1D newTheta,
      DoubleMatrix1D sigma) {

    DoubleMatrix2D covariance = decmp.solve(DoubleMatrixUtils.getIdentityMatrix2D(alpha.getNumberOfRows()));
    DoubleMatrix2D bT = getBTranspose(jacobian, sigma);
    DoubleMatrix2D inverseJacobian = decmp.solve(bT);
    return new LeastSquareResults(newChiSqr, newTheta, covariance, inverseJacobian);
  }

  private DoubleMatrix1D getError(
      Function1D<DoubleMatrix1D,
      DoubleMatrix1D> func,
      DoubleMatrix1D observedValues,
      DoubleMatrix1D sigma, DoubleMatrix1D theta) {

    int n = observedValues.getNumberOfElements();
    DoubleMatrix1D modelValues = func.evaluate(theta);
    ArgChecker.isTrue(n == modelValues.getNumberOfElements(),
        "Number of data points different between model (" + modelValues.getNumberOfElements() + ") and observed (" + n + ")");
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = (observedValues.getEntry(i) - modelValues.getEntry(i)) / sigma.getEntry(i);
    }

    return new DoubleMatrix1D(res);
  }

  private DoubleMatrix2D getBTranspose(DoubleMatrix2D jacobian, DoubleMatrix1D sigma) {
    int n = jacobian.getNumberOfRows();
    int m = jacobian.getNumberOfColumns();

    double[][] res = new double[m][n];

    for (int i = 0; i < n; i++) {
      double sigmaInv = 1.0 / sigma.getEntry(i);
      for (int k = 0; k < m; k++) {
        res[k][i] = jacobian.getEntry(i, k) * sigmaInv;
      }
    }
    return new DoubleMatrix2D(res);
  }

  private DoubleMatrix2D getJacobian(
      Function1D<DoubleMatrix1D,
      DoubleMatrix2D> jac,
      DoubleMatrix1D sigma,
      DoubleMatrix1D theta) {

    DoubleMatrix2D res = jac.evaluate(theta);
    double[][] data = res.getData();
    int n = res.getNumberOfRows();
    int m = res.getNumberOfColumns();
    ArgChecker.isTrue(theta.getNumberOfElements() == m, "Jacobian is wrong size");
    ArgChecker.isTrue(sigma.getNumberOfElements() == n, "Jacobian is wrong size");

    for (int i = 0; i < n; i++) {
      double sigmaInv = 1.0 / sigma.getEntry(i);
      for (int j = 0; j < m; j++) {
        data[i][j] *= sigmaInv;
      }
    }
    return res;
  }

  private double getChiSqr(DoubleMatrix1D error) {
    return _algebra.getInnerProduct(error, error);
  }

  private DoubleMatrix1D getChiSqrGrad(DoubleMatrix1D error, DoubleMatrix2D jacobian) {
    return (DoubleMatrix1D) _algebra.multiply(error, jacobian);
  }

  @SuppressWarnings("unused")
  private DoubleMatrix1D getDiagonalCurvatureMatrix(DoubleMatrix2D jacobian) {
    int n = jacobian.getNumberOfRows();
    int m = jacobian.getNumberOfColumns();

    double[] alpha = new double[m];

    for (int i = 0; i < m; i++) {
      double sum = 0.0;
      for (int k = 0; k < n; k++) {
        sum += FunctionUtils.square(jacobian.getEntry(k, i));
      }
      alpha[i] = sum;
    }
    return new DoubleMatrix1D(alpha);
  }

  private DoubleMatrix2D getModifiedCurvatureMatrix(DoubleMatrix2D jacobian, double lambda) {

    int m = jacobian.getNumberOfColumns();
    double onePLambda = 1.0 + lambda;
    DoubleMatrix2D alpha = _algebra.matrixTransposeMultiplyMatrix(jacobian);
    // scale the diagonal
    double[][] data = alpha.getData();
    for (int i = 0; i < m; i++) {
      data[i][i] *= onePLambda;
    }
    return alpha;
  }

}
