/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

/**
 * Computes the price of an option in the normally distributed assets hypothesis (Bachelier model).
 */
public final class NormalPriceFunction {

  /**
   * The comparison value used to determine near-zero.
   */
  private static final double NEAR_ZERO = 1e-16;
  /**
   * The normal distribution implementation.
   */
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);

  /**
   * Gets the price function for the option.
   * 
   * @param option  the option description
   * @return the price function
   */
  public Function1D<NormalFunctionData, Double> getPriceFunction(EuropeanVanillaOption option) {
    ArgChecker.notNull(option, "option");
    double strike = option.getStrike();
    double t = option.getTimeToExpiry();
    return new Function1D<NormalFunctionData, Double>() {

      @SuppressWarnings("synthetic-access")
      @Override
      public Double evaluate(NormalFunctionData data) {
        ArgChecker.notNull(data, "data");
        double forward = data.getForward();
        double numeraire = data.getNumeraire();
        double sigma = data.getNormalVolatility();
        double sigmaRootT = sigma * Math.sqrt(t);
        int sign = option.isCall() ? 1 : -1;
        if (sigmaRootT < NEAR_ZERO) {
          double x = sign * (forward - strike);
          return (x > 0 ? numeraire * x : 0.0);
        }
        double arg = sign * (forward - strike) / sigmaRootT;
        return numeraire * (sign * (forward - strike) * NORMAL.getCDF(arg) + sigmaRootT * NORMAL.getPDF(arg));
      }
    };
  }

  /**
   * Computes the price of an option in the normally distributed assets hypothesis (Bachelier model).
   * The first order price derivatives are also provided.
   * 
   * @param option  the option description
   * @param data  the model data
   * @param priceDerivative  an array used to output the derivative of the price with respect to
   *  [0] forward, [1] volatility, [2] strike, the length of the must should be 3
   * @return the price
   */
  public double getPriceAdjoint(EuropeanVanillaOption option, NormalFunctionData data, double[] priceDerivative) {
    ArgChecker.notNull(option, "option");
    ArgChecker.notNull(data, "data");
    ArgChecker.notNull(priceDerivative, "derivatives");
    ArgChecker.isTrue(priceDerivative.length == 3, "array size");
    double strike = option.getStrike();
    double t = option.getTimeToExpiry();
    double forward = data.getForward();
    double numeraire = data.getNumeraire();
    double sigma = data.getNormalVolatility();
    int sign = option.isCall() ? 1 : -1;
    double price;
    double nCDF = 0.0;
    double nPDF = 0.0;
    double arg = 0.0;
    double x = 0.0;
    // Implementation Note: Forward sweep.
    double sigmaRootT = sigma * Math.sqrt(t);
    if (sigmaRootT < NEAR_ZERO) {
      x = sign * (forward - strike);
      price = (x > 0 ? numeraire * x : 0.0);
    } else {
      arg = sign * (forward - strike) / sigmaRootT;
      nCDF = NORMAL.getCDF(arg);
      nPDF = NORMAL.getPDF(arg);
      price = numeraire * (sign * (forward - strike) * nCDF + sigmaRootT * nPDF);
    }
    // Implementation Note: Backward sweep.
    double priceBar = 1.0;
    if (sigmaRootT < NEAR_ZERO) {
      double xBar = (x > 0 ? numeraire : 0.0);
      priceDerivative[0] = sign * xBar;
      priceDerivative[2] = -priceDerivative[0];
      priceDerivative[1] = 0.0;
    } else {
      double nCDFBar = numeraire * (sign * (forward - strike)) * priceBar;
      double nPDFBar = numeraire * sigmaRootT * priceBar;
      double argBar = nPDF * nCDFBar - nPDF * arg * nPDFBar;
      priceDerivative[0] = numeraire * sign * nCDF * priceBar + sign / sigmaRootT * argBar;
      priceDerivative[2] = -priceDerivative[0];
      double sigmaRootTBar = -arg / sigmaRootT * argBar + numeraire * nPDF * priceBar;
      priceDerivative[1] = Math.sqrt(t) * sigmaRootTBar;
    }
    return price;
  }

  /**
   * Computes forward delta of an option in the normally distributed assets hypothesis (Bachelier model).
   * 
   * @param option  the option description
   * @param data  the model data
   * @return delta
   */
  public double getDelta(EuropeanVanillaOption option, NormalFunctionData data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.notNull(data, "data");
    double strike = option.getStrike();
    double t = option.getTimeToExpiry();
    double forward = data.getForward();
    double numeraire = data.getNumeraire();
    double sigma = data.getNormalVolatility();
    int sign = option.isCall() ? 1 : -1;
    double sigmaRootT = sigma * Math.sqrt(t);
    if (sigmaRootT < NEAR_ZERO) {
      double x = sign * (forward - strike);
      if (Math.abs(x) <= NEAR_ZERO) {
        // ambiguous if x and sigmaRootT are tiny, then reference number is returned
        return sign * 0.5 * numeraire;
      }
      return x > 0 ? sign * numeraire : 0.0;
    }
    double arg = sign * (forward - strike) / sigmaRootT;
    double nCDF = NORMAL.getCDF(arg);
    return numeraire * sign * nCDF;
  }

  /**
   * Computes forward gamma of an option in the normally distributed assets hypothesis (Bachelier model).
   * 
   * @param option  the option description
   * @param data  the model data
   * @return gamma
   */
  public double getGamma(EuropeanVanillaOption option, NormalFunctionData data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.notNull(data, "data");
    double strike = option.getStrike();
    double t = option.getTimeToExpiry();
    double forward = data.getForward();
    double numeraire = data.getNumeraire();
    double sigma = data.getNormalVolatility();
    int sign = option.isCall() ? 1 : -1;
    double sigmaRootT = sigma * Math.sqrt(t);
    if (sigmaRootT < NEAR_ZERO) {
      double x = sign * (forward - strike);
      // ambiguous (tend to be infinite) if x and sigmaRootT are tiny, then reference number is returned
      return Math.abs(x) > NEAR_ZERO ? 0.0 : numeraire / Math.sqrt(2.0 * Math.PI) / sigmaRootT;
    }
    double arg = (forward - strike) / sigmaRootT;
    double nPDF = NORMAL.getPDF(arg);
    return numeraire * nPDF / sigmaRootT;
  }

  /**
   * Computes vega of an option in the normally distributed assets hypothesis (Bachelier model).
   * 
   * @param option  the option description
   * @param data  the model data
   * @return vega
   */
  public double getVega(EuropeanVanillaOption option, NormalFunctionData data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.notNull(data, "data");
    double strike = option.getStrike();
    double t = option.getTimeToExpiry();
    double forward = data.getForward();
    double numeraire = data.getNumeraire();
    double sigma = data.getNormalVolatility();
    int sign = option.isCall() ? 1 : -1;
    double rootT = Math.sqrt(t);
    double sigmaRootT = sigma * rootT;
    if (sigmaRootT < NEAR_ZERO) {
      double x = sign * (forward - strike);
      // ambiguous if x and sigmaRootT are tiny, then reference number is returned
      return Math.abs(x) > NEAR_ZERO ? 0.0 : numeraire * rootT / Math.sqrt(2.0 * Math.PI);
    }
    double arg = (forward - strike) / sigmaRootT;
    double nPDF = NORMAL.getPDF(arg);
    return numeraire * nPDF * rootT;
  }

  /**
   * Computes theta of an option in the normally distributed assets hypothesis (Bachelier model).
   * 
   * @param option  the option description
   * @param data  the model data
   * @return theta
   */
  public double getTheta(EuropeanVanillaOption option, NormalFunctionData data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.notNull(data, "data");
    double strike = option.getStrike();
    double t = option.getTimeToExpiry();
    double forward = data.getForward();
    double numeraire = data.getNumeraire();
    double sigma = data.getNormalVolatility();
    int sign = option.isCall() ? 1 : -1;
    double rootT = Math.sqrt(t);
    double sigmaRootT = sigma * rootT;
    if (sigmaRootT < NEAR_ZERO) {
      double x = sign * (forward - strike);
      // ambiguous if x and sigmaRootT are tiny, then reference number is returned
      return Math.abs(x) > NEAR_ZERO ? 0.0 : -0.5 * numeraire * sigma / rootT / Math.sqrt(2.0 * Math.PI);
    }
    double arg = (forward - strike) / sigmaRootT;
    double nPDF = NORMAL.getPDF(arg);
    return -0.5 * numeraire * nPDF * sigma / rootT;
  }

}
