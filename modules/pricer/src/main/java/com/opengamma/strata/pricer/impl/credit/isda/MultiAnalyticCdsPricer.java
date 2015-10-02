/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.math.impl.util.Epsilon.epsilon;
import static com.opengamma.strata.math.impl.util.Epsilon.epsilonP;
import static com.opengamma.strata.pricer.impl.credit.isda.DoublesScheduleGenerator.getIntegrationsPoints;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;

/**
 *
 */
public class MultiAnalyticCdsPricer {
  private static final double HALFDAY = 1 / 730.;
  /** Default value for determining if results consistent with ISDA model versions 1.8.2 or lower are to be calculated */
  private static final AccrualOnDefaultFormulae DEFAULT_FORMULA = AccrualOnDefaultFormulae.ORIGINAL_ISDA;
  /** True if results consistent with ISDA model versions 1.8.2 or lower are to be calculated */
  private final AccrualOnDefaultFormulae _formula;
  private final double _omega;

  /**
   * For consistency with the ISDA model version 1.8.2 and lower, a bug in the accrual on default calculation
   * has been reproduced.
   */
  public MultiAnalyticCdsPricer() {
    _formula = DEFAULT_FORMULA;
    _omega = HALFDAY;
  }

  /**
   *  For consistency with the ISDA model version 1.8.2 and lower, a bug in the accrual on default calculation
   * has been reproduced.
   * @param formula which accrual on default formulae to use.
   */
  public MultiAnalyticCdsPricer(AccrualOnDefaultFormulae formula) {
    ArgChecker.notNull(formula, "formula");
    _formula = formula;
    if (formula == AccrualOnDefaultFormulae.ORIGINAL_ISDA) {
      _omega = HALFDAY;
    } else {
      _omega = 0.0;
    }
  }

  /**
   * Present value for the payer of premiums (i.e. the buyer of protection)
   * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @param premium The common CDS premium (as a fraction)
   * @param cleanOrDirty Clean or dirty price
   * @return The PV on unit notional
   */
  public double[] pv(
      MultiCdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double premium,
      CdsPriceType cleanOrDirty) {

    int n = cds.getNumMaturities();
    double[] premiums = new double[n];
    Arrays.fill(premiums, premium);
    return pv(cds, yieldCurve, creditCurve, premiums, cleanOrDirty);
  }

  /**
   * Present value for the payer of premiums (i.e. the buyer of protection)
   * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @param premiums The CDS premiums (as fractions)
   * @param cleanOrDirty Clean or dirty price
   * @return The PV on unit notional
   */
  public double[] pv(MultiCdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double[] premiums,
      CdsPriceType cleanOrDirty) {

    int n = cds.getNumMaturities();
    ArgChecker.notEmpty(premiums, "premiums");
    ArgChecker.isTrue(n == premiums.length, "premiums wrong length. Should be {}, but is {}", n, premiums.length);
    double[] pv = new double[n];

    if (cds.getProtectionEnd(cds.getNumMaturities() - 1) <= 0.0) { //all CDSs have expired
      return pv;
    }
    // TODO check for any repeat calculations
    double[] rpv01 = pvPremiumLegPerUnitSpread(cds, yieldCurve, creditCurve, cleanOrDirty);
    double[] proLeg = protectionLeg(cds, yieldCurve, creditCurve);
    for (int i = 0; i < n; i++) {
      pv[i] = proLeg[i] - premiums[i] * rpv01[i];
    }
    return pv;
  }

  /**
   * Present value (clean price) for the payer of premiums (i.e. the buyer of protection)
   * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @param premiums The CDS premiums (as fractions)
   * @return The PV
   */
  public double[] pv(
      MultiCdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double[] premiums) {

    return pv(cds, yieldCurve, creditCurve, premiums, CdsPriceType.CLEAN);
  }

  /**
   * Present value (clean price) for the payer of premiums (i.e. the buyer of protection)
  * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @param premium The common CDS premium (as a fraction)
   * @return The PV
   */
  public double[] pv(
      MultiCdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double premium) {

    return pv(cds, yieldCurve, creditCurve, premium, CdsPriceType.CLEAN);
  }

  /**
   * Sensitivity of the present value (for the payer of premiums, i.e. the buyer of protection) to the zero hazard rate
   *  of a given node (knot) of the credit curve. This is per unit of notional
  * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @param fractionalSpread The <b>fraction</b> spread
   * @param creditCurveNode The credit curve node
   * @return PV sensitivity to one node (knot) on the credit (hazard rate/survival) curve
   */

  /**
   * The par spread par spread for a given yield and credit (hazard rate/survival) curve)
   * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @return the par spread
   */
  public double[] parSpread(MultiCdsAnalytic cds, IsdaCompliantYieldCurve yieldCurve, IsdaCompliantCreditCurve creditCurve) {
    if (cds.getProtectionEnd(0) <= 0.0) { //short cut already expired CDSs
      throw new IllegalArgumentException("A CDSs has expired - cannot compute a par spread for it");
    }

    double[] rpv01 = pvPremiumLegPerUnitSpread(cds, yieldCurve, creditCurve, CdsPriceType.CLEAN);
    double[] proLeg = protectionLeg(cds, yieldCurve, creditCurve);
    int n = cds.getNumMaturities();
    double[] s = new double[n];
    for (int i = 0; i < n; i++) {
      s[i] = proLeg[i] / rpv01[i];
    }

    return s;
  }

  /**
   * Sensitivity of the par spread (the fixed payment on the premium leg that make the PV of the CDS zero for a given yield
   * and credit (hazard rate/survival) curve) to the zero hazard rate of a given node (knot) of the credit curve.
  * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @param creditCurveNode The credit curve node
   * @return Par spread sensitivity to one node (knot) on the credit (hazard rate/survival) curve
   */

  /**
   * This is the present value of the premium leg per unit of fractional spread - hence it is equal to 10,000 times the RPV01
   * (Risky PV01). The actual PV of the leg is this multiplied by the notional and the fractional spread (i.e. spread in basis
   * points divided by 10,000)
   * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @param cleanOrDirty Clean or dirty price
   * @return 10,000 times the RPV01 (on a notional of 1)
   */
  public double[] pvPremiumLegPerUnitSpread(
      MultiCdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      CdsPriceType cleanOrDirty) {

    ArgChecker.notNull(cds, "null cds");
    ArgChecker.notNull(yieldCurve, "null yieldCurve");
    ArgChecker.notNull(creditCurve, "null creditCurve");

    double[] integrationSchedule = null;
    int nMat = cds.getNumMaturities();
    if (cds.isPayAccOnDefault()) {
      integrationSchedule = getIntegrationsPoints(
          cds.getEffectiveProtectionStart(), cds.getProtectionEnd(nMat - 1), yieldCurve, creditCurve);
    }
    double df = yieldCurve.getDiscountFactor(cds.getCashSettleTime());

    double[] pv = new double[nMat];
    int start = 0;
    double runningPV = 0;
    for (int matIndex = 0; matIndex < nMat; matIndex++) {
      if (cds.getProtectionEnd(matIndex) <= 0.0) { //skip expired CDSs (they have zero pv)
        continue;
      }

      int end = cds.getPaymentIndexForMaturity(matIndex);
      for (int i = start; i < end; i++) {
        CdsCoupon coupon = cds.getStandardCoupon(i);
        double q = creditCurve.getDiscountFactor(coupon.getEffEnd());
        double p = yieldCurve.getDiscountFactor(coupon.getPaymentTime());
        runningPV += coupon.getYearFrac() * p * q;
      }

      if (cds.isPayAccOnDefault()) {
        double accPV = 0;
        for (int i = start; i < end; i++) {
          CdsCoupon coupon = cds.getStandardCoupon(i);
          accPV += calculateSinglePeriodAccrualOnDefault(
              coupon, cds.getEffectiveProtectionStart(), integrationSchedule, yieldCurve, creditCurve);
        }
        runningPV += accPV;
      }

      double pvMat = runningPV;
      CdsCoupon terminalCoupon = cds.getTerminalCoupon(matIndex);
      double q = creditCurve.getDiscountFactor(terminalCoupon.getEffEnd());
      double p = yieldCurve.getDiscountFactor(terminalCoupon.getPaymentTime());
      pvMat += terminalCoupon.getYearFrac() * p * q;
      if (cds.isPayAccOnDefault()) {
        pvMat += calculateSinglePeriodAccrualOnDefault(
            terminalCoupon, cds.getEffectiveProtectionStart(), integrationSchedule, yieldCurve, creditCurve);
      }

      pv[matIndex] = pvMat / df;
      if (cleanOrDirty == CdsPriceType.CLEAN) {
        pv[matIndex] -= cds.getAccruedPremiumPerUnitSpread(matIndex);
      }
      start = Math.max(0, end);
    }

    return pv;
  }

  /**
   * The sensitivity (on a unit notional) of the (scaled) RPV01 to the zero hazard rate of a given node (knot) of the credit curve.
   * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @param creditCurveNode The credit curve node
   * @return  sensitivity (on a unit notional)
   */

  //TODO this is identical to the function in AnalyticCDSPricer 
  private double calculateSinglePeriodAccrualOnDefault(
      CdsCoupon coupon,
      double stepin,
      double[] integrationPoints,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve) {

    double start = Math.max(coupon.getEffStart(), stepin);
    if (start >= coupon.getEffEnd()) {
      return 0.0; //this coupon has already expired 
    }
    double[] knots = DoublesScheduleGenerator.truncateSetInclusive(start, coupon.getEffEnd(), integrationPoints);

    double t = knots[0];
    double ht0 = creditCurve.getRT(t);
    double rt0 = yieldCurve.getRT(t);
    double b0 = Math.exp(-rt0 - ht0); // this is the risky discount factor

    double t0 = t - coupon.getEffStart() + _omega;
    double pv = 0.0;
    int nItems = knots.length;
    for (int j = 1; j < nItems; ++j) {
      t = knots[j];
      double ht1 = creditCurve.getRT(t);
      double rt1 = yieldCurve.getRT(t);
      double b1 = Math.exp(-rt1 - ht1);

      double dt = knots[j] - knots[j - 1];

      double dht = ht1 - ht0;
      double drt = rt1 - rt0;
      double dhrt = dht + drt + 1e-50; // to keep consistent with ISDA c code

      double tPV;
      if (_formula == AccrualOnDefaultFormulae.MARKIT_FIX) {
        if (Math.abs(dhrt) < 1e-5) {
          tPV = dht * dt * b0 * epsilonP(-dhrt);
        } else {
          tPV = dht * dt / dhrt * ((b0 - b1) / dhrt - b1);
        }
      } else {
        double t1 = t - coupon.getEffStart() + _omega;
        if (Math.abs(dhrt) < 1e-5) {
          tPV = dht * b0 * (t0 * epsilon(-dhrt) + dt * epsilonP(-dhrt));
        } else {
          tPV = dht / dhrt * (t0 * b0 - t1 * b1 + dt / dhrt * (b0 - b1));
        }
        t0 = t1;
      }

      pv += tPV;
      ht0 = ht1;
      rt0 = rt1;
      b0 = b1;
    }
    return coupon.getYFRatio() * pv;
  }

  //    double b0 = p0 * q0; // this is the risky discount factor
  //      // TODO once the maths is written up in a white paper, check these formula again, since tests again finite difference
  //      // could miss some subtle error

  /**
   * Compute the present value of the protection leg with a notional of 1, which is given by the integral
   * $\frac{1-R}{P(T_{v})} \int_{T_a} ^{T_b} P(t) \frac{dQ(t)}{dt} dt$ where $P(t)$ and $Q(t)$ are the discount and survival curves
   * respectively, $T_a$ and $T_b$ are the start and end of the protection respectively, $T_v$ is the valuation time (all measured
   * from $t = 0$, 'today') and $R$ is the recovery rate.
   * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @return The value of the protection leg (on a unit notional)
   */
  public double[] protectionLeg(MultiCdsAnalytic cds, IsdaCompliantYieldCurve yieldCurve, IsdaCompliantCreditCurve creditCurve) {
    ArgChecker.notNull(cds, "null cds");
    ArgChecker.notNull(yieldCurve, "null yieldCurve");
    ArgChecker.notNull(creditCurve, "null creditCurve");

    // Compute the discount factor discounting the upfront payment made on the cash settlement date back to the valuation date
    double df = yieldCurve.getDiscountFactor(cds.getCashSettleTime());
    double factor = cds.getLGD() / df;
    int nMat = cds.getNumMaturities();
    double start = cds.getEffectiveProtectionStart();
    double[] fullIntegrationSchedule = getIntegrationsPoints(start, cds.getProtectionEnd(nMat - 1), yieldCurve, creditCurve);
    double[] pv = new double[nMat];
    double runningPV = 0;
    for (int matIndex = 0; matIndex < nMat; matIndex++) {
      double end = cds.getProtectionEnd(matIndex);
      if (end <= 0.0) {
        continue; //short cut already expired CDSs
      }

      double[] integrationSchedule = DoublesScheduleGenerator.truncateSetInclusive(start, end, fullIntegrationSchedule);
      runningPV += protectionLegInterval(integrationSchedule, yieldCurve, creditCurve);
      pv[matIndex] = runningPV * factor;
      start = end;
    }

    return pv;
  }

  private double protectionLegInterval(
      double[] integrationSchedule,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve) {

    double ht0 = creditCurve.getRT(integrationSchedule[0]);
    double rt0 = yieldCurve.getRT(integrationSchedule[0]);
    double b0 = Math.exp(-ht0 - rt0); // risky discount factor

    double pv = 0.0;
    int n = integrationSchedule.length;
    for (int i = 1; i < n; ++i) {

      double ht1 = creditCurve.getRT(integrationSchedule[i]);
      double rt1 = yieldCurve.getRT(integrationSchedule[i]);
      double b1 = Math.exp(-ht1 - rt1);

      double dht = ht1 - ht0;
      double drt = rt1 - rt0;
      double dhrt = dht + drt;

      // The formula has been modified from ISDA (but is equivalent) to avoid log(exp(x)) and explicitly calculating the time
      // step - it also handles the limit
      double dPV;
      if (Math.abs(dhrt) < 1e-5) {
        dPV = dht * b0 * epsilon(-dhrt);
      } else {
        dPV = (b0 - b1) * dht / dhrt;
      }

      pv += dPV;
      ht0 = ht1;
      rt0 = rt1;
      b0 = b1;
    }
    return pv;
  }

  /**
   * The sensitivity of the PV of the protection leg to the zero hazard rate of a given node (knot) of the credit curve.
   * @param cds analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit (or survival) curve
   * @param creditCurveNode The credit curve node
   * @return  sensitivity (on a unit notional)
   */
  //        (creditCurveNode != creditCurve.getNumberOfKnots() - 1 && cds.getProtectionStart() >= creditCurve.getTimeAtIndex(creditCurveNode + 1))) {
  //      return 0.0; // can't have any sensitivity in this case
  //    // Compute the discount factor discounting the upfront payment made on the cash settlement date back to the valuation date

}
