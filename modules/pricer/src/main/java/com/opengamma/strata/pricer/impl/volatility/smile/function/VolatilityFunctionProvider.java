/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile.function;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Provides functions that return volatility and its sensitivity to volatility model parameters. 
 * 
 * @param <T> Type of the data needed for the volatility function
 */
public abstract class VolatilityFunctionProvider<T extends SmileModelData> {

  private static final double EPS = 1.0e-6;

  /**
   * Computes the volatility. 
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param data  the model data
   * @return the volatility
   */
  public abstract double getVolatility(double forward, double strike, double timeToExpiry, T data);

  /**
   * Computes volatility and the adjoint (volatility sensitivity to forward, strike and model parameters). 
   * <p>
   * By default the derivatives are computed by central finite difference approximation. 
   * This should be overridden in each subclass. 
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param data  the model data
   * @return the sensitivities
   */
  public ValueDerivatives getVolatilityAdjoint(double forward, double strike, double timeToExpiry, T data) {
    ArgChecker.isTrue(forward >= 0.0, "forward must be greater than zero");

    double[] res = new double[2 + data.getNumberOfParameters()]; // fwd, strike, the model parameters
    double volatility = getVolatility(forward, strike, timeToExpiry, data);
    res[0] = forwardBar(forward, strike, timeToExpiry, data);
    res[1] = strikeBar(forward, strike, timeToExpiry, data);
    Function1D<T, Double> func = getVolatilityFunction(forward, strike, timeToExpiry);
    double[] modelAdjoint = paramBar(func, data);
    System.arraycopy(modelAdjoint, 0, res, 2, data.getNumberOfParameters());
    return ValueDerivatives.of(volatility, res);
  }

  //-------------------------------------------------------------------------
  private double forwardBar(double forward, double strike, double timeToExpiry, T data) {
    double volUp = getVolatility(forward + EPS, strike, timeToExpiry, data);
    double volDown = getVolatility(forward - EPS, strike, timeToExpiry, data);
    return 0.5 * (volUp - volDown) / EPS;
  }

  private double strikeBar(double forward, double strike, double timeToExpiry, T data) {
    double volUp = getVolatility(forward, strike + EPS, timeToExpiry, data);
    double volDown = getVolatility(forward, strike - EPS, timeToExpiry, data);
    return 0.5 * (volUp - volDown) / EPS;
  }

  private Function1D<T, Double> getVolatilityFunction(double forward, double strike, double timeToExpiry) {
    return new Function1D<T, Double>() {
      @Override
      public Double evaluate(T data) {
        ArgChecker.notNull(data, "data");
        return getVolatility(forward, strike, timeToExpiry, data);
      }
    };
  }

  private double[] paramBar(Function1D<T, Double> func, T data) {
    int n = data.getNumberOfParameters();
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = paramBar(func, data, i);
    }
    return res;
  }

  @SuppressWarnings("unchecked")
  private double paramBar(Function1D<T, Double> func, T data, int index) {
    double mid = data.getParameter(index);
    double up = mid + EPS;
    double down = mid - EPS;
    if (data.isAllowed(index, down)) {
      if (data.isAllowed(index, up)) {
        T dUp = (T) data.with(index, up);
        T dDown = (T) data.with(index, down);
        return 0.5 * (func.evaluate(dUp) - func.evaluate(dDown)) / EPS;
      }
      T dDown = (T) data.with(index, down);
      return (func.evaluate(data) - func.evaluate(dDown)) / EPS;
    }
    ArgChecker.isTrue(data.isAllowed(index, up), "No values and index {} = {} are allowed", index, mid);
    T dUp = (T) data.with(index, up);
    return (func.evaluate(dUp) - func.evaluate(data)) / EPS;
  }

}
