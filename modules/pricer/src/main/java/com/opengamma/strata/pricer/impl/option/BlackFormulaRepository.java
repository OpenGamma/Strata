/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.rootfinding.NewtonRaphsonSingleRootFinder;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

/**
 * The primary repository for Black formulas, including the price, common greeks and implied volatility.
 * <p>
 * Other classes that have higher level abstractions (e.g. option data bundles) should call these functions.
 * As the numeraire (e.g. the zero bond p(0,T) in the T-forward measure) in the Black formula is just a multiplication
 * factor, all prices, input/output, are <b>forward</b> prices, i.e. (spot price)/numeraire.
 * Note that a "reference value" is returned if computation comes across an ambiguous expression.
 */
public final class BlackFormulaRepository {

  private static final Logger log = LoggerFactory.getLogger(BlackFormulaRepository.class);

  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);
  private static final double LARGE = 1e13;
  private static final double SMALL = 1e-13;
  /** The comparison value used to determine near-zero. */
  private static final double NEAR_ZERO = 1e-16;
  /** Limit defining "close of ATM forward" to avoid the formula singularity. **/
  private static final double ATM_LIMIT = 1.0E-3;
  private static final double ROOT_ACCURACY = 1.0E-7;
  private static final NewtonRaphsonSingleRootFinder ROOT_FINDER = new NewtonRaphsonSingleRootFinder(ROOT_ACCURACY);

  // restricted constructor
  private BlackFormulaRepository() {
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the forward price.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param isCall  true for call, false for put
   * @return the forward price
   */
  public static double price(
      double forward,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      boolean isCall) {

    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    int sign = isCall ? 1 : -1;
    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);
    double d1 = 0d;
    double d2 = 0d;

    if (bFwd && bStr) {
      log.info("(large value)/(large value) ambiguous");
      return isCall ? (forward >= strike ? forward : 0d) : (strike >= forward ? strike : 0d);
    }
    if (sigmaRootT < SMALL) {
      return Math.max(sign * (forward - strike), 0d);
    }
    if (Math.abs(forward - strike) < SMALL || bSigRt) {
      d1 = 0.5 * sigmaRootT;
      d2 = -0.5 * sigmaRootT;
    } else {
      d1 = Math.log(forward / strike) / sigmaRootT + 0.5 * sigmaRootT;
      d2 = d1 - sigmaRootT;
    }

    double nF = NORMAL.getCDF(sign * d1);
    double nS = NORMAL.getCDF(sign * d2);
    double first = nF == 0d ? 0d : forward * nF;
    double second = nS == 0d ? 0d : strike * nS;

    double res = sign * (first - second);
    return Math.max(0., res);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the price without numeraire and its derivatives.
   * <p>
   * The derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to the forward
   * <li>[1] derivative with respect to the strike
   * <li>[2] derivative with respect to the time to expiry
   * <li>[3] derivative with respect to the volatility
   * </ul>
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param isCall  true for call, false for put
   * @return the forward price and its derivatives 
   */
  public static ValueDerivatives priceAdjoint(
      double forward,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      boolean isCall) {

    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    int sign = isCall ? 1 : -1;
    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);
    double d1 = 0d;
    double d2 = 0d;

    if (bFwd && bStr) {
      log.info("(large value)/(large value) ambiguous");
      double price = isCall ? (forward >= strike ? forward : 0d) : (strike >= forward ? strike : 0d); // ???
      return ValueDerivatives.of(price, DoubleArray.filled(4)); // ??
    }
    if (sigmaRootT < SMALL) {
      boolean isItm = (sign * (forward - strike)) > 0;
      double price = isItm ? sign * (forward - strike) : 0d;
      return ValueDerivatives.of(price, DoubleArray.of(isItm ? sign : 0d, isItm ? -sign : 0d, 0d, 0d));
    }
    if (Math.abs(forward - strike) < SMALL || bSigRt) {
      d1 = 0.5 * sigmaRootT;
      d2 = -0.5 * sigmaRootT;
    } else {
      d2 = Math.log(forward / strike) / sigmaRootT - 0.5 * sigmaRootT;
      d1 = d2 + sigmaRootT;
    }

    double nF = NORMAL.getCDF(sign * d1);
    double nS = NORMAL.getCDF(sign * d2);
    double first = nF == 0d ? 0d : forward * nF;
    double second = nS == 0d ? 0d : strike * nS;
    double res = sign * (first - second);
    double price = Math.max(0.0d, res);

    // Backward sweep
    double resBar = 1.0;
    double firstBar = sign * resBar;
    double secondBar = -sign * resBar;
    double forwardBar = nF * firstBar;
    double strikeBar = nS * secondBar;
    double nFBar = forward * firstBar;
    double d1Bar = sign * NORMAL.getPDF(sign * d1) * nFBar;
    // Implementation Note: d2Bar = 0; no need to implement it.
    // Methodology Note: d2Bar is optimal exercise boundary. The derivative at the optimal point is 0.
    double sigmaRootTBar = d1Bar;
    double lognormalVolBar = Math.sqrt(timeToExpiry) * sigmaRootTBar;
    double timeToExpiryBar = 0.5 / Math.sqrt(timeToExpiry) * lognormalVol * sigmaRootTBar;
    return ValueDerivatives.of(price, DoubleArray.of(forwardBar, strikeBar, timeToExpiryBar, lognormalVolBar));
  }

  /**
   * Computes the price without numeraire and its derivatives of the first and second order.
   * <p>
   * The first order derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to the forward
   * <li>[1] derivative with respect to the strike
   * <li>[2] derivative with respect to the time to expiry
   * <li>[3] derivative with respect to the volatility
   * </ul>
   * The price and the second order derivatives are in the ValueDerivatives which is the first element of the returned pair.
   * <p>
   * The second order derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to the forward
   * <li>[1] derivative with respect to the strike
   * <li>[2] derivative with respect to the volatility
   * </ul>
   * The second order derivatives are in the double[][] which is the second element of the returned pair.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param isCall  true for call, false for put
   * @return the forward price and its derivatives 
   */
  public static Pair<ValueDerivatives, double[][]> priceAdjoint2(
      double forward,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      boolean isCall) {
    // Forward sweep
    double discountFactor = 1.0;
    double sqrttheta = Math.sqrt(timeToExpiry);
    double omega = isCall ? 1 : -1;
    // Implementation Note: Forward sweep.
    double volPeriod = 0, kappa = 0, d1 = 0, d2 = 0;
    double x = 0;
    double p;
    if (strike < NEAR_ZERO || sqrttheta < NEAR_ZERO) {
      x = omega * (forward - strike);
      p = (x > 0 ? discountFactor * x : 0.0);
      volPeriod = sqrttheta < NEAR_ZERO ? 0 : (lognormalVol * sqrttheta);
    } else {
      volPeriod = lognormalVol * sqrttheta;
      kappa = Math.log(forward / strike) / volPeriod - 0.5 * volPeriod;
      d1 = NORMAL.getCDF(omega * (kappa + volPeriod));
      d2 = NORMAL.getCDF(omega * kappa);
      p = discountFactor * omega * (forward * d1 - strike * d2);
    }
    // Implementation Note: Backward sweep.
    double[][] bsD2 = new double[3][3];
    double pBar = 1.0;
    double density1 = 0.0;
    double d1Bar = 0.0;
    double forwardBar = 0, strikeBar = 0, volPeriodBar = 0, lognormalVolBar = 0, sqrtthetaBar = 0, timeToExpiryBar = 0;
    if (strike < NEAR_ZERO || sqrttheta < NEAR_ZERO) {
      forwardBar = (x > 0 ? discountFactor * omega : 0.0);
      strikeBar = (x > 0 ? -discountFactor * omega : 0.0);
    } else {
      d1Bar = discountFactor * omega * forward * pBar;
      density1 = NORMAL.getPDF(omega * (kappa + volPeriod));
      // Implementation Note: kappa_bar = 0; no need to implement it.
      // Methodology Note: kappa_bar is optimal exercise boundary. The
      // derivative at the optimal point is 0.
      forwardBar = discountFactor * omega * d1 * pBar;
      strikeBar = -discountFactor * omega * d2 * pBar;
      volPeriodBar = density1 * omega * d1Bar;
      lognormalVolBar = sqrttheta * volPeriodBar;
      sqrtthetaBar = lognormalVol * volPeriodBar;
      timeToExpiryBar = 0.5 / sqrttheta * sqrtthetaBar;
    }
    DoubleArray bsD = DoubleArray.of(forwardBar, strikeBar, timeToExpiryBar, lognormalVolBar);
    if (strike < NEAR_ZERO || sqrttheta < NEAR_ZERO) {
      return Pair.of(ValueDerivatives.of(p, bsD), bsD2);
    }
    // Backward sweep: second derivative
    double d2Bar = -discountFactor * omega * strike;
    double density2 = NORMAL.getPDF(omega * kappa);
    double d1Kappa = omega * density1;
    double d1KappaKappa = -(kappa + volPeriod) * d1Kappa;
    double d2Kappa = omega * density2;
    double d2KappaKappa = -kappa * d2Kappa;
    double kappaKappaBar2 = d1KappaKappa * d1Bar + d2KappaKappa * d2Bar;
    double kappaV = -Math.log(forward / strike) / (volPeriod * volPeriod) - 0.5;
    double kappaVV = 2 * Math.log(forward / strike) / (volPeriod * volPeriod * volPeriod);
    double d1TotVV = density1 * omega * (-(kappa + volPeriod) * (kappaV + 1) * (kappaV + 1) + kappaVV);
    double d2TotVV = d2KappaKappa * kappaV * kappaV + d2Kappa * kappaVV;
    double vVbar2 = d1Bar * d1TotVV + d2Bar * d2TotVV;
    double volVolBar2 = vVbar2 * timeToExpiry;
    double kappaStrikeBar2 = -discountFactor * omega * d2Kappa;
    double kappaStrike = -1.0 / (strike * volPeriod);
    double strikeStrikeBar2 = (kappaKappaBar2 * kappaStrike + 2 * kappaStrikeBar2) * kappaStrike;
    double kappaStrikeV = 1.0 / strike / (volPeriod * volPeriod);
    double d1VK = -omega * (kappa + volPeriod) * density1 * (kappaV + 1) * kappaStrike + omega * density1 * kappaStrikeV;
    double d2V = d2Kappa * kappaV;
    double d2VK = -omega * kappa * density2 * kappaV * kappaStrike + omega * density2 * kappaStrikeV;
    double strikeD2Bar2 = -discountFactor * omega;
    double strikeVolblackBar2 = strikeD2Bar2 * d2V + d1Bar * d1VK + d2Bar * d2VK;
    double strikeVolBar2 = strikeVolblackBar2 * sqrttheta;
    double kappaForward = 1.0 / (forward * volPeriod);
    double forwardForwardBar2 = discountFactor * omega * d1Kappa * kappaForward;
    double strikeForwardBar2 = discountFactor * omega * d1Kappa * kappaStrike;
    double volForwardBar2 = discountFactor * omega * d1Kappa * (kappaV + 1) * sqrttheta;
    bsD2[0][0] = forwardForwardBar2;
    bsD2[0][2] = volForwardBar2;
    bsD2[2][0] = volForwardBar2;
    bsD2[0][1] = strikeForwardBar2;
    bsD2[1][0] = strikeForwardBar2;
    bsD2[2][2] = volVolBar2;
    bsD2[1][2] = strikeVolBar2;
    bsD2[2][1] = strikeVolBar2;
    bsD2[1][1] = strikeStrikeBar2;
    return Pair.of(ValueDerivatives.of(p, bsD), bsD2);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the forward driftless delta.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param isCall  true for call, false for put
   * @return the forward driftless delta
   */
  public static double delta(
      double forward,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      boolean isCall) {

    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    int sign = isCall ? 1 : -1;

    double d1 = 0d;
    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);

    if (bSigRt) {
      return isCall ? 1d : 0d;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return (isCall ? (forward > strike ? 1d : 0d) : (forward > strike ? 0d : -1d));
      }
      log.info("(log 1d)/0., ambiguous value");
      return isCall ? 0.5 : -0.5;
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d1 = 0.5 * sigmaRootT;
    } else {
      d1 = Math.log(forward / strike) / sigmaRootT + 0.5 * sigmaRootT;
    }

    return sign * NORMAL.getCDF(sign * d1);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the strike for the delta.
   * 
   * @param forward  the forward value of the underlying
   * @param forwardDelta  the forward delta
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param isCall  true for call, false for put
   * @return the strike
   */
  public static double strikeForDelta(
      double forward,
      double forwardDelta,
      double timeToExpiry,
      double lognormalVol,
      boolean isCall) {

    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue((isCall && forwardDelta > 0 && forwardDelta < 1) ||
        (!isCall && forwardDelta > -1 && forwardDelta < 0), "delta out of range", forwardDelta);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    int sign = isCall ? 1 : -1;
    double d1 = sign * NORMAL.getInverseCDF(sign * forwardDelta);

    double sigmaSqT = lognormalVol * lognormalVol * timeToExpiry;
    if (Double.isNaN(sigmaSqT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaSqT = 1d;
    }

    return forward * Math.exp(-d1 * Math.sqrt(sigmaSqT) + 0.5 * sigmaSqT);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the driftless dual delta.
   * <p>
   * This is the first derivative of option price with respect to strike.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param isCall  true for call, false for put
   * @return the driftless dual delta
   */
  public static double dualDelta(
      double forward,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      boolean isCall) {

    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    int sign = isCall ? 1 : -1;

    double d2 = 0d;
    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);

    if (bSigRt) {
      return isCall ? 0d : 1d;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return (isCall ? (forward > strike ? -1d : 0d) : (forward > strike ? 0d : 1d));
      }
      log.info("(log 1d)/0., ambiguous value");
      return isCall ? -0.5 : 0.5;
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d2 = -0.5 * sigmaRootT;
    } else {
      d2 = Math.log(forward / strike) / sigmaRootT - 0.5 * sigmaRootT;
    }

    return -sign * NORMAL.getCDF(sign * d2);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the simple delta.
   * <p>
   * Note that this is not the standard delta one is accustomed to.
   * The argument of the cumulative normal is simply {@code d = Math.log(forward / strike) / sigmaRootT}.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param isCall  true for call, false for put
   * @return the simple delta
   */
  public static double simpleDelta(
      double forward,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      boolean isCall) {

    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    int sign = isCall ? 1 : -1;

    double d = 0d;
    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);

    if (bSigRt) {
      return isCall ? 0.5 : -0.5;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return (isCall ? (forward > strike ? 1d : 0d) : (forward > strike ? 0d : -1d));
      }
      log.info("(log 1d)/0., ambiguous");
      return isCall ? 0.5 : -0.5;
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d = 0d;
    } else {
      d = Math.log(forward / strike) / sigmaRootT;
    }

    return sign * NORMAL.getCDF(sign * d);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the forward driftless gamma.
   * <p>
   * This is the second order sensitivity of the forward option value to the forward.
   * <p>
   * $\frac{\partial^2 FV}{\partial^2 f}$
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @return the forward driftless gamma
   */
  public static double gamma(double forward, double strike, double timeToExpiry, double lognormalVol) {
    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    double d1 = 0d;
    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);

    if (bSigRt) {
      return 0d;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return 0d;
      }
      log.info("(log 1d)/0d ambiguous");
      return bFwd ? NORMAL.getPDF(0d) : NORMAL.getPDF(0d) / forward / sigmaRootT;
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d1 = 0.5 * sigmaRootT;
    } else {
      d1 = Math.log(forward / strike) / sigmaRootT + 0.5 * sigmaRootT;
    }

    double nVal = NORMAL.getPDF(d1);
    return nVal == 0d ? 0d : nVal / forward / sigmaRootT;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the driftless dual gamma.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @return the driftless dual gamma
   */
  public static double dualGamma(double forward, double strike, double timeToExpiry, double lognormalVol) {
    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    double d2 = 0d;
    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);

    if (bSigRt) {
      return 0d;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return 0d;
      }
      log.info("(log 1d)/0d ambiguous");
      return bStr ? NORMAL.getPDF(0d) : NORMAL.getPDF(0d) / strike / sigmaRootT;
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d2 = -0.5 * sigmaRootT;
    } else {
      d2 = Math.log(forward / strike) / sigmaRootT - 0.5 * sigmaRootT;
    }

    double nVal = NORMAL.getPDF(d2);
    return nVal == 0d ? 0d : nVal / strike / sigmaRootT;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the driftless cross gamma.
   * <p>
   * This is the sensitity of the delta to the strike.
   * <p>
   * $\frac{\partial^2 V}{\partial f \partial K}$.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @return the driftless cross gamma
   */
  public static double crossGamma(double forward, double strike, double timeToExpiry, double lognormalVol) {
    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    double d2 = 0d;
    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);

    if (bSigRt) {
      return 0d;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return 0d;
      }
      log.info("(log 1d)/0d ambiguous");
      return bFwd ? -NORMAL.getPDF(0d) : -NORMAL.getPDF(0d) / forward / sigmaRootT;
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d2 = -0.5 * sigmaRootT;
    } else {
      d2 = Math.log(forward / strike) / sigmaRootT - 0.5 * sigmaRootT;
    }

    double nVal = NORMAL.getPDF(d2);
    return nVal == 0d ? 0d : -nVal / forward / sigmaRootT;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the theta (non-forward).
   * <p>
   * This is the sensitivity of the present value to a change in time to maturity.
   * <p>
   * $\-frac{\partial * V}{\partial T}$.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param isCall  true for call, false for put
   * @param interestRate  the interest rate
   * @return theta
   */
  public static double theta(
      double forward,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      boolean isCall,
      double interestRate) {

    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");

    if (-interestRate > LARGE) {
      return 0d;
    }
    double driftLess = driftlessTheta(forward, strike, timeToExpiry, lognormalVol);
    if (Math.abs(interestRate) < SMALL) {
      return driftLess;
    }

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    int sign = isCall ? 1 : -1;

    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);
    double d1 = 0d;
    double d2 = 0d;

    double priceLike = Double.NaN;
    double rt =
        (timeToExpiry < SMALL && Math.abs(interestRate) > LARGE) ? (interestRate > 0d ? 1d : -1d) : interestRate * timeToExpiry;
    if (bFwd && bStr) {
      log.info("(large value)/(large value) ambiguous");
      priceLike = isCall ? (forward >= strike ? forward : 0d) : (strike >= forward ? strike : 0d);
    } else {
      if (sigmaRootT < SMALL) {
        if (rt > LARGE) {
          priceLike = isCall ? (forward > strike ? forward : 0d) : (forward > strike ? 0d : -forward);
        } else {
          priceLike = isCall ?
              (forward > strike ? forward - strike * Math.exp(-rt) : 0d) :
              (forward > strike ? 0d : -forward + strike * Math.exp(-rt));
        }
      } else {
        if (Math.abs(forward - strike) < SMALL | bSigRt) {
          d1 = 0.5 * sigmaRootT;
          d2 = -0.5 * sigmaRootT;
        } else {
          d1 = Math.log(forward / strike) / sigmaRootT + 0.5 * sigmaRootT;
          d2 = d1 - sigmaRootT;
        }
        double nF = NORMAL.getCDF(sign * d1);
        double nS = NORMAL.getCDF(sign * d2);
        double first = nF == 0d ? 0d : forward * nF;
        double second = ((nS == 0d) | (Math.exp(-interestRate * timeToExpiry) == 0d)) ?
            0d :
            strike * Math.exp(-interestRate * timeToExpiry) * nS;
        priceLike = sign * (first - second);
      }
    }

    double res = (interestRate > LARGE && Math.abs(priceLike) < SMALL) ? 0d : interestRate * priceLike;
    return Math.abs(res) > LARGE ? res : driftLess + res;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the theta (non-forward).
   * <p>
   * This is the sensitivity of the present value to a change in time to maturity
   * <p>
   * $\-frac{\partial * V}{\partial T}$.
   * <p>
   * This is consistent with {@link BlackScholesFormulaRepository}.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param isCall  true for call, false for put
   * @param interestRate  the interest rate
   * @return theta
   */
  public static double thetaMod(
      double forward,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      boolean isCall,
      double interestRate) {

    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");

    if (-interestRate > LARGE) {
      return 0d;
    }
    double driftLess = driftlessTheta(forward, strike, timeToExpiry, lognormalVol);
    if (Math.abs(interestRate) < SMALL) {
      return driftLess;
    }

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    int sign = isCall ? 1 : -1;

    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);
    double d2 = 0d;

    double priceLike = Double.NaN;
    double rt =
        (timeToExpiry < SMALL && Math.abs(interestRate) > LARGE) ? (interestRate > 0d ? 1d : -1d) : interestRate * timeToExpiry;
    if (bFwd && bStr) {
      log.info("(large value)/(large value) ambiguous");
      priceLike = isCall ? 0d : (strike >= forward ? strike : 0d);
    } else {
      if (sigmaRootT < SMALL) {
        if (rt > LARGE) {
          priceLike = 0d;
        } else {
          priceLike = isCall ? (forward > strike ? -strike : 0d) : (forward > strike ? 0d : +strike);
        }
      } else {
        if (Math.abs(forward - strike) < SMALL | bSigRt) {
          d2 = -0.5 * sigmaRootT;
        } else {
          d2 = Math.log(forward / strike) / sigmaRootT - 0.5 * sigmaRootT;
        }
        double nS = NORMAL.getCDF(sign * d2);
        priceLike = (nS == 0d) ? 0d : -sign * strike * nS;
      }
    }

    double res = (interestRate > LARGE && Math.abs(priceLike) < SMALL) ? 0d : interestRate * priceLike;
    return Math.abs(res) > LARGE ? res : driftLess + res;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the forward driftless theta.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @return the driftless theta
   */
  public static double driftlessTheta(double forward, double strike, double timeToExpiry, double lognormalVol) {
    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }

    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);
    double d1 = 0d;

    if (bSigRt) {
      return 0d;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return 0d;
      }
      log.info("log(1)/0 ambiguous");
      if (rootT < SMALL) {
        return forward < SMALL ?
            -NORMAL.getPDF(0d) * lognormalVol / 2. :
            (lognormalVol < SMALL ?
                -forward * NORMAL.getPDF(0d) / 2. :
                -forward * NORMAL.getPDF(0d) * lognormalVol / 2. / rootT);
      }
      if (lognormalVol < SMALL) {
        return bFwd ? -NORMAL.getPDF(0d) / 2. / rootT : -forward * NORMAL.getPDF(0d) * lognormalVol / 2. / rootT;
      }
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d1 = 0.5 * sigmaRootT;
    } else {
      d1 = Math.log(forward / strike) / sigmaRootT + 0.5 * sigmaRootT;
    }

    double nVal = NORMAL.getPDF(d1);
    return nVal == 0d ? 0d : -forward * nVal * lognormalVol / 2. / rootT;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the forward vega.
   * <p>
   * This is the sensitivity of the option's forward price wrt the implied volatility (which
   * is just the spot vega divided by the numeraire).
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @return the forward vega
   */
  public static double vega(double forward, double strike, double timeToExpiry, double lognormalVol) {
    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }
    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);
    double d1 = 0d;

    if (bSigRt) {
      return 0d;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return 0d;
      }
      log.info("log(1)/0 ambiguous");
      return (rootT < SMALL && forward > LARGE) ? NORMAL.getPDF(0d) : forward * rootT * NORMAL.getPDF(0d);
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d1 = 0.5 * sigmaRootT;
    } else {
      d1 = Math.log(forward / strike) / sigmaRootT + 0.5 * sigmaRootT;
    }

    double nVal = NORMAL.getPDF(d1);
    return nVal == 0d ? 0d : forward * rootT * nVal;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the driftless vanna.
   * <p>
   * This is the second order derivative of the option value, once to the underlying forward
   * and once to volatility.
   * <p>
   * $\frac{\partial^2 FV}{\partial f \partial \sigma}$.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @return the driftless vanna
   */
  public static double vanna(double forward, double strike, double timeToExpiry, double lognormalVol) {
    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }

    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);
    double d1 = 0d;
    double d2 = 0d;

    if (bSigRt) {
      return 0d;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return 0d;
      }
      log.info("log(1)/0 ambiguous");
      return lognormalVol < SMALL ? -NORMAL.getPDF(0d) / lognormalVol : NORMAL.getPDF(0d) * rootT;
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d1 = 0.5 * sigmaRootT;
      d2 = -0.5 * sigmaRootT;
    } else {
      d1 = Math.log(forward / strike) / sigmaRootT + 0.5 * sigmaRootT;
      d2 = d1 - sigmaRootT;
    }

    double nVal = NORMAL.getPDF(d1);
    return nVal == 0d ? 0d : -nVal * d2 / lognormalVol;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the driftless dual vanna.
   * <p>
   * This is the second order derivative of the option value, once to the strike and
   * once to volatility.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @return the driftless dual vanna
   */
  public static double dualVanna(double forward, double strike, double timeToExpiry, double lognormalVol) {
    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }

    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);
    double d1 = 0d;
    double d2 = 0d;

    if (bSigRt) {
      return 0d;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return 0d;
      }
      log.info("log(1)/0 ambiguous");
      return lognormalVol < SMALL ? -NORMAL.getPDF(0d) / lognormalVol : -NORMAL.getPDF(0d) * rootT;
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d1 = 0.5 * sigmaRootT;
      d2 = -0.5 * sigmaRootT;
    } else {
      d1 = Math.log(forward / strike) / sigmaRootT + 0.5 * sigmaRootT;
      d2 = d1 - sigmaRootT;
    }

    double nVal = NORMAL.getPDF(d2);
    return nVal == 0d ? 0d : nVal * d1 / lognormalVol;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the driftless vomma (aka volga).
   * <p>
   * This is the second order derivative of the option forward price with respect
   * to the implied volatility.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @return the driftless vomma
   */
  public static double vomma(double forward, double strike, double timeToExpiry, double lognormalVol) {
    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
      sigmaRootT = 1d;
    }

    boolean bFwd = (forward > LARGE);
    boolean bStr = (strike > LARGE);
    boolean bSigRt = (sigmaRootT > LARGE);
    double d1 = 0d;
    double d2 = 0d;

    if (bSigRt) {
      return 0d;
    }
    if (sigmaRootT < SMALL) {
      if (Math.abs(forward - strike) >= SMALL && !(bFwd && bStr)) {
        return 0d;
      }
      log.info("log(1)/0 ambiguous");
      if (bFwd) {
        return rootT < SMALL ? NORMAL.getPDF(0d) / lognormalVol : forward * NORMAL.getPDF(0d) * rootT / lognormalVol;
      }
      return lognormalVol < SMALL ?
          forward * NORMAL.getPDF(0d) * rootT / lognormalVol :
          -forward * NORMAL.getPDF(0d) * timeToExpiry * lognormalVol / 4.;
    }
    if (Math.abs(forward - strike) < SMALL | (bFwd && bStr)) {
      d1 = 0.5 * sigmaRootT;
      d2 = -0.5 * sigmaRootT;
    } else {
      d1 = Math.log(forward / strike) / sigmaRootT + 0.5 * sigmaRootT;
      d2 = d1 - sigmaRootT;
    }

    double nVal = NORMAL.getPDF(d1);
    double res = nVal == 0d ? 0d : forward * nVal * rootT * d1 * d2 / lognormalVol;
    return res;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the driftless volga (aka vomma).
   * <p>
   * This is the second order derivative of the option forward price with respect
   * to the implied volatility.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @return the driftless volga
   */
  public static double volga(double forward, double strike, double timeToExpiry, double lognormalVol) {
    return vomma(forward, strike, timeToExpiry, lognormalVol);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the log-normal implied volatility.
   * 
   * @param price The forward price, which is the market price divided by the numeraire,
   *   for example the zero bond p(0,T) for the T-forward measure
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param isCall  true for call, false for put
   * @return log-normal (Black) implied volatility
   */
  public static double impliedVolatility(
      double price,
      double forward,
      double strike,
      double timeToExpiry,
      boolean isCall) {

    ArgChecker.isTrue(price >= -NEAR_ZERO * forward, "negative/NaN price; have {}", price);
    ArgChecker.isTrue(forward > 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);

    ArgChecker.isFalse(Double.isInfinite(forward), "forward is Infinity");
    ArgChecker.isFalse(Double.isInfinite(strike), "strike is Infinity");
    ArgChecker.isFalse(Double.isInfinite(timeToExpiry), "timeToExpiry is Infinity");

    double intrinsicPrice = Math.max(0., (isCall ? 1 : -1) * (forward - strike));

    double targetPrice = price - intrinsicPrice;
    // Math.max(0., price - intrinsicPrice) should not used for least chi square
    double sigmaGuess = 0.3;
    return impliedVolatility(targetPrice, forward, strike, timeToExpiry, sigmaGuess);
  }

  /**
   * Computes the log-normal implied volatility and its derivative with respect to price.
   * 
   * @param price The forward price, which is the market price divided by the numeraire,
   *   for example the zero bond p(0,T) for the T-forward measure
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param isCall  true for call, false for put
   * @return log-normal (Black) implied volatility and tis derivative w.r.t. the price
   */
  public static ValueDerivatives impliedVolatilityAdjoint(
      double price,
      double forward,
      double strike,
      double timeToExpiry,
      boolean isCall) {

    ArgChecker.isTrue(price >= -NEAR_ZERO * forward, "negative/NaN price; have {}", price);
    ArgChecker.isTrue(forward > 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);

    ArgChecker.isFalse(Double.isInfinite(forward), "forward is Infinity");
    ArgChecker.isFalse(Double.isInfinite(strike), "strike is Infinity");
    ArgChecker.isFalse(Double.isInfinite(timeToExpiry), "timeToExpiry is Infinity");

    double intrinsicPrice = Math.max(0., (isCall ? 1 : -1) * (forward - strike));

    double targetPrice = price - intrinsicPrice;
    // Math.max(0., price - intrinsicPrice) should not used for least chi square
    double sigmaGuess = 0.3;
    return impliedVolatilityAdjoint(targetPrice, forward, strike, timeToExpiry, sigmaGuess);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the log-normal (Black) implied volatility of an out-the-money
   * European option starting from an initial guess.
   * 
   * @param otmPrice The forward price, which is the market price divided by the numeraire,
   *   for example the zero bond p(0,T) for the T-forward measure
   *   This MUST be an OTM price, i.e. a call price for strike >= forward and a put price otherwise.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param volGuess  a guess of the implied volatility
   * @return log-normal (Black) implied volatility
   */
  public static double impliedVolatility(
      double otmPrice,
      double forward,
      double strike,
      double timeToExpiry,
      double volGuess) {

    ArgChecker.isTrue(otmPrice >= -NEAR_ZERO * forward, "negative/NaN otmPrice; have {}", otmPrice);
    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(volGuess >= 0d, "negative/NaN volGuess; have {}", volGuess);

    ArgChecker.isFalse(Double.isInfinite(otmPrice), "otmPrice is Infinity");
    ArgChecker.isFalse(Double.isInfinite(forward), "forward is Infinity");
    ArgChecker.isFalse(Double.isInfinite(strike), "strike is Infinity");
    ArgChecker.isFalse(Double.isInfinite(timeToExpiry), "timeToExpiry is Infinity");
    ArgChecker.isFalse(Double.isInfinite(volGuess), "volGuess is Infinity");

    if (Math.abs(otmPrice) < NEAR_ZERO * forward) {
      return 0.0d;
    }
    ArgChecker.isTrue(otmPrice < Math.min(forward, strike), "otmPrice of {} exceeded upper bound of {}", otmPrice,
        Math.min(forward, strike));

    if (forward == strike) {
      return NORMAL.getInverseCDF(0.5 * (otmPrice / forward + 1)) * 2 / Math.sqrt(timeToExpiry);
    }

    boolean isCall = strike >= forward;

    Function<Double, Double> priceFunc = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return price(forward, strike, timeToExpiry, x, isCall);
      }
    };

    Function<Double, Double> vegaFunc = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return vega(forward, strike, timeToExpiry, x);
      }
    };

    GenericImpliedVolatiltySolver solver = new GenericImpliedVolatiltySolver(priceFunc, vegaFunc);
    return solver.impliedVolatility(otmPrice, volGuess);
  }

  /**
   * Computes the log-normal (Black) implied volatility of an out-the-money European option starting 
   * from an initial guess and the derivative of the volatility w.r.t. the price.
   * 
   * @param otmPrice The forward price, which is the market price divided by the numeraire,
   *   for example the zero bond p(0,T) for the T-forward measure
   *   This MUST be an OTM price, i.e. a call price for strike >= forward and a put price otherwise.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param volGuess  a guess of the implied volatility
   * @return log-normal (Black) implied volatility and derivative with respect to the price
   */
  public static ValueDerivatives impliedVolatilityAdjoint(
      double otmPrice,
      double forward,
      double strike,
      double timeToExpiry,
      double volGuess) {

    if (Math.abs(otmPrice) < NEAR_ZERO * forward) {
      return ValueDerivatives.of(0.0d, DoubleArray.of(0.0d));
    }
    double impliedVolatility = impliedVolatility(otmPrice, forward, strike, timeToExpiry, volGuess);
    boolean isCall = strike >= forward;
    ValueDerivatives price = priceAdjoint(forward, strike, timeToExpiry, impliedVolatility, isCall);
    double dpricedvol = price.getDerivative(3);
    double dvoldprice = 1.0d / dpricedvol;
    return ValueDerivatives.of(impliedVolatility, DoubleArray.of(dvoldprice));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied strike from delta and volatility in the Black formula.
   * 
   * @param delta The option delta
   * @param isCall  true for call, false for put
   * @param forward The forward.
   * @param time The time to expiry.
   * @param volatility The volatility.
   * @return the strike.
   */
  public static double impliedStrike(double delta, boolean isCall, double forward, double time, double volatility) {
    ArgChecker.isTrue(delta > -1 && delta < 1, "Delta out of range");
    ArgChecker.isTrue(isCall ^ (delta < 0), "Delta incompatible with call/put: {}, {}", isCall, delta);
    ArgChecker.isTrue(forward > 0, "Forward negative");
    double omega = (isCall ? 1d : -1d);
    double strike = forward *
        Math.exp(-volatility * Math.sqrt(time) * omega * NORMAL.getInverseCDF(omega * delta) + volatility * volatility *
            time / 2);
    return strike;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied strike and its derivatives from delta and volatility in the Black formula.
   * 
   * @param delta The option delta
   * @param isCall  true for call, false for put
   * @param forward  the forward
   * @param time  the time to expiry
   * @param volatility  the volatility
   * @param derivatives  the mutated array of derivatives of the implied strike with respect to the input
   *   Derivatives with respect to: [0] delta, [1] forward, [2] time, [3] volatility.
   * @return the strike
   */
  public static double impliedStrike(
      double delta,
      boolean isCall,
      double forward,
      double time,
      double volatility,
      double[] derivatives) {

    ArgChecker.isTrue(delta > -1 && delta < 1, "Delta out of range");
    ArgChecker.isTrue(isCall ^ (delta < 0), "Delta incompatible with call/put: {}, {}", isCall, delta);
    ArgChecker.isTrue(forward > 0, "Forward negative");
    double omega = (isCall ? 1d : -1d);
    double sqrtt = Math.sqrt(time);
    double n = NORMAL.getInverseCDF(omega * delta);
    double part1 = Math.exp(-volatility * sqrtt * omega * n + volatility * volatility * time / 2);
    double strike = forward * part1;
    // Backward sweep
    double strikeBar = 1d;
    double part1Bar = forward * strikeBar;
    double nBar = part1 * -volatility * Math.sqrt(time) * omega * part1Bar;
    derivatives[0] = omega / NORMAL.getPDF(n) * nBar;
    derivatives[1] = part1 * strikeBar;
    derivatives[2] = part1 * (-volatility * omega * n * 0.5 / sqrtt + volatility * volatility / 2) * part1Bar;
    derivatives[3] = part1 * (-sqrtt * omega * n + volatility * time) * part1Bar;
    return strike;
  }

  /**
   * Compute the log-normal implied volatility from a normal volatility using an approximate initial guess and a root-finder.
   * <p>
   * The forward and the strike must be positive.
   * <p>
   * Reference: Hagan, P. S. Volatility conversion calculator. Technical report, Bloomberg.
   * 
   * @param forward  the forward rate/price
   * @param strike  the option strike
   * @param timeToExpiry  the option time to expiration
   * @param normalVolatility  the normal implied volatility
   * @return the Black implied volatility
   */
  public static double impliedVolatilityFromNormalApproximated(
      final double forward,
      final double strike,
      final double timeToExpiry,
      final double normalVolatility) {
    ArgChecker.isTrue(strike > 0, "strike must be strictly positive");
    ArgChecker.isTrue(forward > 0, "strike must be strictly positive");
    // initial guess
    double guess = impliedVolatilityFromNormalApproximated2(forward, strike, timeToExpiry, normalVolatility);
    // Newton-Raphson method
    final Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double volatility) {
        return NormalFormulaRepository
            .impliedVolatilityFromBlackApproximated(forward, strike, timeToExpiry, volatility) - normalVolatility;
      }
    };
    return ROOT_FINDER.getRoot(func, guess);
  }

  /**
   * Compute the log-normal implied volatility from a normal volatility using an approximate initial guess and a 
   * root-finder and compute the derivative of the log-normal volatility with respect to the input normal volatility.
   * <p>
   * The forward and the strike must be positive.
   * <p>
   * Reference: Hagan, P. S. Volatility conversion calculator. Technical report, Bloomberg.
   * 
   * @param forward  the forward rate/price
   * @param strike  the option strike
   * @param timeToExpiry  the option time to expiration
   * @param normalVolatility  the normal implied volatility
   * @return the Black implied volatility and its derivative
   */
  public static ValueDerivatives impliedVolatilityFromNormalApproximatedAdjoint(
      final double forward,
      final double strike,
      final double timeToExpiry,
      final double normalVolatility) {

    ArgChecker.isTrue(strike > 0, "strike must be strictly positive");
    ArgChecker.isTrue(forward > 0, "strike must be strictly positive");
    // initial guess
    double guess = impliedVolatilityFromNormalApproximated2(forward, strike, timeToExpiry, normalVolatility);
    // Newton-Raphson method
    final Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double volatility) {
        return NormalFormulaRepository
            .impliedVolatilityFromBlackApproximated(forward, strike, timeToExpiry, volatility) - normalVolatility;
      }
    };
    double impliedVolatilityBlack = ROOT_FINDER.getRoot(func, guess);
    double derivativeInverse = NormalFormulaRepository
        .impliedVolatilityFromBlackApproximatedAdjoint(forward, strike, timeToExpiry, impliedVolatilityBlack).getDerivative(0);
    double derivative = 1.0 / derivativeInverse;
    return ValueDerivatives.of(impliedVolatilityBlack, DoubleArray.of(derivative));
  }

  /**
   * Compute the normal implied volatility from a normal volatility using an approximate explicit formula.
   * <p>
   * The formula is usually not good enough to be used as such, but provide a good initial guess for a 
   * root-finding procedure. Use {@link BlackFormulaRepository#impliedVolatilityFromNormalApproximated} for
   * more precision.
   * <p>
   * The forward and the strike must be positive.
   * <p>
   * Reference: Hagan, P. S. Volatility conversion calculator. Technical report, Bloomberg.
   * 
   * @param forward  the forward rate/price
   * @param strike  the option strike
   * @param timeToExpiry  the option time to expiration
   * @param normalVolatility  the normal implied volatility
   * @return the Black implied volatility
   */
  public static double impliedVolatilityFromNormalApproximated2(
      double forward,
      double strike,
      double timeToExpiry,
      double normalVolatility) {
    ArgChecker.isTrue(strike > 0, "strike must be strctly positive");
    ArgChecker.isTrue(forward > 0, "strike must be strctly positive");
    double lnFK = Math.log(forward / strike);
    double s2t = normalVolatility * normalVolatility * timeToExpiry;
    if (Math.abs((forward - strike) / strike) < ATM_LIMIT) {
      double factor1 = 1.0d / Math.sqrt(forward * strike);
      double factor2 = (1.0d + s2t / (24.0d * forward * strike)) / (1.0d + lnFK * lnFK / 24.0d);
      return normalVolatility * factor1 * factor2;
    }
    double factor1 = lnFK / (forward - strike);
    double factor2 = (1.0d + (1.0d - lnFK * lnFK / 120.0d) * s2t / (24.0d * forward * strike));
    return normalVolatility * factor1 * factor2;
  }

}
