/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;

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
   * @param option  the option
   * @param forward  the forward value of the underlying
   * @param data  the model data
   * @return the volatility
   */
  public abstract double getVolatility(EuropeanVanillaOption option, double forward, T data);

  /**
   * Computes the volatility sensitivity to the model parameters by means of central finite difference. 
   * <p>
   * This should be overridden where possible. 
   * 
   * @param option  the option
   * @param forward  the forward value of the underlying
   * @param data  the model data
   * @return the sensitivities
   */
  public double[] getVolatilityModelAdjoint(EuropeanVanillaOption option, double forward, T data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.isTrue(forward >= 0.0, "forward must be greater than zero");
    Function1D<T, Double> func = getVolatilityFunction(option, forward);
    return paramBar(func, data);
  }

  /**
   * Computes volatility and the adjoint (volatility sensitivity to forward, strike and model parameters). 
   * <p>
   * This should be overridden where possible. 
   * 
   * @param option  the option, not null
   * @param forward  the forward value of the underlying
   * @param data  the model data
   * @return the sensitivities
   */
  public double[] getVolatilityAdjoint(EuropeanVanillaOption option, double forward, T data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.isTrue(forward >= 0.0, "forward must be greater than zero");

    double[] res = new double[3 + data.getNumberOfParameters()]; //vol, fwd, strike, the model parameters
    res[0] = getVolatility(option, forward, data);
    res[1] = forwardBar(option, forward, data);
    res[2] = strikeBar(option, forward, data);
    double[] modelAdjoint = getVolatilityModelAdjoint(option, forward, data);
    System.arraycopy(modelAdjoint, 0, res, 3, data.getNumberOfParameters());
    return res;
  }

  //-------------------------------------------------------------------------
  private double forwardBar(EuropeanVanillaOption option, double forward, T data) {
    double volUp = getVolatility(option, forward + EPS, data);
    double volDown = getVolatility(option, forward - EPS, data);
    return 0.5 * (volUp - volDown) / EPS;
  }

  private double strikeBar(EuropeanVanillaOption option, double forward, T data) {
    EuropeanVanillaOption optionUp =
        EuropeanVanillaOption.of(option.getStrike() + EPS, option.getTimeToExpiry(), option.getPutCall());
    EuropeanVanillaOption optionDw =
        EuropeanVanillaOption.of(option.getStrike() - EPS, option.getTimeToExpiry(), option.getPutCall());
    double volUp = getVolatility(optionUp, forward, data);
    double volDown = getVolatility(optionDw, forward, data);
    return 0.5 * (volUp - volDown) / EPS;
  }

  private Function1D<T, Double> getVolatilityFunction(EuropeanVanillaOption option, double forward) {
    return new Function1D<T, Double>() {
      @Override
      public Double evaluate(T data) {
        ArgChecker.notNull(data, "data");
        return getVolatility(option, forward, data);
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
