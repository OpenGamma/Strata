/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile.function;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.math.DoubleMath;
import com.google.common.primitives.Doubles;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;

/**
 * The Hagan SABR volatility function provider.
 * <p>
 * This class provides the functions of volatility and its sensitivity to the SABR model parameters based on the original
 * Hagan SABR formula.  
 * <p>
 * Reference: Hagan, P.; Kumar, D.; Lesniewski, A. & Woodward, D. "Managing smile risk", Wilmott Magazine, 2002, September, 84-108
 */
@BeanDefinition
public final class SABRHaganVolatilityFunctionProvider
    extends VolatilityFunctionProvider<SABRFormulaData> implements ImmutableBean, Serializable {

  /**
   * Default implementation. 
   */
  public static final SABRHaganVolatilityFunctionProvider DEFAULT = new SABRHaganVolatilityFunctionProvider();

  /* internal parameters */
  private static final double CUTOFF_MONEYNESS = 1e-12;
  private static final double SMALL_Z = 1e-6;
  private static final double LARGE_NEG_Z = -1e6;
  private static final double LARGE_POS_Z = 1e8;
  private static final double BETA_EPS = 1e-8;
  private static final double RHO_EPS = 1e-5;
  private static final double RHO_EPS_NEGATIVE = 1e-8;
  private static final double ATM_EPS = 1e-7;

  @Override
  public Function1D<SABRFormulaData, Double> getVolatilityFunction(EuropeanVanillaOption option, double forward) {

    ArgChecker.notNull(option, "option");
    ArgChecker.isTrue(forward >= 0.0, "forward must be greater than zero");
    return new Function1D<SABRFormulaData, Double>() {
      @Override
      public Double evaluate(SABRFormulaData data) {
        ArgChecker.notNull(data, "data");
        return getVolatility(option, forward, data);
      }
    };
  }

  @Override
  public Function1D<SABRFormulaData, double[]> getVolatilityAdjointFunction(EuropeanVanillaOption option, double forward) {

    ArgChecker.notNull(option, "option");
    ArgChecker.isTrue(forward >= 0.0, "forward must be greater than zero");
    return new Function1D<SABRFormulaData, double[]>() {
      @Override
      public double[] evaluate(SABRFormulaData data) {
        ArgChecker.notNull(data, "data");
        return getVolatilityAdjoint(option, forward, data);
      }
    };
  }

  @Override
  public Function1D<SABRFormulaData, double[][]> getVolatilityAdjointFunction(
      double forward,
      double[] strikes,
      double timeToExpiry) {

    return getVolatilityAdjointFunctionByCallingSingleStrikes(forward, strikes, timeToExpiry);
  }

  @Override
  public Function1D<SABRFormulaData, double[]> getModelAdjointFunction(EuropeanVanillaOption option, double forward) {

    ArgChecker.notNull(option, "option");
    ArgChecker.isTrue(forward >= 0.0, "forward must be greater than zero");
    return new Function1D<SABRFormulaData, double[]>() {
      @Override
      public double[] evaluate(SABRFormulaData data) {
        ArgChecker.notNull(data, "data");
        return getVolatilityModelAdjoint(option, forward, data);
      }
    };
  }

  @Override
  public Function1D<SABRFormulaData, double[][]> getModelAdjointFunction(
      double forward,
      double[] strikes,
      double timeToExpiry) {

    return getModelAdjointFunctionByCallingSingleStrikes(forward, strikes, timeToExpiry);
  }

  /**
   * Computes volatility based on the standard Hagan formula. 
   * 
   * @param option  the option.
   * @param forward  the forward value of the underlying
   * @param data  the SABR data.
   * @return the volatility
   */
  public double getVolatility(EuropeanVanillaOption option, double forward, SABRFormulaData data) {

    double timeToExpiry = option.getTimeToExpiry();
    double strike = option.getStrike();
    double alpha = data.getAlpha();
    double beta = data.getBeta();
    double rho = data.getRho();
    double nu = data.getNu();
    if (alpha == 0.0) {
      return 0.0;
    }
    double cutoff = forward * CUTOFF_MONEYNESS;
    double k;
    if (strike < cutoff) {
      Logger s_logger = LoggerFactory.getLogger(SABRHaganVolatilityFunctionProvider.class);
      s_logger.info("Given strike of {} is less than cutoff at {}, therefore the strike is taken as {}", new Object[] {
        strike, cutoff, cutoff });
      k = cutoff;
    } else {
      k = strike;
    }
    double vol, z, zOverChi;
    double beta1 = 1 - beta;
    if (DoubleMath.fuzzyEquals(forward, k, ATM_EPS)) {
      double f1 = Math.pow(forward, beta1);
      vol = alpha *
          (1 + timeToExpiry *
              (beta1 * beta1 * alpha * alpha / 24 / f1 / f1 + rho * alpha * beta * nu / 4 / f1 + nu * nu *
                  (2 - 3 * rho * rho) / 24)) / f1;
    } else {
      if (DoubleMath.fuzzyEquals(beta, 0, BETA_EPS)) {
        double ln = Math.log(forward / k);
        z = nu * Math.sqrt(forward * k) * ln / alpha;
        zOverChi = getZOverChi(rho, z);
        vol = alpha * ln * zOverChi *
            (1 + timeToExpiry * (alpha * alpha / forward / k + nu * nu * (2 - 3 * rho * rho)) / 24) / (forward - k);
      } else if (DoubleMath.fuzzyEquals(beta, 1, BETA_EPS)) {
        double ln = Math.log(forward / k);
        z = nu * ln / alpha;
        zOverChi = getZOverChi(rho, z);
        vol = alpha * zOverChi * (1 + timeToExpiry * (rho * alpha * nu / 4 + nu * nu * (2 - 3 * rho * rho) / 24));
      } else {
        double ln = Math.log(forward / k);
        double f1 = Math.pow(forward * k, beta1);
        double f1Sqrt = Math.sqrt(f1);
        double lnBetaSq = Math.pow(beta1 * ln, 2);
        z = nu * f1Sqrt * ln / alpha;
        zOverChi = getZOverChi(rho, z);
        double first = alpha / (f1Sqrt * (1 + lnBetaSq / 24 + lnBetaSq * lnBetaSq / 1920));
        double second = zOverChi;
        double third = 1 + timeToExpiry * (beta1 * beta1 * alpha * alpha / 24 / f1 +
            rho * nu * beta * alpha / 4 / f1Sqrt + nu * nu * (2 - 3 * rho * rho) / 24);
        vol = first * second * third;
      }
    }
    //There is nothing to prevent the nu * nu * (2 - 3 * rho * rho) / 24 part taking the third term, and hence the volatility negative
    return vol;
    // return Math.max(0.0, vol);
  }

  /**
   * Computes volatility based on the standard Hagan formula. 
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value
   * @param timeToExpiry  the time to expiry
   * @param alpha  the alpha parameter
   * @param beta  the beta parameter
   * @param rho  the rho parameter
   * @param nu  the nu parameter
   * @return the volatility
   */
  public double getVolatility(double forward, double strike, double timeToExpiry, double alpha, double beta,
      double rho, double nu) {
    ArgChecker.isTrue(forward > 0, "Forward must be > 0");
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, timeToExpiry, PutCall.CALL);
    SABRFormulaData data = SABRFormulaData.of(alpha, beta, rho, nu);
    return getVolatility(option, forward, data);
  }

  /**
   * Computes the volatility sensitivity to the SABR parameters. 
   * <p>
   * This returns an array with alpha, beta, rho and nu sensitivities.
   * 
   * @param option  the option.
   * @param forward  the forward value of the underlying
   * @param data  the SABR data.
   * @return the sensitivities
   */
  public double[] getVolatilityModelAdjoint(EuropeanVanillaOption option, double forward, SABRFormulaData data) {

    double[] volatilityAdjoint = new double[4];
    double alpha = data.getAlpha();
    double strike = option.getStrike();
    double cutoff = forward * CUTOFF_MONEYNESS;
    if (strike < cutoff) {
      Logger s_logger = LoggerFactory.getLogger(SABRHaganVolatilityFunctionProvider.class);
      s_logger.info("Given strike of {} is less than cutoff at {}, therefore the strike is taken as {}",
          new Object[] {strike, cutoff, cutoff });
      strike = cutoff;
    }
    double timeToExpiry = option.getTimeToExpiry();
    double beta = data.getBeta();
    double betaStar = 1 - beta;
    double rho = data.getRho();
    double nu = data.getNu();
    if (alpha == 0.0) {
      Arrays.fill(volatilityAdjoint, 0.0);
      if (DoubleMath.fuzzyEquals(forward, strike, ATM_EPS)) { //TODO this should be relative
        volatilityAdjoint[3] = (1 + (2 - 3 * rho * rho) * nu * nu / 24 * timeToExpiry) / Math.pow(forward, betaStar);
      } else {
        //for non-atm options the alpha sensitivity at alpha = 0 is infinite. Returning this will most likely break calibrations,
        // so we return an arbitrary large number
        volatilityAdjoint[3] = 1e7;
      }
      return volatilityAdjoint;
    }

    // Implementation note: Forward sweep.
    double sfK = Math.pow(forward * strike, betaStar / 2);
    double lnrfK = Math.log(forward / strike);
    double z = nu / alpha * sfK * lnrfK;
    double sf1 = sfK *
        (1 + betaStar * betaStar / 24 * (lnrfK * lnrfK) + Math.pow(betaStar, 4) / 1920 * Math.pow(lnrfK, 4));
    double sf2 = (1 + (Math.pow(betaStar * alpha / sfK, 2) / 24 + (rho * beta * nu * alpha) / (4 * sfK) + (2 - 3 *
        rho * rho) *
        nu * nu / 24) *
        timeToExpiry);

    // Implementation note: Backward sweep.
    double[] zOverChi = zOverChiWithDev(rho, z);
    double vBar = 1;
    double sf2Bar = alpha / sf1 * zOverChi[0] * vBar;
    double sf1Bar = -alpha / (sf1 * sf1) * zOverChi[0] * sf2 * vBar;
    double rzxzBar = alpha / sf1 * sf2 * vBar;
    double zBar = zOverChi[2] * rzxzBar;
    //    double xzBar = 0;

    double sfKBar = nu / alpha * lnrfK * zBar + sf1 / sfK * sf1Bar -
        (Math.pow(betaStar * alpha, 2) / Math.pow(sfK, 3) / 12 + (rho * beta * nu * alpha) / 4 / (sfK * sfK)) *
        timeToExpiry
        * sf2Bar;

    double nuBar = 1 / alpha * sfK * lnrfK * zBar +
        ((rho * beta * alpha) / (4 * sfK) + (2 - 3 * rho * rho) * nu / 12) * timeToExpiry * sf2Bar;
    double rhoBar = zOverChi[1] * rzxzBar + ((beta * nu * alpha) / (4 * sfK) - rho * nu * nu / 4) * timeToExpiry *
        sf2Bar;

    double alphaBar = -nu / (alpha * alpha) * sfK * lnrfK * zBar +
        ((betaStar * alpha / sfK) * (betaStar / sfK) / 12 + (rho * beta * nu) / (4 * sfK)) * timeToExpiry * sf2Bar + 1 /
        sf1
        * zOverChi[0] * sf2 * vBar;
    double betaBar = -0.5 * Math.log(forward * strike) * sfK * sfKBar - sfK *
        (betaStar / 12 * (lnrfK * lnrfK) + Math.pow(betaStar, 3) / 480 * Math.pow(lnrfK, 4)) * sf1Bar
        + (-betaStar * alpha * alpha / sfK / sfK / 12 + rho * nu * alpha / 4 / sfK) * timeToExpiry * sf2Bar;

    volatilityAdjoint[0] = alphaBar;
    volatilityAdjoint[1] = betaBar;
    volatilityAdjoint[2] = rhoBar;
    volatilityAdjoint[3] = nuBar;

    return volatilityAdjoint;
  }

  /**
   * Computes the Black implied volatility in the SABR model and its derivatives.
   * <p>
   * This returns an array with [0] the volatility, [1] Derivative w.r.t the forward, [2] the derivative w.r.t the strike, 
   * [3] the derivative w.r.t. to alpha, [4] the derivative w.r.t. to beta, [5] the derivative w.r.t. to rho, and 
   * [6] the derivative w.r.t. to nu.
   * 
   * @param option The option.
   * @param forward The forward value of the underlying
   * @param data The SABR data.
   * @return the volatility and sensitivities
   */
  public double[] getVolatilityAdjoint(EuropeanVanillaOption option, double forward, SABRFormulaData data) {
    /**
     * The array storing the price and derivatives.
     */
     double[] volatilityAdjoint = new double[7];
     double alpha = data.getAlpha();

    double strike = option.getStrike();
     double cutoff = forward * CUTOFF_MONEYNESS;
    if (strike < cutoff) {
      Logger s_logger = LoggerFactory.getLogger(SABRHaganVolatilityFunctionProvider.class);
      s_logger.info(
          "Given strike of {} is less than cutoff at {}, therefore the strike is taken as {}",
          new Object[] {strike, cutoff, cutoff });
      strike = cutoff;
    }

     double timeToExpiry = option.getTimeToExpiry();

     double beta = data.getBeta();
     double betaStar = 1 - beta;
     double rho = data.getRho();
     double nu = data.getNu();
     double rhoStar = 1.0 - rho;

    if (alpha == 0.0) {
      Arrays.fill(volatilityAdjoint, 0.0);
      if (DoubleMath.fuzzyEquals(forward, strike, ATM_EPS)) { //TODO should this is relative
        volatilityAdjoint[3] = (1 + (2 - 3 * rho * rho) * nu * nu / 24 * timeToExpiry) / Math.pow(forward, betaStar);
      } else {
        //for non-atm options the alpha sensitivity at alpha = 0 is infinite. Returning this will most likely break calibrations,
        // so we return an arbitrary large number
        volatilityAdjoint[3] = 1e7;
      }
      return volatilityAdjoint;
    }

    // Implementation note: Forward sweep.
    double sfK = Math.pow(forward * strike, betaStar / 2);
    double lnrfK = Math.log(forward / strike);
    double z = nu / alpha * sfK * lnrfK;
    double rzxz;
    double xz = 0;
    if (DoubleMath.fuzzyEquals(z, 0.0, SMALL_Z)) {
      rzxz = 1.0 - 0.5 * z * rho; //small z expansion to z^2 terms
    } else {
      if (DoubleMath.fuzzyEquals(rhoStar, 0.0, RHO_EPS)) {
        if (z >= 1.0) {
          if (rhoStar == 0.0) {
            rzxz = 0.0;
            xz = Double.POSITIVE_INFINITY;
          } else {
            xz = (Math.log(2 * (z - 1)) - Math.log(rhoStar));
            rzxz = z / xz;
          }
        } else {
          xz = -Math.log(1 - z) - 0.5 * Math.pow(z / (z - 1.0), 2) * rhoStar;
          rzxz = z / xz;
        }
      } else {
        double arg;
        if (z < LARGE_NEG_Z) {
          arg = (rho * rho - 1) / 2 / z; //get rounding errors due to fine balanced cancellation for very large negative z
        } else if (z > LARGE_POS_Z) {
          arg = 2 * (z - rho);
        } else {
          arg = (Math.sqrt(1 - 2 * rho * z + z * z) + z - rho);
        }
        if (arg <= 0.0) { //Mathematically this cannot be less than zero, but you know what computers are like.
          rzxz = 0.0;
        } else {
          xz = Math.log(arg / (1 - rho));
          rzxz = z / xz;
        }
      }
    }
    double sf1 = sfK * (1 + betaStar * betaStar / 24 * (lnrfK * lnrfK) + Math.pow(betaStar, 4) / 1920 * Math.pow(lnrfK, 4));
    double sf2 = (1 + (Math.pow(betaStar * alpha / sfK, 2) / 24 + (rho * beta * nu * alpha) /
        (4 * sfK) + (2 - 3 * rho * rho) * nu * nu / 24) * timeToExpiry);
    volatilityAdjoint[0] = alpha / sf1 * rzxz * sf2;

    // Implementation note: Backward sweep.
    double vBar = 1;
    double sf2Bar = alpha / sf1 * rzxz * vBar;
    double sf1Bar = -alpha / (sf1 * sf1) * rzxz * sf2 * vBar;
    double rzxzBar = alpha / sf1 * sf2 * vBar;
    double zBar;
    double xzBar = 0.0;
    if (DoubleMath.fuzzyEquals(z, 0.0, SMALL_Z)) {
      zBar = -rho / 2 * rzxzBar;
    } else {
      if (DoubleMath.fuzzyEquals(rhoStar, 0.0, RHO_EPS)) {
        if (z >= 1.0) {
          if (z == 1.0) {
            zBar = 0.0;
          } else {
            double chiDz = 1 / (z - 1);
            xzBar = -rzxzBar * z / (xz * xz);
            zBar = volatilityAdjoint[0] / z + chiDz * xzBar;
          }
        } else {
          zBar = -1.0 / Math.log(1 - z) * (1 + z / Math.log(1 - z) / (1 - z)) * rzxzBar;
          xzBar = -z / (xz * xz) * rzxzBar;
        }
      } else {
        if (z < LARGE_NEG_Z) {
          zBar = 1 / xz * rzxzBar + xzBar / (xz * xz) * rzxzBar;
        } else if (z > LARGE_POS_Z) {
          zBar = 1 / xz * rzxzBar - xzBar / (xz * xz) * rzxzBar;
        } else {
          xzBar = -z / (xz * xz) * rzxzBar;
          zBar = 1 / xz * rzxzBar + 1 / ((Math.sqrt(1 - 2 * rho * z + z * z) + z - rho)) *
              (0.5 * Math.pow(1 - 2 * rho * z + z * z, -0.5) * (-2 * rho + 2 * z) + 1) * xzBar;
        }
      }
    }

    double lnrfKBar = sfK * (betaStar * betaStar / 12 * lnrfK + Math.pow(betaStar, 4) / 1920 * 4 * Math.pow(lnrfK, 3))
        * sf1Bar + nu / alpha * sfK * zBar;
    double sfKBar = nu / alpha * lnrfK * zBar + sf1 / sfK * sf1Bar - (Math.pow(betaStar * alpha, 2) / Math.pow(sfK, 3)
        / 12 + (rho * beta * nu * alpha) / 4 / (sfK * sfK)) * timeToExpiry * sf2Bar;
    double strikeBar = -1 / strike * lnrfKBar + betaStar * sfK / (2 * strike) * sfKBar;
    double forwardBar = 1 / forward * lnrfKBar + betaStar * sfK / (2 * forward) * sfKBar;
    double nuBar = 1 / alpha * sfK * lnrfK * zBar + ((rho * beta * alpha) / (4 * sfK)
        + (2 - 3 * rho * rho) * nu / 12) * timeToExpiry * sf2Bar;

    double rhoBar;
    if (Math.abs(forward - strike) < ATM_EPS) {
      rhoBar = -z / 2 * rzxzBar;
    } else {
      if (DoubleMath.fuzzyEquals(rhoStar, 0.0, RHO_EPS)) {
        if (z >= 1) {
          if (rhoStar == 0.0) {
            rhoBar = Double.NEGATIVE_INFINITY; //the derivative at rho = 1 is infinite  - this sets it to some arbitrary large number
          } else {
            rhoBar = xzBar * (1.0 / rhoStar + (0.5 - z) / (z - 1.0) / (z - 1.0));
          }
        } else {
          rhoBar = (0.5 * Math.pow(z / (1 - z), 2) + 0.25 * (z - 4.0) * Math.pow(z / (1.0 - z), 3) / (1.0 - z) *
              rhoStar) * xzBar;
        }
      } else {
        rhoBar = (1 / (Math.sqrt(1 - 2 * rho * z + z * z) + z - rho) *
            (-Math.pow(1 - 2 * rho * z + z * z, -0.5) * z - 1) + 1 / rhoStar) * xzBar;
      }
    }
    rhoBar += ((beta * nu * alpha) / (4 * sfK) - rho * nu * nu / 4) * timeToExpiry * sf2Bar;

    double alphaBar = -nu / (alpha * alpha) * sfK * lnrfK * zBar + ((betaStar * alpha / sfK) * (betaStar / sfK) / 12
        + (rho * beta * nu) / (4 * sfK)) * timeToExpiry * sf2Bar + 1 / sf1 * rzxz * sf2 * vBar;
    double betaBar = -0.5 * Math.log(forward * strike) * sfK * sfKBar - sfK *
        (betaStar / 12 * (lnrfK * lnrfK) + Math.pow(betaStar, 3) / 480 * Math.pow(lnrfK, 4)) * sf1Bar
        + (-betaStar * alpha * alpha / sfK / sfK / 12 + rho * nu * alpha / 4 / sfK) * timeToExpiry * sf2Bar;

    volatilityAdjoint[1] = forwardBar;
    volatilityAdjoint[2] = strikeBar;
    volatilityAdjoint[3] = alphaBar;
    volatilityAdjoint[4] = betaBar;
    volatilityAdjoint[5] = rhoBar;
    volatilityAdjoint[6] = nuBar;

    return volatilityAdjoint;
  }

  /**
   * Computes the first and second order derivatives of the Black implied volatility in the SABR model. 
   * <p>
   * The first derivative values will be stored in the input array {@code volatilityD} 
   * The array contains, [0] Derivative w.r.t the forward, [1] the derivative w.r.t the strike, [2] the derivative w.r.t. to alpha,
   * [3] the derivative w.r.t. to beta, [4] the derivative w.r.t. to rho, and [5] the derivative w.r.t. to nu. 
   * Thus the length of the array should be 6.
   * <p>
   * The second derivative values will be stored in the input array {@code volatilityD2}. 
   * Only the second order derivative with respect to the forward and strike are implemented.
   * The array contains [0][0] forward-forward; [0][1] forward-strike; [1][1] strike-strike.
   * Thus the size should be 2 x 2.
   * <p>
   * Around ATM, a first order expansion is used to due to some 0/0-type indetermination. 
   * The second order derivative produced is poor around ATM.
   * 
   * @param option The option.
   * @param forward the forward value of the underlying
   * @param data The SABR data.
   * @param volatilityD  the array used to return the first order derivative. 
   * @param volatilityD2 The array of array used to return the second order derivative. 
   * @return The Black implied volatility.
   */
  public double getVolatilityAdjoint2(
      EuropeanVanillaOption option,
      double forward, SABRFormulaData data,
      double[] volatilityD,
      double[][] volatilityD2) {
    double k = Math.max(option.getStrike(), 0.000001);
    double theta = option.getTimeToExpiry();
    double alpha = data.getAlpha();
    double beta = data.getBeta();
    double rho = data.getRho();
    double nu = data.getNu();
    // Forward
    double h0 = (1 - beta) / 2;
    double h1 = forward * k;
    double h1h0 = Math.pow(h1, h0);
    double h12 = h1h0 * h1h0;
    double h2 = Math.log(forward / k);
    double h22 = h2 * h2;
    double h23 = h22 * h2;
    double h24 = h23 * h2;
    double f1 = h1h0 * (1 + h0 * h0 / 6.0 * (h22 + h0 * h0 / 20.0 * h24));
    double f2 = nu / alpha * h1h0 * h2;
    double f3 = h0 * h0 / 6.0 * alpha * alpha / h12 + rho * beta * nu * alpha / 4.0 / h1h0 + (2 - 3 * rho * rho) /
        24.0 * nu * nu;
    double sqrtf2 = Math.sqrt(1 - 2 * rho * f2 + f2 * f2);
    double f2x = 0.0;
    double x = 0.0, xp = 0, xpp = 0;
    if (DoubleMath.fuzzyEquals(f2, 0.0, SMALL_Z)) {
      f2x = 1.0 - 0.5 * f2 * rho; //small f2 expansion to f2^2 terms
    } else {
      if (DoubleMath.fuzzyEquals(rho, 1.0, RHO_EPS)) {
        x = f2 < 1.0 ? -Math.log(1.0 - f2) - 0.5 * Math.pow(f2 / (f2 - 1.0), 2) * (1.0 - rho) : Math
            .log(2.0 * f2 - 2.0) - Math.log(1.0 - rho);
      } else {
        x = Math.log((sqrtf2 + f2 - rho) / (1 - rho));
      }
      xp = 1. / sqrtf2;
      xpp = (rho - f2) / Math.pow(sqrtf2, 3.0);
      f2x = f2 / x;
    }
    double sigma = alpha / f1 * f2x * (1 + f3 * theta);
    // First level
    double h0Dbeta = -0.5;
    double sigmaDf1 = -sigma / f1;
    double sigmaDf2 = 0;
    if (DoubleMath.fuzzyEquals(f2, 0.0, SMALL_Z)) {
      sigmaDf2 = alpha / f1 * (1 + f3 * theta) * -0.5 * rho;
    } else {
      sigmaDf2 = alpha / f1 * (1 + f3 * theta) * (1.0 / x - f2 * xp / (x * x));
    }
    double sigmaDf3 = alpha / f1 * f2x * theta;
    double sigmaDf4 = f2x / f1 * (1 + f3 * theta);
    double sigmaDx = -alpha / f1 * f2 / (x * x) * (1 + f3 * theta);
    double[][] sigmaD2ff = new double[3][3];
    sigmaD2ff[0][0] = -sigmaDf1 / f1 + sigma / (f1 * f1); //OK
    sigmaD2ff[0][1] = -sigmaDf2 / f1;
    sigmaD2ff[0][2] = -sigmaDf3 / f1;
    if (DoubleMath.fuzzyEquals(f2, 0.0, SMALL_Z)) {
      sigmaD2ff[1][2] = alpha / f1 * -0.5 * rho * theta;
    } else {
      sigmaD2ff[1][1] = alpha / f1 * (1 + f3 * theta) *
          (-2 * xp / (x * x) - f2 * xpp / (x * x) + 2 * f2 * xp * xp / (x * x * x));
      sigmaD2ff[1][2] = alpha / f1 * theta * (1.0 / x - f2 * xp / (x * x));
    }
    sigmaD2ff[2][2] = 0.0;
    //      double sigma = alpha / f1 * f2x * (1 + f3 * theta);
    // Second level
    double[] f1Dh = new double[3];
    double[] f2Dh = new double[3];
    double[] f3Dh = new double[3];
    f1Dh[0] = h1h0 * (h0 * (h22 / 3.0 + h0 * h0 / 40.0 * h24)) + Math.log(h1) * f1;
    f1Dh[1] = h0 * f1 / h1;
    f1Dh[2] = h1h0 * (h0 * h0 / 6.0 * (2.0 * h2 + h0 * h0 / 5.0 * h23));
    f2Dh[0] = Math.log(h1) * f2;
    f2Dh[1] = h0 * f2 / h1;
    f2Dh[2] = nu / alpha * h1h0;
    f3Dh[0] = h0 / 3.0 * alpha * alpha / h12 - 2 * h0 * h0 / 6.0 * alpha * alpha / h12 * Math.log(h1) - rho * beta *
        nu * alpha / 4.0 / h1h0 * Math.log(h1);
    f3Dh[1] = -2 * h0 * h0 / 6.0 * alpha * alpha / h12 * h0 / h1 - rho * beta * nu * alpha / 4.0 / h1h0 * h0 / h1;
    f3Dh[2] = 0.0;
    double[] f1Dp = new double[4]; // Derivative to sabr parameters
    double[] f2Dp = new double[4];
    double[] f3Dp = new double[4];
    double[] f4Dp = new double[4];
    f1Dp[0] = 0.0;
    f1Dp[1] = f1Dh[0] * h0Dbeta;
    f1Dp[2] = 0.0;
    f1Dp[3] = 0.0;
    f2Dp[0] = -f2 / alpha;
    f2Dp[1] = f2Dh[0] * h0Dbeta;
    f2Dp[2] = 0.0;
    f2Dp[3] = h1h0 * h2 / alpha;
    f3Dp[0] = h0 * h0 / 3.0 * alpha / h12 + rho * beta * nu / 4.0 / h1h0;
    f3Dp[1] = rho * nu * alpha / 4.0 / h1h0 + f3Dh[0] * h0Dbeta;
    f3Dp[2] = beta * nu * alpha / 4.0 / h1h0 - rho / 4.0 * nu * nu;
    f3Dp[3] = rho * beta * alpha / 4.0 / h1h0 + (2 - 3 * rho * rho) / 12.0 * nu;
    f4Dp[0] = 1.0;
    f4Dp[1] = 0.0;
    f4Dp[2] = 0.0;
    f4Dp[3] = 0.0;
    double sigmaDh1 = sigmaDf1 * f1Dh[1] + sigmaDf2 * f2Dh[1] + sigmaDf3 * f3Dh[1];
    double sigmaDh2 = sigmaDf1 * f1Dh[2] + sigmaDf2 * f2Dh[2] + sigmaDf3 * f3Dh[2];
    double[][] f1D2hh = new double[2][2]; // No h0
    double[][] f2D2hh = new double[2][2];
    double[][] f3D2hh = new double[2][2];
    f1D2hh[0][0] = h0 * (h0 - 1) * f1 / (h1 * h1);
    f1D2hh[0][1] = h0 * h1h0 / h1 * h0 * h0 / 6.0 * (2.0 * h2 + 4.0 * h0 * h0 / 20.0 * h23);
    f1D2hh[1][1] = h1h0 * (h0 * h0 / 6.0 * (2.0 + 12.0 * h0 * h0 / 20.0 * h2));
    f2D2hh[0][0] = h0 * (h0 - 1) * f2 / (h1 * h1);
    f2D2hh[0][1] = nu / alpha * h0 * h1h0 / h1;
    f2D2hh[1][1] = 0.0;
    f3D2hh[0][0] = 2 * h0 * (2 * h0 + 1) * h0 * h0 / 6.0 * alpha * alpha / (h12 * h1 * h1) + h0 * (h0 + 1) * rho *
        beta * nu * alpha / 4.0 / (h1h0 * h1 * h1);
    f3D2hh[0][1] = 0.0;
    f3D2hh[1][1] = 0.0;
    double[][] sigmaD2hh = new double[2][2]; // No h0
    for (int loopx = 0; loopx < 2; loopx++) {
      for (int loopy = loopx; loopy < 2; loopy++) {
        sigmaD2hh[loopx][loopy] = (sigmaD2ff[0][0] * f1Dh[loopy + 1] + sigmaD2ff[0][1] * f2Dh[loopy + 1] + sigmaD2ff[0][2] *
            f3Dh[loopy + 1]) *
            f1Dh[loopx + 1] +
            sigmaDf1 *
            f1D2hh[loopx][loopy] +
            (sigmaD2ff[0][1] * f1Dh[loopy + 1] + sigmaD2ff[1][1] * f2Dh[loopy + 1] + sigmaD2ff[1][2] * f3Dh[loopy + 1]) *
            f2Dh[loopx + 1] +
            sigmaDf2 *
            f2D2hh[loopx][loopy]
            +
            (sigmaD2ff[0][2] * f1Dh[loopy + 1] + sigmaD2ff[1][2] * f2Dh[loopy + 1] + sigmaD2ff[2][2] * f3Dh[loopy + 1]) *
            f3Dh[loopx + 1] + sigmaDf3 * f3D2hh[loopx][loopy];
      }
    }
    // Third level
    double h1Df = k;
    double h1Dk = forward;
    double h1D2ff = 0.0;
    double h1D2kf = 1.0;
    double h1D2kk = 0.0;
    double h2Df = 1.0 / forward;
    double h2Dk = -1.0 / k;
    double h2D2ff = -1 / (forward * forward);
    double h2D2fk = 0.0;
    double h2D2kk = 1.0 / (k * k);
    volatilityD[0] = sigmaDh1 * h1Df + sigmaDh2 * h2Df;
    volatilityD[1] = sigmaDh1 * h1Dk + sigmaDh2 * h2Dk;
    volatilityD[2] = sigmaDf1 * f1Dp[0] + sigmaDf2 * f2Dp[0] + sigmaDf3 * f3Dp[0] + sigmaDf4 * f4Dp[0];
    volatilityD[3] = sigmaDf1 * f1Dp[1] + sigmaDf2 * f2Dp[1] + sigmaDf3 * f3Dp[1] + sigmaDf4 * f4Dp[1];
    if (DoubleMath.fuzzyEquals(f2, 0.0, SMALL_Z)) {
      volatilityD[4] = -0.5 * f2 + sigmaDf3 * f3Dp[2];
    } else {
      double xDr;
      if (DoubleMath.fuzzyEquals(rho, 1.0, RHO_EPS)) {
        xDr = f2 > 1.0 ? 1.0 / (1.0 - rho) + (0.5 - f2) / (f2 - 1.0) / (f2 - 1.0) : 0.5 *
            Math.pow(f2 / (1.0 - f2), 2.0) + 0.25 * (f2 - 4.0) * Math.pow(f2 / (f2 - 1.0), 3) / (f2 - 1.0) *
            (1.0 - rho);
        if (Doubles.isFinite(xDr)) {
          volatilityD[4] = sigmaDf1 * f1Dp[2] + sigmaDx * xDr + sigmaDf3 * f3Dp[2] + sigmaDf4 * f4Dp[2];
        } else {
          volatilityD[4] = Double.NEGATIVE_INFINITY;
        }
      } else {
        xDr = (-f2 / sqrtf2 - 1 + (sqrtf2 + f2 - rho) / (1 - rho)) / (sqrtf2 + f2 - rho);
        volatilityD[4] = sigmaDf1 * f1Dp[2] + sigmaDx * xDr + sigmaDf3 * f3Dp[2] + sigmaDf4 * f4Dp[2];
      }
    }
    volatilityD[5] = sigmaDf1 * f1Dp[3] + sigmaDf2 * f2Dp[3] + sigmaDf3 * f3Dp[3] + sigmaDf4 * f4Dp[3];
    volatilityD2[0][0] = (sigmaD2hh[0][0] * h1Df + sigmaD2hh[0][1] * h2Df) * h1Df + sigmaDh1 * h1D2ff +
        (sigmaD2hh[0][1] * h1Df + sigmaD2hh[1][1] * h2Df) * h2Df + sigmaDh2 * h2D2ff;
    volatilityD2[0][1] = (sigmaD2hh[0][0] * h1Dk + sigmaD2hh[0][1] * h2Dk) * h1Df + sigmaDh1 * h1D2kf +
        (sigmaD2hh[0][1] * h1Dk + sigmaD2hh[1][1] * h2Dk) * h2Df + sigmaDh2 * h2D2fk;
    volatilityD2[1][0] = volatilityD2[0][1];
    volatilityD2[1][1] = (sigmaD2hh[0][0] * h1Dk + sigmaD2hh[0][1] * h2Dk) * h1Dk + sigmaDh1 * h1D2kk +
        (sigmaD2hh[0][1] * h1Dk + sigmaD2hh[1][1] * h2Dk) * h2Dk + sigmaDh2 * h2D2kk;
    return sigma;
  }

  private double getZOverChi(double rho, double z) {

    // Implementation comment: To avoid numerical instability (0/0) around ATM the first order approximation is used.
    if (DoubleMath.fuzzyEquals(z, 0.0, SMALL_Z)) {
      return 1.0 - rho * z / 2.0;
    }

    double rhoStar = 1 - rho;
    if (DoubleMath.fuzzyEquals(rhoStar, 0.0, RHO_EPS)) {
      if (z > 1.0) {
        if (rhoStar == 0.0) {
          return 0.0;
        }
        return z / (Math.log(2 * (z - 1)) - Math.log(rhoStar));
      } else if (z < 1.0) {
        return z / (-Math.log(1 - z) - 0.5 * Math.pow(z / (z - 1.0), 2) * rhoStar);
      } else {
        return 0.0;
      }
    }

    double rhoHat = 1 + rho;
    if (DoubleMath.fuzzyEquals(rhoHat, 0.0, RHO_EPS_NEGATIVE)) {
      if (z > -1) {
        return z / Math.log(1 + z);
      } else if (z < -1) {
        if (rhoHat == 0) {
          return 0.0;
        }
        double chi = Math.log(rhoHat) - Math.log(-(1 + z) / rhoStar);
        return z / chi;
      } else {
        return 0.0;
      }
    }

    double arg;
    if (z < LARGE_NEG_Z) {
      arg = (rho * rho - 1) / 2 / z; //get rounding errors due to fine balanced cancellation for very large negative z
    } else if (z > LARGE_POS_Z) {
      arg = 2 * (z - rho);
    } else {
      arg = (Math.sqrt(1 - 2 * rho * z + z * z) + z - rho);
      //Mathematically this cannot be less than zero, but you know what computers are like.
      if (arg <= 0.0) {
        return 0.0;
      }
    }

    double chi = Math.log(arg) - Math.log(rhoStar);
    return z / chi;
  }

  private double[] zOverChiWithDev(double rho, double z) {
    double[] res = new double[3];
    if (DoubleMath.fuzzyEquals(z, 0.0, SMALL_Z)) {
      res[0] = 1 - rho * z / 2;
      res[1] = -z / 2;
      res[2] = -rho / 2;
      return res;
    }
    double rhoStar = 1 - rho;
    if (DoubleMath.fuzzyEquals(rhoStar, 0.0, RHO_EPS)) {
      if (z > 1) {
        if (rhoStar == 0) {
          res[0] = 0.0;
          res[1] = Double.NEGATIVE_INFINITY;
          res[2] = 0;
        } else {
          double temp = Math.log(2 * (z - 1)) - Math.log(rhoStar);
          res[0] = z / temp;
          res[1] = -z / temp / temp * (1.0 / rhoStar + (0.5 - z) / Math.pow(z - 1.0, 2));
          res[2] = 1 / temp - z / temp / temp / Math.sqrt(1.0 - 2.0 * rho * z + z * z);
        }
      } else if (z < 1) {
        double temp = -Math.log(1 - z) - 0.5 * Math.pow(z / (z - 1.0), 2) * rhoStar;
        res[0] = z / temp;
        res[1] = -z /
            temp /
            temp *
            (0.5 * Math.pow(z / (z - 1.0), 2) + (0.25 * z - 1.0) * Math.pow(z / (z - 1.0), 3) /
                (z - 1.0) * rhoStar);
        res[2] = 1 / temp - z / temp / temp / Math.sqrt(1.0 - 2.0 * rho * z + z * z);
      } else {
        throw new MathException("can't handle z=1, rho=1");
      }
      return res;
    }
    double rhoHat = 1 + rho;
    if (DoubleMath.fuzzyEquals(rhoHat, 0.0, RHO_EPS_NEGATIVE)) {
      if (z > -1) {
        double temp = Math.log(1 + z);
        double temp2 = temp * temp;
        res[0] = z / temp;
        res[1] = ((2 * z + 1) / 2 / Math.pow(1 + z, 2) - 1 / rhoStar) * z / temp2;
        res[2] = 1 / temp - z / (1 + z) / temp2;
      } else if (z < -1) {
        if (rhoHat == 0) {
          res[0] = 0;
          double chi = Math.log(RHO_EPS_NEGATIVE) - Math.log(-(1 + z) / rhoStar);
          double chiRho = 1 / RHO_EPS_NEGATIVE + 1 / rhoStar - Math.pow(z / (1 + z), 2);
          res[1] = -chiRho * z / chi / chi; //should be +infinity
          res[2] = 0.0;
        } else {
          double chi = Math.log(rhoHat) - Math.log(-(1 + z) / rhoStar);
          res[0] = z / chi;
          double chiRho = 1 / rhoHat + 1 / rhoStar - Math.pow(z / (1 + z), 2);
          res[1] = -chiRho * z / chi / chi;
          res[2] = 1 / chi + z / chi / chi / (1 + z);
        }
      } else {
        throw new MathException("can't handle z=-1, rho=-1");
      }
      return res;
    }

    //now the non-edge case
    double root = 0;
    double arg;
    double argRho;
    double argZ;
    if (z < LARGE_NEG_Z) {
      root = -z + rho - 1 / 2 / z;
      arg = (rho * rho - 1) / 2 / z; //get rounding errors due to fine balanced cancellation for very large negative z
      argRho = rho / z;
      argZ = -arg / z;
    } else if (z > LARGE_POS_Z) {
      root = z - rho + 1 / 2 / z;
      arg = root + z - rho;
      argRho = -2;
      argZ = 2 - 1 / 2 / z / z;
    } else {
      root = Math.sqrt(1 - 2 * rho * z + z * z);
      arg = root + z - rho;
      argRho = -(z / root + 1);
      argZ = (z - rho) / root + 1;
    }
    if (arg <= 0.0) { //Mathematically this cannot be less than zero, but you know what computers are like.
      res[0] = 0.0;
      res[1] = 0.0;
      res[2] = 0.0;
    } else {
      double chi = Math.log(arg / (1 - rho));
      res[0] = z / chi;
      double chiRho = argRho / arg + 1 / rhoStar;
      double zChi2 = z / chi / chi;
      res[1] = -chiRho * zChi2;
      double chiZ = argZ / arg;
      res[2] = 1 / chi - zChi2 * chiZ;
    }

    return res;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "SABR (Hagan)";
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SABRHaganVolatilityFunctionProvider}.
   * @return the meta-bean, not null
   */
  public static SABRHaganVolatilityFunctionProvider.Meta meta() {
    return SABRHaganVolatilityFunctionProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SABRHaganVolatilityFunctionProvider.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SABRHaganVolatilityFunctionProvider.Builder builder() {
    return new SABRHaganVolatilityFunctionProvider.Builder();
  }

  private SABRHaganVolatilityFunctionProvider() {
  }

  @Override
  public SABRHaganVolatilityFunctionProvider.Meta metaBean() {
    return SABRHaganVolatilityFunctionProvider.Meta.INSTANCE;
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
  /**
   * The meta-bean for {@code SABRHaganVolatilityFunctionProvider}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null);

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    public SABRHaganVolatilityFunctionProvider.Builder builder() {
      return new SABRHaganVolatilityFunctionProvider.Builder();
    }

    @Override
    public Class<? extends SABRHaganVolatilityFunctionProvider> beanType() {
      return SABRHaganVolatilityFunctionProvider.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code SABRHaganVolatilityFunctionProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<SABRHaganVolatilityFunctionProvider> {

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      throw new NoSuchElementException("Unknown property: " + propertyName);
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      throw new NoSuchElementException("Unknown property: " + propertyName);
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public SABRHaganVolatilityFunctionProvider build() {
      return new SABRHaganVolatilityFunctionProvider();
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      return "SABRHaganVolatilityFunctionProvider.Builder{}";
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
