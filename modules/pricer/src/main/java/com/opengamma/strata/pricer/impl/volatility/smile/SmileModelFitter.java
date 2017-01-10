/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import java.util.BitSet;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionFactory;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.minimization.NonLinearParameterTransforms;
import com.opengamma.strata.math.impl.minimization.NonLinearTransformFunction;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResults;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResultsWithTransform;
import com.opengamma.strata.math.impl.statistics.leastsquare.NonLinearLeastSquare;

/**
 * Smile model fitter.
 * <p>
 * Attempts to calibrate a smile model to the implied volatilities of European vanilla options, by minimising the sum of 
 * squares between the market and model implied volatilities.
 * <p>
 * All the options must be for the same expiry and (implicitly) on the same underlying.
 * 
 * @param <T>  the data of smile model to be calibrated
 */
public abstract class SmileModelFitter<T extends SmileModelData> {
  private static final MatrixAlgebra MA = new OGMatrixAlgebra();
  private static final NonLinearLeastSquare SOLVER = new NonLinearLeastSquare(DecompositionFactory.SV_COMMONS, MA, 1e-12);
  private static final Function<DoubleArray, Boolean> UNCONSTRAINED = new Function<DoubleArray, Boolean>() {
    @Override
    public Boolean apply(DoubleArray x) {
      return true;
    }
  };

  private final VolatilityFunctionProvider<T> model;
  private final Function<DoubleArray, DoubleArray> volFunc;
  private final Function<DoubleArray, DoubleMatrix> volAdjointFunc;
  private final DoubleArray marketValues;
  private final DoubleArray errors;

  /**
   * Constructs smile model fitter from forward, strikes, time to expiry, implied volatilities and error values.
   * <p>
   * {@code strikes}, {@code impliedVols} and {@code error} should be the same length and ordered coherently.
   * 
   * @param forward  the forward value of the underlying
   * @param strikes  the ordered values of strikes
   * @param timeToExpiry  the time-to-expiry
   * @param impliedVols  the market implied volatilities
   * @param error  the 'measurement' error to apply to the market volatility of a particular option TODO: Review should this be part of  EuropeanOptionMarketData?
   * @param model  the volatility function provider
   */
  public SmileModelFitter(
      double forward,
      DoubleArray strikes,
      double timeToExpiry,
      DoubleArray impliedVols,
      DoubleArray error,
      VolatilityFunctionProvider<T> model) {
    ArgChecker.notNull(strikes, "strikes");
    ArgChecker.notNull(impliedVols, "implied vols");
    ArgChecker.notNull(error, "errors");
    ArgChecker.notNull(model, "model");
    int n = strikes.size();
    ArgChecker.isTrue(n == impliedVols.size(), "vols not the same length as strikes");
    ArgChecker.isTrue(n == error.size(), "errors not the same length as strikes");

    this.marketValues = impliedVols;
    this.errors = error;
    this.model = model;
    this.volFunc = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray x) {
        final T data = toSmileModelData(x);
        double[] res = new double[n];
        for (int i = 0; i < n; ++i) {
          res[i] = model.volatility(forward, strikes.get(i), timeToExpiry, data);
        }
        return DoubleArray.copyOf(res);
      }
    };
    this.volAdjointFunc = new Function<DoubleArray, DoubleMatrix>() {
      @Override
      public DoubleMatrix apply(DoubleArray x) {
        final T data = toSmileModelData(x);
        double[][] resAdj = new double[n][];
        for (int i = 0; i < n; ++i) {
          DoubleArray deriv = model.volatilityAdjoint(forward, strikes.get(i), timeToExpiry, data).getDerivatives();
          resAdj[i] = deriv.subArray(2).toArrayUnsafe();
        }
        return DoubleMatrix.copyOf(resAdj);
      }
    };
  }

  /**
   * Solves using the default NonLinearParameterTransforms for the concrete implementation.
   * <p>
   * This returns {@link LeastSquareResults}.
   * 
   * @param start  the first guess at the parameter values
   * @return the calibration results
   */
  public LeastSquareResultsWithTransform solve(DoubleArray start) {
    return solve(start, new BitSet());
  }

  /**
   * Solve using the default NonLinearParameterTransforms for the concrete implementation with some parameters fixed 
   * to their initial values (indicated by fixed). 
   * <p>
   * This returns {@link LeastSquareResults}.
   * 
   * @param start  the first guess at the parameter values
   * @param fixed  the parameters are fixed
   * @return the calibration results
   */
  public LeastSquareResultsWithTransform solve(DoubleArray start, BitSet fixed) {
    NonLinearParameterTransforms transform = getTransform(start, fixed);
    return solve(start, transform);
  }

  /**
   * Solve using a user supplied NonLinearParameterTransforms.
   * <p>
   * This returns {@link LeastSquareResults}.
   * 
   * @param start  the first guess at the parameter values
   * @param transform  transform from model parameters to fitting parameters, and vice versa
   * @return the calibration results
   */
  public LeastSquareResultsWithTransform solve(DoubleArray start, NonLinearParameterTransforms transform) {
    NonLinearTransformFunction transFunc = new NonLinearTransformFunction(volFunc, volAdjointFunc, transform);
    LeastSquareResults solRes = SOLVER.solve(marketValues, errors, transFunc.getFittingFunction(),
        transFunc.getFittingJacobian(), transform.transform(start), getConstraintFunction(transform), getMaximumStep());
    return new LeastSquareResultsWithTransform(solRes, transform);
  }

  /**
   * Obtains volatility function of the smile model.
   * <p>
   * The function is defined in {@link VolatilityFunctionProvider}.
   * 
   * @return the function
   */
  protected Function<DoubleArray, DoubleArray> getModelValueFunction() {
    return volFunc;
  }

  /**
   * Obtains Jacobian function of the smile model.
   * <p>
   * The function is defined in {@link VolatilityFunctionProvider}.
   * 
   * @return the function
   */
  protected Function<DoubleArray, DoubleMatrix> getModelJacobianFunction() {
    return volAdjointFunc;
  }

  /**
   * Obtains the maximum number of iterations.
   * 
   * @return the maximum number.
   */
  protected abstract DoubleArray getMaximumStep();

  /**
   * Obtains the nonlinear transformation of parameters from the initial values.
   * 
   * @param start  the initial values
   * @return the nonlinear transformation
   */
  protected abstract NonLinearParameterTransforms getTransform(DoubleArray start);

  /**
   * Obtains the nonlinear transformation of parameters from the initial values with some parameters fixed.
   * 
   * @param start  the initial values
   * @param fixed  the parameters are fixed
   * @return the nonlinear transformation
   */
  protected abstract NonLinearParameterTransforms getTransform(DoubleArray start, BitSet fixed);

  /**
   * Obtains {@code SmileModelData} instance from the model parameters.
   * 
   * @param modelParameters  the model parameters
   * @return the smile model data
   */
  public abstract T toSmileModelData(DoubleArray modelParameters);

  /**
   * Obtains the constraint function.
   * <p>
   * This is defaulted to be "unconstrained".
   * 
   * @param t  the nonlinear transformation
   * @return the constraint function
   */
  protected Function<DoubleArray, Boolean> getConstraintFunction(
      @SuppressWarnings("unused") final NonLinearParameterTransforms t) {
    return UNCONSTRAINED;
  }

  /**
   * Obtains the volatility function provider.
   * 
   * @return the volatility function provider
   */
  public VolatilityFunctionProvider<T> getModel() {
    return model;
  }

}
