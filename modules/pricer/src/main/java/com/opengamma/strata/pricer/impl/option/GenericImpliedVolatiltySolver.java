/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import java.util.function.Function;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.rootfinding.BisectionSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;

/**
 * Finds an implied volatility (a parameter that put into a model gives the market pirce of an option)
 * for any option pricing model that has a 'volatility' parameter.
 * This included the Black-Scholes-Merton model (and derivatives) for European options and
 * Barone-Adesi & Whaley and Bjeksund and Stensland for American options.
 */
public class GenericImpliedVolatiltySolver {

  private static final int MAX_ITERATIONS = 20; // something's wrong if Newton-Raphson taking longer than this
  private static final double VOL_TOL = 1e-9; // 1 part in 100,000 basis points will do for implied vol
  private static final double VOL_GUESS = 0.3;
  private static final double BRACKET_STEP = 0.1;
  private static final double MAX_CHANGE = 0.5;

  /**
   * The price function.
   */
  private final Function<Double, Double> priceFunc;
  /**
   * The combined price and vega function.
   */
  private final Function<Double, double[]> priceAndVegaFunc;

  /**
   * Creates an instance.
   * 
   * @param priceAndVegaFunc  the combined price and vega function
   */
  public GenericImpliedVolatiltySolver(Function<Double, double[]> priceAndVegaFunc) {
    ArgChecker.notNull(priceAndVegaFunc, "priceAndVegaFunc");
    this.priceAndVegaFunc = priceAndVegaFunc;
    this.priceFunc = new Function<Double, Double>() {

      @Override
      public Double apply(Double sigma) {
        return priceAndVegaFunc.apply(sigma)[0];
      }
    };
  }

  /**
   * Creates an instance.
   * 
   * @param priceFunc  the pricing function
   * @param vegaFunc  the vega function
   */
  public GenericImpliedVolatiltySolver(Function<Double, Double> priceFunc, Function<Double, Double> vegaFunc) {
    ArgChecker.notNull(priceFunc, "priceFunc");
    ArgChecker.notNull(vegaFunc, "vegaFunc");
    this.priceFunc = priceFunc;
    this.priceAndVegaFunc = new Function<Double, double[]>() {

      @Override
      public double[] apply(Double sigma) {
        return new double[] {priceFunc.apply(sigma), vegaFunc.apply(sigma)};
      }
    };
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied volatility.
   * 
   * @param optionPrice  the option price
   * @return the volatility
   */
  public double impliedVolatility(double optionPrice) {
    return impliedVolatility(optionPrice, VOL_GUESS);
  }

  /**
   * Computes the implied volatility.
   * 
   * @param optionPrice  the option price
   * @param volGuess  the initial guess
   * @return the volatility
   */
  public double impliedVolatility(double optionPrice, double volGuess) {
    ArgChecker.isTrue(volGuess >= 0.0, "volGuess must be positive; have {}", volGuess);
    ArgChecker.isTrue(Doubles.isFinite(volGuess), "volGuess must be finite; have {} ", volGuess);

    double lowerSigma;
    double upperSigma;

    if (optionPrice == 0.21056699275786067) {
      int i = 0;
    }

    try {
      double[] temp = bracketRoot(optionPrice, volGuess);
      lowerSigma = temp[0];
      upperSigma = temp[1];
    } catch (MathException e) {
      throw new IllegalArgumentException(e.toString() + " No implied Volatility for this price. [price: " + optionPrice + "]");
    }
    double sigma = (lowerSigma + upperSigma) / 2.0;

    double[] pnv = priceAndVegaFunc.apply(sigma);

    // This can happen for American options,
    // where low volatilities puts you in the early excise region which obviously has zero vega
    if (pnv[1] == 0 || Double.isNaN(pnv[1])) {
      return solveByBisection(optionPrice, lowerSigma, upperSigma);
    }
    double diff = pnv[0] - optionPrice;
    boolean above = diff > 0;
    if (above) {
      upperSigma = sigma;
    } else {
      lowerSigma = sigma;
    }

    double trialChange = -diff / pnv[1];
    double actChange;
    if (trialChange > 0.0) {
      actChange = Math.min(MAX_CHANGE, Math.min(trialChange, upperSigma - sigma));
    } else {
      actChange = Math.max(-MAX_CHANGE, Math.max(trialChange, lowerSigma - sigma));
    }

    int count = 0;
    while (Math.abs(actChange) > VOL_TOL) {
      sigma += actChange;
      pnv = priceAndVegaFunc.apply(sigma);

      if (pnv[1] == 0 || Double.isNaN(pnv[1])) {
        return solveByBisection(optionPrice, lowerSigma, upperSigma);
      }

      diff = pnv[0] - optionPrice;
      above = diff > 0;
      if (above) {
        upperSigma = sigma;
      } else {
        lowerSigma = sigma;
      }

      trialChange = -diff / pnv[1];
      if (trialChange > 0.0) {
        actChange = Math.min(MAX_CHANGE, Math.min(trialChange, upperSigma - sigma));
      } else {
        actChange = Math.max(-MAX_CHANGE, Math.max(trialChange, lowerSigma - sigma));
      }

      if (count++ > MAX_ITERATIONS) {
        return solveByBisection(optionPrice, lowerSigma, upperSigma);
      }
    }
    return sigma + actChange; // apply the final change

  }

  //-------------------------------------------------------------------------
  private double[] bracketRoot(double optionPrice, double sigma) {
    BracketRoot bracketer = new BracketRoot();
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double volatility) {
        return priceFunc.apply(volatility) / optionPrice - 1.0;
      }
    };
    return bracketer.getBracketedPoints(
        func,
        Math.max(0.0, sigma - BRACKET_STEP),
        sigma + BRACKET_STEP,
        0d,
        Double.POSITIVE_INFINITY);
  }

  private double solveByBisection(double optionPrice, double lowerSigma, double upperSigma) {
    BisectionSingleRootFinder rootFinder = new BisectionSingleRootFinder(VOL_TOL);
    Function<Double, Double> func = new Function<Double, Double>() {

      @Override
      public Double apply(Double volatility) {
        double trialPrice = priceFunc.apply(volatility);
        return trialPrice / optionPrice - 1.0;
      }
    };
    return rootFinder.getRoot(func, lowerSigma, upperSigma);
  }

}
