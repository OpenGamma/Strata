package com.opengamma.strata.pricer.impl.volatility.smile.fitting;

import java.util.BitSet;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionFactory;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.minimization.NonLinearParameterTransforms;
import com.opengamma.strata.math.impl.minimization.NonLinearTransformFunction;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResults;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResultsWithTransform;
import com.opengamma.strata.math.impl.statistics.leastsquare.NonLinearLeastSquare;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SmileModelData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;

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
  private static final Function1D<DoubleMatrix1D, Boolean> UNCONSTRAINED = new Function1D<DoubleMatrix1D, Boolean>() {
    @Override
    public Boolean evaluate(final DoubleMatrix1D x) {
      return true;
    }
  };

  private final VolatilityFunctionProvider<T> _model;
  private final Function1D<T, double[]> _volFunc;
  private final Function1D<T, double[][]> _volAdjointFunc;
  private final DoubleMatrix1D _marketValues;
  private final DoubleMatrix1D _errors;

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
      double[] strikes,
      double timeToExpiry,
      double[] impliedVols,
      double[] error,
      VolatilityFunctionProvider<T> model) {
    ArgChecker.notNull(strikes, "strikes");
    ArgChecker.notNull(impliedVols, "implied vols");
    ArgChecker.notNull(error, "errors");
    ArgChecker.notNull(model, "model");
    int n = strikes.length;
    ArgChecker.isTrue(n == impliedVols.length, "vols not the same length as strikes");
    ArgChecker.isTrue(n == error.length, "errors not the same length as strikes");

    _marketValues = new DoubleMatrix1D(impliedVols);
    _errors = new DoubleMatrix1D(error);
    _volFunc = model.getVolatilityFunction(forward, strikes, timeToExpiry);
    _volAdjointFunc = model.getModelAdjointFunction(forward, strikes, timeToExpiry);
    _model = model;
  }

  /**
   * Solves using the default NonLinearParameterTransforms for the concrete implementation. 
   * <p>
   * This returns {@link LeastSquareResults}.
   * 
   * @param start  the first guess at the parameter values
   * @return the calibration results
   */
  public LeastSquareResultsWithTransform solve(DoubleMatrix1D start) {
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
  public LeastSquareResultsWithTransform solve(DoubleMatrix1D start, BitSet fixed) {
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
  public LeastSquareResultsWithTransform solve(DoubleMatrix1D start, NonLinearParameterTransforms transform) {
    NonLinearTransformFunction transFunc = new NonLinearTransformFunction(getModelValueFunction(),
        getModelJacobianFunction(), transform);

    LeastSquareResults solRes = SOLVER.solve(_marketValues, _errors, transFunc.getFittingFunction(),
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
  protected Function1D<DoubleMatrix1D, DoubleMatrix1D> getModelValueFunction() {

    return new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
        final T data = toSmileModelData(x);
        final double[] res = _volFunc.evaluate(data);
        return new DoubleMatrix1D(res);
      }
    };
  }

  /**
   * Obtains Jacobian function of the smile model.
   * <p>
   * The function is defined in {@link VolatilityFunctionProvider}.
   * 
   * @return the function
   */
  protected Function1D<DoubleMatrix1D, DoubleMatrix2D> getModelJacobianFunction() {

    return new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public DoubleMatrix2D evaluate(final DoubleMatrix1D x) {
        final T data = toSmileModelData(x);
        //this thing will be (#strikes/vols) x (# model Params)
        final double[][] volAdjoint = _volAdjointFunc.evaluate(data);
        return new DoubleMatrix2D(volAdjoint);
      }
    };
  }

  /**
   * Obtains the maximum number of iterations.
   * 
   * @return  the maximum number. 
   */
  protected abstract DoubleMatrix1D getMaximumStep();

  /**
   * Obtains the nonlinear transformation of parameters from the initial values. 
   * 
   * @param start  the initial values
   * @return  the nonlinear transformation
   */
  protected abstract NonLinearParameterTransforms getTransform(final DoubleMatrix1D start);

  /**
   * Obtains the nonlinear transformation of parameters from the initial values with some parameters fixed. 
   * 
   * @param start  the initial values
   * @param fixed  the parameters are fixed
   * @return  the nonlinear transformation
   */
  protected abstract NonLinearParameterTransforms getTransform(final DoubleMatrix1D start, final BitSet fixed);

  /**
   * Obtains {@code SmileModelData} instance from the model parameters. 
   * 
   * @param modelParameters  the model parameters
   * @return the smile model data
   */
  public abstract T toSmileModelData(final DoubleMatrix1D modelParameters);

  /**
   * Obtains the constraint function. 
   * <p>
   * This is defaulted to be "unconstrained".
   * 
   * @param t  the nonlinear transformation
   * @return the constraint function
   */
  protected Function1D<DoubleMatrix1D, Boolean> getConstraintFunction(
      @SuppressWarnings("unused") final NonLinearParameterTransforms t) {
    return UNCONSTRAINED;
  }

  /**
   * Obtains the volatility function provider. 
   * 
   * @return the volatility function provider
   */
  public VolatilityFunctionProvider<T> getModel() {
    return _model;
  }

}
