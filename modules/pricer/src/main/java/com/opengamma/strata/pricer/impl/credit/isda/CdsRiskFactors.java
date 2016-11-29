/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

/**
 * Risk factors not covered by other calculators.
 * 
 * @see FiniteDifferenceSpreadSensitivityCalculator
 */
public class CdsRiskFactors {

  private final AnalyticCdsPricer pricer;

  public CdsRiskFactors() {
    this.pricer = new AnalyticCdsPricer();
  }

  public CdsRiskFactors(AccrualOnDefaultFormulae formula) {
    this.pricer = new AnalyticCdsPricer(formula);
  }

  /**
   * The sensitivity of a CDS to the recovery rate. Note this is per unit amount, so the change
   * in PV due to a one percent (say from 40% to 41%) rise is RR will be 0.01 * the returned value.
   * 
   * @param cds  the analytic description of a CDS traded at a certain time 
   * @param yieldCurve  the yield (or discount) curve  
   * @param creditCurve  the credit (or survival) curve 
   * @return the recovery rate sensitivity (on a unit notional) 
   */
  public double recoveryRateSensitivity(
      CdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve) {

    CdsAnalytic zeroRR = cds.withRecoveryRate(0);
    return -pricer.protectionLeg(zeroRR, yieldCurve, creditCurve);
  }

  /**
   * Immediately prior to default, the CDS has some value V (to the protection buyer).
   * After default, the contract cancelled, so there is an immediate loss of -V (or a gain if V was negative).
   * The protection buyer pays the accrued interest A and receives 1-RR, so the full
   * Value on Default (VoD) is -V + (1-RR) (where the A has been absorbed as we use the clean price for V). 
   * 
   * @param cds  the analytic description of a CDS traded at a certain time 
   * @param yieldCurve  the yield (or discount) curve  
   * @param creditCurve  the credit (or survival) curve 
   * @param coupon  the coupon of the CDS
   * @return the value on default or jump to default
   */
  public double valueOnDefault(
      CdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double coupon) {

    double pv = pricer.pv(cds, yieldCurve, creditCurve, coupon);
    return -pv + cds.getLGD();
  }

}
