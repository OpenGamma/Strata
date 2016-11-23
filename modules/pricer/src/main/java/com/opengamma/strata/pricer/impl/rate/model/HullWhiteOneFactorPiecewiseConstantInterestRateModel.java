/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.RidderSingleRootFinder;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParameters;

/**
 * Methods related to the Hull-White one factor (extended Vasicek) model with piecewise constant volatility.
 */
@BeanDefinition(style = "light")
public final class HullWhiteOneFactorPiecewiseConstantInterestRateModel implements ImmutableBean, Serializable {

  /**
   * Default instance.
   */
  public static final HullWhiteOneFactorPiecewiseConstantInterestRateModel DEFAULT =
      new HullWhiteOneFactorPiecewiseConstantInterestRateModel();

  //-------------------------------------------------------------------------
  /**
   * Calculates the future convexity factor used in future pricing.
   * <p>
   * The factor is called gamma in the reference: 
   * Henrard, M. "The Irony in the derivatives discounting Part II: the crisis", Wilmott Journal, 2010, 2, 301-316
   * 
   * @param data  the Hull-White model parameters
   * @param t0  the first expiry time
   * @param t1  the first reference time
   * @param t2  the second reference time
   * @return the factor
   */
  public double futuresConvexityFactor(
      HullWhiteOneFactorPiecewiseConstantParameters data,
      double t0,
      double t1,
      double t2) {

    double factor1 = Math.exp(-data.getMeanReversion() * t1) - Math.exp(-data.getMeanReversion() * t2);
    double numerator = 2 * data.getMeanReversion() * data.getMeanReversion() * data.getMeanReversion();
    int indexT0 = 1; // Period in which the time t0 is; volatilityTime[i-1] <= t0 < volatilityTime[i];
    while (t0 > data.getVolatilityTime().get(indexT0)) {
      indexT0++;
    }
    double[] s = new double[indexT0 + 1];
    System.arraycopy(data.getVolatilityTime().toArray(), 0, s, 0, indexT0);
    s[indexT0] = t0;
    double factor2 = 0.0;
    for (int loopperiod = 0; loopperiod < indexT0; loopperiod++) {
      factor2 += data.getVolatility().get(loopperiod) * data.getVolatility().get(loopperiod) *
          (Math.exp(data.getMeanReversion() * s[loopperiod + 1]) - Math.exp(data.getMeanReversion() * s[loopperiod])) *
          (2 - Math.exp(-data.getMeanReversion() * (t2 - s[loopperiod + 1])) -
              Math.exp(-data.getMeanReversion() * (t2 - s[loopperiod])));
    }
    return Math.exp(factor1 / numerator * factor2);
  }

  /**
   * Calculates the future convexity factor and its derivatives with respect to the model volatilities.
   * <p>
   * The factor is called gamma in the reference: 
   * Henrard, M. "The Irony in the derivatives discounting Part II: the crisis", Wilmott Journal, 2010, 2, 301-316
   * 
   * @param data  the Hull-White model parameters
   * @param t0  the expiry time
   * @param t1  the first reference time
   * @param t2  the second reference time
   * @return the factor and drivatives
   */
  public ValueDerivatives futuresConvexityFactorAdjoint(
      HullWhiteOneFactorPiecewiseConstantParameters data,
      double t0,
      double t1,
      double t2) {

    double factor1 = Math.exp(-data.getMeanReversion() * t1) - Math.exp(-data.getMeanReversion() * t2);
    double numerator = 2 * data.getMeanReversion() * data.getMeanReversion() * data.getMeanReversion();
    int indexT0 = 1; // Period in which the time t0 is; volatilityTime[i-1] <= t0 < volatilityTime[i];
    while (t0 > data.getVolatilityTime().get(indexT0)) {
      indexT0++;
    }
    double[] s = new double[indexT0 + 1];
    System.arraycopy(data.getVolatilityTime().toArray(), 0, s, 0, indexT0);
    s[indexT0] = t0;
    double factor2 = 0.0;
    double[] factorExp = new double[indexT0];
    for (int loopperiod = 0; loopperiod < indexT0; loopperiod++) {
      factorExp[loopperiod] =
          (Math.exp(data.getMeanReversion() * s[loopperiod + 1]) - Math.exp(data.getMeanReversion() * s[loopperiod])) *
              (2 - Math.exp(-data.getMeanReversion() * (t2 - s[loopperiod + 1])) -
                  Math.exp(-data.getMeanReversion() * (t2 - s[loopperiod])));
      factor2 += data.getVolatility().get(loopperiod) * data.getVolatility().get(loopperiod) * factorExp[loopperiod];
    }
    double factor = Math.exp(factor1 / numerator * factor2);
    // Backward sweep 
    double factorBar = 1.0;
    double factor2Bar = factor1 / numerator * factor * factorBar;
    double[] derivatives = new double[data.getVolatility().size()];
    for (int loopperiod = 0; loopperiod < indexT0; loopperiod++) {
      derivatives[loopperiod] = 2 * data.getVolatility().get(loopperiod) * factorExp[loopperiod] * factor2Bar;
    }
    return ValueDerivatives.of(factor, DoubleArray.ofUnsafe(derivatives));
  }

  /**
   * Calculates the payment delay convexity factor used in coupons with mismatched dates pricing.
   * 
   * @param parameters  the Hull-White model parameters
   * @param startExpiry  the start expiry time
   * @param endExpiry  the end expiry time
   * @param u  the fixing period start time
   * @param v  the fixing period end time
   * @param tp  the payment time
   * @return the factor
   */
  public double paymentDelayConvexityFactor(
      HullWhiteOneFactorPiecewiseConstantParameters parameters,
      double startExpiry,
      double endExpiry,
      double u,
      double v,
      double tp) {

    double a = parameters.getMeanReversion();
    double factor1 = (Math.exp(-a * v) - Math.exp(-a * tp)) * (Math.exp(-a * v) - Math.exp(-a * u));
    double numerator = 2 * a * a * a;
    int indexStart = Math.abs(Arrays.binarySearch(parameters.getVolatilityTime().toArray(), startExpiry) + 1);
    // Period in which the time startExpiry is; volatilityTime.get(i-1) <= startExpiry < volatilityTime.get(i);
    int indexEnd = Math.abs(Arrays.binarySearch(parameters.getVolatilityTime().toArray(), endExpiry) + 1);
    // Period in which the time endExpiry is; volatilityTime.get(i-1) <= endExpiry < volatilityTime.get(i);
    int sLen = indexEnd - indexStart + 1;
    double[] s = new double[sLen + 1];
    s[0] = startExpiry;
    System.arraycopy(parameters.getVolatilityTime().toArray(), indexStart, s, 1, sLen - 1);
    s[sLen] = endExpiry;
    double factor2 = 0.0;
    double[] exp2as = new double[sLen + 1];
    for (int loopperiod = 0; loopperiod < sLen + 1; loopperiod++) {
      exp2as[loopperiod] = Math.exp(2 * a * s[loopperiod]);
    }
    for (int loopperiod = 0; loopperiod < sLen; loopperiod++) {
      factor2 += parameters.getVolatility().get(loopperiod + indexStart - 1) *
          parameters.getVolatility().get(loopperiod + indexStart - 1) * (exp2as[loopperiod + 1] - exp2as[loopperiod]);
    }
    return Math.exp(factor1 * factor2 / numerator);
  }

  /**
   * Calculates the (zero-coupon) bond volatility divided by a bond numeraire, i.e., alpha, for a given period.
   * 
   * @param data  the Hull-White model data
   * @param startExpiry the start time of the expiry period
   * @param endExpiry  the end time of the expiry period
   * @param numeraireTime  the time to maturity for the bond numeraire
   * @param bondMaturity the time to maturity for the bond
   * @return the re-based bond volatility
   */
  public double alpha(
      HullWhiteOneFactorPiecewiseConstantParameters data,
      double startExpiry,
      double endExpiry,
      double numeraireTime,
      double bondMaturity) {

    double factor1 = Math.exp(-data.getMeanReversion() * numeraireTime) -
        Math.exp(-data.getMeanReversion() * bondMaturity);
    double numerator = 2 * data.getMeanReversion() * data.getMeanReversion() * data.getMeanReversion();
    int indexStart = Math.abs(Arrays.binarySearch(data.getVolatilityTime().toArray(), startExpiry) + 1);
    // Period in which the time startExpiry is; volatilityTime.get(i-1) <= startExpiry < volatilityTime.get(i);
    int indexEnd = Math.abs(Arrays.binarySearch(data.getVolatilityTime().toArray(), endExpiry) + 1);
    // Period in which the time endExpiry is; volatilityTime.get(i-1) <= endExpiry < volatilityTime.get(i);
    int sLen = indexEnd - indexStart + 1;
    double[] s = new double[sLen + 1];
    s[0] = startExpiry;
    System.arraycopy(data.getVolatilityTime().toArray(), indexStart, s, 1, sLen - 1);
    s[sLen] = endExpiry;
    double factor2 = 0d;
    double[] exp2as = new double[sLen + 1];
    for (int loopperiod = 0; loopperiod < sLen + 1; loopperiod++) {
      exp2as[loopperiod] = Math.exp(2 * data.getMeanReversion() * s[loopperiod]);
    }
    for (int loopperiod = 0; loopperiod < sLen; loopperiod++) {
      factor2 += data.getVolatility().get(loopperiod + indexStart - 1) *
          data.getVolatility().get(loopperiod + indexStart - 1) * (exp2as[loopperiod + 1] - exp2as[loopperiod]);
    }
    return factor1 * Math.sqrt(factor2 / numerator);
  }

  /**
   * Calculates the (zero-coupon) bond volatility divided by a bond numeraire, i.e., alpha, for a given period and 
   * its derivatives.
   * <p>
   * The derivative values are the derivatives of the function alpha with respect to the piecewise constant volatilities.
   *  
   * @param data  the Hull-White model data
   * @param startExpiry  the start time of the expiry period
   * @param endExpiry  the end time of the expiry period
   * @param numeraireTime  the time to maturity for the bond numeraire
   * @param bondMaturity  the time to maturity for the bond
   * @return The re-based bond volatility
   */
  public ValueDerivatives alphaAdjoint(
      HullWhiteOneFactorPiecewiseConstantParameters data,
      double startExpiry,
      double endExpiry,
      double numeraireTime,
      double bondMaturity) {

    // Forward sweep
    double factor1 = Math.exp(-data.getMeanReversion() * numeraireTime) -
        Math.exp(-data.getMeanReversion() * bondMaturity);
    double numerator = 2 * data.getMeanReversion() * data.getMeanReversion() * data.getMeanReversion();
    int indexStart = Math.abs(Arrays.binarySearch(data.getVolatilityTime().toArray(), startExpiry) + 1);
    // Period in which the time startExpiry is; volatilityTime.get(i-1) <= startExpiry < volatilityTime.get(i);
    int indexEnd = Math.abs(Arrays.binarySearch(data.getVolatilityTime().toArray(), endExpiry) + 1);
    // Period in which the time endExpiry is; volatilityTime.get(i-1) <= endExpiry < volatilityTime.get(i);
    int sLen = indexEnd - indexStart + 1;
    double[] s = new double[sLen + 1];
    s[0] = startExpiry;
    System.arraycopy(data.getVolatilityTime().toArray(), indexStart, s, 1, sLen - 1);
    s[sLen] = endExpiry;
    double factor2 = 0.0;
    double[] exp2as = new double[sLen + 1];
    for (int loopperiod = 0; loopperiod < sLen + 1; loopperiod++) {
      exp2as[loopperiod] = Math.exp(2 * data.getMeanReversion() * s[loopperiod]);
    }
    for (int loopperiod = 0; loopperiod < sLen; loopperiod++) {
      factor2 += data.getVolatility().get(loopperiod + indexStart - 1) *
          data.getVolatility().get(loopperiod + indexStart - 1) * (exp2as[loopperiod + 1] - exp2as[loopperiod]);
    }
    double sqrtFactor2Num = Math.sqrt(factor2 / numerator);
    double alpha = factor1 * sqrtFactor2Num;
    // Backward sweep 
    double alphaBar = 1.0;
    double factor2Bar = factor1 / sqrtFactor2Num / 2.0 / numerator * alphaBar;
    double[] derivatives = new double[data.getVolatility().size()];
    for (int loopperiod = 0; loopperiod < sLen; loopperiod++) {
      derivatives[loopperiod + indexStart - 1] = 2 * data.getVolatility().get(loopperiod + indexStart - 1) *
          (exp2as[loopperiod + 1] - exp2as[loopperiod]) * factor2Bar;
    }
    return ValueDerivatives.of(alpha, DoubleArray.ofUnsafe(derivatives));
  }

  /**
   * Calculates the exercise boundary for swaptions.
   * <p>
   * Reference: Henrard, M. (2003). "Explicit bond option and swaption formula in Heath-Jarrow-Morton one-factor model". 
   * International Journal of Theoretical and Applied Finance, 6(1):57--72.
   * 
   * @param discountedCashFlow  the cash flow equivalent discounted to today
   * @param alpha  the zero-coupon bond volatilities
   * @return the exercise boundary
   */
  public double kappa(DoubleArray discountedCashFlow, DoubleArray alpha) {
    final Function<Double, Double> swapValue = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        double error = 0.0;
        for (int loopcf = 0; loopcf < alpha.size(); loopcf++) {
          error += discountedCashFlow.get(loopcf) *
              Math.exp(-0.5 * alpha.get(loopcf) * alpha.get(loopcf) - (alpha.get(loopcf) - alpha.get(0)) * x);
        }
        return error;
      }
    };
    BracketRoot bracketer = new BracketRoot();
    double accuracy = 1.0E-8;
    RidderSingleRootFinder rootFinder = new RidderSingleRootFinder(accuracy);
    double[] range = bracketer.getBracketedPoints(swapValue, -2.0, 2.0);
    return rootFinder.getRoot(swapValue, range[0], range[1]);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the beta parameter.
   * <p>
   * This is intended to be used in particular for Bermudan swaption first step of the pricing.
   * <p>
   * Reference: Henrard, "M. Bermudan Swaptions in Gaussian HJM One-Factor Model: Analytical and Numerical Approaches". 
   * SSRN, October 2008. Available at SSRN: http://ssrn.com/abstract=1287982
   * 
   * @param data  the Hull-White model data
   * @param startExpiry the start time of the expiry period
   * @param endExpiry  the end time of the expiry period
   * @return the re-based bond volatility
   */
  public double beta(HullWhiteOneFactorPiecewiseConstantParameters data, double startExpiry, double endExpiry) {
    double numerator = 2 * data.getMeanReversion();
    int indexStart = 1; // Period in which the time startExpiry is; volatilityTime.get(i-1) <= startExpiry < volatilityTime.get(i);
    while (startExpiry > data.getVolatilityTime().get(indexStart)) {
      indexStart++;
    }
    int indexEnd = indexStart; // Period in which the time endExpiry is; volatilityTime.get(i-1) <= endExpiry < volatilityTime.get(i);
    while (endExpiry > data.getVolatilityTime().get(indexEnd)) {
      indexEnd++;
    }
    int sLen = indexEnd - indexStart + 1;
    double[] s = new double[sLen + 1];
    s[0] = startExpiry;
    System.arraycopy(data.getVolatilityTime().toArray(), indexStart, s, 1, sLen - 1);
    s[sLen] = endExpiry;
    double denominator = 0.0;
    for (int loopperiod = 0; loopperiod < sLen; loopperiod++) {
      denominator += data.getVolatility().get(loopperiod + indexStart - 1) *
          data.getVolatility().get(loopperiod + indexStart - 1) *
          (Math.exp(2 * data.getMeanReversion() * s[loopperiod + 1]) - Math.exp(2 * data.getMeanReversion() * s[loopperiod]));
    }
    return Math.sqrt(denominator / numerator);
  }

  /**
   * Calculates the common part of the exercise boundary of European swaptions forward.
   * <p>
   * This is intended to be used in particular for Bermudan swaption first step of the pricing.
   * <p>
   * Reference: Henrard, "M. Bermudan Swaptions in Gaussian HJM One-Factor Model: Analytical and Numerical Approaches". 
   * SSRN, October 2008. Available at SSRN: http://ssrn.com/abstract=1287982
   * 
   * @param discountedCashFlow  the swap discounted cash flows
   * @param alpha2  square of the alpha parameter
   * @param hwH  the H factors
   * @return the exercise boundary
   */
  public double lambda(DoubleArray discountedCashFlow, DoubleArray alpha2, DoubleArray hwH) {
    final Function<Double, Double> swapValue = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        double value = 0.0;
        for (int loopcf = 0; loopcf < alpha2.size(); loopcf++) {
          value += discountedCashFlow.get(loopcf) * Math.exp(-0.5 * alpha2.get(loopcf) - hwH.get(loopcf) * x);
        }
        return value;
      }
    };
    BracketRoot bracketer = new BracketRoot();
    double accuracy = 1.0E-8;
    RidderSingleRootFinder rootFinder = new RidderSingleRootFinder(accuracy);
    double[] range = bracketer.getBracketedPoints(swapValue, -2.0, 2.0);
    return rootFinder.getRoot(swapValue, range[0], range[1]);
  }

  /**
   * Calculates the maturity dependent part of the volatility (function called H in the implementation note).
   * 
   * @param hwParameters  the model parameters
   * @param u  the start time
   * @param v  the end time
   * @return the volatility
   */
  public DoubleMatrix volatilityMaturityPart(
      HullWhiteOneFactorPiecewiseConstantParameters hwParameters,
      double u,
      DoubleMatrix v) {

    double a = hwParameters.getMeanReversion();
    double[][] result = new double[v.rowCount()][];
    double expau = Math.exp(-a * u);
    for (int loopcf1 = 0; loopcf1 < v.rowCount(); loopcf1++) {
      DoubleArray vRow = v.row(loopcf1);
      result[loopcf1] = new double[vRow.size()];
      for (int loopcf2 = 0; loopcf2 < vRow.size(); loopcf2++) {
        result[loopcf1][loopcf2] = (expau - Math.exp(-a * vRow.get(loopcf2))) / a;
      }
    }
    return DoubleMatrix.copyOf(result);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the swap rate for a given value of the standard normal random variable
   * in the {@code P(*,theta)} numeraire.
   * 
   * @param x  the random variable value
   * @param discountedCashFlowFixed  the discounted cash flows equivalent of the swap fixed leg
   * @param alphaFixed  the zero-coupon bond volatilities for the swap fixed leg
   * @param discountedCashFlowIbor  the discounted cash flows equivalent of the swap Ibor leg
   * @param alphaIbor  the zero-coupon bond volatilities for the swap Ibor leg
   * @return the swap rate
   */
  public double swapRate(
      double x,
      DoubleArray discountedCashFlowFixed,
      DoubleArray alphaFixed,
      DoubleArray discountedCashFlowIbor,
      DoubleArray alphaIbor) {

    int sizeIbor = discountedCashFlowIbor.size();
    int sizeFixed = discountedCashFlowFixed.size();
    ArgChecker.isTrue(sizeIbor == alphaIbor.size(), "Length should be equal");
    ArgChecker.isTrue(sizeFixed == alphaFixed.size(), "Length should be equal");
    double numerator = 0.0;
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      numerator += discountedCashFlowIbor.get(loopcf) *
          Math.exp(-alphaIbor.get(loopcf) * x - 0.5 * alphaIbor.get(loopcf) * alphaIbor.get(loopcf));
    }
    double denominator = 0.0;
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      denominator += discountedCashFlowFixed.get(loopcf) *
          Math.exp(-alphaFixed.get(loopcf) * x - 0.5 * alphaFixed.get(loopcf) * alphaFixed.get(loopcf));
    }
    return -numerator / denominator;
  }

  /**
   * Calculates the first order derivative of the swap rate with respect to the value of the standard
   * normal random variable in the {@code P(*,theta)} numeraire.
   *
   * @param x  the random variable value
   * @param discountedCashFlowFixed  the discounted cash flows equivalent of the swap fixed leg
   * @param alphaFixed  the zero-coupon bond volatilities for the swap fixed leg
   * @param discountedCashFlowIbor  the discounted cash flows equivalent of the swap Ibor leg
   * @param alphaIbor  the zero-coupon bond volatilities for the swap Ibor leg
   * @return the first derivative of the swap rate
   */
  public double swapRateDx1(
      double x,
      DoubleArray discountedCashFlowFixed,
      DoubleArray alphaFixed,
      DoubleArray discountedCashFlowIbor,
      DoubleArray alphaIbor) {

    int sizeIbor = discountedCashFlowIbor.size();
    int sizeFixed = discountedCashFlowFixed.size();
    ArgChecker.isTrue(sizeIbor == alphaIbor.size(), "Length should be equal");
    ArgChecker.isTrue(sizeFixed == alphaFixed.size(), "Length should be equal");
    double f = 0.0;
    double df = 0.0;
    double term;
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      term = discountedCashFlowIbor.get(loopcf) *
          Math.exp(-alphaIbor.get(loopcf) * x - 0.5 * alphaIbor.get(loopcf) * alphaIbor.get(loopcf));
      f += term;
      df += -alphaIbor.get(loopcf) * term;
    }
    double g = 0.0;
    double dg = 0.0;
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      term = discountedCashFlowFixed.get(loopcf) *
          Math.exp(-alphaFixed.get(loopcf) * x - 0.5 * alphaFixed.get(loopcf) * alphaFixed.get(loopcf));
      g += term;
      dg += -alphaFixed.get(loopcf) * term;
    }
    return -(df * g - f * dg) / (g * g);
  }

  /**
   * Calculates the second order derivative of the swap rate with respect to the value
   * of the standard normal random variable in the {@code P(*,theta)} numeraire.
   * 
   * @param x  the random variable value
   * @param discountedCashFlowFixed  the discounted cash flows equivalent of the swap fixed leg
   * @param alphaFixed  the zero-coupon bond volatilities for the swap fixed leg
   * @param discountedCashFlowIbor  the discounted cash flows equivalent of the swap Ibor leg
   * @param alphaIbor  the zero-coupon bond volatilities for the swap Ibor leg
   * @return the second derivative of the swap rate
   */
  public double swapRateDx2(
      double x,
      DoubleArray discountedCashFlowFixed,
      DoubleArray alphaFixed,
      DoubleArray discountedCashFlowIbor,
      DoubleArray alphaIbor) {

    int sizeIbor = discountedCashFlowIbor.size();
    int sizeFixed = discountedCashFlowFixed.size();
    ArgChecker.isTrue(sizeIbor == alphaIbor.size(), "Length should be equal");
    ArgChecker.isTrue(sizeFixed == alphaFixed.size(), "Length should be equal");
    double f = 0.0;
    double df = 0.0;
    double df2 = 0.0;
    double term;
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      term = discountedCashFlowIbor.get(loopcf) *
          Math.exp(-alphaIbor.get(loopcf) * x - 0.5 * alphaIbor.get(loopcf) * alphaIbor.get(loopcf));
      f += term;
      df += -alphaIbor.get(loopcf) * term;
      df2 += alphaIbor.get(loopcf) * alphaIbor.get(loopcf) * term;
    }
    double g = 0.0;
    double dg = 0.0;
    double dg2 = 0.0;
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      term = discountedCashFlowFixed.get(loopcf) *
          Math.exp(-alphaFixed.get(loopcf) * x - 0.5 * alphaFixed.get(loopcf) * alphaFixed.get(loopcf));
      g += term;
      dg += -alphaFixed.get(loopcf) * term;
      dg2 += alphaFixed.get(loopcf) * alphaFixed.get(loopcf) * term;
    }
    double g2 = g * g;
    double g3 = g * g2;

    return -df2 / g + (2 * df * dg + f * dg2) / g2 - 2 * f * dg * dg / g3;
  }

  /**
   * Calculates the first order derivative of the swap rate with respect to
   * the {@code discountedCashFlowIbor} in the {@code P(*,theta)} numeraire.
   * 
   * @param x  the random variable value
   * @param discountedCashFlowFixed  the discounted cash flows equivalent of the swap fixed leg
   * @param alphaFixed  the zero-coupon bond volatilities for the swap fixed leg
   * @param discountedCashFlowIbor  the discounted cash flows equivalent of the swap Ibor leg
   * @param alphaIbor  the zero-coupon bond volatilities for the swap Ibor leg
   * @return the swap rate and derivatives
   */
  public ValueDerivatives swapRateDdcfi1(
      double x,
      DoubleArray discountedCashFlowFixed,
      DoubleArray alphaFixed,
      DoubleArray discountedCashFlowIbor,
      DoubleArray alphaIbor) {

    int sizeIbor = discountedCashFlowIbor.size();
    int sizeFixed = discountedCashFlowFixed.size();
    ArgChecker.isTrue(sizeIbor == alphaIbor.size(), "Length should be equal");
    ArgChecker.isTrue(sizeFixed == alphaFixed.size(), "Length should be equal");
    double denominator = 0.0;
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      denominator += discountedCashFlowFixed.get(loopcf) *
          Math.exp(-alphaFixed.get(loopcf) * x - 0.5 * alphaFixed.get(loopcf) * alphaFixed.get(loopcf));
    }
    double numerator = 0.0;
    double[] swapRateDdcfi1 = new double[sizeIbor];
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      double exp = Math.exp(-alphaIbor.get(loopcf) * x - 0.5 * alphaIbor.get(loopcf) * alphaIbor.get(loopcf));
      swapRateDdcfi1[loopcf] = -exp / denominator;
      numerator += discountedCashFlowIbor.get(loopcf) * exp;
    }
    return ValueDerivatives.of(-numerator / denominator, DoubleArray.ofUnsafe(swapRateDdcfi1));
  }

  /**
   * Calculates the first order derivative of the swap rate with respect to the
   * {@code discountedCashFlowFixed} in the {@code P(*,theta)} numeraire.
   * 
   * @param x  the random variable value
   * @param discountedCashFlowFixed  the discounted cash flows equivalent of the swap fixed leg
   * @param alphaFixed  the zero-coupon bond volatilities for the swap fixed leg
   * @param discountedCashFlowIbor  the discounted cash flows equivalent of the swap Ibor leg
   * @param alphaIbor  the zero-coupon bond volatilities for the swap Ibor leg
   * @return the swap rate and derivatives
   */
  public ValueDerivatives swapRateDdcff1(
      double x,
      DoubleArray discountedCashFlowFixed,
      DoubleArray alphaFixed,
      DoubleArray discountedCashFlowIbor,
      DoubleArray alphaIbor) {

    int sizeIbor = discountedCashFlowIbor.size();
    int sizeFixed = discountedCashFlowFixed.size();
    ArgChecker.isTrue(sizeIbor == alphaIbor.size(), "Length should be equal");
    ArgChecker.isTrue(sizeFixed == alphaFixed.size(), "Length should be equal");
    double[] expD = new double[sizeIbor];
    double numerator = 0.0;
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      numerator += discountedCashFlowIbor.get(loopcf) *
          Math.exp(-alphaIbor.get(loopcf) * x - 0.5 * alphaIbor.get(loopcf) * alphaIbor.get(loopcf));
    }
    double denominator = 0.0;
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      expD[loopcf] = Math.exp(-alphaFixed.get(loopcf) * x - 0.5 * alphaFixed.get(loopcf) * alphaFixed.get(loopcf));
      denominator += discountedCashFlowFixed.get(loopcf) * expD[loopcf];
    }
    double ratio = numerator / (denominator * denominator);
    double[] swapRateDdcff1 = new double[sizeFixed];
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      swapRateDdcff1[loopcf] = ratio * expD[loopcf];
    }
    return ValueDerivatives.of(-numerator / denominator, DoubleArray.ofUnsafe(swapRateDdcff1));
  }

  /**
   * Calculates the first order derivative of the swap rate with respect to the {@code alphaIbor} 
   * in the {@code P(*,theta)} numeraire.
   * 
   * @param x  the random variable value
   * @param discountedCashFlowFixed  the discounted cash flows equivalent of the swap fixed leg
   * @param alphaFixed  the zero-coupon bond volatilities for the swap fixed leg
   * @param discountedCashFlowIbor  the discounted cash flows equivalent of the swap Ibor leg
   * @param alphaIbor  the zero-coupon bond volatilities for the swap Ibor leg
   * @return the swap rate and derivatives
   */
  public ValueDerivatives swapRateDai1(
      double x,
      DoubleArray discountedCashFlowFixed,
      DoubleArray alphaFixed,
      DoubleArray discountedCashFlowIbor,
      DoubleArray alphaIbor) {

    int sizeIbor = discountedCashFlowIbor.size();
    int sizeFixed = discountedCashFlowFixed.size();
    ArgChecker.isTrue(sizeIbor == alphaIbor.size(), "Length should be equal");
    ArgChecker.isTrue(sizeFixed == alphaFixed.size(), "Length should be equal");
    double denominator = 0.0;
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      denominator += discountedCashFlowFixed.get(loopcf) *
          Math.exp(-alphaFixed.get(loopcf) * x - 0.5 * alphaFixed.get(loopcf) * alphaFixed.get(loopcf));
    }
    double numerator = 0.0;
    double[] swapRateDai1 = new double[sizeIbor];
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      double exp = Math.exp(-alphaIbor.get(loopcf) * x - 0.5 * alphaIbor.get(loopcf) * alphaIbor.get(loopcf));
      swapRateDai1[loopcf] = discountedCashFlowIbor.get(loopcf) * exp * (x + alphaIbor.get(loopcf)) / denominator;
      numerator += discountedCashFlowIbor.get(loopcf) * exp;
    }
    return ValueDerivatives.of(-numerator / denominator, DoubleArray.ofUnsafe(swapRateDai1));
  }

  /**
   * Calculates the first order derivative of the swap rate with respect to the {@code alphaFixed} 
   * in the {@code P(*,theta)} numeraire.
   * 
   * @param x  the random variable value.
   * @param discountedCashFlowFixed  the discounted cash flows equivalent of the swap fixed leg
   * @param alphaFixed  the zero-coupon bond volatilities for the swap fixed leg
   * @param discountedCashFlowIbor  the discounted cash flows equivalent of the swap Ibor leg
   * @param alphaIbor  the zero-coupon bond volatilities for the swap Ibor leg
   * @return the swap rate and derivatives
   */
  public ValueDerivatives swapRateDaf1(
      double x,
      DoubleArray discountedCashFlowFixed,
      DoubleArray alphaFixed,
      DoubleArray discountedCashFlowIbor,
      DoubleArray alphaIbor) {

    int sizeIbor = discountedCashFlowIbor.size();
    int sizeFixed = discountedCashFlowFixed.size();
    ArgChecker.isTrue(sizeIbor == alphaIbor.size(), "Length should be equal");
    ArgChecker.isTrue(sizeFixed == alphaFixed.size(), "Length should be equal");
    double[] expD = new double[sizeIbor];
    double numerator = 0.0;
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      numerator += discountedCashFlowIbor.get(loopcf) *
          Math.exp(-alphaIbor.get(loopcf) * x - 0.5 * alphaIbor.get(loopcf) * alphaIbor.get(loopcf));
    }
    double denominator = 0.0;
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      expD[loopcf] = discountedCashFlowFixed.get(loopcf) *
          Math.exp(-alphaFixed.get(loopcf) * x - 0.5 * alphaFixed.get(loopcf) * alphaFixed.get(loopcf));
      denominator += expD[loopcf];
    }
    double ratio = numerator / (denominator * denominator);
    double[] swapRateDaf1 = new double[sizeFixed];
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      swapRateDaf1[loopcf] = ratio * expD[loopcf] * (-x - alphaFixed.get(loopcf));
    }
    return ValueDerivatives.of(-numerator / denominator, DoubleArray.ofUnsafe(swapRateDaf1));
  }

  /**
   * Calculates the first order derivative with respect to the discountedCashFlowFixed and to the discountedCashFlowIbor 
   * of the of swap rate second derivative with respect to the random variable x in the {@code P(*,theta)} numeraire.
   * <p>
   * The result is made of a pair of arrays. The first one is the derivative with respect to {@code discountedCashFlowFixed} 
   * and the second one with respect to {@code discountedCashFlowIbor}.
   * 
   * @param x  the random variable value
   * @param discountedCashFlowFixed  the discounted cash flows equivalent of the swap fixed leg
   * @param alphaFixed  the zero-coupon bond volatilities for the swap fixed leg
   * @param discountedCashFlowIbor  the discounted cash flows equivalent of the swap Ibor leg
   * @param alphaIbor  the zero-coupon bond volatilities for the swap Ibor leg
   * @return the swap rate derivatives
   */
  public Pair<DoubleArray, DoubleArray> swapRateDx2Ddcf1(
      double x,
      DoubleArray discountedCashFlowFixed,
      DoubleArray alphaFixed,
      DoubleArray discountedCashFlowIbor,
      DoubleArray alphaIbor) {

    int sizeIbor = discountedCashFlowIbor.size();
    int sizeFixed = discountedCashFlowFixed.size();
    ArgChecker.isTrue(sizeIbor == alphaIbor.size(), "Length should be equal");
    ArgChecker.isTrue(sizeFixed == alphaFixed.size(), "Length should be equal");
    double f = 0.0;
    double df = 0.0;
    double df2 = 0.0;
    double[] termIbor = new double[sizeIbor];
    double[] expIbor = new double[sizeIbor];
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      expIbor[loopcf] = Math.exp(-alphaIbor.get(loopcf) * x - 0.5 * alphaIbor.get(loopcf) * alphaIbor.get(loopcf));
      termIbor[loopcf] = discountedCashFlowIbor.get(loopcf) * expIbor[loopcf];
      f += termIbor[loopcf];
      df += -alphaIbor.get(loopcf) * termIbor[loopcf];
      df2 += alphaIbor.get(loopcf) * alphaIbor.get(loopcf) * termIbor[loopcf];
    }
    double g = 0.0;
    double dg = 0.0;
    double dg2 = 0.0;
    double[] termFixed = new double[sizeFixed];
    double[] expFixed = new double[sizeFixed];
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      expFixed[loopcf] = Math.exp(-alphaFixed.get(loopcf) * x - 0.5 * alphaFixed.get(loopcf) * alphaFixed.get(loopcf));
      termFixed[loopcf] = discountedCashFlowFixed.get(loopcf) * expFixed[loopcf];
      g += termFixed[loopcf];
      dg += -alphaFixed.get(loopcf) * termFixed[loopcf];
      dg2 += alphaFixed.get(loopcf) * alphaFixed.get(loopcf) * termFixed[loopcf];
    }
    double g2 = g * g;
    double g3 = g * g2;
    double g4 = g * g3;
    // Backward sweep
    double dx2Bar = 1d;
    double gBar = (df2 / g2 - 2d * f * dg2 / g3 - 4d * df * dg / g3 + 6d * dg * dg * f / g4) * dx2Bar;
    double dgBar = (2d * df / g2 - 4d * f * dg / g3) * dx2Bar;
    double dg2Bar = f / g2 * dx2Bar;
    double fBar = (dg2 / g2 - 2d * dg * dg / g3) * dx2Bar;
    double dfBar = 2d * dg / g2 * dx2Bar;
    double df2Bar = -dx2Bar / g;

    double[] discountedCashFlowFixedBar = new double[sizeFixed];
    double[] termFixedBar = new double[sizeFixed];
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      termFixedBar[loopcf] = gBar - alphaFixed.get(loopcf) * dgBar + alphaFixed.get(loopcf) * alphaFixed.get(loopcf) * dg2Bar;
      discountedCashFlowFixedBar[loopcf] = expFixed[loopcf] * termFixedBar[loopcf];
    }
    double[] discountedCashFlowIborBar = new double[sizeIbor];
    double[] termIborBar = new double[sizeIbor];
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      termIborBar[loopcf] = fBar - alphaIbor.get(loopcf) * dfBar + alphaIbor.get(loopcf) * alphaIbor.get(loopcf) * df2Bar;
      discountedCashFlowIborBar[loopcf] = expIbor[loopcf] * termIborBar[loopcf];
    }
    return Pair.of(DoubleArray.copyOf(discountedCashFlowFixedBar), DoubleArray.copyOf(discountedCashFlowIborBar));
  }

  /**
   * Calculates the first order derivative with respect to the alphaFixed and to the alphaIbor of
   * the of swap rate second derivative with respect to the random variable x in the
   * {@code P(*,theta)} numeraire.
   * <p>
   * The result is made of a pair of arrays. The first one is the derivative with respect to {@code alphaFixed} and 
   * the second one with respect to {@code alphaIbor}.
   * 
   * @param x  the random variable value
   * @param discountedCashFlowFixed  the discounted cash flows equivalent of the swap fixed leg
   * @param alphaFixed  the zero-coupon bond volatilities for the swap fixed leg
   * @param discountedCashFlowIbor  the discounted cash flows equivalent of the swap Ibor leg
   * @param alphaIbor  the zero-coupon bond volatilities for the swap Ibor leg
   * @return the swap rate derivatives
   */
  public Pair<DoubleArray, DoubleArray> swapRateDx2Da1(double x,
      DoubleArray discountedCashFlowFixed,
      DoubleArray alphaFixed,
      DoubleArray discountedCashFlowIbor,
      DoubleArray alphaIbor) {

    int sizeIbor = discountedCashFlowIbor.size();
    int sizeFixed = discountedCashFlowFixed.size();
    ArgChecker.isTrue(sizeIbor == alphaIbor.size(), "Length should be equal");
    ArgChecker.isTrue(sizeFixed == alphaFixed.size(), "Length should be equal");
    double f = 0.0;
    double df = 0.0;
    double df2 = 0.0;
    double[] termIbor = new double[sizeIbor];
    double[] expIbor = new double[sizeIbor];
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      expIbor[loopcf] = Math.exp(-alphaIbor.get(loopcf) * x - 0.5 * alphaIbor.get(loopcf) * alphaIbor.get(loopcf));
      termIbor[loopcf] = discountedCashFlowIbor.get(loopcf) * expIbor[loopcf];
      f += termIbor[loopcf];
      df += -alphaIbor.get(loopcf) * termIbor[loopcf];
      df2 += alphaIbor.get(loopcf) * alphaIbor.get(loopcf) * termIbor[loopcf];
    }
    double g = 0.0;
    double dg = 0.0;
    double dg2 = 0.0;
    double[] termFixed = new double[sizeFixed];
    double[] expFixed = new double[sizeFixed];
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      expFixed[loopcf] = Math.exp(-alphaFixed.get(loopcf) * x - 0.5 * alphaFixed.get(loopcf) * alphaFixed.get(loopcf));
      termFixed[loopcf] = discountedCashFlowFixed.get(loopcf) * expFixed[loopcf];
      g += termFixed[loopcf];
      dg += -alphaFixed.get(loopcf) * termFixed[loopcf];
      dg2 += alphaFixed.get(loopcf) * alphaFixed.get(loopcf) * termFixed[loopcf];
    }
    double g2 = g * g;
    double g3 = g * g2;
    double g4 = g * g3;
    // Backward sweep
    double dx2Bar = 1d;
    double gBar = (df2 / g2 - 2d * f * dg2 / g3 - 4d * df * dg / g3 + 6d * dg * dg * f / g4) * dx2Bar;
    double dgBar = (2d * df / g2 - 4d * f * dg / g3) * dx2Bar;
    double dg2Bar = f / g2 * dx2Bar;
    double fBar = (dg2 / g2 - 2d * dg * dg / g3) * dx2Bar;
    double dfBar = 2d * dg / g2 * dx2Bar;
    double df2Bar = -dx2Bar / g;

    double[] alphaFixedBar = new double[sizeFixed];
    double[] termFixedBar = new double[sizeFixed];
    for (int loopcf = 0; loopcf < sizeFixed; loopcf++) {
      termFixedBar[loopcf] = gBar - alphaFixed.get(loopcf) * dgBar + alphaFixed.get(loopcf) * alphaFixed.get(loopcf) * dg2Bar;
      alphaFixedBar[loopcf] = termFixed[loopcf] * (-x - alphaFixed.get(loopcf)) * termFixedBar[loopcf] -
          termFixed[loopcf] * dgBar + 2d * alphaFixed.get(loopcf) * termFixed[loopcf] * dg2Bar;
    }
    double[] alphaIborBar = new double[sizeIbor];
    double[] termIborBar = new double[sizeIbor];
    for (int loopcf = 0; loopcf < sizeIbor; loopcf++) {
      termIborBar[loopcf] = fBar - alphaIbor.get(loopcf) * dfBar + alphaIbor.get(loopcf) * alphaIbor.get(loopcf) * df2Bar;
      alphaIborBar[loopcf] = termIbor[loopcf] * (-x - alphaIbor.get(loopcf)) * termIborBar[loopcf] - termIbor[loopcf] *
          dfBar + 2d * alphaIbor.get(loopcf) * termIbor[loopcf] * df2Bar;
    }
    return Pair.of(DoubleArray.copyOf(alphaFixedBar), DoubleArray.copyOf(alphaIborBar));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code HullWhiteOneFactorPiecewiseConstantInterestRateModel}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(HullWhiteOneFactorPiecewiseConstantInterestRateModel.class);

  /**
   * The meta-bean for {@code HullWhiteOneFactorPiecewiseConstantInterestRateModel}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private HullWhiteOneFactorPiecewiseConstantInterestRateModel() {
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(32);
    buf.append("HullWhiteOneFactorPiecewiseConstantInterestRateModel{");
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------

}
