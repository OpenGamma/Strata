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
 * The price function to compute the price of one-touch or no-touch (asset-or-nothing) option in the Black world.
 * <p>
 * This function prices one-touch/no-touch option, where the asset payment can occur at hit for a one-touch option, and 
 * at expiry for a no-touch option.
 * Reference: E. G. Haug (2007) The complete guide to Option Pricing Formulas. Mc Graw Hill. Section 4.19.5.
 */
public class BlackOneTouchAssetPriceFormulaRepository {

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
    double df1 = Math.exp(timeToExpiry * (costOfCarry - rate));
    double lognormalVolSq = lognormalVol * lognormalVol;
    double lognormalVolT = lognormalVol * Math.sqrt(timeToExpiry);
    if (DoubleMath.fuzzyEquals(Math.min(timeToExpiry, lognormalVolSq), 0d, SMALL)) {
      return isKnockIn ? 0d : spot * df1;
    }
    double mu = (costOfCarry - 0.5 * lognormalVolSq) / lognormalVolSq;
    double lambda = Math.sqrt(mu * mu + 2 * rate / lognormalVolSq);
    double m1 = lognormalVolT * (1 + mu);
    double x2 = Math.log(spot / h) / lognormalVolT + m1;
    double y2 = Math.log(h / spot) / lognormalVolT + m1;
    double z = Math.log(h / spot) / lognormalVolT + lambda * lognormalVolT;
    double xE = isKnockIn ?
        getF(spot, z, lognormalVolT, h, mu, lambda, eta, h) :
        getE(spot, df1, x2, y2, h, mu, eta);
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
    double df1 = Math.exp(timeToExpiry * (costOfCarry - rate));
    double lognormalVolSq = lognormalVol * lognormalVol;
    double lognormalVolT = lognormalVol * Math.sqrt(timeToExpiry);
    if (DoubleMath.fuzzyEquals(Math.min(timeToExpiry, lognormalVolSq), 0d, SMALL)) {
      if (isKnockIn) {
        return ValueDerivatives.of(0d, DoubleArray.filled(6));
      }
      double price = df1 * spot;
      derivatives[0] = df1;
      derivatives[1] = -timeToExpiry * price;
      derivatives[2] = timeToExpiry * price;
      derivatives[4] = (costOfCarry - rate) * price;
      return ValueDerivatives.of(price, DoubleArray.ofUnsafe(derivatives));
    }
    double mu = (costOfCarry - 0.5 * lognormalVolSq) / lognormalVolSq;
    double lambda = Math.sqrt(mu * mu + 2d * rate / lognormalVolSq);
    double m1 = lognormalVolT * (1d + mu);
    double x2 = Math.log(spot / h) / lognormalVolT + m1;
    double y2 = Math.log(h / spot) / lognormalVolT + m1;
    double z = Math.log(h / spot) / lognormalVolT + lambda * lognormalVolT;
    double[] eDerivFirst = new double[6];
    double[] eDerivSecond = new double[5];
    double[] fDerivFirst = new double[5];
    double[] fDerivSecond = new double[5];
    double price = isKnockIn ?
        getFAdjoint(spot, z, lognormalVolT, h, mu, lambda, eta, h, fDerivFirst, fDerivSecond) :
        getEAdjoint(spot, df1, x2, y2, h, mu, eta, eDerivFirst, eDerivSecond);
    double zBar = 0.0;
    double y2Bar = 0.0;
    double x2Bar = 0.0;
    double zSqBar = 0.0;
    double y2SqBar = 0.0;
    double x2SqBar = 0.0;
    double zsBar = 0.0;
    double x2sBar = 0.0;
    double y2sBar = 0.0;
    double lambdaBar = 0.0;
    double muBar = 0.0;
    double lognormalVolTBar = 0.0;
    double df1Bar = 0.0;
    if (isKnockIn) {
      zBar = fDerivFirst[1];
      lambdaBar = fDerivFirst[4];
      muBar = fDerivFirst[3];
      lognormalVolTBar = fDerivFirst[2];
      derivatives[0] = fDerivFirst[0];
      zSqBar = fDerivSecond[1];
      zsBar = fDerivSecond[2];
      derivatives[5] = fDerivSecond[0];
    } else {
      y2Bar = eDerivFirst[3];
      x2Bar = eDerivFirst[2];
      muBar = eDerivFirst[4];
      df1Bar = eDerivFirst[1];
      derivatives[0] = eDerivFirst[0];
      x2SqBar = eDerivSecond[1];
      y2SqBar = eDerivSecond[2];
      x2sBar = eDerivSecond[3];
      y2sBar = eDerivSecond[4];
      derivatives[5] = eDerivSecond[0];
    }
    double dxyds = 1d / spot / lognormalVolT;
    double m1Bar = x2Bar + y2Bar;
    muBar += lognormalVolT * m1Bar + mu / lambda * lambdaBar;
    lognormalVolTBar +=
        +(lambda - Math.log(h / spot) / (lognormalVolT * lognormalVolT)) * zBar -
            Math.log(h / spot) / (lognormalVolT * lognormalVolT) * y2Bar -
            Math.log(spot / h) / (lognormalVolT * lognormalVolT) * x2Bar + (1 + mu) * m1Bar;
    double lognormalVolSqBar = -costOfCarry / (lognormalVolSq * lognormalVolSq) * muBar - rate /
        (lognormalVolSq * lognormalVolSq) / lambda * lambdaBar;
    derivatives[0] += dxyds * x2Bar - dxyds * y2Bar - dxyds * zBar;
    derivatives[1] = 1d / lambda / lognormalVolSq * lambdaBar - timeToExpiry * df1 * df1Bar;
    derivatives[2] = 1d / lognormalVolSq * muBar + timeToExpiry * df1 * df1Bar;
    derivatives[3] = 2d * lognormalVol * lognormalVolSqBar + Math.sqrt(timeToExpiry) * lognormalVolTBar;
    derivatives[4] = +(costOfCarry - rate) * df1 * df1Bar + lognormalVolTBar * lognormalVolT * 0.5 / timeToExpiry;
    derivatives[5] += -dxyds * x2Bar / spot + dxyds * y2Bar / spot + dxyds * zBar / spot + dxyds * dxyds * x2SqBar +
        2d * dxyds * x2sBar + dxyds * dxyds * y2SqBar - 2d * dxyds * y2sBar + dxyds * dxyds * zSqBar - 2d * dxyds * zsBar;
    return ValueDerivatives.of(price, DoubleArray.ofUnsafe(derivatives));
  }

  //-------------------------------------------------------------------------
  private double getE(
      double s,
      double df1,
      double x,
      double y,
      double h,
      double mu,
      double eta) {
    return s * df1 * (NORMAL.getCDF(eta * x) - Math.pow(h / s, 2d * (mu + 1d)) * NORMAL.getCDF(eta * y));
  }

  private double getF(
      double s,
      double z,
      double lognormalVolT,
      double h,
      double mu,
      double lambda,
      double eta,
      double barrier) {

    return barrier * (Math.pow(h / s, mu + lambda) * NORMAL.getCDF(eta * z) +
        Math.pow(h / s, mu - lambda) * NORMAL.getCDF(eta * (z - 2d * lambda * lognormalVolT)));
  }

  //-------------------------------------------------------------------------
  // The firstDerivatives are [0] s, [1] df1, [2] x, [3] y, [4] mu.
  // The second derivatives are [0] s twice, [1] x twice, [2] y twice, [3] s and x, [4] s and y.
  private double getEAdjoint(
      double s,
      double df1,
      double x,
      double y,
      double h,
      double mu,
      double eta,
      double[] firstDerivatives,
      double[] secondDerivatives) {

    double n1 = NORMAL.getCDF(eta * x);
    double n2 = NORMAL.getCDF(eta * y);
    double n1df = NORMAL.getPDF(x);
    double n2df = NORMAL.getPDF(y);
    double hsMu = Math.pow(h / s, 2d * (mu + 1d));
    double e = s * df1 * (n1 - hsMu * n2);
    firstDerivatives[0] = df1 * n1 - df1 * hsMu * n2 + 2d * (mu + 1d) * df1 * hsMu * n2; // s
    firstDerivatives[1] = s * (n1 - hsMu * n2); // df1;
    firstDerivatives[2] = s * df1 * n1df * eta; // x
    firstDerivatives[3] = -s * df1 * hsMu * n2df * eta; // y
    firstDerivatives[4] = -2d * Math.log(h / s) * s * df1 * hsMu * n2; // mu
    secondDerivatives[0] =
        -hsMu * n2 * 2d * (mu + 1d) * (2d * mu + 3d) * df1 / s + hsMu * n2 * 4d * (mu + 1d) * df1 / s;
    secondDerivatives[1] = -s * df1 * n1df * eta * x;
    secondDerivatives[2] = hsMu * n2df * s * df1 * eta * y;
    secondDerivatives[3] = df1 * n1df * eta;
    secondDerivatives[4] = -hsMu * n2df * df1 * eta + hsMu * n2df * 2d * (mu + 1d) * df1 * eta;
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
      double barrier,
      double[] firstDerivatives,
      double[] secondDerivatives) {

    double n1 = NORMAL.getCDF(eta * z);
    double n2 = NORMAL.getCDF(eta * (z - 2 * lambda * lognormalVolT));
    double hsMuPLa = Math.pow(h / s, mu + lambda);
    double hsMuMLa = Math.pow(h / s, mu - lambda);
    double f = barrier * (hsMuPLa * n1 + hsMuMLa * n2);
    double fBar = 1.0;
    double n1df = NORMAL.getPDF(eta * z);
    double n2df = NORMAL.getPDF(eta * (z - 2 * lambda * lognormalVolT));
    double hsMuPLaBar = n1 * fBar;
    double hsMuMLaBar = n2 * fBar;
    double n2Bar = hsMuMLa * fBar;
    double n1Bar = hsMuPLa * fBar;
    firstDerivatives[0] =
        barrier * (-(mu + lambda) * hsMuPLa / s * hsMuPLaBar - (mu - lambda) * hsMuMLa / s * hsMuMLaBar); //s
    firstDerivatives[1] = barrier * (n1df * eta * n1Bar + n2df * eta * n2Bar); // z
    firstDerivatives[2] = barrier * (-n2df * eta * 2 * lambda * n2Bar); //lognormalVolT
    firstDerivatives[3] = barrier * (hsMuPLa * Math.log(h / s) * hsMuPLaBar + hsMuMLa * Math.log(h / s) * hsMuMLaBar); // mu
    firstDerivatives[4] = barrier * (hsMuPLa * Math.log(h / s) * hsMuPLaBar - hsMuMLa * Math.log(h / s) * hsMuMLaBar); // lambda
    secondDerivatives[0] = barrier * (hsMuPLa * hsMuPLaBar * (mu + lambda) * (mu + lambda + 1d) / (s * s) +
        hsMuMLa * hsMuMLaBar * (mu - lambda) * (mu - lambda + 1d) / (s * s));
    secondDerivatives[1] = barrier * (-z * n1df * eta * n1Bar - (z - 2 * lambda * lognormalVolT) * n2df * eta * n2Bar);
    secondDerivatives[2] = barrier * (-n1df * n1Bar * (mu + lambda) * eta / s - n2df * n2Bar * (mu - lambda) * eta / s);
    return f;
  }
}
