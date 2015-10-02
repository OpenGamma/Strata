/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionResult;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 *
 */
public class AnalyticSpreadSensitivityCalculator {

  private final IsdaCompliantCreditCurveBuilder _curveBuilder;
  private final AnalyticCdsPricer _pricer;

  public AnalyticSpreadSensitivityCalculator() {
    _curveBuilder = new FastCreditCurveBuilder();
    _pricer = new AnalyticCdsPricer();
  }

  public AnalyticSpreadSensitivityCalculator(AccrualOnDefaultFormulae formula) {
    _curveBuilder = new FastCreditCurveBuilder(formula);
    _pricer = new AnalyticCdsPricer(formula);
  }

  //***************************************************************************************************************
  // parallel CS01 of a CDS from single market quote of that CDS
  //***************************************************************************************************************

  /**
   * The CS01 (or credit DV01)  of a CDS - the sensitivity of the PV to a finite increase of market spread (on NOT the CDS's
   * coupon). If the CDS is quoted as points up-front, this is first converted to a quoted spread, and <b>this</b> is bumped.
   * 
   * @param cds  the analytic description of a CDS traded at a certain time
   * @param quote  the market quote for the CDS - these can be ParSpread, PointsUpFront or QuotedSpread
   * @param yieldCurve  the yield (or discount) curve
   * @return the parallel CS01
   */
  public double parallelCS01(CdsAnalytic cds, CdsQuoteConvention quote, IsdaCompliantYieldCurve yieldCurve) {
    return parallelCS01(cds, quote.getCoupon(), new CdsAnalytic[] {cds}, new CdsQuoteConvention[] {quote}, yieldCurve);
  }

  /**
   * The analytic CS01 (or credit DV01).
   *
   * @param cds  the analytic description of a CDS traded at a certain time
   * @param coupon  the of the traded CDS  (expressed as <b>fractions not basis points</b>)
   * @param yieldCurve  the yield (or discount) curve
   * @param puf  the points up-front (as a fraction)
   * @return the credit DV01
   */
  public double parallelCS01FromPUF(CdsAnalytic cds, double coupon, IsdaCompliantYieldCurve yieldCurve, double puf) {

    IsdaCompliantCreditCurve cc = _curveBuilder.calibrateCreditCurve(cds, coupon, yieldCurve, puf);
    double a = _pricer.protectionLeg(cds, yieldCurve, cc);
    double b = _pricer.annuity(cds, yieldCurve, cc, CdsPriceType.CLEAN);
    double aPrime = _pricer.protectionLegCreditSensitivity(cds, yieldCurve, cc, 0);
    double bPrime = _pricer.pvPremiumLegCreditSensitivity(cds, yieldCurve, cc, 0);
    double s = a / b;
    double dPVdh = aPrime - coupon * bPrime;
    double dSdh = (aPrime - s * bPrime) / b;
    return dPVdh / dSdh;
  }

  /**
   * The analytic CS01 (or credit DV01).
   * 
   * @param cds  the analytic description of a CDS traded at a certain time
   * @param coupon  the of the traded CDS  (expressed as <b>fractions not basis points</b>)
   * @param yieldCurve  the yield (or discount) curve
   * @param marketSpread  the market spread of the reference CDS
   *  (in this case it is irrelevant whether this is par or quoted spread)
   * @return the credit DV01
   */
  public double parallelCS01FromSpread(
      CdsAnalytic cds,
      double coupon,
      IsdaCompliantYieldCurve yieldCurve,
      double marketSpread) {

    IsdaCompliantCreditCurve cc = _curveBuilder.calibrateCreditCurve(cds, marketSpread, yieldCurve);
    double a = _pricer.protectionLeg(cds, yieldCurve, cc);
    double b = a / marketSpread; //shortcut calculation of RPV01
    double diff = marketSpread - coupon;
    if (diff == 0) {
      return b;
    }
    double aPrime = _pricer.protectionLegCreditSensitivity(cds, yieldCurve, cc, 0);
    double bPrime = _pricer.pvPremiumLegCreditSensitivity(cds, yieldCurve, cc, 0);
    double dSdh = (aPrime - marketSpread * bPrime); //note - this has not been divided by b
    return b * (1 + diff * bPrime / dSdh);
  }

  public double parallelCS01(
      CdsAnalytic cds,
      double cdsCoupon,
      CdsAnalytic[] pillarCDSs,
      CdsQuoteConvention[] marketQuotes,
      IsdaCompliantYieldCurve yieldCurve) {

    IsdaCompliantCreditCurve creditCurve = _curveBuilder.calibrateCreditCurve(pillarCDSs, marketQuotes, yieldCurve);
    return parallelCS01FromCreditCurve(cds, cdsCoupon, pillarCDSs, yieldCurve, creditCurve);
  }

  public double parallelCS01FromCreditCurve(
      CdsAnalytic cds,
      double cdsCoupon,
      CdsAnalytic[] bucketCDSs,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve) {

    double[] temp = bucketedCS01FromCreditCurve(cds, cdsCoupon, bucketCDSs, yieldCurve, creditCurve);
    double sum = 0;
    for (double cs : temp) {
      sum += cs;
    }
    return sum;
  }

  //***************************************************************************************************************
  // bucketed CS01 of a CDS from single market quote of that CDS
  //***************************************************************************************************************

  public double[] bucketedCS01FromSpread(
      CdsAnalytic cds,
      double coupon,
      IsdaCompliantYieldCurve yieldCurve,
      double marketSpread,
      CdsAnalytic[] buckets) {

    IsdaCompliantCreditCurve cc = _curveBuilder.calibrateCreditCurve(cds, marketSpread, yieldCurve);
    return bucketedCS01FromCreditCurve(cds, coupon, buckets, yieldCurve, cc);
  }

  public double[] bucketedCS01(
      CdsAnalytic cds,
      double cdsCoupon,
      CdsAnalytic[] pillarCDSs,
      CdsQuoteConvention[] marketQuotes,
      IsdaCompliantYieldCurve yieldCurve) {

    IsdaCompliantCreditCurve creditCurve = _curveBuilder.calibrateCreditCurve(pillarCDSs, marketQuotes, yieldCurve);
    return bucketedCS01FromCreditCurve(cds, cdsCoupon, pillarCDSs, yieldCurve, creditCurve);
  }

  public double[][] bucketedCS01(
      CdsAnalytic[] cds,
      double[] cdsCoupons,
      CdsAnalytic[] pillarCDSs,
      CdsQuoteConvention[] marketQuotes,
      IsdaCompliantYieldCurve yieldCurve) {

    IsdaCompliantCreditCurve creditCurve = _curveBuilder.calibrateCreditCurve(pillarCDSs, marketQuotes, yieldCurve);
    return bucketedCS01FromCreditCurve(cds, cdsCoupons, pillarCDSs, yieldCurve, creditCurve);
  }

  public double[] bucketedCS01FromParSpreads(
      CdsAnalytic cds,
      double cdsCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic[] pillarCDSs,
      double[] spreads) {

    IsdaCompliantCreditCurve creditCurve = _curveBuilder.calibrateCreditCurve(pillarCDSs, spreads, yieldCurve);
    return bucketedCS01FromCreditCurve(cds, cdsCoupon, pillarCDSs, yieldCurve, creditCurve);
  }

  public double[] bucketedCS01FromCreditCurve(
      CdsAnalytic cds,
      double cdsCoupon,
      CdsAnalytic[] bucketCDSs,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.noNulls(bucketCDSs, "bucketCDSs");
    ArgChecker.notNull(creditCurve, "creditCurve");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    LUDecompositionCommons decomp = new LUDecompositionCommons();
    int n = bucketCDSs.length;
    double[] temp = new double[n];
    double[][] res = new double[n][n];
    for (int i = 0; i < n; i++) {
      temp[i] = _pricer.pvCreditSensitivity(cds, yieldCurve, creditCurve, cdsCoupon, i);
      for (int j = 0; j < n; j++) {
        res[j][i] = _pricer.parSpreadCreditSensitivity(bucketCDSs[i], yieldCurve, creditCurve, j);
      }
    }
    DoubleMatrix1D vLambda = new DoubleMatrix1D(temp);
    DoubleMatrix2D jacT = new DoubleMatrix2D(res);
    LUDecompositionResult luRes = decomp.evaluate(jacT);
    DoubleMatrix1D vS = luRes.solve(vLambda);
    return vS.getData();
  }

  public double[][] bucketedCS01FromCreditCurve(
      CdsAnalytic[] cds,
      double[] cdsCoupon,
      CdsAnalytic[] bucketCDSs,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve) {

    ArgChecker.noNulls(cds, "cds");
    ArgChecker.notEmpty(cdsCoupon, "cdsCoupons");
    ArgChecker.noNulls(bucketCDSs, "bucketCDSs");
    ArgChecker.notNull(creditCurve, "creditCurve");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    int m = cds.length;
    ArgChecker.isTrue(m == cdsCoupon.length, m + " CDSs but " + cdsCoupon.length + " coupons");
    LUDecompositionCommons decomp = new LUDecompositionCommons();
    int n = bucketCDSs.length;
    DoubleMatrix2D jacT = new DoubleMatrix2D(n, n);
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        jacT.getData()[j][i] = _pricer.parSpreadCreditSensitivity(bucketCDSs[i], yieldCurve, creditCurve, j);
      }
    }

    double[] vLambda = new double[n];
    double[][] res = new double[m][];
    LUDecompositionResult luRes = decomp.evaluate(jacT);
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        vLambda[j] = _pricer.pvCreditSensitivity(cds[i], yieldCurve, creditCurve, cdsCoupon[i], j);
      }
      res[i] = luRes.solve(vLambda);
    }
    return res;
  }

}
