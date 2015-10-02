/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda.e2e;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.rootfinding.NewtonRaphsonSingleRootFinder;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalytic;
import com.opengamma.strata.pricer.impl.credit.isda.DoublesScheduleGenerator;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurve;

/**
 * 
 */
public class PortfolioSwapAdjustment {
  // this code has been moved from src/main/java to src/test/java

  private static final NewtonRaphsonSingleRootFinder ROOTFINDER = new NewtonRaphsonSingleRootFinder();

  private final CdsIndexCalculator _pricer;

  /**
   * Default constructor
   */
  public PortfolioSwapAdjustment() {
    _pricer = new CdsIndexCalculator();
  }

  /**
   * Adjust the hazard rates of the credit curves of the individual single names in a index
   * so that the index is priced exactly.  The hazard rates are adjusted on
   * a percentage rather than a absolute bases (e.g. all hazard rates are increased by 1%).
   * 
   * @param indexPUF The clean price of the index for unit current notional
   *  (i.e. divide the actual clean price by the current notional) 
   * @param indexCDS analytic description of a CDS traded at a certain time
   * @param indexCoupon The coupon of the index (as a fraction)
   * @param yieldCurve The yield curve
   * @param intrinsicData The credit curves of the individual single names making up the index 
   * @return credit curve adjusted so they will exactly reprice the index.
   */
  public IntrinsicIndexDataBundle adjustCurves(
      double indexPUF,
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.isTrue(indexPUF <= 1.0, "indexPUF must be given as a fraction. Value of {} is too high.", indexPUF);
    ArgChecker.notNull(indexCDS, "indexCDS");
    ArgChecker.isTrue(indexCoupon >= 0, "indexCoupon cannot be negative");
    ArgChecker.isTrue(indexCoupon < 10,
        "indexCoupon should be a fraction. The value of {} would be a coupon of {}", indexCoupon, indexCoupon * 1e4);
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(intrinsicData, "intrinsicData");

    Function1D<Double, Double> func = getHazardRateAdjFunction(indexPUF, indexCDS, indexCoupon, yieldCurve, intrinsicData);
    double x = ROOTFINDER.getRoot(func, 1.0);
    IsdaCompliantCreditCurve[] adjCC = adjustCurves(intrinsicData.getCreditCurves(), x);
    return intrinsicData.withCreditCurves(adjCC);
  }

  /**
   * Adjust the hazard rates of the credit curves of the individual single names in a index so that
   * the index is priced exactly at multiple terms. The hazard rates are multiplied by a piecewise
   * constant adjuster (e.g. all hazard rates between two index terms are increased by the same percentage). 
   * When required extra knots are added to the credit curves, so the adjusted curves returned may contain
   * more knots than the original curves.
   * 
   * @param indexPUF The clean prices of the index for unit current notional.
   * @param indexCDS analytic descriptions of the index for different terms 
   * @param indexCoupon The coupon of the index (as a fraction)
   * @param yieldCurve The yield curve
   * @param intrinsicData The credit curves of the individual single names making up the index 
   * @return credit curve adjusted so they will exactly reprice the index at the different terms.
   */
  public IntrinsicIndexDataBundle adjustCurves(
      double[] indexPUF,
      CdsAnalytic[] indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notEmpty(indexPUF, "indexPUF");
    ArgChecker.noNulls(indexCDS, "indexCDS");
    int nIndexTerms = indexCDS.length;
    ArgChecker.isTrue(nIndexTerms == indexPUF.length,
        "number of indexCDS ({}) does not match number of indexPUF ({})", nIndexTerms, indexPUF.length);
    if (nIndexTerms == 1) {
      return adjustCurves(indexPUF[0], indexCDS[0], indexCoupon, yieldCurve, intrinsicData);
    }
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(intrinsicData, "intrinsicData");
    ArgChecker.isTrue(indexCoupon >= 0, "indexCoupon cannot be negative");
    ArgChecker.isTrue(indexCoupon < 10,
        "indexCoupon should be a fraction. The value of {} would be a coupon of {}", indexCoupon, indexCoupon * 1e4);

    double[] indexKnots = new double[nIndexTerms];
    for (int i = 0; i < nIndexTerms; i++) {
      ArgChecker.isTrue(indexPUF[i] <= 1.0, "indexPUF must be given as a fraction. Value of {} is too high.", indexPUF[i]);
      indexKnots[i] = indexCDS[i].getProtectionEnd();
      if (i > 0) {
        ArgChecker.isTrue(indexKnots[i] > indexKnots[i - 1], "indexCDS must be in assending order of maturity");
      }
    }

    IsdaCompliantCreditCurve[] creditCurves = intrinsicData.getCreditCurves();
    int nCurves = creditCurves.length;
    //we cannot assume that all the credit curves have knots at the same times or that the terms of the indices fall on these knots.
    IsdaCompliantCreditCurve[] modCreditCurves = new IsdaCompliantCreditCurve[nCurves];
    int[][] indexMap = new int[nCurves][nIndexTerms];
    for (int i = 0; i < nCurves; i++) {
      if (creditCurves[i] == null) {
        modCreditCurves[i] = null; //null credit curves correspond to defaulted names, so are ignored 
      } else {
        double[] ccKnots = creditCurves[i].getKnotTimes();
        double[] comKnots = DoublesScheduleGenerator.combineSets(ccKnots, indexKnots);
        int nKnots = comKnots.length;
        if (nKnots == ccKnots.length) {
          modCreditCurves[i] = creditCurves[i];
        } else {
          double[] rt = new double[nKnots];
          for (int j = 0; j < nKnots; j++) {
            rt[j] = creditCurves[i].getRT(comKnots[j]);
          }
          modCreditCurves[i] = IsdaCompliantCreditCurve.makeFromRT(comKnots, rt);
        }

        for (int j = 0; j < nIndexTerms; j++) {
          int index = Arrays.binarySearch(modCreditCurves[i].getKnotTimes(), indexKnots[j]);
          if (index < 0) {
            throw new MathException("This should not happen. There is a bug in the logic");
          }
          indexMap[i][j] = index;
        }
      }
    }

    int[] startKnots = new int[nCurves];
    int[] endKnots = new int[nCurves];
    double alpha = 1.0;
    for (int i = 0; i < nIndexTerms; i++) {
      if (i == (nIndexTerms - 1)) {
        for (int jj = 0; jj < nCurves; jj++) {
          if (modCreditCurves[jj] != null) {
            endKnots[jj] = modCreditCurves[jj].getNumberOfKnots();
          }
        }
      } else {
        for (int jj = 0; jj < nCurves; jj++) {
          if (modCreditCurves[jj] != null) {
            endKnots[jj] = indexMap[jj][i] + 1;
          }
        }
      }

      IntrinsicIndexDataBundle modIntrinsicData = intrinsicData.withCreditCurves(modCreditCurves);
      Function1D<Double, Double> func = getHazardRateAdjFunction(indexPUF[i], indexCDS[i], indexCoupon, yieldCurve,
          modIntrinsicData, startKnots, endKnots);
      alpha = ROOTFINDER.getRoot(func, alpha);
      modCreditCurves = adjustCurves(modCreditCurves, alpha, startKnots, endKnots);
      startKnots = endKnots.clone();
    }

    return intrinsicData.withCreditCurves(modCreditCurves);
  }

  private Function1D<Double, Double> getHazardRateAdjFunction(
      double indexPUF,
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    IsdaCompliantCreditCurve[] creditCurves = intrinsicData.getCreditCurves();
    double clean = intrinsicData.getIndexFactor() * indexPUF;
    return new Function1D<Double, Double>() {
      @Override
      public Double evaluate(Double x) {
        IsdaCompliantCreditCurve[] adjCurves = adjustCurves(creditCurves, x);
        return _pricer.indexPV(indexCDS, indexCoupon, yieldCurve, intrinsicData.withCreditCurves(adjCurves)) - clean;
      }
    };
  }

  private Function1D<Double, Double> getHazardRateAdjFunction(
      double indexPUF,
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData,
      int[] firstKnots,
      int[] lastKnots) {

    IsdaCompliantCreditCurve[] creditCurves = intrinsicData.getCreditCurves();
    double clean = intrinsicData.getIndexFactor() * indexPUF;
    return new Function1D<Double, Double>() {
      @Override
      public Double evaluate(Double x) {
        IsdaCompliantCreditCurve[] adjCurves = adjustCurves(creditCurves, x, firstKnots, lastKnots);
        return _pricer.indexPV(indexCDS, indexCoupon, yieldCurve, intrinsicData.withCreditCurves(adjCurves)) - clean;
      }
    };
  }

  private IsdaCompliantCreditCurve[] adjustCurves(IsdaCompliantCreditCurve[] creditCurve, double amount) {
    int nCurves = creditCurve.length;
    IsdaCompliantCreditCurve[] adjCurves = new IsdaCompliantCreditCurve[nCurves];
    for (int jj = 0; jj < nCurves; jj++) {
      adjCurves[jj] = adjustCreditCurve(creditCurve[jj], amount);
    }
    return adjCurves;
  }

  private IsdaCompliantCreditCurve adjustCreditCurve(IsdaCompliantCreditCurve creditCurve, double amount) {
    if (creditCurve == null) {
      return creditCurve;
    }
    int nKnots = creditCurve.getNumberOfKnots();
    double[] rt = creditCurve.getRt();
    double[] rtAdj = new double[nKnots];
    for (int i = 0; i < nKnots; i++) {
      rtAdj[i] = rt[i] * amount;
    }
    return IsdaCompliantCreditCurve.makeFromRT(creditCurve.getKnotTimes(), rtAdj);
  }

  private IsdaCompliantCreditCurve[] adjustCurves(
      IsdaCompliantCreditCurve[] creditCurve,
      double amount,
      int[] firstKnots,
      int[] lastknots) {

    int nCurves = creditCurve.length;
    IsdaCompliantCreditCurve[] adjCurves = new IsdaCompliantCreditCurve[nCurves];
    for (int jj = 0; jj < nCurves; jj++) {
      if (creditCurve[jj] == null) {
        adjCurves[jj] = null;
      } else {
        adjCurves[jj] = adjustCreditCurve(creditCurve[jj], amount, firstKnots[jj], lastknots[jj]);
      }
    }
    return adjCurves;
  }

  private IsdaCompliantCreditCurve adjustCreditCurve(
      IsdaCompliantCreditCurve creditCurve,
      double amount,
      int firstKnot,
      int lastKnot) {

    double[] rt = creditCurve.getRt();
    double[] rtAdj = rt.clone();
    for (int i = firstKnot; i < lastKnot; i++) {
      rtAdj[i] = rt[i] * amount;
    }
    return IsdaCompliantCreditCurve.makeFromRT(creditCurve.getKnotTimes(), rtAdj);
  }

}
