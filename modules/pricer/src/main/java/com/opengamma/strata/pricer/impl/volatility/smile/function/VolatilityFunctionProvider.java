/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile.function;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;

/**
 * Provides functions that return volatility and its sensitivity to volatility model parameters. 
 * 
 * @param <T> Type of the data needed for the volatility function
 */
public abstract class VolatilityFunctionProvider<T extends SmileModelData> {

  private static final MatrixAlgebra MATRIX_ALG = new OGMatrixAlgebra();
  private static final double EPS = 1.0e-6;

  /**
   * Returns a function that, given data of type T, calculates the volatility.
   * 
   * @param option  the option
   * @param forward  the forward value of the underlying
   * @return the function
   */
  public abstract Function1D<T, Double> getVolatilityFunction(EuropeanVanillaOption option, double forward);

  /**
   * Returns a function that, given data of type T, calculates the volatilities for the given strikes. 
   * 
   * @param forward  the forward
   * @param strikes  set of strikes
   * @param timeToExpiry  time-to-expiry
   * @return A set of volatilities for the given strikes
   */
  public Function1D<T, double[]> getVolatilityFunction(double forward, double[] strikes, double timeToExpiry) {

    int n = strikes.length;
    List<Function1D<T, Double>> funcs = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      PutCall putCall = strikes[i] >= forward ? PutCall.CALL : PutCall.PUT;
      funcs.add(getVolatilityFunction(EuropeanVanillaOption.of(strikes[i], timeToExpiry, putCall), forward));
    }
    return new Function1D<T, double[]>() {
      @Override
      public double[] evaluate(final T data) {
        double[] res = new double[n];
        for (int i = 0; i < n; i++) {
          res[i] = funcs.get(i).evaluate(data);
        }
        return res;
      }

    };
  }

  /**
   * Returns a function  that calculates volatility and the adjoint (volatility sensitivity to forward, strike and model parameters)
   * by means of central finite difference. 
   * <p>
   * This should be overridden where possible. 
   * 
   * @param option  the option, not null
   * @param forward  the forward value of the underlying
   * @return Returns a function that, given data of type T, calculates the volatility adjoint
   */
  public Function1D<T, double[]> getVolatilityAdjointFunction(EuropeanVanillaOption option, double forward) {

    ArgChecker.notNull(option, "option");
    ArgChecker.isTrue(forward >= 0.0, "forward must be greater than zero");
    Function1D<T, Double> func = getVolatilityFunction(option, forward);
    return new Function1D<T, double[]>() {
      @Override
      public double[] evaluate(final T data) {
        ArgChecker.notNull(data, "data");
        double[] x = new double[3 + data.getNumberOfParameters()]; //vol, fwd, strike, the model parameters
        x[0] = func.evaluate(data);
        x[1] = forwardBar(option, forward, data);
        x[2] = strikeBar(option, forward, data);
        System.arraycopy(paramBar(func, data), 0, x, 3, data.getNumberOfParameters());
        return x;
      }
    };
  }

  /**
   * Returns a function that calculates the volatility sensitivity to model parameters by means of central finite difference. 
   * <p>
   * This should be overridden where possible.
   * 
   * @param option  the option, not null
   * @param forward  the forward value of the underlying
   * @return Returns a function that, given data of type T, calculates the volatility model sensitivity
   */
  public Function1D<T, double[]> getModelAdjointFunction(EuropeanVanillaOption option, double forward) {

    ArgChecker.notNull(option, "option");
    ArgChecker.isTrue(forward >= 0.0, "forward must be greater than zero");
    Function1D<T, Double> func = getVolatilityFunction(option, forward);
    return new Function1D<T, double[]>() {
      @Override
      public final double[] evaluate(final T data) {
        ArgChecker.notNull(data, "data");
        return paramBar(func, data);
      }
    };
  }

  /**
   * Obtains a function that calculates the volatility adjoint set by finite difference. 
   * <p>
   * The function returns a matrix (i.e. double[][]). 
   * The first column is volatility at each strike, the second and third are the forward and strike sensitivity at each strike, 
   * and the remaining columns are the model parameter sensitivities are each strike. 
   * <p>
   * This should be overridden where possible.
   * 
   * @param forward  forward value of underlying
   * @param strikes  strikes
   * @param timeToExpiry  time-toExpiry
   * @return The function
   */
  public Function1D<T, double[][]> getVolatilityAdjointFunction(double forward, double[] strikes, double timeToExpiry) {

    int n = strikes.length;
    Function1D<T, double[]> func = getVolatilityFunction(forward, strikes, timeToExpiry);
    return new Function1D<T, double[][]>() {
      @Override
      public double[][] evaluate(final T data) {
        ArgChecker.notNull(data, "data");
        double[][] res = new double[3 + data.getNumberOfParameters()][n];
        res[0] = func.evaluate(data);
        res[1] = forwardBar(strikes, timeToExpiry, forward, data);
        res[2] = strikeBar(strikes, timeToExpiry, forward, data);
        double[][] temp = paramBarSet(func, data);
        int m = temp.length;
        for (int i = 0; i < m; i++) {
          res[3 + i] = temp[i];
        }
        return MATRIX_ALG.getTranspose(new DoubleMatrix2D(res)).getData();
      }
    };
  }

  /**
   * Obtains a function that calculates the volatility adjoint set for respective strikes. 
   * 
   * @param forward  the forward value of underlying
   * @param strikes  the strikes
   * @param timeToExpiry  the time-toExpiry
   * @return the function
   */
  protected Function1D<T, double[][]> getVolatilityAdjointFunctionByCallingSingleStrikes(double forward,
      double[] strikes, double timeToExpiry) {

    int n = strikes.length;
    List<Function1D<T, double[]>> funcs = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      PutCall putCall = strikes[i] >= forward ? PutCall.CALL : PutCall.PUT;
      funcs.add(getVolatilityAdjointFunction(EuropeanVanillaOption.of(strikes[i], timeToExpiry, putCall), forward));
    }
    return new Function1D<T, double[][]>() {
      @Override
      public double[][] evaluate(final T data) {
        double[][] res = new double[n][];
        for (int i = 0; i < n; i++) {
          res[i] = funcs.get(i).evaluate(data);
        }
        return res;
      }
    };
  }

  /**
   * Obtains a function that calculates the volatility sensitivity to model parameters for a range of strikes by means of
   * central finite difference. 
   * <p>
   * The function returns a matrix (i.e. double[][]) of volatility sensitivities to model parameters, 
   * where the columns are the model parameter sensitivities at each strike. 
   * <p>
   * This should be overridden where possible.
   * 
   * @param forward  the forward value of underlying
   * @param strikes  the strikes
   * @param timeToExpiry  the time-toExpiry
   * @return the function
   */
  public Function1D<T, double[][]> getModelAdjointFunction(double forward, double[] strikes, double timeToExpiry) {

    Function1D<T, double[]> func = getVolatilityFunction(forward, strikes, timeToExpiry);
    return new Function1D<T, double[][]>() {
      @Override
      public double[][] evaluate(final T data) {
        ArgChecker.notNull(data, "data");
        double[][] temp = paramBarSet(func, data);
        return MATRIX_ALG.getTranspose(new DoubleMatrix2D(temp)).getData();
      }
    };
  }

  /**
   * Obtains a function that calculates the volatility sensitivity to model parameters for respective strikes.
   * 
   * @param forward Forward value of underlying
   * @param strikes The strikes
   * @param timeToExpiry Time-toExpiry
   * @return the function
   */
  protected Function1D<T, double[][]> getModelAdjointFunctionByCallingSingleStrikes(double forward, double[] strikes,
      double timeToExpiry) {

    int n = strikes.length;
    List<Function1D<T, double[]>> funcs = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      PutCall putCall = strikes[i] >= forward ? PutCall.CALL : PutCall.PUT;
      funcs.add(getModelAdjointFunction(EuropeanVanillaOption.of(strikes[i], timeToExpiry, putCall), forward));
    }
    return new Function1D<T, double[][]>() {
      @Override
      public double[][] evaluate(final T data) {
        double[][] res = new double[n][];
        for (int i = 0; i < n; i++) {
          res[i] = funcs.get(i).evaluate(data);
        }
        return res;
      }
    };
  }

  private double forwardBar(EuropeanVanillaOption option, double forward, T data) {
    Function1D<T, Double> funcUp = getVolatilityFunction(option, forward + EPS);
    Function1D<T, Double> funcDown = getVolatilityFunction(option, forward - EPS);
    return (funcUp.evaluate(data) - funcDown.evaluate(data)) / 2 / EPS;
  }

  private double[] forwardBar(double[] strikes, double timeToExpiry, double forward, T data) {
    int n = strikes.length;
    Function1D<T, double[]> funcUp = getVolatilityFunction(forward + EPS, strikes, timeToExpiry);
    Function1D<T, double[]> funcDown = getVolatilityFunction(forward - EPS, strikes, timeToExpiry);
    double[] res = new double[n];
    double[] up = funcUp.evaluate(data);
    double[] down = funcDown.evaluate(data);
    for (int i = 0; i < n; i++) {
      res[i] = 0.5 * (up[i] - down[i]) / EPS;
    }
    return res;
  }

  private double strikeBar(EuropeanVanillaOption option, double forward, T data) {
    EuropeanVanillaOption optionUp =
        EuropeanVanillaOption.of(option.getStrike() + EPS, option.getTimeToExpiry(), option.getPutCall());
    EuropeanVanillaOption optionDw =
        EuropeanVanillaOption.of(option.getStrike() - EPS, option.getTimeToExpiry(), option.getPutCall());
    Function1D<T, Double> funcUp = getVolatilityFunction(optionUp, forward);
    Function1D<T, Double> funcDown = getVolatilityFunction(optionDw, forward);
    return 0.5 * (funcUp.evaluate(data) - funcDown.evaluate(data)) / EPS;
  }

  private double[] strikeBar(double[] strikes, double timeToExpiry, double forward, T data) {
    int n = strikes.length;
    double[] res = new double[n];
    double[] strikesUp = new double[n];
    double[] strikesDown = new double[n];
    for (int i = 0; i < n; i++) {
      strikesUp[i] = strikes[i] + EPS;
    }
    for (int i = 0; i < n; i++) {
      strikesDown[i] = strikes[i] - EPS;
    }
    Function1D<T, double[]> funcUp = getVolatilityFunction(forward, strikesUp, timeToExpiry);
    Function1D<T, double[]> funcDown = getVolatilityFunction(forward, strikesDown, timeToExpiry);
    double[] up = funcUp.evaluate(data);
    double[] down = funcDown.evaluate(data);
    for (int i = 0; i < n; i++) {
      res[i] = 0.5 * (up[i] - down[i]) / EPS;
    }
    return res;
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

  private double[][] paramBarSet(final Function1D<T, double[]> func, final T data) {
    int n = data.getNumberOfParameters();
    double[][] res = new double[n][];
    for (int i = 0; i < n; i++) {
      res[i] = paramBarSet(func, data, i);
    }
    return res;
  }

  /**
   * Get the model's sensitivity to its parameters by finite-difference, taking care on the boundary of allowed regions
   * @param func Function that gives the volatility for a set of model parameters
   * @param data Model parameters
   * @param index the index of the model parameter
   * @return The first derivative of the volatility WRT the parameter given by index
   */
  @SuppressWarnings("unchecked")
  private double[] paramBarSet(Function1D<T, double[]> func, T data, int index) {
    double mid = data.getParameter(index);
    double up = mid + EPS;
    double down = mid - EPS;
    if (data.isAllowed(index, down)) {
      if (data.isAllowed(index, up)) {
        T dUp = (T) data.with(index, up);
        T dDown = (T) data.with(index, down);
        double[] rUp = func.evaluate(dUp);
        double[] rDown = func.evaluate(dDown);
        int m = rUp.length;
        double[] res = new double[m];
        for (int i = 0; i < m; i++) {
          res[i] = 0.5 * (rUp[i] - rDown[i]) / EPS;
        }
        return res;
      }
      double[] rMid = func.evaluate(data);
      double[] rDown = func.evaluate((T) data.with(index, down));
      int m = rMid.length;
      double[] res = new double[m];
      for (int i = 0; i < m; i++) {
        res[i] = 0.5 * (rMid[i] - rDown[i]) / EPS;
      }
      return res;
    }
    ArgChecker.isTrue(data.isAllowed(index, up), "No values and index {} = {} are allowed", index, mid);
    double[] rMid = func.evaluate(data);
    double[] rUp = func.evaluate((T) data.with(index, up));
    int m = rMid.length;
    double[] res = new double[m];
    for (int i = 0; i < m; i++) {
      res[i] = 0.5 * (rUp[i] - rMid[i]) / EPS;
    }
    return res;
  }

}
