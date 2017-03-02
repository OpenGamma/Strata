/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import java.util.function.Function;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Provides functions that return volatility and its sensitivity to volatility model parameters.
 * 
 * @param <T> Type of the data needed for the volatility function
 */
public abstract class VolatilityFunctionProvider<T extends SmileModelData> {

  private static final double EPS = 1.0e-6;

  /**
   * Calculates the volatility.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param data  the model data
   * @return the volatility
   */
  public abstract double volatility(double forward, double strike, double timeToExpiry, T data);

  /**
   * Calculates volatility and the adjoint (volatility sensitivity to forward, strike and model parameters). 
   * <p>
   * By default the derivatives are computed by central finite difference approximation.
   * This should be overridden in each subclass.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param data  the model data
   * @return the volatility and associated derivatives
   */
  public ValueDerivatives volatilityAdjoint(double forward, double strike, double timeToExpiry, T data) {
    ArgChecker.isTrue(forward >= 0.0, "forward must be greater than zero");

    double[] res = new double[2 + data.getNumberOfParameters()]; // fwd, strike, the model parameters
    double volatility = volatility(forward, strike, timeToExpiry, data);
    res[0] = forwardBar(forward, strike, timeToExpiry, data);
    res[1] = strikeBar(forward, strike, timeToExpiry, data);
    Function<T, Double> func = getVolatilityFunction(forward, strike, timeToExpiry);
    double[] modelAdjoint = paramBar(func, data);
    System.arraycopy(modelAdjoint, 0, res, 2, data.getNumberOfParameters());
    return ValueDerivatives.of(volatility, DoubleArray.ofUnsafe(res));
  }

  /**
   * Computes the first and second order derivatives of the volatility.
   * <p>
   * The first derivative values will be stored in the input array {@code volatilityD} 
   * The array contains, [0] Derivative w.r.t the forward, [1] the derivative w.r.t the strike, then followed by model 
   * parameters.
   * Thus the length of the array should be 2 + (number of model parameters).  
   * <p>
   * The second derivative values will be stored in the input array {@code volatilityD2}. 
   * Only the second order derivative with respect to the forward and strike must be implemented.
   * The array contains [0][0] forward-forward; [0][1] forward-strike; [1][1] strike-strike.
   * Thus the size should be 2 x 2.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param data  the model data
   * @param volatilityD  the array used to return the first order derivative
   * @param volatilityD2  the array of array used to return the second order derivative
   * @return the volatility
   */
  public abstract double volatilityAdjoint2(
      double forward,
      double strike,
      double timeToExpiry,
      T data,
      double[] volatilityD,
      double[][] volatilityD2);

  //-------------------------------------------------------------------------
  private double forwardBar(double forward, double strike, double timeToExpiry, T data) {
    double volUp = volatility(forward + EPS, strike, timeToExpiry, data);
    double volDown = volatility(forward - EPS, strike, timeToExpiry, data);
    return 0.5 * (volUp - volDown) / EPS;
  }

  private double strikeBar(double forward, double strike, double timeToExpiry, T data) {
    double volUp = volatility(forward, strike + EPS, timeToExpiry, data);
    double volDown = volatility(forward, strike - EPS, timeToExpiry, data);
    return 0.5 * (volUp - volDown) / EPS;
  }

  private Function<T, Double> getVolatilityFunction(double forward, double strike, double timeToExpiry) {
    return new Function<T, Double>() {
      @Override
      public Double apply(T data) {
        ArgChecker.notNull(data, "data");
        return volatility(forward, strike, timeToExpiry, data);
      }
    };
  }

  private double[] paramBar(Function<T, Double> func, T data) {
    int n = data.getNumberOfParameters();
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = paramBar(func, data, i);
    }
    return res;
  }

  @SuppressWarnings("unchecked")
  private double paramBar(Function<T, Double> func, T data, int index) {
    double mid = data.getParameter(index);
    double up = mid + EPS;
    double down = mid - EPS;
    if (data.isAllowed(index, down)) {
      if (data.isAllowed(index, up)) {
        T dUp = (T) data.with(index, up);
        T dDown = (T) data.with(index, down);
        return 0.5 * (func.apply(dUp) - func.apply(dDown)) / EPS;
      }
      T dDown = (T) data.with(index, down);
      return (func.apply(data) - func.apply(dDown)) / EPS;
    }
    ArgChecker.isTrue(data.isAllowed(index, up), "No values and index {} = {} are allowed", index, mid);
    T dUp = (T) data.with(index, up);
    return (func.apply(dUp) - func.apply(data)) / EPS;
  }

}
