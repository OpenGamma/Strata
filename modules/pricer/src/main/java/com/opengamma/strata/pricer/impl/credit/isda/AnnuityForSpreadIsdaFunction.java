/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

/**
 */
public class AnnuityForSpreadIsdaFunction extends AnnuityForSpreadFunction {
  private static final AnalyticCdsPricer PRICER = new AnalyticCdsPricer();
  private final CdsAnalytic cds;
  private final IsdaCompliantYieldCurve yieldCurve;
  private final CreditCurveCalibrator calibrator;

  /**
   * For a given quoted spread (aka 'flat' spread), this function returns the risky annuity
   * (aka risky PV01, RPV01 or risky duration).
   * This works by first calibrating a constant hazard rate that recovers the given spread,
   * then computing the value of the annuity from this constant hazard rate.
   * The ISDA standard CDS model is used for these calculations.
   * 
   * @param cds  the analytic description of a CDS traded at a certain time 
   * @param yieldCurve  the calibrated yield curve 
   */
  public AnnuityForSpreadIsdaFunction(CdsAnalytic cds, IsdaCompliantYieldCurve yieldCurve) {
    this.cds = cds;
    this.yieldCurve = yieldCurve;
    this.calibrator = new CreditCurveCalibrator(new CdsAnalytic[] {cds}, yieldCurve);
  }

  @Override
  public Double apply(Double spread) {
    IsdaCompliantCreditCurve cc = calibrator.calibrate(new double[] {spread});
    return PRICER.annuity(cds, yieldCurve, cc, CdsPriceType.CLEAN);
  }

}
