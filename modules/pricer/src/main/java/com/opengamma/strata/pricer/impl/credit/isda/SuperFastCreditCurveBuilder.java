/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

/**
 * 
 */
public class SuperFastCreditCurveBuilder extends IsdaCompliantCreditCurveBuilder {

  public SuperFastCreditCurveBuilder() {
    super();

  }

  public SuperFastCreditCurveBuilder(AccrualOnDefaultFormulae formula) {
    super(formula);
  }

  public SuperFastCreditCurveBuilder(AccrualOnDefaultFormulae formula, ArbitrageHandling arbHandle) {
    super(formula, arbHandle);
  }

  //-------------------------------------------------------------------------
  @Override
  public IsdaCompliantCreditCurve calibrateCreditCurve(
      CdsAnalytic[] calibrationCDSs,
      double[] premiums,
      IsdaCompliantYieldCurve yieldCurve,
      double[] pointsUpfront) {

    CreditCurveCalibrator calibrator = new CreditCurveCalibrator(
        calibrationCDSs, yieldCurve, getAccOnDefaultFormula(), getArbHanding());
    return calibrator.calibrate(premiums, pointsUpfront);
  }

}
