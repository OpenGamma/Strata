/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * The price function to compute the price of one-touch or no-touch (cash-or-nothing) option in the Black world.
 * <p>
 * This function prices one-touch/no-touch option, where the cash payment can occur at hit for a one-touch option, and 
 * at expiry for a no-touch option.
 * Reference: E. G. Haug (2007) The complete guide to Option Pricing Formulas. Mc Graw Hill. Section 4.19.5.
 */
public class BlackOneTouchCashPriceFormulaRepository {

  /**
   * The normal distribution implementation used in the pricing.
   */
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);
  /**
   * Small parameter.
   */
  private static final double SMALL = 1.0e-6;

  /**
   * Computes the price of a one-touch/no-touch option.
   * 
   * @param spot  the spot 
   * @param timeToExpiry  the time to expiry 
   * @param costOfCarry  the cost of carry 
   * @param rate  the interest rate
   * @param lognormalVol  the lognormal volatility
   * @param barrier  the barrier
   * @return the price
   */
  public double price(
      double spot,
      double timeToExpiry,
      double costOfCarry,
      double rate,
      double lognormalVol,
      SimpleConstantContinuousBarrier barrier) {

    ArgChecker.notNull(barrier, "barrier");
    boolean isKnockIn = barrier.getKnockType().isKnockIn();
    boolean isDown = barrier.getBarrierType().isDown();
    double h = barrier.getBarrierLevel();
    ArgChecker.isFalse(isDown && spot <= barrier.getBarrierLevel(),
        "The Data is not consistent with an alive barrier (DOWN and spot<=barrier).");
    ArgChecker.isFalse(!isDown && spot >= barrier.getBarrierLevel(),
        "The Data is not consistent with an alive barrier (UP and spot>=barrier).");
    double eta = isDown ? 1 : -1;
    double df2 = Math.exp(-rate * timeToExpiry);
    double lognormalVolSq = lognormalVol * lognormalVol;
    double lognormalVolT = lognormalVol * Math.sqrt(timeToExpiry);
    if (DoubleMath.fuzzyEquals(Math.min(timeToExpiry, lognormalVolSq), 0d, SMALL)) {
      return isKnockIn ? 0d : df2;
    }
    double mu = (costOfCarry - 0.5 * lognormalVolSq) / lognormalVolSq;
    double lambda = Math.sqrt(mu * mu + 2 * rate / lognormalVolSq);
    double m1 = lognormalVolT * (1 + mu);
    double x2 = Math.log(spot / h) / lognormalVolT + m1;
    double y2 = Math.log(h / spot) / lognormalVolT + m1;
    double z = Math.log(h / spot) / lognormalVolT + lambda * lognormalVolT;
    double xE = isKnockIn ?
        getF(spot, z, lognormalVolT, h, mu, lambda, eta) :
        getE(spot, df2, x2, y2, lognormalVolT, h, mu, eta);
    return xE;
  }

  /**
   * Computes the price and derivatives of a one-touch/no-touch option.
   * <p>
   * The derivatives are [0] spot, [1] rate, [2] cost-of-carry, [3] volatility, [4] timeToExpiry, [5] spot twice.
   * 
   * @param spot  the spot 
   * @param timeToExpiry  the time to expiry 
   * @param costOfCarry  the cost of carry 
   * @param rate  the interest rate
   * @param lognormalVol  the lognormal volatility
   * @param barrier  the barrier
   * @return the price and derivatives
   */
  public ValueDerivatives priceAdjoint(
      double spot,
      double timeToExpiry,
      double costOfCarry,
      double rate,
      double lognormalVol,
      SimpleConstantContinuousBarrier barrier) {

    ArgChecker.notNull(barrier, "barrier");
    double[] derivatives = new double[6];
    boolean isKnockIn = barrier.getKnockType().isKnockIn();
    boolean isDown = barrier.getBarrierType().isDown();
    double h = barrier.getBarrierLevel();
    ArgChecker.isFalse(isDown && spot <= barrier.getBarrierLevel(),
        "The Data is not consistent with an alive barrier (DOWN and spot<=barrier).");
    ArgChecker.isFalse(!isDown && spot >= barrier.getBarrierLevel(),
        "The Data is not consistent with an alive barrier (UP and spot>=barrier).");

    double eta = isDown ? 1 : -1;
    double df2 = Math.exp(-rate * timeToExpiry);
    double lognormalVolSq = lognormalVol * lognormalVol;
    double lognormalVolT = lognormalVol * Math.sqrt(timeToExpiry);
    if (DoubleMath.fuzzyEquals(Math.min(timeToExpiry, lognormalVolSq), 0d, SMALL)) {
      if (isKnockIn) {
        return ValueDerivatives.of(0d, DoubleArray.filled(6));
      }
      double price = df2;
      derivatives[1] = -timeToExpiry * price;
      derivatives[4] = -rate * price;
      return ValueDerivatives.of(price, DoubleArray.ofUnsafe(derivatives));
    }
    double mu = (costOfCarry - 0.5 * lognormalVolSq) / lognormalVolSq;
    double lambda = Math.sqrt(mu * mu + 2d * rate / lognormalVolSq);
    double m1 = lognormalVolT * (1d + mu);
    double x2 = Math.log(spot / h) / lognormalVolT + m1;
    double y2 = Math.log(h / spot) / lognormalVolT + m1;
    double z = Math.log(h / spot) / lognormalVolT + lambda * lognormalVolT;
    double[] eDerivFirst = new double[6];
    double[] eDerivSecond = new double[6];
    double[] fDerivFirst = new double[5];
    double[] fDerivSecond = new double[5];
    double price = isKnockIn ?
        getFAdjoint(spot, z, lognormalVolT, h, mu, lambda, eta, fDerivFirst, fDerivSecond) :
        getEAdjoint(spot, df2, x2, y2, lognormalVolT, h, mu, eta, eDerivFirst, eDerivSecond);
    double zBar = 0.0;
    double y2Bar = 0.0;
    double x2Bar = 0.0;
    double zSqBar = 0.0;
    double y2SqBar = 0.0;
    double x2SqBar = 0.0;
    double zsBar = 0.0;
    double y2sBar = 0.0;
    double lambdaBar = 0.0;
    double muBar = 0.0;
    double lognormalVolTBar = 0.0;
    double df2Bar = 0.0;
    if (isKnockIn) {
      zBar = fDerivFirst[1];
      lambdaBar = fDerivFirst[4]; // only F has lambda dependence, which in turn is a function of mu, see muBar+= below
      muBar = fDerivFirst[3];
      lognormalVolTBar = fDerivFirst[2];
      derivatives[0] = fDerivFirst[0];
      zSqBar = fDerivSecond[1];
      zsBar = fDerivSecond[2];
      derivatives[5] = fDerivSecond[0];
    } else {
      y2Bar = eDerivFirst[3];
      x2Bar = eDerivFirst[2];
      muBar = eDerivFirst[5];
      lognormalVolTBar = eDerivFirst[4];
      df2Bar = eDerivFirst[1];
      derivatives[0] = eDerivFirst[0];
      y2SqBar = eDerivSecond[2];
      x2SqBar = eDerivSecond[1];
      y2sBar = eDerivSecond[3];
      derivatives[5] = eDerivSecond[0];
    }
    double dxyds = 1d / spot / lognormalVolT;
    double m1Bar = x2Bar + y2Bar;
    muBar += +lognormalVolT * m1Bar + mu / lambda * lambdaBar;
    lognormalVolTBar +=
        +(lambda - Math.log(h / spot) / (lognormalVolT * lognormalVolT)) * zBar -
            Math.log(h / spot) / (lognormalVolT * lognormalVolT) * y2Bar -
            Math.log(spot / h) / (lognormalVolT * lognormalVolT) * x2Bar + (1 + mu) * m1Bar;
    double lognormalVolSqBar = -costOfCarry / (lognormalVolSq * lognormalVolSq) * muBar - rate /
        (lognormalVolSq * lognormalVolSq) / lambda * lambdaBar;
    derivatives[0] += dxyds * x2Bar - dxyds * y2Bar - dxyds * zBar;
    derivatives[1] = -timeToExpiry * df2 * df2Bar + lambdaBar / lambda / lognormalVolSq;
    derivatives[2] = muBar / lognormalVolSq;
    derivatives[3] = 2d * lognormalVol * lognormalVolSqBar + Math.sqrt(timeToExpiry) * lognormalVolTBar;
    derivatives[4] = -rate * df2 * df2Bar + lognormalVolTBar * lognormalVolT * 0.5 / timeToExpiry;
    derivatives[5] += -dxyds * x2Bar / spot + dxyds * y2Bar / spot + dxyds * zBar / spot + dxyds * dxyds * x2SqBar +
        dxyds * dxyds * y2SqBar - 2d * dxyds * y2sBar + dxyds * dxyds * zSqBar - 2d * dxyds * zsBar;
    return ValueDerivatives.of(price, DoubleArray.ofUnsafe(derivatives));
  }

  //-------------------------------------------------------------------------
  private double getE(
      double s,
      double df2,
      double x,
      double y,
      double lognormalVolT,
      double h,
      double mu,
      double eta) {

    return df2 * (NORMAL.getCDF(eta * (x - lognormalVolT)) - Math.pow(h / s, 2d * mu) * NORMAL.getCDF(eta * (y - lognormalVolT)));
  }

  private double getF(
      double s,
      double z,
      double lognormalVolT,
      double h,
      double mu,
      double lambda,
      double eta) {

    return Math.pow(h / s, mu + lambda) * NORMAL.getCDF(eta * z) +
        Math.pow(h / s, mu - lambda) * NORMAL.getCDF(eta * (z - 2d * lambda * lognormalVolT));
  }

  //-------------------------------------------------------------------------
  // The firstDerivatives are [0] s, [1] df2, [2] x, [3] y, [4] lognormalVolT, [5] mu.
  // The second derivatives are [0] s twice, [1] x twice, [2] y twice, [3] s and y.
  private double getEAdjoint(
      double s,
      double df2,
      double x,
      double y,
      double lognormalVolT,
      double h,
      double mu,
      double eta,
      double[] firstDerivatives,
      double[] secondDerivatives) {

    double n1 = NORMAL.getCDF(eta * (x - lognormalVolT));
    double n2 = NORMAL.getCDF(eta * (y - lognormalVolT));
    double hsMu = Math.pow(h / s, 2 * mu);
    double e = df2 * (n1 - hsMu * n2);
    double n1df = NORMAL.getPDF(eta * (x - lognormalVolT));
    double n2df = NORMAL.getPDF(eta * (y - lognormalVolT));
    double hsMuBar = df2 * -n2;
    double n2Bar = df2 * -hsMu;
    double n1Bar = df2;
    firstDerivatives[0] = -2d * mu * hsMu / s * hsMuBar; // s
    firstDerivatives[1] = n1 - hsMu * n2; // df2;
    firstDerivatives[2] = n1df * eta * n1Bar; // x
    firstDerivatives[3] = n2df * eta * n2Bar; // y
    firstDerivatives[4] = n2df * -eta * n2Bar + n1df * -eta * n1Bar; // lognormalVolT
    firstDerivatives[5] = 2d * Math.log(h / s) * hsMu * hsMuBar; // mu
    secondDerivatives[0] = hsMu * hsMuBar * 2d * mu * (2d * mu + 1d) / (s * s);
    secondDerivatives[1] = -n1df * n1Bar * (x - lognormalVolT) * eta;
    secondDerivatives[2] = -n2df * n2Bar * (y - lognormalVolT) * eta;
    secondDerivatives[3] = -2d * mu * n2df * eta * n2Bar / s;
    return e;
  }

  // The firstDerivatives are [0] s, [1] z, [2] lognormalVolT, [3] mu, [4] lambda.
  // The second derivatives are [0] s twice, [1] z twice, [2] s and z.
  private double getFAdjoint(
      double s,
      double z,
      double lognormalVolT,
      double h,
      double mu,
      double lambda,
      double eta,
      double[] firstDerivatives,
      double[] secondDerivatives) {

    double n1 = NORMAL.getCDF(eta * z);
    double n2 = NORMAL.getCDF(eta * (z - 2 * lambda * lognormalVolT));
    double hsMuPLa = Math.pow(h / s, mu + lambda);
    double hsMuMLa = Math.pow(h / s, mu - lambda);
    double f = hsMuPLa * n1 + hsMuMLa * n2;
    double fBar = 1.0;
    double n1df = NORMAL.getPDF(eta * z);
    double n2df = NORMAL.getPDF(eta * (z - 2 * lambda * lognormalVolT));
    double hsMuPLaBar = n1 * fBar;
    double hsMuMLaBar = n2 * fBar;
    double n2Bar = hsMuMLa * fBar;
    double n1Bar = hsMuPLa * fBar;
    firstDerivatives[0] = -(mu + lambda) * hsMuPLa / s * hsMuPLaBar - (mu - lambda) * hsMuMLa / s * hsMuMLaBar; //s
    firstDerivatives[1] = n1df * eta * n1Bar + n2df * eta * n2Bar; // z
    firstDerivatives[2] = -n2df * eta * 2 * lambda * n2Bar; //lognormalVolT
    firstDerivatives[3] = hsMuPLa * Math.log(h / s) * hsMuPLaBar + hsMuMLa * Math.log(h / s) * hsMuMLaBar; // mu
    firstDerivatives[4] = hsMuPLa * Math.log(h / s) * hsMuPLaBar - hsMuMLa * Math.log(h / s) * hsMuMLaBar; // lambda
    secondDerivatives[0] = hsMuPLa * hsMuPLaBar * (mu + lambda) * (mu + lambda + 1d) / (s * s) +
        hsMuMLa * hsMuMLaBar * (mu - lambda) * (mu - lambda + 1d) / (s * s);
    secondDerivatives[1] = -z * n1df * eta * n1Bar - (z - 2 * lambda * lognormalVolT) * n2df * eta * n2Bar;
    secondDerivatives[2] = -n1df * n1Bar * (mu + lambda) * eta / s - n2df * n2Bar * (mu - lambda) * eta / s;
    return f;
  }
}
