/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda.e2e;

import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getIMMDateSet;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getNextIMMDate;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getPrevIMMDate;
import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalytic;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalyticFactory;
import com.opengamma.strata.pricer.impl.credit.isda.CdsPriceType;
import com.opengamma.strata.pricer.impl.credit.isda.CdsRiskFactors;
import com.opengamma.strata.pricer.impl.credit.isda.FastCreditCurveBuilder;
import com.opengamma.strata.pricer.impl.credit.isda.FiniteDifferenceSpreadSensitivityCalculator;
import com.opengamma.strata.pricer.impl.credit.isda.HedgeRatioCalculator;
import com.opengamma.strata.pricer.impl.credit.isda.InterestRateSensitivityCalculator;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaBaseTest;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurveBuilder;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurveBuilder.ArbitrageHandling;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurve;

/**
 * End-to-end test for single name CDSs
 */
@Test
public class SingleNameCdsE2ETest extends IsdaBaseTest {

  // Calculators: all calculations are based on ORIGINAL_ISDA
  private static final FiniteDifferenceSpreadSensitivityCalculator FD_SPREAD_SENSE_CAL = new FiniteDifferenceSpreadSensitivityCalculator();
  private static final CdsRiskFactors RISK_CAL = new CdsRiskFactors();
  private static final HedgeRatioCalculator HEDGE_CAL = new HedgeRatioCalculator();
  private static final InterestRateSensitivityCalculator IR_CAL = new InterestRateSensitivityCalculator();

  // Trade
  private static final CdsAnalyticFactory CDS_FACTORY = new CdsAnalyticFactory(0.4);
  private static final double NOTIONAL = 1e6;
  private static final double COUPON = 0.01;
  private static final LocalDate TRADE_DATE = LocalDate.of(2011, Month.JUNE, 13);
  private static final LocalDate NEXT_IMM = getNextIMMDate(TRADE_DATE);
  private static final LocalDate STEPIN = TRADE_DATE.plusDays(1);
  private static final LocalDate CASH_SETTLE_DATE = DEFAULT_CALENDAR.shift(TRADE_DATE, 3); // AKA valuation date
  private static final LocalDate STARTDATE = getPrevIMMDate(TRADE_DATE);

  // Yield curve
  private static final LocalDate SPOT_DATE = LocalDate.of(2011, Month.JUNE, 15);
  private static final String[] YIELD_CURVE_POINTS = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y",
      "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y"};
  private static final String[] YIELD_CURVE_INSTRUMENTS = new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S",
      "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S"};
  private static final double[] YIELD_CURVE_RATES = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935,
      0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017,
      0.03092, 0.0316, 0.03231, 0.03367, 0.03419, 0.03411, 0.03412};
  private static final IsdaCompliantYieldCurve YIELD_CURVE = makeYieldCurve(TRADE_DATE, SPOT_DATE, YIELD_CURVE_POINTS,
      YIELD_CURVE_INSTRUMENTS, YIELD_CURVE_RATES, ACT360, D30360, Period.ofYears(1));

  // Credit curve form pillar CDSs
  private static final Period[] TENORS = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3),
      Period.ofYears(5), Period.ofYears(7), Period.ofYears(10)};
  private static final LocalDate[] PILLAR_DATES = getIMMDateSet(NEXT_IMM, TENORS);
  private static final LocalDate[] IMM_DATES = getIMMDateSet(NEXT_IMM, 41);
  private static final LocalDate[] MATURITIES_6M_STEP;
  private static final LocalDate[] MATURITIES_1Y_STEP;
  private static final double[] SPREADS = new double[] {0.007926718, 0.007926718, 0.012239372, 0.016978579,
      0.019270856, 0.02086048};
  private static final CdsAnalytic[] PILLAR_CDSS;
  private static final IsdaCompliantCreditCurve CREDIT_CURVE;
  static {
    final IsdaCompliantCreditCurveBuilder curveBuilder = new FastCreditCurveBuilder(ORIGINAL_ISDA,
        ArbitrageHandling.ZeroHazardRate);

    final int nPillars = PILLAR_DATES.length;
    PILLAR_CDSS = new CdsAnalytic[nPillars];
    for (int i = 0; i < nPillars; i++) {
      PILLAR_CDSS[i] = new CdsAnalytic(TRADE_DATE, STEPIN, CASH_SETTLE_DATE, STARTDATE, PILLAR_DATES[i],
          PAY_ACC_ON_DEFAULT, PAYMENT_INTERVAL, STUB, PROCTECTION_START, RECOVERY_RATE);
    }

    CREDIT_CURVE = curveBuilder.calibrateCreditCurve(PILLAR_CDSS, SPREADS, YIELD_CURVE);

    final int n = IMM_DATES.length;
    final LocalDate[] temp = new LocalDate[n];
    int count = 0;
    for (int i = 0; i < n; i = i + 2) {
      temp[count++] = IMM_DATES[i];
    }
    MATURITIES_6M_STEP = new LocalDate[count];
    System.arraycopy(temp, 0, MATURITIES_6M_STEP, 0, count);

    count = 0;
    for (int i = 0; i < n; i = i + 4) {
      temp[count++] = IMM_DATES[i];
    }
    MATURITIES_1Y_STEP = new LocalDate[count];
    System.arraycopy(temp, 0, MATURITIES_1Y_STEP, 0, count);
  }

  // Bucket CDSs
  private static final Period[] BUCKETS = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2),
      Period.ofYears(3), Period.ofYears(4), Period.ofYears(5), Period.ofYears(6), Period.ofYears(7), Period.ofYears(8),
      Period.ofYears(9), Period.ofYears(10), Period.ofYears(12), Period.ofYears(15), Period.ofYears(20),
      Period.ofYears(25), Period.ofYears(30)};
  private static final LocalDate[] BUCKET_DATES = getIMMDateSet(NEXT_IMM, BUCKETS);
  private static final CdsAnalytic[] BUCKET_CDSS = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, BUCKET_DATES);

  // Hedge CDSs
  private static final Period[] HEDGES = new Period[] {Period.of(1, 6, 0), Period.of(2, 0, 0), Period.of(6, 0, 0),
      Period.of(9, 0, 0), Period.of(20, 0, 0)};
  private static final LocalDate[] HEDGE_DATES = getIMMDateSet(NEXT_IMM, HEDGES);
  private static final CdsAnalytic[] HEDGE_CDSS = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, HEDGE_DATES);
  private static final double[] HEDGE_COUPON = new double[HEDGES.length];
  static {
    Arrays.fill(HEDGE_COUPON, COUPON);
  }

  private static final double TOL = 1.0e-8;

  /**
   * Standard CDS with short maturity
   */
  @Test
  public void IMMCDSTest1() {
    // Build pricing CDS
    Period tenor = Period.of(2, 3, 0);
    LocalDate maturityDate = NEXT_IMM.plus(tenor); // 2013-09-20
    CdsAnalytic pricingCDS = new CdsAnalytic(TRADE_DATE, STEPIN, CASH_SETTLE_DATE, STARTDATE, maturityDate,
        PAY_ACC_ON_DEFAULT, PAYMENT_INTERVAL, STUB, PROCTECTION_START, RECOVERY_RATE);

    int accrualDays = pricingCDS.getAccuredDays();
    double accruedPremium = pricingCDS.getAccruedPremium(COUPON) * NOTIONAL;
    double cleanPV = PRICER.pv(pricingCDS, YIELD_CURVE, CREDIT_CURVE, COUPON) * NOTIONAL;
    double dirtyPV = PRICER.pv(pricingCDS, YIELD_CURVE, CREDIT_CURVE, COUPON, CdsPriceType.DIRTY) * NOTIONAL;
    IsdaCompliantYieldCurve constantCurve = new IsdaCompliantYieldCurve(1.0, 0.0);
    double expectedLoss = PRICER.protectionLeg(pricingCDS, constantCurve, CREDIT_CURVE) * NOTIONAL;
    double cleanRPV01 = PRICER.annuity(pricingCDS, YIELD_CURVE, CREDIT_CURVE);
    double dirtyRPV01 = PRICER.annuity(pricingCDS, YIELD_CURVE, CREDIT_CURVE, CdsPriceType.DIRTY);
    double parSpread = PRICER.parSpread(pricingCDS, YIELD_CURVE, CREDIT_CURVE) * TEN_THOUSAND; // BPS
    double parallelIR01 = IR_CAL.parallelIR01(pricingCDS, COUPON, CREDIT_CURVE, YIELD_CURVE) * NOTIONAL;
    double[] bucketedIR01 = IR_CAL.bucketedIR01(pricingCDS, COUPON, CREDIT_CURVE, YIELD_CURVE);
    for (int i = 0; i < bucketedIR01.length; ++i) {
      bucketedIR01[i] *= NOTIONAL;
    }
    double parallelCS01 = FD_SPREAD_SENSE_CAL.parallelCS01FromCreditCurve(pricingCDS, COUPON, BUCKET_CDSS, YIELD_CURVE,
        CREDIT_CURVE, ONE_BP) * ONE_BP * NOTIONAL;
    double[] bucketedCS01 = FD_SPREAD_SENSE_CAL.bucketedCS01FromCreditCurve(pricingCDS, COUPON, BUCKET_CDSS,
        YIELD_CURVE, CREDIT_CURVE, ONE_BP);
    for (int i = 0; i < bucketedCS01.length; ++i) {
      bucketedCS01[i] *= (NOTIONAL * ONE_BP);
    }
    double valueOnDefault = RISK_CAL.valueOnDefault(pricingCDS, YIELD_CURVE, CREDIT_CURVE, COUPON) * NOTIONAL;
    double recovery01 = RISK_CAL.recoveryRateSensitivity(pricingCDS, YIELD_CURVE, CREDIT_CURVE) * NOTIONAL; // Analytic
    double[] bucketCDSCoupons = new double[PILLAR_CDSS.length];
    Arrays.fill(bucketCDSCoupons, COUPON);
    DoubleMatrix1D hedgeRatio = HEDGE_CAL.getHedgeRatios(pricingCDS, COUPON, HEDGE_CDSS, HEDGE_COUPON,
        CREDIT_CURVE, YIELD_CURVE);

    double[] expectedBIR01 = new double[] {-3.554654175175198E-4, -0.011674986050841385, 0.027624587315561167,
        0.02670611760208219, 0.03873563315243134, -0.1856733208432937, -0.4532763188576372, 0.05369920429154629, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    double[] expectedBCS01 = new double[] {-0.0598207645016724, -0.17567436897888977, 146.42339811065176,
        74.44971215266743, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    double[] expectedRatio = new double[] {-0.4961595903306217, 1.496146243832723, 1.4643659774973175E-10,
        -1.7784851094986757E-11, 1.6747710535268755E-11};

    assertEquals("accrual days", 86, accrualDays);
    assertEqualsRelativeTol("accrued premium", 2388.888888888889, accruedPremium, TOL);
    assertEqualsRelativeTol("clean PV", 3353.454362533141, cleanPV, TOL);
    assertEqualsRelativeTol("dirty PV", 964.5654736442459, dirtyPV, TOL);
    assertEqualsRelativeTol("expected loss", 26064.87887963509, expectedLoss, TOL);
    assertEqualsRelativeTol("clean RPV01", 2.2126685809458135, cleanRPV01, TOL);
    assertEqualsRelativeTol("dirty RPV01", 2.451557469834703, dirtyRPV01, TOL);
    assertEqualsRelativeTol("par spread (BPS)", 115.15570109057948, parSpread, TOL);
    assertEqualsRelativeTol("parallel IR01", -0.5042019973929002, parallelIR01, TOL);
    assertDoubleArray("bucketed IR01", expectedBIR01, bucketedIR01, TOL);
    assertEqualsRelativeTol("parallel CS01", 220.59286685899292, parallelCS01, TOL);
    assertDoubleArray("bucketed CS01", expectedBCS01, bucketedCS01, TOL);
    assertEqualsRelativeTol("value on default", 596646.5456374668, valueOnDefault, TOL);
    assertEqualsRelativeTol("recovery01", -42466.90028665213, recovery01, TOL);
    assertDoubleArray("hedge ratio", expectedRatio, hedgeRatio.getData(), TOL);
  }

  /**
   * Standard CDS with longer maturity
   */
  @Test
  public void IMMCDSTest2() {
    // Build pricing CDS
    Period tenor = Period.of(17, 0, 0);
    LocalDate maturityDate = NEXT_IMM.plus(tenor); // 2028-06-20
    CdsAnalytic pricingCDS = new CdsAnalytic(TRADE_DATE, STEPIN, CASH_SETTLE_DATE, STARTDATE, maturityDate,
        PAY_ACC_ON_DEFAULT, PAYMENT_INTERVAL, STUB, PROCTECTION_START, RECOVERY_RATE);

    int accrualDays = pricingCDS.getAccuredDays();
    double accruedPremium = pricingCDS.getAccruedPremium(COUPON) * NOTIONAL;
    double cleanPV = PRICER.pv(pricingCDS, YIELD_CURVE, CREDIT_CURVE, COUPON) * NOTIONAL;
    double dirtyPV = PRICER.pv(pricingCDS, YIELD_CURVE, CREDIT_CURVE, COUPON, CdsPriceType.DIRTY) * NOTIONAL;
    IsdaCompliantYieldCurve constantCurve = new IsdaCompliantYieldCurve(1.0, 0.0);
    double expectedLoss = PRICER.protectionLeg(pricingCDS, constantCurve, CREDIT_CURVE) * NOTIONAL;
    double cleanRPV01 = PRICER.annuity(pricingCDS, YIELD_CURVE, CREDIT_CURVE);
    double dirtyRPV01 = PRICER.annuity(pricingCDS, YIELD_CURVE, CREDIT_CURVE, CdsPriceType.DIRTY);
    double parSpread = PRICER.parSpread(pricingCDS, YIELD_CURVE, CREDIT_CURVE) * TEN_THOUSAND; // BPS
    double parallelIR01 = IR_CAL.parallelIR01(pricingCDS, COUPON, CREDIT_CURVE, YIELD_CURVE) * NOTIONAL;
    double[] bucketedIR01 = IR_CAL.bucketedIR01(pricingCDS, COUPON, CREDIT_CURVE, YIELD_CURVE);
    for (int i = 0; i < bucketedIR01.length; ++i) {
      bucketedIR01[i] *= NOTIONAL;
    }
    double parallelCS01 = FD_SPREAD_SENSE_CAL.parallelCS01FromCreditCurve(pricingCDS, COUPON, BUCKET_CDSS, YIELD_CURVE,
        CREDIT_CURVE, ONE_BP) * ONE_BP * NOTIONAL;
    double[] bucketedCS01 = FD_SPREAD_SENSE_CAL.bucketedCS01FromCreditCurve(pricingCDS, COUPON, BUCKET_CDSS,
        YIELD_CURVE, CREDIT_CURVE, ONE_BP);
    for (int i = 0; i < bucketedCS01.length; ++i) {
      bucketedCS01[i] *= (NOTIONAL * ONE_BP);
    }
    double valueOnDefault = RISK_CAL.valueOnDefault(pricingCDS, YIELD_CURVE, CREDIT_CURVE, COUPON) * NOTIONAL;
    double recovery01 = RISK_CAL.recoveryRateSensitivity(pricingCDS, YIELD_CURVE, CREDIT_CURVE) * NOTIONAL;
    double[] bucketCDSCoupons = new double[PILLAR_CDSS.length];
    Arrays.fill(bucketCDSCoupons, COUPON);
    DoubleMatrix1D hedgeRatio = HEDGE_CAL.getHedgeRatios(pricingCDS, COUPON, HEDGE_CDSS, HEDGE_COUPON,
        CREDIT_CURVE, YIELD_CURVE);

    double[] expectedBIR01 = new double[] {0.10195891236852717, -0.011674986061249726, 0.027624587339847295,
        0.026706117595143297, 0.03873563314549244, -0.1856733208294159, -0.8543973007979488, -2.568224954563325,
        -5.036815800052441, -6.087580077140942, -7.045275780076521, -7.52983564417109, -7.836262445909403,
        -8.103245255519642, -8.297088633868466, -8.394421414925635, -15.941139568542706, -25.22143603045368,
        -3.6720683527224907, 0.0, 0.0};
    double[] expectedBCS01 = new double[] {-0.2488307417891633, -0.8251527622848975, -2.0651772812685376,
        -2.5937087356209254, -3.333937820704236, -4.131638490473266, -5.022628897066728, -6.003569124668484,
        -7.0661200600175, -8.199981770373732, -14.139374540145244, -30.47380986237469, 482.1018269742794,
        496.1353877235286, 0.0, 0.0};
    double[] expectedRatio = new double[] {3.7335290727514284E-4, -9.055784969918724E-4, -9.918711889929754E-4,
        0.16704483857139465, 0.8382732358018894};

    assertEquals("accrual days", 86, accrualDays);
    assertEqualsRelativeTol("accrued premium", 2388.888888888889, accruedPremium, TOL);
    assertEqualsRelativeTol("clean PV", 127835.89621485685, cleanPV, TOL);
    assertEqualsRelativeTol("dirty PV", 125447.00732596798, dirtyPV, TOL);
    assertEqualsRelativeTol("expected loss", 296188.29576502816, expectedLoss, TOL);
    assertEqualsRelativeTol("clean RPV01", 10.410958359321803, cleanRPV01, TOL);
    assertEqualsRelativeTol("dirty RPV01", 10.649847248210692, dirtyRPV01, TOL);
    assertEqualsRelativeTol("par spread (BPS)", 222.78974884228086, parSpread, TOL);
    assertEqualsRelativeTol("parallel IR01", -106.57168877478695, parallelIR01, TOL);
    assertDoubleArray("bucketed IR01", expectedBIR01, bucketedIR01, TOL);
    assertEqualsRelativeTol("parallel CS01", 891.7864206657855, parallelCS01, TOL);
    assertDoubleArray("bucketed CS01", expectedBCS01, bucketedCS01, TOL);
    assertEqualsRelativeTol("value on default", 472164.1037851431, valueOnDefault, TOL);
    assertEqualsRelativeTol("recovery01", -386575.79968012485, recovery01, TOL);
    assertDoubleArray("hedge ratio", expectedRatio, hedgeRatio.getData(), TOL);
  }

  /**
   * Example of legacy CDS
   */
  @Test
  public void LegacyCDSTest() {
    Period tenor = Period.of(10, 0, 0);
    LocalDate startDate = STEPIN; // T+1 start
    LocalDate maturityDate = STEPIN.plus(tenor); // counted from effective date
    CdsAnalytic pricingCDS = new CdsAnalytic(TRADE_DATE, STEPIN, CASH_SETTLE_DATE, startDate, maturityDate,
        PAY_ACC_ON_DEFAULT, PAYMENT_INTERVAL, STUB, PROCTECTION_START, RECOVERY_RATE);

    double coupon = PRICER.parSpread(pricingCDS, YIELD_CURVE, CREDIT_CURVE); // coupon s.t. zero upfront value
    int accrualDays = pricingCDS.getAccuredDays();
    double accruedPremium = pricingCDS.getAccruedPremium(COUPON) * NOTIONAL;
    double cleanPV = PRICER.pv(pricingCDS, YIELD_CURVE, CREDIT_CURVE, coupon) * NOTIONAL;
    double dirtyPV = PRICER.pv(pricingCDS, YIELD_CURVE, CREDIT_CURVE, coupon, CdsPriceType.DIRTY) * NOTIONAL;
    IsdaCompliantYieldCurve constantCurve = new IsdaCompliantYieldCurve(1.0, 0.0);
    double expectedLoss = PRICER.protectionLeg(pricingCDS, constantCurve, CREDIT_CURVE) * NOTIONAL;
    double cleanRPV01 = PRICER.annuity(pricingCDS, YIELD_CURVE, CREDIT_CURVE);
    double dirtyRPV01 = PRICER.annuity(pricingCDS, YIELD_CURVE, CREDIT_CURVE, CdsPriceType.DIRTY);
    double parSpread = coupon * TEN_THOUSAND; // BPS
    double parallelIR01 = IR_CAL.parallelIR01(pricingCDS, coupon, CREDIT_CURVE, YIELD_CURVE) * NOTIONAL;
    double[] bucketedIR01 = IR_CAL.bucketedIR01(pricingCDS, coupon, CREDIT_CURVE, YIELD_CURVE);
    for (int i = 0; i < bucketedIR01.length; ++i) {
      bucketedIR01[i] *= NOTIONAL;
    }
    double parallelCS01 = FD_SPREAD_SENSE_CAL.parallelCS01FromCreditCurve(pricingCDS, coupon, BUCKET_CDSS, YIELD_CURVE,
        CREDIT_CURVE, ONE_BP) * ONE_BP * NOTIONAL;
    double[] bucketedCS01 = FD_SPREAD_SENSE_CAL.bucketedCS01FromCreditCurve(pricingCDS, coupon, BUCKET_CDSS,
        YIELD_CURVE, CREDIT_CURVE, ONE_BP);
    for (int i = 0; i < bucketedCS01.length; ++i) {
      bucketedCS01[i] *= (NOTIONAL * ONE_BP);
    }
    double valueOnDefault = RISK_CAL.valueOnDefault(pricingCDS, YIELD_CURVE, CREDIT_CURVE, coupon) * NOTIONAL;
    double recovery01 = RISK_CAL.recoveryRateSensitivity(pricingCDS, YIELD_CURVE, CREDIT_CURVE) * NOTIONAL;
    double[] bucketCDSCoupons = new double[PILLAR_CDSS.length];
    Arrays.fill(bucketCDSCoupons, COUPON);
    DoubleMatrix1D hedgeRatio = HEDGE_CAL.getHedgeRatios(pricingCDS, coupon, HEDGE_CDSS, HEDGE_COUPON,
        CREDIT_CURVE, YIELD_CURVE);

    double[] expectedBIR01 = new double[] {-0.006036998789760162, -0.00868633129313956, 0.09937045325481009,
        0.16375497519094395, 0.24138012230667805, 0.48729564533500636, 1.2036552658745148, 0.37070870320676796,
        -1.3907831437343088, -1.8674246091698876, -2.3723618600424157, -2.4828678812094385, -2.561667216488539,
        -2.595901377966392, -0.017970836957426073, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    double[] expectedBCS01 = new double[] {2.770988993816559E-5, -3.717554042381721E-6, 6.128880736255837E-5,
        3.365298040236553E-4, 6.236849925400634E-4, 2.3975163521150478E-4, 5.054209273325228E-4, 6.192977519692278E-4,
        2.2397053656142418E-4, 11.28402038486076, 758.7836222882893, 0.0, 0.0, 0.0, 0.0, 0.0};
    double[] expectedRatio = new double[] {-0.01656660944695296, 0.06035788587176887, 0.05918772036678788,
        0.887754035080094, 0.14988679402430255};

    assertEquals("accrual days", 0, accrualDays);
    assertEqualsRelativeTol("accrued premium", 0.0, accruedPremium, TOL);
    assertEqualsRelativeTol("clean PV", 0.0, cleanPV, TOL);
    assertEqualsRelativeTol("dirty PV", 0.0, dirtyPV, TOL);
    assertEqualsRelativeTol("expected loss", 185422.2147273289, expectedLoss, TOL);
    assertEqualsRelativeTol("clean RPV01", 7.701071597676212, cleanRPV01, TOL);
    assertEqualsRelativeTol("dirty RPV01", 7.701071597676212, dirtyRPV01, TOL); // no accrued for legacy CDS
    assertEqualsRelativeTol("par spread (BPS)", 208.54469451401945, parSpread, TOL);
    assertEqualsRelativeTol("parallel IR01", -10.735451012489072, parallelIR01, TOL);
    assertDoubleArray("bucketed IR01", expectedBIR01, bucketedIR01, TOL);
    assertEqualsRelativeTol("parallel CS01", 769.5220353102217, parallelCS01, TOL);
    assertDoubleArray("bucketed CS01", expectedBCS01, bucketedCS01, TOL);
    assertEqualsRelativeTol("value on default", 600000.0, valueOnDefault, TOL); // zero PV, no accrued
    assertEqualsRelativeTol("recovery01", -267669.60396132956, recovery01, TOL);
    assertDoubleArray("hedge ratio", expectedRatio, hedgeRatio.getData(), TOL);
  }

  private void assertEqualsRelativeTol(String message, double expected, double result, double relTol) {
    double tol = Math.max(1.0, Math.abs(expected)) * relTol;
    assertEquals(message, expected, result, tol);
  }

  private void assertDoubleArray(String message, double[] expected, double[] result, double relTol) {
    int nValues = expected.length;
    assertEquals(nValues, result.length);
    for (int i = 0; i < nValues; ++i) {
      assertEqualsRelativeTol(message + "(" + i + "-th element)", expected[i], result[i], relTol);
    }

  }
}
