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
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Calculates the hedge ratio.
 */
public class HedgeRatioCalculator {

  MatrixAlgebra MA = new OGMatrixAlgebra();

  private final AnalyticCdsPricer _pricer;
  private final IsdaCompliantCreditCurveBuilder _builder;

  /**
   * Default constructor.
   */
  public HedgeRatioCalculator() {
    _pricer = new AnalyticCdsPricer();
    _builder = new FastCreditCurveBuilder();
  }

  /**
   * Constructor specifying formula used in pricer and credit curve builder.
   * 
   * @param formula The formula
   */
  public HedgeRatioCalculator(AccrualOnDefaultFormulae formula) {
    ArgChecker.notNull(formula, "formula");
    _pricer = new AnalyticCdsPricer(formula);
    _builder = new FastCreditCurveBuilder(formula);
  }

  //-------------------------------------------------------------------------
  /**
   * The sensitivity of the PV of a CDS to the zero hazard rates at the knots of the credit curve.
   *
   * @param cds  the CDS 
   * @param coupon  the coupon
   * @param creditCurve  the credit Curve
   * @param yieldCurve  the yield curve
   * @return vector of sensitivities 
   */
  public DoubleMatrix1D getCurveSensitivities(
      CdsAnalytic cds,
      double coupon,
      IsdaCompliantCreditCurve creditCurve,
      IsdaCompliantYieldCurve yieldCurve) {

    // TODO this should be handled directly by the pricer  
    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(creditCurve, "creditCurve");
    ArgChecker.notNull(yieldCurve, "yieldCurve");

    int nKnots = creditCurve.getNumberOfKnots();
    double[] sense = new double[nKnots];
    for (int i = 0; i < nKnots; i++) {
      sense[i] = _pricer.pvCreditSensitivity(cds, yieldCurve, creditCurve, coupon, i);
    }
    return new DoubleMatrix1D(sense);
  }

  /**
   * The sensitivity of a set of CDSs to the zero hazard rates at the knots of the credit curve.
   * The element (i,j) is the sensitivity of the PV of the jth CDS to the ith knot.
   * 
   * @param cds  the set of CDSs
   * @param coupons  the coupons of the CDSs
   * @param creditCurve  the credit Curve
   * @param yieldCurve  the yield curve
   * @return matrix of sensitivities
   */
  public DoubleMatrix2D getCurveSensitivities(
      CdsAnalytic[] cds,
      double[] coupons,
      IsdaCompliantCreditCurve creditCurve,
      IsdaCompliantYieldCurve yieldCurve) {

    ArgChecker.noNulls(cds, "cds");
    ArgChecker.notEmpty(coupons, "coupons");
    ArgChecker.notNull(creditCurve, "creditCurve");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    int nCDS = cds.length;
    ArgChecker.isTrue(nCDS == coupons.length, "number of coupons not equal number of CDS");
    int nKnots = creditCurve.getNumberOfKnots();
    double[][] sense = new double[nKnots][nCDS];

    for (int i = 0; i < nCDS; i++) {
      for (int j = 0; j < nKnots; j++) {
        sense[j][i] = _pricer.pvCreditSensitivity(cds[i], yieldCurve, creditCurve, coupons[i], j);
      }
    }
    return new DoubleMatrix2D(sense);
  }

  //-------------------------------------------------------------------------
  /**
   * Hedge a CDS with other CDSs on the same underlying (single-name or index) at different maturities.
   * <p>
   * The hedge is such that the total portfolio (the CDS <b>minus</b> the hedging CDSs, with notionals of the
   * CDS notional times the computed hedge ratios) is insensitive to infinitesimal changes to the the credit curve.
   * <p>
   * Here the credit curve is built using the hedging CDSs as pillars. 
   * 
   * @param cds  the CDS to be hedged
   * @param coupon  the coupon of the CDS to be hedged
   * @param hedgeCDSs  the CDSs to hedge with - these are also used to build the credit curve
   * @param hedgeCDSCoupons  the coupons of the CDSs to hedge with/build credit curve
   * @param hegdeCDSPUF  the PUF of the CDSs to build credit curve
   * @param yieldCurve  the yield curve
   * @return the hedge ratios,
   *  since we use a unit notional, the ratios should be multiplied by -notional to give the hedge notional amounts
   */
  public DoubleMatrix1D getHedgeRatios(CdsAnalytic cds, double coupon, CdsAnalytic[] hedgeCDSs, double[] hedgeCDSCoupons, double[] hegdeCDSPUF,
      IsdaCompliantYieldCurve yieldCurve) {
    IsdaCompliantCreditCurve cc = _builder.calibrateCreditCurve(hedgeCDSs, hedgeCDSCoupons, yieldCurve, hegdeCDSPUF);
    return getHedgeRatios(cds, coupon, hedgeCDSs, hedgeCDSCoupons, cc, yieldCurve);
  }

  /**
   * Hedge a CDS with other CDSs on the same underlying (single-name or index) at different maturities.
   * <p>
   * The hedge is such that the total portfolio (the CDS <b>minus</b> the hedging CDSs, with notionals of the
   * CDS notional times the computed hedge ratios) is insensitive to infinitesimal changes to the the credit curve.
   * <p>
   * If the number of hedge-CDSs equals the number of credit-curve knots, the system is square
   * and is solved exactly (see below).<br>
   * If the number of hedge-CDSs is less than the number of credit-curve knots, the system is
   * solved in a least-square sense (i.e. is hedge is not exact).<br>
   * If the number of hedge-CDSs is greater than the number of credit-curve knots, the system
   * cannot be solved. <br>
   * The system may not solve if the maturities if the hedging CDSs and very different from the
   * knot times (i.e. the sensitivity matrix is singular). 
   * 
   * @param cds  the CDS to be hedged
   * @param coupon  the coupon of the CDS to be hedged
   * @param hedgeCDSs  the CDSs to hedge with - these are also used to build the credit curve
   * @param hedgeCDSCoupons  the coupons of the CDSs to hedge with/build credit curve
   * @param creditCurve The credit curve  
   * @param yieldCurve the yield curve 
   * @return the hedge ratios,
   *  since we use a unit notional, the ratios should be multiplied by -notional to give the hedge notional amounts
   */
  public DoubleMatrix1D getHedgeRatios(CdsAnalytic cds, double coupon, CdsAnalytic[] hedgeCDSs, double[] hedgeCDSCoupons, IsdaCompliantCreditCurve creditCurve,
      IsdaCompliantYieldCurve yieldCurve) {
    DoubleMatrix1D cdsSense = getCurveSensitivities(cds, coupon, creditCurve, yieldCurve);
    DoubleMatrix2D hedgeSense = getCurveSensitivities(hedgeCDSs, hedgeCDSCoupons, creditCurve, yieldCurve);
    return getHedgeRatios(cdsSense, hedgeSense);
  }

  /**
   * Hedge a CDS with other CDSs on the same underlying (single-name or index) at different maturities.
   * <p>
   * The hedge is such that the total portfolio (the CDS <b>minus</b> the hedging CDSs, with notionals of the
   * CDS notional times the computed hedge ratios) is insensitive to infinitesimal changes to the the credit curve. 
   * <p>
   * If the number of hedge-CDSs equals the number of credit-curve knots, the system is
   * square and is solved exactly (see below).<br>
   * If the number of hedge-CDSs is less than the number of credit-curve knots, the system is
   * solved in a least-square sense (i.e. is hedge is not exact).<br>
   * If the number of hedge-CDSs is greater than the number of credit-curve knots, the system
   * cannot be solved. <br>
   * The system may not solve if the maturities if the hedging CDSs and very different from the
   * knot times (i.e. the sensitivity matrix is singular).
   * 
   * @param cdsSensitivities  the vector of sensitivities of the CDS to the zero hazard rates at the credit curve knots
   * @param hedgeCDSSensitivities  the matrix of sensitivities of the hedging-CDSs to the zero hazard rates
   *  at the credit curve knots. The (i,j) element is the sensitivity of the jth CDS to the ith knot. 
   * @return the hedge ratios,
   *  since we use a unit notional, the ratios should be multiplied by -notional to give the hedge notional amounts
   */
  public DoubleMatrix1D getHedgeRatios(DoubleMatrix1D cdsSensitivities, DoubleMatrix2D hedgeCDSSensitivities) {
    ArgChecker.notNull(hedgeCDSSensitivities, "hedgeCDSSensitivities");
    int nRows = hedgeCDSSensitivities.getNumberOfRows();
    int nCols = hedgeCDSSensitivities.getNumberOfColumns();
    ArgChecker.isTrue(nRows == cdsSensitivities.getNumberOfElements(), "Number of matrix rows does not match vector length");
    if (nCols == nRows) {
      LUDecompositionCommons decomp = new LUDecompositionCommons();
      LUDecompositionResult luRes = decomp.evaluate(hedgeCDSSensitivities);
      return getHedgeRatios(cdsSensitivities, luRes);
    } else {
      if (nRows < nCols) {
        //Under-specified. No unique solution exists. There are  curve knots but hedging instruments  
        throw new IllegalArgumentException("Under-specified. No unique solution exists. There are " + nRows +
            " curve knots but " + nCols + " hedging instruments.");
      } else {
        //over-specified. Solve in a least-square sense 
        DoubleMatrix2D senseT = MA.getTranspose(hedgeCDSSensitivities);
        DoubleMatrix2D a = (DoubleMatrix2D) MA.multiply(senseT, hedgeCDSSensitivities);
        DoubleMatrix1D b = (DoubleMatrix1D) MA.multiply(senseT, cdsSensitivities);
        LUDecompositionCommons decomp = new LUDecompositionCommons();
        LUDecompositionResult luRes = decomp.evaluate(a);
        return getHedgeRatios(b, luRes);
      }
    }
  }

  public DoubleMatrix1D getHedgeRatios(DoubleMatrix1D cdsSensitivities, LUDecompositionResult luRes) {
    ArgChecker.notNull(cdsSensitivities, "cdsSensitivities");
    ArgChecker.notNull(luRes, " luRes");
    DoubleMatrix1D w = luRes.solve(cdsSensitivities);
    return w;
  }

}
