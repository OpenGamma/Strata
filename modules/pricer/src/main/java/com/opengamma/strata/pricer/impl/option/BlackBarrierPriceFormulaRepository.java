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
 * The price function to compute the price of barrier option in the Black world.
 * Reference: E. G. Haug (2007) The complete guide to Option Pricing Formulas. Mc Graw Hill. Section 4.17.1.
 */
public class BlackBarrierPriceFormulaRepository {

  /**
   * The normal distribution implementation used in the pricing.
   */
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);

  /**
   * Small parameter.
   */
  private static final double SMALL = 1.0e-6;

  /**
   * Computes the price of a barrier option.
   * 
   * @param spot  the spot 
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry 
   * @param costOfCarry  the cost of carry 
   * @param rate  the interest rate
   * @param lognormalVol  the lognormal volatility
   * @param isCall  true if call, false otherwise
   * @param barrier  the barrier
   * @return the price
   */
  public double price(
      double spot,
      double strike,
      double timeToExpiry,
      double costOfCarry,
      double rate,
      double lognormalVol,
      boolean isCall,
      SimpleConstantContinuousBarrier barrier) {

    ArgChecker.notNull(barrier, "barrier");
    boolean isKnockIn = barrier.getKnockType().isKnockIn();
    boolean isDown = barrier.getBarrierType().isDown();
    double h = barrier.getBarrierLevel();
    ArgChecker.isFalse(isDown && spot <= barrier.getBarrierLevel(),
        "The Data is not consistent with an alive barrier (DOWN and spot<=barrier).");
    ArgChecker.isFalse(!isDown && spot >= barrier.getBarrierLevel(),
        "The Data is not consistent with an alive barrier (UP and spot>=barrier).");
    int phi = isCall ? 1 : -1;
    double eta = isDown ? 1 : -1;
    double df1 = Math.exp(timeToExpiry * (costOfCarry - rate));
    double df2 = Math.exp(-rate * timeToExpiry);
    double sigmaSq = lognormalVol * lognormalVol;
    double sigmaT = lognormalVol * Math.sqrt(timeToExpiry);
    if (DoubleMath.fuzzyEquals(Math.min(timeToExpiry, sigmaSq), 0d, SMALL)) {
      if (isKnockIn) {
        return 0d;
      }
      double dscFwd = df1 * spot;
      double dscStr = df2 * strike;
      return isCall ? (dscFwd >= dscStr ? dscFwd - dscStr : 0d) : (dscStr >= dscFwd ? dscStr - dscFwd : 0d);
    }
    double mu = (costOfCarry - 0.5 * sigmaSq) / sigmaSq;
    double m1 = sigmaT * (1 + mu);
    double x1 = Math.log(spot / strike) / sigmaT + m1;
    double x2 = Math.log(spot / h) / sigmaT + m1;
    double y1 = Math.log(h * h / spot / strike) / sigmaT + m1;
    double y2 = Math.log(h / spot) / sigmaT + m1;
    double xA = getA(spot, strike, df1, df2, x1, sigmaT, phi);
    double xB = getA(spot, strike, df1, df2, x2, sigmaT, phi);
    double xC = getC(spot, strike, df1, df2, y1, sigmaT, h, mu, phi, eta);
    double xD = getC(spot, strike, df1, df2, y2, sigmaT, h, mu, phi, eta);
    if (isKnockIn) { // KnockIn
      if (isDown) {
        if (isCall) {
          return strike > h ? xC : xA - xB + xD;
        }
        return strike > h ? xB - xC + xD : xA;
      }
      if (isCall) {
        return strike > h ? xA : xB - xC + xD;
      }
      return strike > h ? xA - xB + xD : xC;
    }
    // KnockOut
    if (isDown) {
      if (isCall) {
        return strike > h ? xA - xC : xB - xD;
      }
      return strike > h ? xA - xB + xC - xD : 0d;
    }
    if (isCall) {
      return strike > h ? 0d : xA - xB + xC - xD;
    }
    return strike > h ? xB - xD : xA - xC;
  }

  /**
   * Computes the price and derivatives of a barrier option.
   * 
   * The derivatives are [0] spot, [1] strike, [2] rate, [3] cost-of-carry, [4] volatility, [5] timeToExpiry, [6] spot twice
   * 
   * @param spot  the spot 
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry 
   * @param costOfCarry  the cost of carry 
   * @param rate  the interest rate
   * @param lognormalVol  the lognormal volatility
   * @param isCall  true if call, false otherwise
   * @param barrier  the barrier
   * @return the price and derivatives
   */
  public ValueDerivatives priceAdjoint(
      double spot,
      double strike,
      double timeToExpiry,
      double costOfCarry,
      double rate,
      double lognormalVol,
      boolean isCall,
      SimpleConstantContinuousBarrier barrier) {

    ArgChecker.notNull(barrier, "barrier");
    double[] derivatives = new double[7];
    boolean isKnockIn = barrier.getKnockType().isKnockIn();
    boolean isDown = barrier.getBarrierType().isDown();
    double h = barrier.getBarrierLevel();
    ArgChecker.isFalse(isDown && spot <= barrier.getBarrierLevel(),
        "The Data is not consistent with an alive barrier (DOWN and spot<=barrier).");
    ArgChecker.isFalse(!isDown && spot >= barrier.getBarrierLevel(),
        "The Data is not consistent with an alive barrier (UP and spot>=barrier).");
    int phi = isCall ? 1 : -1;
    double eta = isDown ? 1 : -1;
    double df1 = Math.exp(timeToExpiry * (costOfCarry - rate));
    double df2 = Math.exp(-rate * timeToExpiry);
    double lognormalVolSq = lognormalVol * lognormalVol;
    double lognormalVolT = lognormalVol * Math.sqrt(timeToExpiry);
    if (DoubleMath.fuzzyEquals(Math.min(timeToExpiry, lognormalVolSq), 0d, SMALL)) {
      if (isKnockIn) {
        return ValueDerivatives.of(0d, DoubleArray.filled(7));
      }
      double dscFwd = df1 * spot;
      double dscStr = df2 * strike;
      double price = 0d;
      if (isCall) {
        if (dscFwd >= dscStr) {
          price = dscFwd - dscStr;
          derivatives[0] = df1;
          derivatives[1] = -df2;
          derivatives[2] = -timeToExpiry * price;
          derivatives[3] = timeToExpiry * dscFwd;
          derivatives[5] = (costOfCarry - rate) * dscFwd + rate * dscStr;
        }
      } else {
        if (dscStr >= dscFwd) {
          price = dscStr - dscFwd;
          derivatives[0] = -df1;
          derivatives[1] = df2;
          derivatives[2] = -timeToExpiry * price;
          derivatives[3] = -timeToExpiry * dscFwd;
          derivatives[5] = -rate * dscStr - (costOfCarry - rate) * dscFwd;
        }
      }
      return ValueDerivatives.of(price, DoubleArray.ofUnsafe(derivatives));
    }
    double mu = (costOfCarry - 0.5 * lognormalVolSq) / lognormalVolSq;
    double m1 = lognormalVolT * (1 + mu);
    double x1 = Math.log(spot / strike) / lognormalVolT + m1;
    double x2 = Math.log(spot / h) / lognormalVolT + m1;
    double y1 = Math.log(h * h / spot / strike) / lognormalVolT + m1;
    double y2 = Math.log(h / spot) / lognormalVolT + m1;
    double[] aDerivFirst = new double[6];
    double[][] aDerivSecond = new double[2][2];
    double xA = getAAdjoint(spot, strike, df1, df2, x1, lognormalVolT, phi, aDerivFirst, aDerivSecond);
    double[] bDerivFirst = new double[6];
    double[][] bDerivSecond = new double[2][2];
    double xB = getAAdjoint(spot, strike, df1, df2, x2, lognormalVolT, phi, bDerivFirst, bDerivSecond);
    double[] cDerivFirst = new double[7];
    double[][] cDerivSecond = new double[2][2];
    double xC = getCAdjoint(spot, strike, df1, df2, y1, lognormalVolT, h, mu, phi, eta, cDerivFirst, cDerivSecond);
    double[] dDerivFirst = new double[7];
    double[][] dDerivSecond = new double[2][2];
    double xD = getCAdjoint(spot, strike, df1, df2, y2, lognormalVolT, h, mu, phi, eta, dDerivFirst, dDerivSecond);
    double xDBar = 0d;
    double xCBar = 0d;
    double xBBar = 0d;
    double xABar = 0d;
    double price;
    if (isKnockIn) { // IN start
      if (isDown) { // DOWN start
        if (isCall) { // Call start
          if (strike > h) {
            xCBar = 1d;
            price = xC;
          } else {
            xABar = 1d;
            xBBar = -1d;
            xDBar = 1d;
            price = xA - xB + xD;
          }
        } else { // Put start
          if (strike > h) {
            xBBar = 1d;
            xCBar = -1d;
            xDBar = 1d;
            price = xB - xC + xD;
          } else {
            xABar = 1d;
            price = xA;
          }
        } // DOWN end
      } else { // UP start
        if (isCall) {
          if (strike > h) {
            xABar = 1d;
            price = xA;
          } else {
            xBBar = 1d;
            xCBar = -1d;
            xDBar = 1d;
            price = xB - xC + xD;
          }
        } else {
          if (strike > h) {
            xABar = 1d;
            xBBar = -1d;
            xDBar = 1d;
            price = xA - xB + xD;
          } else {
            xCBar = 1d;
            price = xC;
          }
        } // UP end
      } // IN end
    } else { // OUT start
      if (isDown) { // DOWN start
        if (isCall) { // CALL start
          if (strike > h) {
            xABar = 1d;
            xCBar = -1d;
            price = xA - xC;
          } else {
            xBBar = 1d;
            xDBar = -1d;
            price = xB - xD;
          }
        } else { // PUT start
          if (strike > h) {
            xABar = 1d;
            xBBar = -1d;
            xCBar = 1d;
            xDBar = -1d;
            price = xA - xB + xC - xD;
          } else {
            price = 0d;
          } // PUT end
        } // DOWN end
      } else { // UP start
        if (isCall) {
          if (strike > h) {
            price = 0d;
          } else {
            xABar = 1d;
            xBBar = -1d;
            xCBar = 1d;
            xDBar = -1d;
            price = xA - xB + xC - xD;
          }
        } else {
          if (strike > h) {
            xBBar = 1d;
            xDBar = -1d;
            price = xB - xD;
          } else {
            xABar = 1d;
            xCBar = -1d;
            price = xA - xC;
          } // PUT end
        } // UP end
      } // OUT end
    }
    double dxyds = 1d / spot / lognormalVolT;
    double x1Bar = aDerivFirst[4] * xABar;
    double x2Bar = bDerivFirst[4] * xBBar;
    double y1Bar = cDerivFirst[4] * xCBar;
    double y2Bar = dDerivFirst[4] * xDBar;
    double m1Bar = x1Bar + x2Bar + y1Bar + y2Bar;
    double muBar = cDerivFirst[6] * xCBar + dDerivFirst[6] * xDBar + lognormalVolT * m1Bar;
    double lognormalVolTBar = aDerivFirst[5] * xABar + bDerivFirst[5] * xBBar + cDerivFirst[5] * xCBar + dDerivFirst[5] * xDBar -
        Math.log(h / spot) / (lognormalVolT * lognormalVolT) * y2Bar -
        Math.log(h * h / spot / strike) / (lognormalVolT * lognormalVolT) * y1Bar -
        Math.log(spot / h) / (lognormalVolT * lognormalVolT) * x2Bar -
        Math.log(spot / strike) / (lognormalVolT * lognormalVolT) * x1Bar + (1d + mu) * m1Bar;
    double lognormalVolSqBar = -costOfCarry / (lognormalVolSq * lognormalVolSq) * muBar;
    double df2Bar = aDerivFirst[3] * xABar + bDerivFirst[3] * xBBar + cDerivFirst[3] * xCBar + dDerivFirst[3] * xDBar;
    double df1Bar = aDerivFirst[2] * xABar + bDerivFirst[2] * xBBar + cDerivFirst[2] * xCBar + dDerivFirst[2] * xDBar;
    derivatives[0] = aDerivFirst[0] * xABar + bDerivFirst[0] * xBBar + cDerivFirst[0] * xCBar + dDerivFirst[0] * xDBar +
        x1Bar * dxyds + x2Bar * dxyds - y1Bar * dxyds - y2Bar * dxyds;
    derivatives[1] = aDerivFirst[1] * xABar + bDerivFirst[1] * xBBar + cDerivFirst[1] * xCBar + dDerivFirst[1] * xDBar -
        (x1Bar + y1Bar) / strike / lognormalVolT;
    derivatives[2] = -timeToExpiry * (df1 * df1Bar + df2 * df2Bar);
    derivatives[3] = timeToExpiry * df1 * df1Bar + muBar / lognormalVolSq;
    derivatives[4] = 2d * lognormalVol * lognormalVolSqBar + Math.sqrt(timeToExpiry) * lognormalVolTBar;
    derivatives[5] =
        (costOfCarry - rate) * df1 * df1Bar - rate * df2 * df2Bar + lognormalVolTBar * lognormalVolT * 0.5 / timeToExpiry;
    derivatives[6] = aDerivSecond[0][0] * xABar + bDerivSecond[0][0] * xBBar + cDerivSecond[0][0] * xCBar +
        dDerivSecond[0][0] * xDBar + 2d * xABar * aDerivSecond[0][1] * dxyds + 2d * xBBar * bDerivSecond[0][1] * dxyds -
        2d * xCBar * cDerivSecond[0][1] * dxyds - 2d * xDBar * dDerivSecond[0][1] * dxyds +
        xABar * aDerivSecond[1][1] * dxyds * dxyds + xBBar * bDerivSecond[1][1] * dxyds * dxyds +
        xCBar * cDerivSecond[1][1] * dxyds * dxyds + xDBar * dDerivSecond[1][1] * dxyds * dxyds - x1Bar * dxyds / spot -
        x2Bar * dxyds / spot + y1Bar * dxyds / spot + y2Bar * dxyds / spot;
    return ValueDerivatives.of(price, DoubleArray.ofUnsafe(derivatives));
  }

  //-------------------------------------------------------------------------
  private double getA(
      double s,
      double k,
      double df1,
      double df2,
      double x,
      double lognormalVolT,
      double phi) {

    return phi * (s * df1 * NORMAL.getCDF(phi * x) - k * df2 * NORMAL.getCDF(phi * (x - lognormalVolT)));
  }

  private double getC(
      double s,
      double k,
      double df1,
      double df2,
      double y,
      double lognormalVolT,
      double h,
      double mu,
      double phi,
      double eta) {

    return phi * (s * df1 * Math.pow(h / s, 2d * (mu + 1d)) * NORMAL.getCDF(eta * y) -
        k * df2 * Math.pow(h / s, 2d * mu) * NORMAL.getCDF(eta * (y - lognormalVolT)));
  }

  //-------------------------------------------------------------------------
  // The first derivatives are [0] s, [1] k, [2] df1, [3] df2, [4] x, [5] lognormalVolT
  // The second derivatives are [0][0] s twice, [0][1] s and x, [1][0] s and x , [1][1] x twice
  private double getAAdjoint(
      double s,
      double k,
      double df1,
      double df2,
      double x,
      double lognormalVolT,
      double phi,
      double[] firstderivatives,
      double[][] secondderivatives) {

    //  Forward sweep
    double n1 = NORMAL.getCDF(phi * x);
    double n2 = NORMAL.getCDF(phi * (x - lognormalVolT));
    double a = phi * (s * df1 * n1 - k * df2 * n2);
    // Backward sweep
    double n2Bar = phi * -k * df2;
    double n1Bar = phi * s * df1;
    firstderivatives[0] = phi * df1 * n1;
    firstderivatives[1] = phi * -df2 * n2;
    firstderivatives[2] = phi * s * n1;
    firstderivatives[3] = phi * -k * n2;
    double n1df = NORMAL.getPDF(x);
    double n2df = NORMAL.getPDF(x - lognormalVolT);
    firstderivatives[4] = n1df * phi * n1Bar + n2df * phi * n2Bar;
    firstderivatives[5] = n2df * -phi * n2Bar;
    secondderivatives[0][0] = 0d;
    secondderivatives[1][0] = n1df * df1;
    secondderivatives[0][1] = secondderivatives[1][0];
    secondderivatives[1][1] = -x * n1df * phi * n1Bar - (x - lognormalVolT) * n2df * phi * n2Bar;
    return a;
  }

  // The first derivatives are [0] s, [1] k, [2] df1, [3] df2, [4] y, [5] lognormalVolT, [6] mu.
  // The second derivatives are [0][0] s twice, [0][1] s and y, [1][0] s and y , [1][1] y twice.
  private double getCAdjoint(
      double s,
      double k,
      double df1,
      double df2,
      double y,
      double lognormalVolT,
      double h,
      double mu,
      double phi,
      double eta,
      double[] firstDerivatives,
      double[][] secondDerivatives) {

    //  Forward sweep
    double n1 = NORMAL.getCDF(eta * y);
    double n2 = NORMAL.getCDF(eta * (y - lognormalVolT));
    double hsMu1 = Math.pow(h / s, 2d * (mu + 1d));
    double hsMu = Math.pow(h / s, 2d * mu);
    double c = phi * (s * df1 * hsMu1 * n1 - k * df2 * hsMu * n2);
    // Backward sweep
    double n1df = NORMAL.getPDF(y);
    double n2df = NORMAL.getPDF(y - lognormalVolT);
    double hsMuBar = phi * -k * df2 * n2;
    double hsMu1Bar = phi * s * df1 * n1;
    double n2Bar = phi * -k * df2 * hsMu;
    double n1Bar = phi * s * df1 * hsMu1;
    firstDerivatives[0] = phi * df1 * hsMu1 * n1 - 2d * mu * hsMu / s * hsMuBar - 2d * (mu + 1d) * hsMu1 / s * hsMu1Bar; // s
    firstDerivatives[1] = phi * -df2 * hsMu * n2; // k
    firstDerivatives[2] = phi * s * hsMu1 * n1; // df1
    firstDerivatives[3] = phi * -k * hsMu * n2; // df2
    firstDerivatives[4] = n1df * eta * n1Bar + n2df * eta * n2Bar; // y
    firstDerivatives[5] = -n2df * eta * n2Bar; // lognormalVolT
    firstDerivatives[6] = 2d * Math.log(h / s) * hsMu * hsMuBar + 2d * Math.log(h / s) * hsMu1 * hsMu1Bar; // mu
    secondDerivatives[0][0] = -2d * (mu + 1d) * phi * df1 * hsMu1 * n1 / s + hsMu * hsMuBar * 2d * mu * (2d * mu + 1d) / (s * s) -
        hsMu1 * hsMu1Bar * 2d * (mu + 1d) / (s * s) + hsMu1 * hsMu1Bar * 2d * (mu + 1d) * (2d * mu + 3d) / (s * s);
    secondDerivatives[0][1] = hsMu1 * n1df * phi * df1 * eta - 2d * mu * hsMu / s * phi * -k * df2 * n2df * eta -
        hsMu1 * n1df * 2d * (mu + 1d) * phi * df1 * eta;
    secondDerivatives[1][0] = secondDerivatives[0][1];
    secondDerivatives[1][1] = -y * n1df * eta * n1Bar - (y - lognormalVolT) * n2df * eta * n2Bar;
    return c;
  }
}
