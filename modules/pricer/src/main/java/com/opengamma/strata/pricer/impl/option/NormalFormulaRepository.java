/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import java.util.function.Function;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.rootfinding.BisectionSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.product.common.PutCall;

/**
 * The primary location for normal model formulas.
 */
public final class NormalFormulaRepository {

  /**
   * The normal distribution implementation.
   */
  private static final ProbabilityDistribution<Double> DISTRIBUTION = new NormalDistribution(0, 1);
  /**
   * The comparison value used to determine near-zero.
   */
  private static final double NEAR_ZERO = 1e-16;
  /**
   * The maximal number of iterations in the root solving algorithm.
   */
  private static final int MAX_ITERATIONS = 100;
  /**
   * The solution precision.
   */
  private static final double EPS = 1e-15;

  /** Limit defining "close to ATM forward" to avoid the formula singularity in the impliedVolatilityFromBlackVolatility. **/
  private static final double ATM_LIMIT = 1.0E-3;

  // restricted constructor
  private NormalFormulaRepository() {
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the forward price.
   * <p>
   * Note that the 'numeraire' is a simple multiplier and is the responsibility of the caller.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param normalVol  the normal volatility
   * @param putCall  whether it is put or call
   * @return the forward price
   */
  public static double price(double forward, double strike, double timeToExpiry, double normalVol, PutCall putCall) {
    double sigmaRootT = normalVol * Math.sqrt(timeToExpiry);
    int sign = putCall.isCall() ? 1 : -1;
    if (sigmaRootT < NEAR_ZERO) {
      double x = sign * (forward - strike);
      return (x > 0 ? x : 0d);
    }
    double arg = sign * (forward - strike) / sigmaRootT;
    double cdf = DISTRIBUTION.getCDF(arg);
    double pdf = DISTRIBUTION.getPDF(arg);
    return sign * (forward - strike) * cdf + sigmaRootT * pdf;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the price and first order derivatives.
   * <p>
   * The derivatives are stored in an array with:
   * <ul>
   * <li>[0] derivative with respect to the forward
   * <li>[1] derivative with respect to the volatility
   * <li>[2] derivative with respect to the strike
   * </ul>
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param normalVol  the normal volatility
   * @param numeraire  the numeraire
   * @param putCall  whether it is put or call
   * @return the price and associated derivatives
   */
  public static ValueDerivatives priceAdjoint(
      double forward,
      double strike,
      double timeToExpiry,
      double normalVol,
      double numeraire,
      PutCall putCall) {

    int sign = putCall.isCall() ? 1 : -1;
    double price;
    double cdf = 0d;
    double pdf = 0d;
    double arg = 0d;
    double x = 0d;
    // Implementation Note: Forward sweep.
    double sigmaRootT = normalVol * Math.sqrt(timeToExpiry);
    if (sigmaRootT < NormalFormulaRepository.NEAR_ZERO) {
      x = sign * (forward - strike);
      price = (x > 0 ? numeraire * x : 0d);
    } else {
      arg = sign * (forward - strike) / sigmaRootT;
      cdf = NormalFormulaRepository.DISTRIBUTION.getCDF(arg);
      pdf = NormalFormulaRepository.DISTRIBUTION.getPDF(arg);
      price = numeraire * (sign * (forward - strike) * cdf + sigmaRootT * pdf);
    }
    // Implementation Note: Backward sweep.
    double forwardDerivative;
    double volatilityDerivative;
    double strikeDerivative;
    double priceBar = 1d;
    if (sigmaRootT < NormalFormulaRepository.NEAR_ZERO) {
      double xBar = (x > 0 ? numeraire : 0d);
      forwardDerivative = sign * xBar;
      strikeDerivative = -forwardDerivative;
      volatilityDerivative = 0d;
    } else {
      double cdfBar = numeraire * (sign * (forward - strike)) * priceBar;
      double pdfBar = numeraire * sigmaRootT * priceBar;
      double argBar = pdf * cdfBar - pdf * arg * pdfBar;
      forwardDerivative = numeraire * sign * cdf * priceBar + sign / sigmaRootT * argBar;
      strikeDerivative = -forwardDerivative;
      double sigmaRootTBar = -arg / sigmaRootT * argBar + numeraire * pdf * priceBar;
      volatilityDerivative = Math.sqrt(timeToExpiry) * sigmaRootTBar;
    }
    return ValueDerivatives.of(price, DoubleArray.of(forwardDerivative, volatilityDerivative, strikeDerivative));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the delta.
   * <p>
   * Note that the 'numeraire' is a simple multiplier and is the responsibility of the caller.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param normalVol  the normal volatility
   * @param putCall  whether it is put or call
   * @return the delta
   */
  public static double delta(double forward, double strike, double timeToExpiry, double normalVol, PutCall putCall) {
    int sign = putCall.isCall() ? 1 : -1;
    double sigmaRootT = normalVol * Math.sqrt(timeToExpiry);
    if (sigmaRootT < NEAR_ZERO) {
      double x = sign * (forward - strike);
      if (Math.abs(x) <= NEAR_ZERO) {
        // ambiguous if x and sigmaRootT are tiny, then reference number is returned
        return sign * 0.5;
      }
      return x > 0 ? sign : 0d;
    }
    double arg = sign * (forward - strike) / sigmaRootT;
    double cdf = DISTRIBUTION.getCDF(arg);
    return sign * cdf;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the gamma.
   * <p>
   * Note that the 'numeraire' is a simple multiplier and is the responsibility of the caller.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param normalVol  the normal volatility
   * @param putCall  whether it is put or call
   * @return the gamma
   */
  public static double gamma(double forward, double strike, double timeToExpiry, double normalVol, PutCall putCall) {
    int sign = putCall.isCall() ? 1 : -1;
    double sigmaRootT = normalVol * Math.sqrt(timeToExpiry);
    if (sigmaRootT < NEAR_ZERO) {
      double x = sign * (forward - strike);
      // ambiguous (tend to be infinite) if x and sigmaRootT are tiny, then reference number is returned
      return Math.abs(x) > NEAR_ZERO ? 0d : 1d / Math.sqrt(2d * Math.PI) / sigmaRootT;
    }
    double arg = (forward - strike) / sigmaRootT;
    double pdf = DISTRIBUTION.getPDF(arg);
    return pdf / sigmaRootT;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the theta.
   * <p>
   * Note that the 'numeraire' is a simple multiplier and is the responsibility of the caller.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param normalVol  the normal volatility
   * @param putCall  whether it is put or call
   * @return the theta
   */
  public static double theta(double forward, double strike, double timeToExpiry, double normalVol, PutCall putCall) {
    int sign = putCall.isCall() ? 1 : -1;
    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = normalVol * rootT;
    if (sigmaRootT < NEAR_ZERO) {
      double x = sign * (forward - strike);
      // ambiguous if x and sigmaRootT are tiny, then reference number is returned
      return Math.abs(x) > NEAR_ZERO ? 0d : -0.5 * normalVol / rootT / Math.sqrt(2d * Math.PI);
    }
    double arg = (forward - strike) / sigmaRootT;
    double pdf = DISTRIBUTION.getPDF(arg);
    return -0.5 * pdf * normalVol / rootT;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the vega.
   * <p>
   * Note that the 'numeraire' is a simple multiplier and is the responsibility of the caller.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param normalVol  the normal volatility
   * @param putCall  whether it is put or call
   * @return the vega
   */
  public static double vega(double forward, double strike, double timeToExpiry, double normalVol, PutCall putCall) {
    int sign = putCall.isCall() ? 1 : -1;
    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = normalVol * rootT;
    if (sigmaRootT < NEAR_ZERO) {
      double x = sign * (forward - strike);
      // ambiguous if x and sigmaRootT are tiny, then reference number is returned
      return Math.abs(x) > NEAR_ZERO ? 0d : rootT / Math.sqrt(2d * Math.PI);
    }
    double arg = (forward - strike) / sigmaRootT;
    double pdf = DISTRIBUTION.getPDF(arg);
    return pdf * rootT;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied volatility.
   * <p>
   * If the volatility data is not zero, it is used as a starting point for the volatility search.
   * <p>
   * Note that the 'numeraire' is a simple multiplier and is the responsibility of the caller.
   * 
   * @param optionPrice  the price of the option
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param initialNormalVol  the normal volatility used to start the search
   * @param numeraire  the numeraire
   * @param putCall  whether it is put or call
   * @return the implied volatility
   */
  public static double impliedVolatility(
      double optionPrice,
      double forward,
      double strike,
      double timeToExpiry,
      double initialNormalVol,
      double numeraire,
      PutCall putCall) {

    double intrinsicPrice = numeraire * Math.max(0, (putCall.isCall() ? 1 : -1) * (forward - strike));
    ArgChecker.isTrue(optionPrice > intrinsicPrice || DoubleMath.fuzzyEquals(optionPrice, intrinsicPrice, 1e-6),
        "Option price (" + optionPrice + ") less than intrinsic value (" + intrinsicPrice + ")");
    if (Double.doubleToLongBits(optionPrice) == Double.doubleToLongBits(intrinsicPrice)) {
      return 0d;
    }
    double sigma = (Math.abs(initialNormalVol) < 1e-10 ? 0.3 * forward : initialNormalVol);
    double maxChange = 0.5 * forward;
    ValueDerivatives price = priceAdjoint(forward, strike, timeToExpiry, sigma, numeraire, putCall);
    double vega = price.getDerivative(1);
    double change = (price.getValue() - optionPrice) / vega;
    double sign = Math.signum(change);
    change = sign * Math.min(maxChange, Math.abs(change));
    if (change > 0 && change > sigma) {
      change = sigma;
    }
    int count = 0;
    while (Math.abs(change) > EPS) {
      sigma -= change;
      price = priceAdjoint(forward, strike, timeToExpiry, sigma, numeraire, putCall);
      vega = price.getDerivative(1);
      change = (price.getValue() - optionPrice) / vega;
      sign = Math.signum(change);
      change = sign * Math.min(maxChange, Math.abs(change));
      if (change > 0 && change > sigma) {
        change = sigma;
      }
      if (count++ > MAX_ITERATIONS) {
        BracketRoot bracketer = new BracketRoot();
        BisectionSingleRootFinder rootFinder = new BisectionSingleRootFinder(EPS);
        Function<Double, Double> func = new Function<Double, Double>() {
          @Override
          public Double apply(Double volatility) {
            return numeraire * price(forward, strike, timeToExpiry, volatility, putCall) - optionPrice;
          }
        };
        double[] range = bracketer.getBracketedPoints(func, 0d, 10d);
        return rootFinder.getRoot(func, range[0], range[1]);
      }
    }
    return sigma;
  }

  /**
   * Compute the implied volatility using an approximate explicit transformation formula.
   * <p>
   * Reference: Hagan, P. S. Volatility conversion calculator. Technical report, Bloomberg.
   * 
   * @param forward  the forward rate/price
   * @param strike  the option strike
   * @param timeToExpiry  the option time to maturity
   * @param blackVolatility  the Black implied volatility
   * @return the implied volatility
   */
  public static double impliedVolatilityFromBlackApproximated(
      double forward,
      double strike,
      double timeToExpiry,
      double blackVolatility) {
    ArgChecker.isTrue(strike > 0, "strike must be strictly positive");
    ArgChecker.isTrue(forward > 0, "strike must be strictly positive");
    double lnFK = Math.log(forward / strike);
    double s2t = blackVolatility * blackVolatility * timeToExpiry;
    if (Math.abs((forward - strike) / strike) < ATM_LIMIT) {
      double factor1 = Math.sqrt(forward * strike);
      double factor2 = (1.0d + lnFK * lnFK / 24.0d) / (1.0d + s2t / 24.0d + s2t * s2t / 5670.0d);
      return blackVolatility * factor1 * factor2;
    }
    double factor1 = (forward - strike) / lnFK;
    double factor2 = 1.0d / (1.0d + (1.0d - lnFK * lnFK / 120.0d) / 24.0d * s2t + s2t * s2t / 5670.0d);
    return blackVolatility * factor1 * factor2;
  }

  /**
   * Compute the implied volatility using an approximate explicit transformation formula and its derivative 
   * with respect to the input Black volatility.
   * <p>
   * Reference: Hagan, P. S. Volatility conversion calculator. Technical report, Bloomberg.
   * 
   * @param forward  the forward rate/price
   * @param strike  the option strike
   * @param timeToExpiry  the option time to maturity
   * @param blackVolatility  the Black implied volatility
   * @return the implied volatility and its derivative
   */
  public static ValueDerivatives impliedVolatilityFromBlackApproximatedAdjoint(
      double forward,
      double strike,
      double timeToExpiry,
      double blackVolatility) {
    ArgChecker.isTrue(strike > 0, "strike must be strictly positive");
    ArgChecker.isTrue(forward > 0, "strike must be strictly positive");
    double lnFK = Math.log(forward / strike);
    double s2t = blackVolatility * blackVolatility * timeToExpiry;
    if (Math.abs((forward - strike) / strike) < ATM_LIMIT) {
      double factor1 = Math.sqrt(forward * strike);
      double factor2 = (1.0d + lnFK * lnFK / 24.0d) / (1.0d + s2t / 24.0d + s2t * s2t / 5670.0d);
      double normalVol = blackVolatility * factor1 * factor2;
      // Backward sweep
      double blackVolatilityBar = factor1 * factor2;
      double factor2Bar = blackVolatility * factor1;
      double s2tBar = -(1.0d + lnFK * lnFK / 24.0d) /
          ((1.0d + s2t / 24.0d + s2t * s2t / 5670.0d) * (1.0d + s2t / 24.0d + s2t * s2t / 5670.0d)) *
          (1.0d / 24.0d + s2t / 2835.0d) * factor2Bar;
      blackVolatilityBar += 2.0d * blackVolatility * timeToExpiry * s2tBar;
      return ValueDerivatives.of(normalVol, DoubleArray.of(blackVolatilityBar));
    }
    double factor1 = (forward - strike) / lnFK;
    double factor2 = 1.0d / (1.0d + (1.0d - lnFK * lnFK / 120.0d) / 24.0d * s2t + s2t * s2t / 5670.0d);
    double normalVol = blackVolatility * factor1 * factor2;
    // Backward sweep
    double blackVolatilityBar = factor1 * factor2;
    double factor2Bar = blackVolatility * factor1;
    double s2tBar = -factor2 * factor2 * ((1.0d - lnFK * lnFK / 120.0d) / 24.0d + s2t / 2835.0d) * factor2Bar;
    blackVolatilityBar += 2.0d * blackVolatility * timeToExpiry * s2tBar;
    return ValueDerivatives.of(normalVol, DoubleArray.of(blackVolatilityBar));
  }

}
