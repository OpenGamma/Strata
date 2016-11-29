/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.math.impl.util.Epsilon.epsilon;

import java.util.Arrays;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.rootfinding.NewtonRaphsonSingleRootFinder;

/**
 * The bond pricer.
 */
public class AnalyticBondPricer {

  private static final NewtonRaphsonSingleRootFinder ROOTFINDER = new NewtonRaphsonSingleRootFinder();
  private final AnalyticCdsPricer pricer = new AnalyticCdsPricer();

  //-------------------------------------------------------------------------
  /**
   * Compute the equivalent CDS spread for a bond. This works by first finding a constant
   * hazard rate that reprices the bond (given the supplied yield curve), the using this hazard
   * rate to calculate the par spread of a CDS.
   * 
   * @param bond  the simple analytic representation of a fixed coupon bond
   * @param yieldCurve  the yield curve 
   * @param bondPrice  the bond price (for unit notional). Can be given clean or dirty (see below).
   *  The dirty price cannot be below that of the bond's recovery rate or greater than its risk free price
   * @param cleanOrDirty  the clean or dirty price for the bond 
   * @param cds  the analytic description of a CDS traded at a certain time for which the spread is calculated
   * @see #getHazardRate
   * @return the equivalent CDS spread
   */
  public double getEquivalentCdsSpread(
      BondAnalytic bond,
      IsdaCompliantYieldCurve yieldCurve,
      double bondPrice,
      CdsPriceType cleanOrDirty,
      CdsAnalytic cds) {

    double lambda = getHazardRate(bond, yieldCurve, bondPrice, cleanOrDirty);
    IsdaCompliantCreditCurve cc = new IsdaCompliantCreditCurve(cds.getProtectionEnd(), lambda);
    return pricer.parSpread(cds, yieldCurve, cc);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the constant hazard rate implied from a bond price.
   *
   * @param bond  the simple analytic representation of a fixed coupon bond
   * @param yieldCurve  the yield curve 
   * @param bondPrice  the bond price (for unit notional). Can be given clean or dirty (see below).
   *  The dirty price cannot be below that of the bond's recovery rate or greater than its risk free price
   * @param cleanOrDirty  the clean or dirty price for the bond 
   * @return the implied hazard rate
   */
  public double getHazardRate(
      BondAnalytic bond,
      IsdaCompliantYieldCurve yieldCurve,
      double bondPrice,
      CdsPriceType cleanOrDirty) {

    ArgChecker.isTrue(bondPrice > 0.0, "Bond price must be positive");
    Function<Double, Double> priceFunc = getBondPriceForHazardRateFunction(bond, yieldCurve, cleanOrDirty);

    double zeroRiskPrice = priceFunc.apply(0.);
    if (bondPrice == zeroRiskPrice) {
      return 0.0;
    }
    if (bondPrice > zeroRiskPrice) {
      throw new IllegalArgumentException("Bond price of " + bondPrice + ", is greater that zero-risk price of " +
          zeroRiskPrice + ". It is not possible to imply a hazard rate for this bond. Please check inputs");
    }
    double dp = cleanOrDirty == CdsPriceType.DIRTY ? bondPrice : bondPrice + bond.getAccruedInterest();
    if (dp <= bond.getRecoveryRate()) {
      throw new IllegalArgumentException("The dirty price of " + dp + " give, is less than the bond's recovery rate of " +
          bond.getRecoveryRate() + ". Please check inputs");
    }

    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double lambda) {
        return priceFunc.apply(lambda) - bondPrice;
      }
    };

    double guess = 0.01;
    return ROOTFINDER.getRoot(func, guess);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the bond price for a given level of a constant hazard rate.
   * 
   * @param bond  the simple analytic representation of a fixed coupon bond
   * @param yieldCurve  the yield curve 
   * @param hazardRate  the hazard rate, can be zero
   * @param cleanOrDirty  the clean or dirty price for the bond 
   * @see #getBondPriceForHazardRateFunction
   * @return the bond price
   */
  public double bondPriceForHazardRate(
      BondAnalytic bond, IsdaCompliantYieldCurve yieldCurve, double hazardRate, CdsPriceType cleanOrDirty) {
    return getBondPriceForHazardRateFunction(bond, yieldCurve, cleanOrDirty).apply(hazardRate);
  }

  //-------------------------------------------------------------------------
  /**
   * This gives a function that allows you to price a bond for any level of a constant hazard rate.
   * 
   * @param bond  the simple analytic representation of a fixed coupon bond
   * @param yieldCurve  the yield curve 
   * @param cleanOrDirty  the clean or dirty price for the bond 
   * @return a function of hazard rate to bond price 
   */
  public Function<Double, Double> getBondPriceForHazardRateFunction(
      BondAnalytic bond,
      IsdaCompliantYieldCurve yieldCurve,
      CdsPriceType cleanOrDirty) {

    ArgChecker.notNull(bond, "bond");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(cleanOrDirty, "cleanOrDirty");

    int nPayments = bond.getnPayments();
    double[] discPayments = new double[nPayments];

    for (int i = 0; i < nPayments; i++) {
      discPayments[i] = bond.getPaymentAmount(i) * yieldCurve.getDiscountFactor(bond.getPaymentTime(i));
    }

    double exp = bond.getPaymentTime(nPayments - 1);
    int index = Arrays.binarySearch(yieldCurve.getKnotTimes(), exp);
    double[] temp;
    if (index >= 0) {
      temp = new double[index + 1];
      System.arraycopy(yieldCurve.getKnotTimes(), 0, temp, 0, index + 1);
    } else {
      index = -(index + 1);
      temp = new double[index + 1];
      System.arraycopy(yieldCurve.getKnotTimes(), 0, temp, 0, index);
      temp[index] = exp;
    }

    double[] intNodes = temp;
    int nNodes = intNodes.length;
    double[] rt = new double[nNodes];
    for (int i = 0; i < nNodes; i++) {
      rt[i] = yieldCurve.getRT(intNodes[i]);
    }

    return new Function<Double, Double>() {

      @Override
      public Double apply(Double lambda) {

        double riskyDisPayments = cleanOrDirty == CdsPriceType.CLEAN ? -bond.getAccruedInterest() : 0.0;
        for (int i = 0; i < nPayments; i++) {
          double q = Math.exp(-lambda * bond.getPaymentTime(i));
          riskyDisPayments += discPayments[i] * q;
        }
        if (bond.getRecoveryRate() == 0.0) {
          return riskyDisPayments;
        }

        double[] ht = new double[nNodes];
        double[] b = new double[nNodes];
        for (int i = 0; i < nNodes; ++i) {
          ht[i] = lambda * intNodes[i];
          b[i] = Math.exp(-rt[i] - ht[i]);
        }

        double defaultPV = 0.0;
        {
          double dht = ht[0];
          double drt = rt[0];
          double dhrt = dht + drt;
          double dPV;
          if (Math.abs(dhrt) < 1e-5) {
            dPV = dht * epsilon(-dhrt);
          } else {
            dPV = (1 - b[0]) * dht / dhrt;
          }
          defaultPV += dPV;
        }
        for (int i = 1; i < nNodes; ++i) {

          double dht = ht[i] - ht[i - 1];
          double drt = rt[i] - rt[i - 1];
          double dhrt = dht + drt;
          double dPV;
          if (Math.abs(dhrt) < 1e-5) {
            dPV = dht * b[i - 1] * epsilon(-dhrt);
          } else {
            dPV = (b[i - 1] - b[i]) * dht / dhrt;
          }
          defaultPV += dPV;
        }

        return riskyDisPayments + bond.getRecoveryRate() * defaultPV;
      }
    };

  }

}
