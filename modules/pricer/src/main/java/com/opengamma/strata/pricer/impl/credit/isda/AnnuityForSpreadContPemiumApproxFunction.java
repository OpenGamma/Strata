/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import com.opengamma.strata.math.impl.util.Epsilon;

/**
 * 
 */
public class AnnuityForSpreadContPemiumApproxFunction extends AnnuityForSpreadFunction {

  private final int n;
  private final double[] knots;
  private final double[] rt;
  private final double lgd;
  private final double eta;

  /**
   * For a given quoted spread (aka 'flat' spread), this function returns the risky annuity
   * (aka risky PV01, RPV01 or risky duration).
   * This works by using the credit triangle approximation; that is, the premiums are assumed to be paid continuously.
   * 
   * @param cds  the analytic description of a CDS traded at a certain time 
   * @param yieldCurve  the calibrated yield curve 
   */
  public AnnuityForSpreadContPemiumApproxFunction(CdsAnalytic cds, IsdaCompliantYieldCurve yieldCurve) {
    this.knots = DoublesScheduleGenerator.truncateSetInclusive(
        cds.getEffectiveProtectionStart(), cds.getProtectionEnd(), yieldCurve.getKnotTimes());
    this.n = this.knots.length;
    this.rt = new double[n];
    for (int i = 0; i < n; i++) {
      this.rt[i] = yieldCurve.getRT(this.knots[i]);
    }
    this.lgd = cds.getLGD();
    this.eta = cds.getCoupon(0).getYFRatio();
  }

  @Override
  public Double apply(Double spread) {
    double lambda = eta * spread / lgd;
    return annuity(lambda);
  }

  private double annuity(double hazardRate) {
    double rt0 = rt[0];
    double sum = 0;
    for (int i = 1; i < n; i++) {
      double rt1 = rt[i];
      double dt = knots[i] - knots[i - 1];
      double theta = rt1 - rt0 + hazardRate * dt;
      sum += dt * Math.exp(-rt0 - hazardRate * knots[i - 1]) * Epsilon.epsilon(-theta);
      rt0 = rt1;
    }
    return eta * sum;
  }

}
