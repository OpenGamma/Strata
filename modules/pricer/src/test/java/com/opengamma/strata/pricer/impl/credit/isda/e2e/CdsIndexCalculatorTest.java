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

import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.pricer.impl.credit.isda.AnalyticCdsPricer;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalytic;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalyticFactory;
import com.opengamma.strata.pricer.impl.credit.isda.CdsPriceType;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaBaseTest;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurve;
import com.opengamma.strata.pricer.impl.credit.isda.PointsUpFront;

/**
 * 
 */
@Test
public class CdsIndexCalculatorTest extends IsdaBaseTest {

  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 2, 13);
  private static final Period[] INDEX_PILLARS = INDEX_TENORS;
  private static final double INDEX_RR = CDX_NA_HY_21_RECOVERY_RATE;
  private static final IsdaCompliantCreditCurve[] CREDIT_CURVES = getCDX_NA_HY_20140213_CreditCurves();
  private static final double[] RECOVERY_RATES = CDX_NA_HY_20140213_RECOVERY_RATES;
  private static final IntrinsicIndexDataBundle INTRINSIC_DATA = new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES);
  private static IsdaCompliantYieldCurve YIELD_CURVE = ISDA_USD_20140213;

  private static CdsIndexCalculator INDEX_CAL = new CdsIndexCalculator();
  private static CdsAnalyticFactory FACTORY = new CdsAnalyticFactory(INDEX_RR);

  private static final LocalDate EXPIRY = LocalDate.of(2014, 3, 19);
  private static final LocalDate MATURITY = LocalDate.of(2018, 12, 20);
  private static final PortfolioSwapAdjustment PSA = new PortfolioSwapAdjustment();
  private static final PointsUpFront[] PILLAR_PUF;
  private static double[] PRICES = CDX_NA_HY_20140213_PRICES;
  private static final double INDEX_COUPON = CDX_NA_HY_21_COUPON;
  private static final CdsAnalytic FWD_START_CDX = FACTORY.makeForwardStartingCds(TRADE_DATE, EXPIRY, MATURITY);
  private static final CdsAnalytic[] PILLAR_CDX = FACTORY.makeCdx(TRADE_DATE, INDEX_TENORS);

  static {
    final int n = PRICES.length;
    PILLAR_PUF = new PointsUpFront[n];
    for (int i = 0; i < n; i++) {
      PILLAR_PUF[i] = new PointsUpFront(INDEX_COUPON, 1 - PRICES[i]);
    }
  }

  /**
   * Test IR01, JTD, recovery01
   */
  @Test
  public void sensitivitiesTest() {
    AnalyticCdsPricer pricer = new AnalyticCdsPricer();
    double tol = 1.0e-12;
    CdsAnalytic cdx = FACTORY.makeCdx(TRADE_DATE, Period.ofYears(5));
    double indexCoupon = 300 * ONE_BP;
    int[] defaultedNames = new int[] {2, 15, 37, 51 };
    IntrinsicIndexDataBundle intrinsicDataWithDefaulted = INTRINSIC_DATA.withDefault(defaultedNames);

    double pv = INDEX_CAL.indexPV(cdx, indexCoupon, YIELD_CURVE, intrinsicDataWithDefaulted);
    double parallelIR01 = INDEX_CAL.parallelIR01(cdx, indexCoupon, YIELD_CURVE, intrinsicDataWithDefaulted);
    double[] bucketedIR01 = INDEX_CAL.bucketedIR01(cdx, indexCoupon, YIELD_CURVE, intrinsicDataWithDefaulted);
    double[] jumpToDefault = INDEX_CAL.jumpToDefault(cdx, indexCoupon, YIELD_CURVE, intrinsicDataWithDefaulted);
    double[] recovery01 = INDEX_CAL.recovery01(cdx, indexCoupon, YIELD_CURVE, intrinsicDataWithDefaulted);

    double[] rates = YIELD_CURVE.getKnotZeroRates();
    int nRates = rates.length;
    double[] bumpedRatesParallel = new double[nRates];
    double[][] bumpedRatesBucket = new double[nRates][nRates];
    for (int i = 0; i < nRates; ++i) {
      bumpedRatesBucket[i] = Arrays.copyOf(rates, nRates);
      bumpedRatesBucket[i][i] += ONE_BP;
      bumpedRatesParallel[i] = rates[i] + ONE_BP;
    }

    IsdaCompliantYieldCurve ycParallelBump = YIELD_CURVE.withRates(bumpedRatesParallel);
    double pvParallelBump = INDEX_CAL.indexPV(cdx, indexCoupon, ycParallelBump, intrinsicDataWithDefaulted);
    assertEquals(pvParallelBump - pv, parallelIR01, tol);
    for (int i = 0; i < nRates; ++i) {
      IsdaCompliantYieldCurve ycBump = YIELD_CURVE.withRates(bumpedRatesBucket[i]);
      double pvBump = INDEX_CAL.indexPV(cdx, indexCoupon, ycBump, intrinsicDataWithDefaulted);
      assertEquals(pvBump - pv, bucketedIR01[i], tol);
    }

    int size = intrinsicDataWithDefaulted.getIndexSize();
    for (int i = 0; i < size; ++i) {
      double ref;
      if (intrinsicDataWithDefaulted.isDefaulted(i)) {
        ref = 0.0;
      } else {
        double weight = intrinsicDataWithDefaulted.getWeight(i);
        ref = intrinsicDataWithDefaulted.getLGD(i) * weight -
            pricer.pv(cdx, YIELD_CURVE, intrinsicDataWithDefaulted.getCreditCurves()[i], indexCoupon) * weight;
      }
      assertEquals(ref, jumpToDefault[i], tol);
    }

    double[] zeroRecoveryRates = new double[size];
    Arrays.fill(zeroRecoveryRates, 0.0);
    double[] weights = new double[size];
    double sum = 0.0;
    for (int i = 0; i < size; ++i) {
      weights[i] = intrinsicDataWithDefaulted.getWeight(i);
      sum += recovery01[i];
    }
    IntrinsicIndexDataBundle bundleZeroRecoveryRates = new IntrinsicIndexDataBundle(
        intrinsicDataWithDefaulted.getCreditCurves(),
        zeroRecoveryRates, weights).withDefault(defaultedNames);
    double ref = -INDEX_CAL.indexProtLeg(cdx, YIELD_CURVE, bundleZeroRecoveryRates);
    assertEquals(sum, ref, Math.abs(ref) * tol);
  }

  /**
   * 
   */
  @Test
  public void pvAndPufTest() {
    final double tol = 1.0e-12;
    final CdsAnalytic cdx = FACTORY.makeCdx(TRADE_DATE, Period.ofYears(3).plusMonths(5));
    final double indexCoupon = 300 * 1.0e-4;
    final CdsAnalytic[] pillarCdx = FACTORY.makeCdx(TRADE_DATE, INDEX_PILLARS);

    /*
     * No defaulted names
     */
    final double res = INDEX_CAL.indexPUF(cdx, indexCoupon, YIELD_CURVE, INTRINSIC_DATA);
    final double resDirty = INDEX_CAL.indexPV(cdx, indexCoupon, YIELD_CURVE, INTRINSIC_DATA, CdsPriceType.DIRTY);
    final double sp = INDEX_CAL.intrinsicIndexSpread(cdx, YIELD_CURVE, INTRINSIC_DATA);
    final double resSpread = INDEX_CAL.indexPUF(cdx, sp, YIELD_CURVE, INTRINSIC_DATA);
    assertEquals(0.0, resSpread, tol);
    final double resAvgSpread = INDEX_CAL.averageSpread(cdx, YIELD_CURVE, INTRINSIC_DATA);
    final double resAnnuity = INDEX_CAL.indexAnnuity(cdx, YIELD_CURVE, INTRINSIC_DATA, cdx.getCashSettleTime() + 0.01);

    final int indexSize = CREDIT_CURVES.length;
    double ref = 0.0;
    double refDirty = 0.0;
    double refAvgSpread = 0.0;
    double refAnnuity = 0.0;
    final AnalyticCdsPricer cdsPricer = new AnalyticCdsPricer();
    for (int i = 0; i < indexSize; ++i) {
      final CdsAnalytic cds = cdx.withRecoveryRate(1.0 - INTRINSIC_DATA.getLGD(i));
      ref += INTRINSIC_DATA.getWeight(i) * cdsPricer.pv(cds, YIELD_CURVE, CREDIT_CURVES[i], indexCoupon);
      refDirty += INTRINSIC_DATA.getWeight(i) * cdsPricer.pv(cds, YIELD_CURVE, CREDIT_CURVES[i], indexCoupon, CdsPriceType.DIRTY);
      refAvgSpread += INTRINSIC_DATA.getWeight(i) * cdsPricer.parSpread(cds, YIELD_CURVE, CREDIT_CURVES[i]);
      refAnnuity += INTRINSIC_DATA.getWeight(i) * cdsPricer.annuity(cds, YIELD_CURVE, CREDIT_CURVES[i], CdsPriceType.CLEAN, cdx.getCashSettleTime() + 0.01);
    }
    ref /= INTRINSIC_DATA.getIndexFactor();

    assertEquals(ref, res, tol);
    assertEquals(refDirty, resDirty, tol);
    assertEquals(refAvgSpread, resAvgSpread, tol);
    assertEquals(refAnnuity, resAnnuity, tol);

    final IsdaCompliantCreditCurve impliedCurve = INDEX_CAL.impliedIndexCurve(pillarCdx, indexCoupon, YIELD_CURVE, INTRINSIC_DATA);
    final int nPillars = INDEX_PILLARS.length;
    for (int i = 0; i < nPillars; ++i) {
      final double puf = cdsPricer.pv(pillarCdx[i], YIELD_CURVE, impliedCurve, indexCoupon);
      final double pufFullCurve = INDEX_CAL.indexPV(pillarCdx[i], indexCoupon, YIELD_CURVE, INTRINSIC_DATA);
      assertEquals(pufFullCurve, puf, tol);
    }

    /*
     * Defaulted names
     */
    final int[] defaultedNames = new int[] {2, 15, 37, 51 };
    final IntrinsicIndexDataBundle intrinsicDataWithDefaulted = INTRINSIC_DATA.withDefault(defaultedNames);
    final double resDefaulted = INDEX_CAL.indexPUF(cdx, indexCoupon, YIELD_CURVE, intrinsicDataWithDefaulted);
    final double resDefaultedDirty = INDEX_CAL.indexPV(cdx, indexCoupon, YIELD_CURVE, intrinsicDataWithDefaulted, CdsPriceType.DIRTY);
    final double resDefaultedAvgSpread = INDEX_CAL.averageSpread(cdx, YIELD_CURVE, intrinsicDataWithDefaulted);
    ref = 0.0;
    refDirty = 0.0;
    refAvgSpread = 0.0;
    for (int i = 0; i < indexSize; ++i) {
      if (!intrinsicDataWithDefaulted.isDefaulted(i)) {
        final CdsAnalytic cds = cdx.withRecoveryRate(1.0 - intrinsicDataWithDefaulted.getLGD(i));
        ref += intrinsicDataWithDefaulted.getWeight(i) * cdsPricer.pv(cds, YIELD_CURVE, CREDIT_CURVES[i], indexCoupon);
        refDirty += intrinsicDataWithDefaulted.getWeight(i) * cdsPricer.pv(cds, YIELD_CURVE, CREDIT_CURVES[i], indexCoupon, CdsPriceType.DIRTY);
        refAvgSpread += intrinsicDataWithDefaulted.getWeight(i) * cdsPricer.parSpread(cds, YIELD_CURVE, CREDIT_CURVES[i]);
      }
    }
    ref /= intrinsicDataWithDefaulted.getIndexFactor();
    assertEquals(ref, resDefaulted, tol);
    assertEquals(refDirty, resDefaultedDirty, tol);
    refAvgSpread /= intrinsicDataWithDefaulted.getIndexFactor();
    assertEquals(refAvgSpread, resDefaultedAvgSpread, tol);

    final double valTime = DayCounts.ACT_365F.yearFraction(TRADE_DATE, DEFAULT_CALENDAR.shift(TRADE_DATE, 3));
    final double resWithValTime = INDEX_CAL.indexPV(cdx, indexCoupon, YIELD_CURVE, INTRINSIC_DATA, CdsPriceType.CLEAN, valTime);
    assertEquals(res * INTRINSIC_DATA.getIndexFactor(), resWithValTime, tol);

    final IsdaCompliantCreditCurve impliedCurveDefaulted = INDEX_CAL.impliedIndexCurve(pillarCdx, indexCoupon, YIELD_CURVE, intrinsicDataWithDefaulted);
    for (int i = 0; i < nPillars; ++i) {
      final double puf = cdsPricer.pv(pillarCdx[i], YIELD_CURVE, impliedCurveDefaulted, indexCoupon);
      final double pufFullCurve = INDEX_CAL.indexPV(pillarCdx[i], indexCoupon, YIELD_CURVE, intrinsicDataWithDefaulted) / intrinsicDataWithDefaulted.getIndexFactor();
      assertEquals(pufFullCurve, puf, tol);
    }

    /*
     * IllegalArgument exception thrown
     */
    final int[] defInd = new int[indexSize];
    for (int i = 0; i < indexSize; ++i) {
      defInd[i] = i;
    }
    final IntrinsicIndexDataBundle allDefaulted = INTRINSIC_DATA.withDefault(defInd);
    try {
      INDEX_CAL.indexPUF(cdx, indexCoupon, YIELD_CURVE, allDefaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Index completely defaulted - not possible to rescale for PUF", e.getMessage());
    }
    try {
      INDEX_CAL.intrinsicIndexSpread(cdx, YIELD_CURVE, allDefaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Every name in the index is defaulted - cannot calculate a spread", e.getMessage());
    }
    try {
      INDEX_CAL.averageSpread(cdx, YIELD_CURVE, allDefaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Every name in the index is defaulted - cannot calculate a spread", e.getMessage());
    }
    try {
      INDEX_CAL.impliedIndexCurve(pillarCdx, indexCoupon, YIELD_CURVE, allDefaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Every name in the index is defaulted - cannot calculate implied index curve", e.getMessage());
    }
  }

  /**
   * Default adjusted index with intrinsic data
   */
  public void forwardIntrinsicTest() {
    final double timeToExpiry = ACT365F.yearFraction(TRADE_DATE, EXPIRY);

    /**
     * Test expected default settlement
     */
    final int miniIndexSize = 3;
    final IsdaCompliantCreditCurve[] ccs = Arrays.copyOf(INTRINSIC_DATA.getCreditCurves(), miniIndexSize);
    final IsdaCompliantCreditCurve[] ccsDefault = Arrays.copyOf(INTRINSIC_DATA.getCreditCurves(), miniIndexSize);
    final double[] rrs = new double[miniIndexSize];
    final BitSet bitset = new BitSet(miniIndexSize);

    for (int i = 0; i < miniIndexSize; ++i) {
      rrs[i] = 1.0 - INTRINSIC_DATA.getLGD(i);
    }

    final IntrinsicIndexDataBundle dataNoDefault = new IntrinsicIndexDataBundle(ccs, rrs);
    final int defaultedName = 1;
    bitset.set(defaultedName);
    ccsDefault[defaultedName] = null;
    final IntrinsicIndexDataBundle dataDefault = new IntrinsicIndexDataBundle(ccsDefault, rrs, bitset);

    final double expectedDefaultSettlement = INDEX_CAL.expectedDefaultSettlementValue(timeToExpiry, dataDefault);
    final double expectedDefaultSettlementNo = INDEX_CAL.expectedDefaultSettlementValue(timeToExpiry, dataNoDefault);
    double ref = 0.0;
    for (int i = 0; i < miniIndexSize; ++i) {
      ref += (1.0 - ccs[i].getSurvivalProbability(timeToExpiry)) * (1.0 - rrs[i]) / miniIndexSize;
    }
    assertEquals(ref, expectedDefaultSettlementNo, 1.0e-12);
    final double refDiff = ccs[defaultedName].getSurvivalProbability(timeToExpiry) * (1.0 - rrs[defaultedName]) / miniIndexSize;
    assertEquals(expectedDefaultSettlementNo + refDiff, expectedDefaultSettlement, 1.0e-12);

    /**
     * Test indexProtLeg with forward starting CDS index
     */
    final double prot = INDEX_CAL.indexProtLeg(FWD_START_CDX, YIELD_CURVE, dataNoDefault);
    final double protDefault = INDEX_CAL.indexProtLeg(FWD_START_CDX, YIELD_CURVE, dataDefault);

    final AnalyticCdsPricer defaultPricer = new AnalyticCdsPricer();
    final CdsAnalytic zeroRecCDX = FWD_START_CDX.withRecoveryRate(0.0);
    final double baseTime = 0.0;
    final double refProtDiff = defaultPricer.protectionLeg(zeroRecCDX, YIELD_CURVE, dataNoDefault.getCreditCurve(defaultedName), baseTime) * (1.0 - rrs[defaultedName]) / miniIndexSize /
        YIELD_CURVE.getDiscountFactor(FWD_START_CDX.getCashSettleTime());
    ref = 0.0;
    for (int i = 0; i < miniIndexSize; ++i) {
      ref += defaultPricer.protectionLeg(zeroRecCDX, YIELD_CURVE, ccs[i], baseTime) * (1.0 - rrs[i]) / miniIndexSize / YIELD_CURVE.getDiscountFactor(FWD_START_CDX.getCashSettleTime());
    }
    assertEquals(ref, prot, 1.0e-12);
    assertEquals(prot - refProtDiff, protDefault, 1.0e-12);

    /**
     * Test indexAnnuity with forward starting CDS index
     */
    final double annuity = INDEX_CAL.indexAnnuity(FWD_START_CDX, YIELD_CURVE, dataNoDefault);
    final double annuityDefault = INDEX_CAL.indexAnnuity(FWD_START_CDX, YIELD_CURVE, dataDefault);
    final double refAnnuityDiff = defaultPricer.annuity(FWD_START_CDX, YIELD_CURVE, dataNoDefault.getCreditCurve(defaultedName), CdsPriceType.CLEAN, baseTime) /
        miniIndexSize / YIELD_CURVE.getDiscountFactor(FWD_START_CDX.getCashSettleTime());
    ref = 0.0;
    for (int i = 0; i < miniIndexSize; ++i) {
      ref += defaultPricer.annuity(zeroRecCDX, YIELD_CURVE, ccs[i], CdsPriceType.CLEAN, baseTime) / miniIndexSize / YIELD_CURVE.getDiscountFactor(FWD_START_CDX.getCashSettleTime());
    }
    assertEquals(ref, annuity, 1.0e-12);
    assertEquals(annuity - refAnnuityDiff, annuityDefault, 1.0e-12);

    /**
     *  defaultAdjustedForwardIndexValue and defaultAdjustedForwardSpread from computation above
     */
    final double atmFwd = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, YIELD_CURVE, INDEX_COUPON, dataNoDefault);
    final double atmFwdDefault = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, YIELD_CURVE, INDEX_COUPON, dataDefault);
    assertEquals(prot - INDEX_COUPON * annuity + expectedDefaultSettlementNo, atmFwd, 1.0e-12);
    assertEquals((protDefault - INDEX_COUPON * annuityDefault) + expectedDefaultSettlement, atmFwdDefault, 1.0e-12);
    final double atmFwdSp = INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, timeToExpiry, YIELD_CURVE, dataNoDefault);
    final double atmFwdSpDefault = INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, timeToExpiry, YIELD_CURVE, dataDefault);
    assertEquals((prot + expectedDefaultSettlementNo) / annuity, atmFwdSp, 1.0e-12);
    assertEquals((protDefault + expectedDefaultSettlement) / annuityDefault, atmFwdSpDefault, 1.0e-12);

    /**
     * Exception expected
     */
    final double negativeTime = -timeToExpiry;
    try {
      INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, negativeTime, YIELD_CURVE, INDEX_COUPON, INTRINSIC_DATA);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("timeToExpiry given as " + negativeTime, e.getMessage());
    }
    final double longTime = FWD_START_CDX.getEffectiveProtectionStart() * 1.5;
    try {
      INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, longTime, YIELD_CURVE, INDEX_COUPON, INTRINSIC_DATA);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("effective protection start of " + FWD_START_CDX.getEffectiveProtectionStart() + " is less than time to expiry of " + longTime + ". Must provide a forward starting CDS",
          e.getMessage());
    }
    try {
      INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, negativeTime, YIELD_CURVE, INTRINSIC_DATA);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("timeToExpiry given as " + negativeTime, e.getMessage());
    }
    try {
      INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, longTime, YIELD_CURVE, INTRINSIC_DATA);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("effective protection start of " + FWD_START_CDX.getEffectiveProtectionStart() + " is less than time to expiry of " + longTime + ". Must provide a forward starting CDS",
          e.getMessage());
    }
  }

  /**
   * Default adjusted index with homogeneous pool
   */
  public void forwardHomogeneousTest() {
    final double timeToExpiry = ACT365F.yearFraction(TRADE_DATE, EXPIRY);

    final IsdaCompliantCreditCurve indexCurve = CREDIT_CURVE_BUILDER.calibrateCreditCurve(PILLAR_CDX, PILLAR_PUF, YIELD_CURVE);
    final double res = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, YIELD_CURVE, INDEX_COUPON, indexCurve);

    final int nPillars = PILLAR_PUF.length;
    final double[] pillarPufValues = new double[nPillars];
    for (int i = 0; i < nPillars; ++i) {
      pillarPufValues[i] = PILLAR_PUF[i].getPointsUpFront();
    }
    final int size = INTRINSIC_DATA.getIndexSize();
    final IsdaCompliantCreditCurve[] ccs = new IsdaCompliantCreditCurve[size];
    final double[] rrs = new double[size];

    /**
     * Use copies of the single credit curve, indexCurve
     */
    Arrays.fill(ccs, indexCurve.clone());
    Arrays.fill(rrs, 1. - FWD_START_CDX.getLGD());
    final IntrinsicIndexDataBundle homData = new IntrinsicIndexDataBundle(ccs, rrs);
    final double ref = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, YIELD_CURVE, INDEX_COUPON, homData);
    assertEquals(ref, res, 1.0e-12);

    /**
     * Fwd spread
     */
    final double FwdSpread = INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, timeToExpiry, YIELD_CURVE, indexCurve);
    final double zeroPuf = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, YIELD_CURVE, FwdSpread, indexCurve);
    final double FwdSpreadRef = INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, timeToExpiry, YIELD_CURVE, homData);
    assertEquals(0.0, zeroPuf, 1.0e-12);
    assertEquals(FwdSpreadRef, FwdSpread, 1.0e-12);

    /**
     * Start with another but identical credit curves, still close atm forward
     */
    Arrays.fill(ccs, INTRINSIC_DATA.getCreditCurve(3).clone());
    final IntrinsicIndexDataBundle homData1 = new IntrinsicIndexDataBundle(ccs, rrs);
    final IntrinsicIndexDataBundle homData1Adj = PSA.adjustCurves(pillarPufValues, PILLAR_CDX, INDEX_COUPON, YIELD_CURVE, homData1);
    final double ref1 = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, YIELD_CURVE, INDEX_COUPON, homData1Adj);
    assertEquals(ref1, res, 1.0e-5);

    /**
     * Exception expected
     */
    final double negativeTime = -timeToExpiry;
    try {
      INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, negativeTime, YIELD_CURVE, INDEX_COUPON, indexCurve);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("timeToExpiry given as " + negativeTime, e.getMessage());
    }
    final double longTime = FWD_START_CDX.getEffectiveProtectionStart() * 1.5;
    try {
      INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, longTime, YIELD_CURVE, INDEX_COUPON, indexCurve);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("effective protection start of " + FWD_START_CDX.getEffectiveProtectionStart() + " is less than time to expiry of " + longTime + ". Must provide a forward starting CDS",
          e.getMessage());
    }
    try {
      INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, negativeTime, YIELD_CURVE, indexCurve);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("timeToExpiry given as " + negativeTime, e.getMessage());
    }
    try {
      INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, longTime, YIELD_CURVE, indexCurve);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("effective protection start of " + FWD_START_CDX.getEffectiveProtectionStart() + " is less than time to expiry of " + longTime + ". Must provide a forward starting CDS",
          e.getMessage());
    }
  }

  /**
   * 
   */
  public void forwardHomogeneousDefaultTest() {
    final double timeToExpiry = ACT365F.yearFraction(TRADE_DATE, EXPIRY);

    final int initialIndexSize = INTRINSIC_DATA.getIndexSize();
    final IsdaCompliantCreditCurve indexCurve = CREDIT_CURVE_BUILDER.calibrateCreditCurve(PILLAR_CDX, PILLAR_PUF, YIELD_CURVE);

    double initialDefaultSettlement = 0.0;
    int numDefaults = 0;

    /**
     * No default
     */
    final double noDefault = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, initialIndexSize, YIELD_CURVE, INDEX_COUPON, indexCurve, initialDefaultSettlement, numDefaults);
    final double noDefaultRef = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, YIELD_CURVE, INDEX_COUPON, indexCurve);
    assertEquals(noDefaultRef, noDefault, 1.0e-12);

    final IsdaCompliantCreditCurve[] ccs = new IsdaCompliantCreditCurve[initialIndexSize];
    final double[] rrs = new double[initialIndexSize];
    Arrays.fill(ccs, indexCurve.clone());
    Arrays.fill(rrs, 1.0 - FWD_START_CDX.getLGD());

    /**
     * With default
     */
    final int[] defaulted = new int[] {2, 42, 55, 56, 82, 81 };
    final IntrinsicIndexDataBundle dataDefaulted = (new IntrinsicIndexDataBundle(ccs, rrs)).withDefault(defaulted);
    initialDefaultSettlement = initialDefaultSettlementCalculator(dataDefaulted);
    numDefaults = defaulted.length;
    final double atmFwd = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, initialIndexSize, YIELD_CURVE, INDEX_COUPON, indexCurve, initialDefaultSettlement, numDefaults);
    final double atmFwdRef = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, YIELD_CURVE, INDEX_COUPON, dataDefaulted);
    assertEquals(atmFwdRef, atmFwd, 1.0e-12);

    /**
     * Fwd spread
     */
    final double FwdSpread = INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, timeToExpiry, initialIndexSize, YIELD_CURVE, indexCurve, initialDefaultSettlement, numDefaults);
    final double zeroPuf = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, initialIndexSize, YIELD_CURVE, FwdSpread, indexCurve, initialDefaultSettlement, numDefaults);
    final double FwdSpreadRef = INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, timeToExpiry, YIELD_CURVE, dataDefaulted);
    assertEquals(0.0, zeroPuf, 1.0e-12);
    assertEquals(FwdSpreadRef, FwdSpread, 1.0e-12);

    /**
     * With default using another but identical credit curves, close atm value returned
     */
    final IsdaCompliantCreditCurve sampleCC = INTRINSIC_DATA.getCreditCurve(22).clone();
    Arrays.fill(ccs, sampleCC);
    final IntrinsicIndexDataBundle dataDefaulted1 = (new IntrinsicIndexDataBundle(ccs, rrs)).withDefault(defaulted);
    initialDefaultSettlement = initialDefaultSettlementCalculator(dataDefaulted1);
    final int nPillars = PILLAR_CDX.length;
    final double[] pufValues = new double[nPillars];
    for (int i = 0; i < nPillars; ++i) {
      pufValues[i] = PILLAR_PUF[i].getPointsUpFront();
    }
    final IntrinsicIndexDataBundle dataDefaulted1Adj = PSA.adjustCurves(pufValues, PILLAR_CDX, INDEX_COUPON, YIELD_CURVE, dataDefaulted1);
    final double atmFwdRef1 = INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, timeToExpiry, YIELD_CURVE, INDEX_COUPON, dataDefaulted1Adj);
    assertEquals(atmFwdRef1, atmFwd, 1.0e-5);

    /**
     * Exception expected
     */
    final double negativeTime = -timeToExpiry;
    try {
      INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, negativeTime, initialIndexSize, YIELD_CURVE, INDEX_COUPON, indexCurve, initialDefaultSettlement, numDefaults);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("timeToExpiry given as " + negativeTime, e.getMessage());
    }
    final double longTime = FWD_START_CDX.getEffectiveProtectionStart() * 1.5;
    try {
      INDEX_CAL.defaultAdjustedForwardIndexValue(FWD_START_CDX, longTime, initialIndexSize, YIELD_CURVE, INDEX_COUPON, indexCurve, initialDefaultSettlement, numDefaults);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("effective protection start of " + FWD_START_CDX.getEffectiveProtectionStart() + " is less than time to expiry of " + longTime + ". Must provide a forward starting CDS",
          e.getMessage());
    }
    try {
      INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, negativeTime, initialIndexSize, YIELD_CURVE, indexCurve, initialDefaultSettlement, numDefaults);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("timeToExpiry given as " + negativeTime, e.getMessage());
    }
    try {
      INDEX_CAL.defaultAdjustedForwardSpread(FWD_START_CDX, longTime, initialIndexSize, YIELD_CURVE, indexCurve, initialDefaultSettlement, numDefaults);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("effective protection start of " + FWD_START_CDX.getEffectiveProtectionStart() + " is less than time to expiry of " + longTime + ". Must provide a forward starting CDS",
          e.getMessage());
    }

    final int smallInitialSize = 0;
    try {
      INDEX_CAL.expectedDefaultSettlementValue(smallInitialSize, timeToExpiry, indexCurve, FWD_START_CDX.getLGD(), initialDefaultSettlement, smallInitialSize);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("initialIndexSize is " + smallInitialSize, e.getMessage());
    }
    final int negativeDefault = -5;
    try {
      INDEX_CAL.expectedDefaultSettlementValue(initialIndexSize, timeToExpiry, indexCurve, FWD_START_CDX.getLGD(), initialDefaultSettlement, negativeDefault);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("negative numDefaults", e.getMessage());
    }
    final int largeDefault = initialIndexSize + 1;
    try {
      INDEX_CAL.expectedDefaultSettlementValue(initialIndexSize, timeToExpiry, indexCurve, FWD_START_CDX.getLGD(), initialDefaultSettlement, largeDefault);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("More defaults (" + largeDefault + ") than size of index (" + initialIndexSize + ")", e.getMessage());
    }
  }

  private double initialDefaultSettlementCalculator(final IntrinsicIndexDataBundle intrinsicData) {
    final int size = intrinsicData.getIndexSize();
    double res = 0.0;
    for (int i = 0; i < size; ++i) {
      if (intrinsicData.isDefaulted(i)) {
        res += intrinsicData.getLGD(i);
      }
    }
    return res / size;
  }
}
