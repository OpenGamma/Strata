/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.rootfinding.BisectionSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;

/**
 * Computes the implied volatility from the option price in a normally distributed asset price world.
 */
public class NormalImpliedVolatilityFormula {

  /**
   * The singleton implementation.
   */
  public static final NormalImpliedVolatilityFormula DEFAULT = new NormalImpliedVolatilityFormula();

  /** The function used to compute the price with normal hypothesis. */
  private static final NormalPriceFunction NORMAL_PRICE_FUNCTION = new NormalPriceFunction();
  /** The maximal number of iterations in the root solving algorithm. */
  private static final int MAX_ITERATIONS = 100;
  /** The solution precision. */
  private static final double EPS = 1e-15;

  // restricted constructor
  private NormalImpliedVolatilityFormula() {
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied volatility from the option price in a normally distributed asset price world.
   * <p>
   * If the volatility data is not zero, it is used as a starting point for the volatility search.
   * 
   * @param data  the model data 
   * @param option  the option
   * @param optionPrice  the option price
   * @return the implied volatility
   */
  public double impliedVolatility(
      NormalFunctionData data,
      EuropeanVanillaOption option,
      double optionPrice) {

    double numeraire = data.getNumeraire();
    boolean isCall = option.isCall();
    double f = data.getForward();
    double k = option.getStrike();
    double intrinsicPrice = numeraire * Math.max(0, (isCall ? 1 : -1) * (f - k));
    ArgChecker.isTrue(optionPrice > intrinsicPrice || DoubleMath.fuzzyEquals(optionPrice, intrinsicPrice, 1e-6),
        "option price (" + optionPrice + ") less than intrinsic value (" + intrinsicPrice + ")");
    if (Double.doubleToLongBits(optionPrice) == Double.doubleToLongBits(intrinsicPrice)) {
      return 0.0;
    }
    double sigma = (Math.abs(data.getNormalVolatility()) < 1e-10 ? 0.3 * f : data.getNormalVolatility());
    NormalFunctionData newData = NormalFunctionData.of(f, numeraire, sigma);
    double maxChange = 0.5 * f;
    double[] priceDerivative = new double[3];
    double price = NORMAL_PRICE_FUNCTION.getPriceAdjoint(option, newData, priceDerivative);
    double vega = priceDerivative[1];
    double change = (price - optionPrice) / vega;
    double sign = Math.signum(change);
    change = sign * Math.min(maxChange, Math.abs(change));
    if (change > 0 && change > sigma) {
      change = sigma;
    }
    int count = 0;
    while (Math.abs(change) > EPS) {
      sigma -= change;
      newData = NormalFunctionData.of(f, numeraire, sigma);
      price = NORMAL_PRICE_FUNCTION.getPriceAdjoint(option, newData, priceDerivative);
      vega = priceDerivative[1];
      change = (price - optionPrice) / vega;
      sign = Math.signum(change);
      change = sign * Math.min(maxChange, Math.abs(change));
      if (change > 0 && change > sigma) {
        change = sigma;
      }
      if (count++ > MAX_ITERATIONS) {
        BracketRoot bracketer = new BracketRoot();
        BisectionSingleRootFinder rootFinder = new BisectionSingleRootFinder(EPS);
        Function1D<Double, Double> func = new Function1D<Double, Double>() {
          @SuppressWarnings({"synthetic-access"})
          @Override
          public Double evaluate(Double volatility) {
            NormalFunctionData myData = NormalFunctionData.of(data.getForward(), data.getNumeraire(), volatility);
            return NORMAL_PRICE_FUNCTION.getPriceFunction(option).evaluate(myData) - optionPrice;
          }
        };
        double[] range = bracketer.getBracketedPoints(func, 0.0, 10.0);
        return rootFinder.getRoot(func, range[0], range[1]);
      }
    }
    return sigma;
  }

}
