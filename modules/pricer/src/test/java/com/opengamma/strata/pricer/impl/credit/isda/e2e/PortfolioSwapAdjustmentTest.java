/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda.e2e;

import static com.opengamma.strata.pricer.impl.credit.isda.YieldCurveProvider.ISDA_USD_20140213;
import static com.opengamma.strata.pricer.impl.credit.isda.e2e.CdsIndexProvider.CDX_NA_HY_20140213_PRICES;
import static com.opengamma.strata.pricer.impl.credit.isda.e2e.CdsIndexProvider.CDX_NA_HY_20140213_RECOVERY_RATES;
import static com.opengamma.strata.pricer.impl.credit.isda.e2e.CdsIndexProvider.CDX_NA_HY_21_COUPON;
import static com.opengamma.strata.pricer.impl.credit.isda.e2e.CdsIndexProvider.CDX_NA_HY_21_RECOVERY_RATE;
import static com.opengamma.strata.pricer.impl.credit.isda.e2e.CdsIndexProvider.INDEX_TENORS;
import static com.opengamma.strata.pricer.impl.credit.isda.e2e.CdsIndexProvider.getCDX_NA_HY_20140213_CreditCurves;
import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.BitSet;

import org.testng.annotations.Test;

import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalytic;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalyticFactory;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaBaseTest;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurve;

/**
 * 
 */
public class PortfolioSwapAdjustmentTest extends IsdaBaseTest {

  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 2, 13);
  private static final Period[] INDEX_PILLARS = INDEX_TENORS;
  private static final double INDEX_COUPON = CDX_NA_HY_21_COUPON;
  private static final double INDEX_RR = CDX_NA_HY_21_RECOVERY_RATE;
  private static final IsdaCompliantCreditCurve[] CREDIT_CURVES = getCDX_NA_HY_20140213_CreditCurves();
  private static final double[] RECOVERY_RATES = CDX_NA_HY_20140213_RECOVERY_RATES;
  private static final IntrinsicIndexDataBundle INTRINSIC_DATA = new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES);
  private static IsdaCompliantYieldCurve YIELD_CURVE = ISDA_USD_20140213;
  private static double[] PRICES = CDX_NA_HY_20140213_PRICES;

  private static PortfolioSwapAdjustment PSA = new PortfolioSwapAdjustment();
  private static CdsIndexCalculator INDEX_CAL = new CdsIndexCalculator();
  private static CdsAnalyticFactory FACTORY = new CdsAnalyticFactory(INDEX_RR);

  @Test
  public void singleTermAdjustmentTest() {
    final CdsAnalytic[] cdx = FACTORY.makeCdx(TRADE_DATE, INDEX_PILLARS);
    final int n = cdx.length;

    for (int i = 0; i < n; i++) {
      final double puf = 1 - PRICES[i];
      final IntrinsicIndexDataBundle adjCurves = PSA.adjustCurves(puf, cdx[i], INDEX_COUPON, YIELD_CURVE, INTRINSIC_DATA);
      final double puf2 = INDEX_CAL.indexPUF(cdx[i], INDEX_COUPON, YIELD_CURVE, adjCurves);
      assertEquals(puf, puf2, 1e-14);
    }
  }

  @Test
  public void singleTermAdjustmentWithDefaultsTest() {
    final CdsAnalytic[] cdx = FACTORY.makeCdx(TRADE_DATE, INDEX_PILLARS);
    final int n = cdx.length;

    final IntrinsicIndexDataBundle data = INTRINSIC_DATA.withDefault(7);

    for (int i = 0; i < n; i++) {
      final double puf = 1 - PRICES[i];
      final IntrinsicIndexDataBundle adjCurves = PSA.adjustCurves(puf, cdx[i], INDEX_COUPON, YIELD_CURVE, data);
      final double puf2 = INDEX_CAL.indexPUF(cdx[i], INDEX_COUPON, YIELD_CURVE, adjCurves);
      assertEquals(puf, puf2, 1e-14);
    }

    /**
     * Exception expected
     */
    final double largePuf = 1.2;
    try {
      PSA.adjustCurves(largePuf, cdx[1], INDEX_COUPON, YIELD_CURVE, data);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("indexPUF must be given as a fraction. Value of " + largePuf + " is too high.", e.getMessage());
    }
    final double negativeCoupon = -100. * 1.e-4;
    try {
      PSA.adjustCurves(0.6, cdx[1], negativeCoupon, YIELD_CURVE, data);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("indexCoupon cannot be negative", e.getMessage());
    }
    final double largeCoupon = 250.;
    try {
      PSA.adjustCurves(0.6, cdx[1], largeCoupon, YIELD_CURVE, data);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("indexCoupon should be a fraction. The value of " + largeCoupon + " would be a coupon of " + largeCoupon * 1.e4, e.getMessage());
    }

    /**
     * null curve for a defaulted names
     */
    final int[] defaulted = new int[] {1, 33, 45, 89 };
    BitSet bitset = new BitSet(INTRINSIC_DATA.getIndexSize());
    final IsdaCompliantCreditCurve[] ccs = INTRINSIC_DATA.getCreditCurves().clone();
    final double[] rrs = new double[INTRINSIC_DATA.getIndexSize()];
    for (int i = 0; i < defaulted.length; ++i) {
      bitset.set(defaulted[i]);
      ccs[defaulted[i]] = null;
    }
    for (int i = 0; i < INTRINSIC_DATA.getIndexSize(); ++i) {
      rrs[i] = 1. - INTRINSIC_DATA.getLGD(i);
    }

    final IntrinsicIndexDataBundle dataWithDefault = new IntrinsicIndexDataBundle(ccs, rrs, bitset);
    for (int i = 0; i < n; i++) {
      final double puf = 1 - PRICES[i];
      final IntrinsicIndexDataBundle adjCurves = PSA.adjustCurves(puf, cdx[i], INDEX_COUPON, YIELD_CURVE, dataWithDefault);
      final double puf2 = INDEX_CAL.indexPUF(cdx[i], INDEX_COUPON, YIELD_CURVE, adjCurves);
      assertEquals(puf, puf2, 1e-14);
    }
  }

  @Test
  public void multiTermAdjustmentTest() {
    final CdsAnalytic[] cdx = FACTORY.makeCdx(TRADE_DATE, INDEX_PILLARS);
    final int n = cdx.length;

    final double[] puf = new double[n];
    for (int i = 0; i < n; i++) {
      puf[i] = 1 - PRICES[i];
    }
    final IntrinsicIndexDataBundle adjCurves = PSA.adjustCurves(puf, cdx, INDEX_COUPON, YIELD_CURVE, INTRINSIC_DATA);
    for (int i = 0; i < n; i++) {
      final double puf2 = INDEX_CAL.indexPUF(cdx[i], INDEX_COUPON, YIELD_CURVE, adjCurves);
      assertEquals(puf[i], puf2, 1e-14);
    }

    /**
     * Reduces into the flat curve case
     */
    for (int i = 0; i < n; ++i) {
      final IntrinsicIndexDataBundle sglCv = PSA.adjustCurves(puf[i], cdx[i], INDEX_COUPON, YIELD_CURVE, INTRINSIC_DATA);
      final IntrinsicIndexDataBundle mltCv = PSA.adjustCurves(
          new double[] {puf[i]},
          new CdsAnalytic[] {cdx[i]}, INDEX_COUPON, YIELD_CURVE, INTRINSIC_DATA);
      sglCv.equals(mltCv);
    }

    /**
     * Exception expected
     */
    final double[] shortPufs = Arrays.copyOf(puf, n - 2);
    try {
      PSA.adjustCurves(shortPufs, cdx, INDEX_COUPON, YIELD_CURVE, INTRINSIC_DATA);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("number of indexCDS (" + cdx.length + ") does not match number of indexPUF (" + shortPufs.length + ")", e.getMessage());
    }
    final double negativeCoupon = -100. * 1.e-4;
    try {
      PSA.adjustCurves(puf, cdx, negativeCoupon, YIELD_CURVE, INTRINSIC_DATA);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("indexCoupon cannot be negative", e.getMessage());
    }
    final double largeCoupon = 250.;
    try {
      PSA.adjustCurves(puf, cdx, largeCoupon, YIELD_CURVE, INTRINSIC_DATA);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("indexCoupon should be a fraction. The value of " + largeCoupon + " would be a coupon of " + largeCoupon * 1.e4, e.getMessage());
    }
    final double[] largePufs = Arrays.copyOf(puf, n);
    final int position = 2;
    largePufs[position] += 10.;
    try {
      PSA.adjustCurves(largePufs, cdx, INDEX_COUPON, YIELD_CURVE, INTRINSIC_DATA);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("indexPUF must be given as a fraction. Value of " + largePufs[position] + " is too high.", e.getMessage());
    }
    final CdsAnalytic[] wrongCdx = Arrays.copyOf(cdx, n);
    wrongCdx[1] = cdx[2];
    wrongCdx[2] = cdx[1];
    try {
      PSA.adjustCurves(puf, wrongCdx, INDEX_COUPON, YIELD_CURVE, INTRINSIC_DATA);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("indexCDS must be in assending order of maturity", e.getMessage());
    }

  }

  @Test
  public void multiTermAdjustmentWithDefaultTest() {
    final CdsAnalytic[] cdx = FACTORY.makeCdx(TRADE_DATE, INDEX_PILLARS);
    final int n = cdx.length;
    final IntrinsicIndexDataBundle data = INTRINSIC_DATA.withDefault(0, 4, 78);
    final double[] puf = new double[n];
    for (int i = 0; i < n; i++) {
      puf[i] = 1 - PRICES[i];
    }
    final IntrinsicIndexDataBundle adjCurves = PSA.adjustCurves(puf, cdx, INDEX_COUPON, YIELD_CURVE, data);
    for (int i = 0; i < n; i++) {
      final double puf2 = INDEX_CAL.indexPUF(cdx[i], INDEX_COUPON, YIELD_CURVE, adjCurves);
      assertEquals(puf[i], puf2, 1e-14);
    }

    /**
     * null curve for a defaulted names
     */
    final int[] defaulted = new int[] {1, 33, 45, 89 };
    BitSet bitset = new BitSet(INTRINSIC_DATA.getIndexSize());
    final IsdaCompliantCreditCurve[] ccs = INTRINSIC_DATA.getCreditCurves().clone();
    final double[] rrs = new double[INTRINSIC_DATA.getIndexSize()];
    for (int i = 0; i < defaulted.length; ++i) {
      bitset.set(defaulted[i]);
      ccs[defaulted[i]] = null;
    }
    for (int i = 0; i < INTRINSIC_DATA.getIndexSize(); ++i) {
      rrs[i] = 1. - INTRINSIC_DATA.getLGD(i);
    }

    final IntrinsicIndexDataBundle dataWithDefault = new IntrinsicIndexDataBundle(ccs, rrs, bitset);
    final IntrinsicIndexDataBundle dataWithDefaultAdj = PSA.adjustCurves(puf, cdx, INDEX_COUPON, YIELD_CURVE, dataWithDefault);
    for (int i = 0; i < n; i++) {
      final double puf2 = INDEX_CAL.indexPUF(cdx[i], INDEX_COUPON, YIELD_CURVE, dataWithDefaultAdj);
      assertEquals(puf[i], puf2, 1e-14);
    }
  }

  /**
   * breakpoints of a credit curve are not modified 
   */
  @Test
  public void knotsCoincideTest() {
    final CdsAnalytic[] cdx = FACTORY.makeCdx(TRADE_DATE, INDEX_PILLARS);
    final int nPillars = cdx.length;
    final int indexSize = INTRINSIC_DATA.getIndexSize();

    final double[] indexKnots = new double[nPillars];
    final double[] rt = new double[nPillars];
    final double[] puf = new double[nPillars];
    for (int i = 0; i < nPillars; ++i) {
      indexKnots[i] = cdx[i].getProtectionEnd();
      rt[i] = indexKnots[i] * 0.05;
      puf[i] = 1 - PRICES[i];
    }

    final int position = 32;
    final IsdaCompliantCreditCurve[] ccs = INTRINSIC_DATA.getCreditCurves().clone();
    ccs[position] = IsdaCompliantCreditCurve.makeFromRT(indexKnots, rt);

    final double[] rrs = new double[indexSize];
    for (int i = 0; i < indexSize; ++i) {
      rrs[i] = 1. - INTRINSIC_DATA.getLGD(i);
    }

    final IntrinsicIndexDataBundle data = new IntrinsicIndexDataBundle(ccs, rrs);
    final IntrinsicIndexDataBundle dataAdj = PSA.adjustCurves(puf, cdx, INDEX_COUPON, YIELD_CURVE, data);

    for (int i = 0; i < nPillars; ++i) {
      final double puf2 = INDEX_CAL.indexPUF(cdx[i], INDEX_COUPON, YIELD_CURVE, dataAdj);
      assertEquals(puf[i], puf2, 1e-14);
    }
  }
}
