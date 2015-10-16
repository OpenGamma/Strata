/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda.e2e;

import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getIMMDateSet;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getNextIMMDate;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getPrevIMMDate;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.perturb.ShiftType;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionResult;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.pricer.impl.credit.isda.AnalyticSpreadSensitivityCalculator;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalytic;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalyticFactory;
import com.opengamma.strata.pricer.impl.credit.isda.CdsQuotedSpread;
import com.opengamma.strata.pricer.impl.credit.isda.FastCreditCurveBuilder;
import com.opengamma.strata.pricer.impl.credit.isda.FiniteDifferenceSpreadSensitivityCalculator;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaBaseTest;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurveBuilder;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurveBuilder.ArbitrageHandling;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurve;
import com.opengamma.strata.pricer.impl.credit.isda.MarketQuoteConverter;

/**
 * This code generates the results in the paper <i>The Pricing and Risk Management of Credit Default Swaps, with a Focus
 * on the ISDA Model</i>. Tests either produce tables for Latex (directly using dumpLatexTable or via the Exce2LaTeX pluin)
 * or data sets used to produce graphs in Excel.  
 */
@Test(enabled = false)
public class CdsPaperExamples extends IsdaBaseTest {
  private static final MatrixAlgebra MA = new OGMatrixAlgebra();
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yy");

  private static final MarketQuoteConverter PUF_CONVERTER = new MarketQuoteConverter();
  private static final FiniteDifferenceSpreadSensitivityCalculator FD_SPREAD_SENSE_CAL = new FiniteDifferenceSpreadSensitivityCalculator();
  private static final AnalyticSpreadSensitivityCalculator ANALYTIC_SPREAD_SENSE_CAL = new AnalyticSpreadSensitivityCalculator();
  private static final CdsAnalyticFactory CDS_FACTORY = new CdsAnalyticFactory(0.4);

  private static final LocalDate TODAY = LocalDate.of(2011, Month.JUNE, 13);
  private static final LocalDate NEXT_IMM = getNextIMMDate(TODAY);

  private static final LocalDate TRADE_DATE = LocalDate.of(2011, Month.JUNE, 13);
  private static final LocalDate STEPIN = TRADE_DATE.plusDays(1);
  private static final LocalDate CASH_SETTLE_DATE = DEFAULT_CALENDAR.shift(TRADE_DATE, 3); // AKA valuation date
  private static final LocalDate STARTDATE = FOLLOWING.adjust(getPrevIMMDate(TRADE_DATE), DEFAULT_CALENDAR); // LocalDate.of(2011, Month.MARCH, 21);

  private static final Period[] TENORS = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3),
      Period.ofYears(5), Period.ofYears(7), Period.ofYears(10)};
  private static final LocalDate[] PILLAR_DATES = getIMMDateSet(NEXT_IMM, TENORS);
  private static final LocalDate[] IMM_DATES = getIMMDateSet(NEXT_IMM, 41);
  private static final LocalDate[] MATURITIES_6M_STEP;
  private static final LocalDate[] MATURITIES_1Y_STEP;

  // yield curve
  private static final LocalDate SPOT_DATE = LocalDate.of(2011, Month.JUNE, 15);
  private static final String[] YIELD_CURVE_POINTS = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y", "4Y", "5Y",
      "6Y", "7Y", "8Y", "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y"};
  private static final String[] YIELD_CURVE_INSTRUMENTS = new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S", "S", "S",
      "S", "S", "S", "S", "S", "S", "S", "S", "S", "S"};
  private static final double[] YIELD_CURVE_RATES = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838,
      0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017,
      0.03092, 0.0316, 0.03231, 0.03367, 0.03419, 0.03411, 0.03412};
  private static final IsdaCompliantYieldCurve YIELD_CURVE = makeYieldCurve(TRADE_DATE, SPOT_DATE, YIELD_CURVE_POINTS,
      YIELD_CURVE_INSTRUMENTS, YIELD_CURVE_RATES, ACT360, D30360, Period.ofYears(1));

  private static final double COUPON = 0.01;
  private static final double[] SPREADS = new double[] {0.007926718, 0.007926718, 0.012239372, 0.016978579, 0.019270856,
      0.02086048};
  private static final CdsAnalytic[] PILLAR_CDSS;
  private static final IsdaCompliantCreditCurve CREDIT_CURVE;

  static {
    final IsdaCompliantCreditCurveBuilder curveBuilder = new FastCreditCurveBuilder(MARKIT_FIX, ArbitrageHandling.ZeroHazardRate);

    final int nPillars = PILLAR_DATES.length;
    PILLAR_CDSS = new CdsAnalytic[nPillars];
    for (int i = 0; i < nPillars; i++) {
      PILLAR_CDSS[i] = new CdsAnalytic(TRADE_DATE, STEPIN, CASH_SETTLE_DATE, STARTDATE, PILLAR_DATES[i], PAY_ACC_ON_DEFAULT,
          PAYMENT_INTERVAL, STUB, PROCTECTION_START, RECOVERY_RATE);
    }

    CREDIT_CURVE = curveBuilder.calibrateCreditCurve(PILLAR_CDSS, SPREADS, YIELD_CURVE);

    int n = IMM_DATES.length;
    LocalDate[] temp = new LocalDate[n];
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

  /**
   * Print out the knots times (year fractions) and corresponding zero rates for the calibrated yield curve used in these
   * examples
   */
  @Test(description = "Demo", enabled = false)
  public void yieldCurveDump() {
    int n = YIELD_CURVE.getNumberOfKnots();
    System.out.println("Tenor\tRate\tYear Fraction\tZero Rate");
    for (int i = 0; i < n; i++) {
      System.out.println(YIELD_CURVE_POINTS[i] + "\t" + YIELD_CURVE_RATES[i] + "\t" + YIELD_CURVE.getTimeAtIndex(i) + "\t" +
          YIELD_CURVE.getZeroRateAtIndex(i));
    }
  }

  /**
   * Print on the credit curve pillar dates and their spreads, along with the year fractions and (calibrated) survival probabilities
   */
  @Test(description = "Demo", enabled = false)
  public void creditCurveDump() {
    int n = PILLAR_DATES.length;
    System.out.println("Tenor\tMaturity\tSpread (bps)\tYear Fraction\tSurvival Probability");
    for (int i = 0; i < n; i++) {
      double t = CREDIT_CURVE.getTimeAtIndex(i);
      System.out.println(TENORS[i].toString() + "\t" + PILLAR_DATES[i].format(DATE_FORMAT) + "\t" + SPREADS[i] * TEN_THOUSAND +
          "\t" + t + "\t" + CREDIT_CURVE.getSurvivalProbability(t));
    }
  }

  /**
   * Price a set of CDSs using standard ISDA model (1.8.2), OG's suggested fix and the (incorrect) Markit 'fit'
   */
  @Test(description = "Demo", enabled = false)
  public void threeWayPriceTest() {
    double notional = 1e7;
    Period[] tenors = new Period[] {Period.ofMonths(3), Period.ofMonths(6), Period.ofYears(1), Period.ofYears(5),
        Period.ofYears(10)};
    CdsAnalytic[] cds = CDS_FACTORY.makeImmCds(TRADE_DATE, tenors);
    int n = tenors.length;
    double[][] res = new double[3][n];
    for (int i = 0; i < n; i++) {
      res[0][i] = notional * PRICER_OG_FIX.pv(cds[i], YIELD_CURVE, CREDIT_CURVE, 0.01);
      res[1][i] = notional * PRICER.pv(cds[i], YIELD_CURVE, CREDIT_CURVE, 0.01);
      res[2][i] = notional * PRICER_MARKIT_FIX.pv(cds[i], YIELD_CURVE, CREDIT_CURVE, 0.01);
    }

    System.out.println(DoubleMatrix.copyOf(res));
  }

  /**
   * Plots price against hazard rate for various recovery rates
   */
  @Test(description = "Demo", enabled = false)
  public void priceVsHazardRate() {
    double[] recoveryRates = new double[] {0, 0.25, 0.5, 0.75, 1.0};
    CdsAnalyticFactory factory = new CdsAnalyticFactory();
    CdsAnalytic cds = factory.makeImmCds(TRADE_DATE, Period.ofYears(5));
    int n = recoveryRates.length;
    System.out.print("Hazard rate");
    for (int j = 0; j < n; j++) {
      System.out.print("\tRecovery " + (recoveryRates[j] * 100) + "%");
    }
    System.out.print("\n");
    for (int i = 0; i < 100; i++) {
      double lambda = 0.8 * i / 100.;
      System.out.print(lambda);
      IsdaCompliantCreditCurve cc = new IsdaCompliantCreditCurve(5.0, lambda);
      for (int j = 0; j < n; j++) {
        cds = cds.withRecoveryRate(recoveryRates[j]);
        double price = PRICER.pv(cds, YIELD_CURVE, cc, 0.05);
        System.out.print("\t" + price);
      }
      System.out.print("\n");
    }
  }

  /**
   * Plots points-up-front (PUF) against quoted spread for coupons of 0bps, 100bps and 500bps
   */
  @Test(description = "Demo", enabled = false)
  public void pufVsQuotedSpread() {
    CdsAnalyticFactory factory = new CdsAnalyticFactory(0.4);
    CdsAnalytic cds = factory.makeImmCds(TRADE_DATE, Period.ofYears(10));

    for (int i = 0; i < 100; i++) {
      double qs = i * 10;
      double puf0 = PUF_CONVERTER.convert(cds, new CdsQuotedSpread(0.0, qs * ONE_BP), YIELD_CURVE).getPointsUpFront();
      double puf100 = PUF_CONVERTER.convert(cds, new CdsQuotedSpread(0.01, qs * ONE_BP), YIELD_CURVE).getPointsUpFront();
      double puf500 = PUF_CONVERTER.convert(cds, new CdsQuotedSpread(0.05, qs * ONE_BP), YIELD_CURVE).getPointsUpFront();
      System.out.println(qs + "\t" + puf0 + "\t" + puf100 + "\t" + puf500);
    }
  }

  /**
   * Print a table of the par rate sensitivity to the to zero hazard rates at the credit curve nodes for a set of CDSs with recovery rate 40\%
   */
  @Test(description = "Demo", enabled = false)
  public void parRateSensitivityTest() {
    CdsAnalyticFactory factory = new CdsAnalyticFactory();
    int nPillars = PILLAR_DATES.length;

    System.out.print("CDS Maturity");
    for (int i = 0; i < nPillars; i++) {
      System.out.print("\t" + TENORS[i].toString());
    }
    System.out.print("\n");

    for (int i = 0; i < IMM_DATES.length; i = i + 2) {
      LocalDate mat = IMM_DATES[i];
      System.out.print(mat.format(DATE_FORMAT));
      CdsAnalytic cds = factory.makeCds(TRADE_DATE, STARTDATE, mat);
      for (int j = 0; j < nPillars; j++) {
        double sense = PRICER.parSpreadCreditSensitivity(cds, YIELD_CURVE, CREDIT_CURVE, j);
        System.out.print("\t" + sense);
      }
      System.out.print("\n");
    }
  }

  /**
   * Print the present value sensitivity of CDSs with coupons of 100bps to the zero hazard rates at the credit curve nodes.
   * <b>Note:</b> This does not appear as a table in the paper.
   */
  @Test(description = "Demo", enabled = false)
  public void pvSensitivityTest() {
    double coupon = 0.01;
    CdsAnalyticFactory factory = new CdsAnalyticFactory(0.4);
    int nPillars = PILLAR_DATES.length;

    System.out.print("CDS Maturity");
    for (int i = 0; i < nPillars; i++) {
      System.out.print("\t" + TENORS[i].toString());
    }
    System.out.print("\n");

    for (int i = 0; i < nPillars; i++) {
      LocalDate mat = PILLAR_DATES[i];
      System.out.print(mat.format(DATE_FORMAT));
      CdsAnalytic cds = factory.makeCds(TRADE_DATE, STARTDATE, mat);
      for (int j = 0; j < nPillars; j++) {
        double sense = PRICER.pvCreditSensitivity(cds, YIELD_CURVE, CREDIT_CURVE, coupon, j);
        System.out.print("\t" + sense);
      }
      System.out.print("\n");
    }
  }

  /**
   * For a set of CDSs with different maturities (every other IMM date) calculate the hedge ratios - the amount of the 
   * standard (pillar) CDSs that will give the same first order PV sensitivity to the credit curve knots. When the 
   * CDS in question IS a pillar CDS, the hedge ratio is of course exactly 1.0 for that pillar 9and zero everywhere else)
   * <b>Note:</b> This does not appear as a table in the paper, however it does provide hedge ratios for the hedgingPerformanceDemo.
   */
  @Test(description = "Demo", enabled = false)
  public void pvHedgingDemo() {
    double coupon = 0.01;

    int nPillars = PILLAR_DATES.length;
    double[][] res = new double[nPillars][nPillars];
    for (int i = 0; i < nPillars; i++) {
      LocalDate mat = PILLAR_DATES[i];
      CdsAnalytic cds = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, mat);
      for (int j = 0; j < nPillars; j++) {
        double sense = PRICER.pvCreditSensitivity(cds, YIELD_CURVE, CREDIT_CURVE, coupon, j);
        res[j][i] = sense;
      }
    }
    DoubleMatrix jacT = DoubleMatrix.copyOf(res);
    LUDecompositionCommons decomp = new LUDecompositionCommons();
    LUDecompositionResult luRes = decomp.evaluate(jacT);

    System.out.print("CDS Maturity");
    for (int i = 0; i < nPillars; i++) {
      System.out.print("\t" + TENORS[i].toString());
    }
    System.out.print("\n");

    for (int i = 0; i < IMM_DATES.length; i = i + 2) {
      LocalDate mat = IMM_DATES[i];
      System.out.print(mat.format(DATE_FORMAT));
      CdsAnalytic cds = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, mat);
      DoubleArray vLambda = DoubleArray.of(
          nPillars,
          j -> PRICER.pvCreditSensitivity(cds, YIELD_CURVE, CREDIT_CURVE, coupon, j));

      DoubleArray w = luRes.solve(vLambda);
      for (int j = 0; j < nPillars; j++) {
        System.out.print("\t" + w.get(j));
      }
      System.out.print("\n");
    }
  }

  /**
   * Take a hedged CDS portfolio (1.0 of June 20th 2015 and -0.47556 & -0.52474 of June 20th 2014 & June 20th 2016),
   * and compute its change in PV for both parallel shifts and tilts to the credit curve. The results are shown for a
   * notional of 10MM. 
   */
  @Test(description = "Demo", enabled = false)
  public void hedgingPerformanceDemo() {
    double coupon = 0.01;
    double notional = 1e7;
    LocalDate mat = LocalDate.of(2015, Month.JUNE, 20);
    LocalDate mat1 = LocalDate.of(2014, Month.JUNE, 20);
    LocalDate mat2 = LocalDate.of(2016, Month.JUNE, 20);
    double[] hedgeRatios = new double[] {1.0, -0.47556, -0.52474};

    CdsAnalytic[] cdsPort = new CdsAnalytic[3];
    cdsPort[0] = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, mat);
    cdsPort[1] = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, mat1);
    cdsPort[2] = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, mat2);

    double basePVH = 0;
    for (int i = 0; i < 3; i++) {
      basePVH += notional * hedgeRatios[i] * PRICER.pv(cdsPort[i], YIELD_CURVE, CREDIT_CURVE, coupon);
    }

    System.out.println("Basis Points\tParallel\tTilt");
    for (int k = 0; k < 101; k++) {
      double bump = -100 + 2.0 * k;
      IsdaCompliantCreditCurve cc = bumpCurve(CREDIT_CURVE, bump * ONE_BP);
      IsdaCompliantCreditCurve ccTilt = tiltCurve(CREDIT_CURVE, bump * ONE_BP);

      double pvH = 0;
      double pvTilt = 0;
      for (int i = 0; i < 3; i++) {
        pvH += notional * hedgeRatios[i] * PRICER.pv(cdsPort[i], YIELD_CURVE, cc, coupon);
        pvTilt += notional * hedgeRatios[i] * PRICER.pv(cdsPort[i], YIELD_CURVE, ccTilt, coupon);
      }
      System.out.println(bump + "\t" + (pvH - basePVH) + "\t" + (pvTilt - basePVH));
    }
  }

  /**
   * The sensitivity of the PV of a set of CDSs to the par spreads of the CDSs used to construct the credit curve.
   * The last column shows the sensitivity of all the spreads moving in parallel. The (priced) CDSs all have a coupon of 100bps.
   * All CDSs have a recovery rate of 40\% and the Trade date is 13-Jun-2011. <p>
   * This uses the method dumpLatexTable to format the output into a Latex table. 
   */
  @Test(description = "Demo", enabled = false)
  public void analyticCS01test() {

    int nMat = MATURITIES_6M_STEP.length;
    double[] coupons = new double[nMat];
    Arrays.fill(coupons, COUPON);
    CdsAnalytic[] cds = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, MATURITIES_6M_STEP);
    double[][] analCS01 = ANALYTIC_SPREAD_SENSE_CAL.bucketedCS01FromCreditCurve(cds, coupons, PILLAR_CDSS, YIELD_CURVE,
        CREDIT_CURVE);

    int nPillars = PILLAR_DATES.length;
    String[] columnHeadings = new String[nPillars + 1];
    for (int i = 0; i < nPillars; i++) {
      columnHeadings[i] = TENORS[i].toString();
    }
    columnHeadings[nPillars] = "Total";

    String[] rowHeadings = new String[nMat];
    double[][] data = new double[nMat][nPillars + 1];
    for (int i = 0; i < nMat; i++) {
      rowHeadings[i] = MATURITIES_6M_STEP[i].format(DATE_FORMAT);
      System.arraycopy(analCS01[i], 0, data[i], 0, nPillars);
      double sum = 0;
      for (int j = 0; j < nPillars; j++) {
        sum += analCS01[i][j];
      }
      data[i][nPillars] = sum;
    }

    System.out.println(dumpLatexTable("Tenors", "CDS Maturities", columnHeadings, rowHeadings, data, 4));
  }

  /**
   * The sensitivity of the PV of 8Y CDSs to the par spreads of the CDSs used to construct the credit curve.  The calculation
   * methods are analytic and forward finite difference (or bump and reprice). The bump in the forward difference is 1bps.<p>
   *  This uses the method dumpLatexTable to format the output into a Latex table. 
   */
  @Test(description = "Demo", enabled = false)
  void analyticVFDCS01Test() {
    LocalDate mat = LocalDate.of(2019, Month.JUNE, 20);
    CdsAnalytic cds = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, mat);
    double[] analCS01 = ANALYTIC_SPREAD_SENSE_CAL
        .bucketedCS01FromCreditCurve(cds, COUPON, PILLAR_CDSS, YIELD_CURVE, CREDIT_CURVE);
    double pCS01 = FD_SPREAD_SENSE_CAL.parallelCS01FromParSpreads(
        cds, COUPON, YIELD_CURVE, PILLAR_CDSS, SPREADS, ONE_BP, ShiftType.ABSOLUTE);
    double[] bCS01 = FD_SPREAD_SENSE_CAL.bucketedCS01FromCreditCurve(
        cds, COUPON, PILLAR_CDSS, YIELD_CURVE, CREDIT_CURVE, ONE_BP);

    int nPillars = PILLAR_DATES.length;
    String[] columnHeadings = new String[nPillars + 2];

    double[][] data = new double[2][nPillars + 2];
    double sumA = 0;
    double sumFD = 0;
    for (int i = 0; i < nPillars; i++) {
      columnHeadings[i] = TENORS[i].toString();
      data[0][i] = analCS01[i];
      sumA += analCS01[i];
      data[1][i] = bCS01[i];
      sumFD += bCS01[i];
    }
    data[0][nPillars] = sumA;
    data[0][nPillars + 1] = sumA;
    data[1][nPillars] = sumFD;
    data[1][nPillars + 1] = pCS01;
    columnHeadings[nPillars] = "Sum";
    columnHeadings[nPillars + 1] = "Parallel";
    String[] rowHeadings = new String[] {"Analytic", "Forward FD"};
    System.out.println(dumpLatexTable("Tenors", "Calculation Method", columnHeadings, rowHeadings, data, 5));
  }

  /**
   * This calculates hedge ratios from bucketed CS01. The main point this demonstrates is that this gives exactly (up to
   * numerical tolerances) as from  pvHedgingDemo, but this involves a lot more work (i.e. calculating the CS01). <p>
   *  <b>Note:</b> This does not appear as a table in the paper. 
   */
  @Test(description = "Demo", enabled = false)
  public void spreadHedgeDemo() {
    LUDecompositionCommons decomp = new LUDecompositionCommons();
    int nPillars = PILLAR_CDSS.length;
    double[] coupons = new double[nPillars];
    Arrays.fill(coupons, COUPON);
    double[][] temp = ANALYTIC_SPREAD_SENSE_CAL.bucketedCS01FromCreditCurve(PILLAR_CDSS, coupons, PILLAR_CDSS, YIELD_CURVE,
        CREDIT_CURVE);
    DoubleMatrix jacT = MA.getTranspose(DoubleMatrix.copyOf(temp));
    LUDecompositionResult decRes = decomp.evaluate(jacT);

    int nMat = MATURITIES_6M_STEP.length;

    double[][] res = new double[nMat][];
    CdsAnalytic[] cds = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, MATURITIES_6M_STEP);
    for (int i = 0; i < nMat; i++) {
      double[] vs = ANALYTIC_SPREAD_SENSE_CAL.bucketedCS01FromCreditCurve(cds[i], COUPON, PILLAR_CDSS, YIELD_CURVE, CREDIT_CURVE);
      res[i] = decRes.solve(vs);
    }
    DoubleMatrix hedge = DoubleMatrix.copyOf(res);
    System.out.println(hedge);
  }

  /**
   * This demonstrates that a constant (flat) hazard rate does not correspond to the same par spread for all maturities - 
   * in this example the spread varies between 59.3 and 59.4bps 
   */
  @Test(description = "Demo", enabled = false)
  public void flatHazardTest() {
    IsdaCompliantCreditCurve flat = new IsdaCompliantCreditCurve(1.0, 0.01);
    int nMat = IMM_DATES.length;
    CdsAnalytic[] cds = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, IMM_DATES);
    System.out.println("Year fraction\tSpread (bps)");
    for (int i = 0; i < nMat; i++) {
      double t = ACT365F.yearFraction(TRADE_DATE, IMM_DATES[i]);
      double s = PRICER_OG_FIX.parSpread(cds[i], YIELD_CURVE, flat);
      System.out.println(t + "\t" + s * TEN_THOUSAND);
    }
  }

  /**
   * Compute bucketed and parallel CS01 by bumping 'flat spreads' by 1bps. That, is the spread at each pillar is set
   * to a constant value, a credit curve is bootstrapped and the CDS prices; the spread and each pillar is then bumped
   * up by 1bps an the calculation repeated. The difference from the base is the (bucketed)  CS01  
   */
  @Test(description = "Demo", enabled = false)
  public void bucketedCS01FromFlatSpreadDemo() {
    MarketQuoteConverter puf_con = new MarketQuoteConverter();
    int nMat = MATURITIES_1Y_STEP.length;
    int nPillars = PILLAR_CDSS.length;
    CdsAnalytic[] cds = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, MATURITIES_1Y_STEP);
    System.out.print("CDS maturities");
    for (int j = 0; j < nPillars; j++) {
      System.out.print("\t" + TENORS[j]);
    }
    System.out.print("\tSum\tParallel\n");
    for (int i = 0; i < nMat; i++) {
      double puf = PRICER.pv(cds[i], YIELD_CURVE, CREDIT_CURVE, COUPON);
      double qs = puf_con.pufToQuotedSpread(cds[i], COUPON, YIELD_CURVE, puf);
      double[] spreads = new double[nPillars];
      Arrays.fill(spreads, qs);
      double[] bCS01 = FD_SPREAD_SENSE_CAL.bucketedCS01FromParSpreads(
          cds[i], COUPON, YIELD_CURVE, PILLAR_CDSS, spreads, ONE_BP, ShiftType.ABSOLUTE);
      double pCS01 = FD_SPREAD_SENSE_CAL.parallelCS01FromParSpreads(
          cds[i], COUPON, YIELD_CURVE, PILLAR_CDSS, spreads, ONE_BP, ShiftType.ABSOLUTE);

      System.out.print(MATURITIES_1Y_STEP[i].format(DATE_FORMAT));
      double sum = 0;
      for (int j = 0; j < nPillars; j++) {
        sum += bCS01[j];
        System.out.print("\t" + bCS01[j]);
      }
      System.out.print("\t" + sum + "\t" + pCS01);
      System.out.print("\n");
    }
  }

  /**
   * Compute bucketed and parallel CS01 by bumping 'quoted' by 1bps - see paper for details 
   */
  @Test(description = "Demo", enabled = false)
  public void bucketedCS01FromQuotedSpreadDemo() {
    MarketQuoteConverter puf_con = new MarketQuoteConverter();
    int nMat = MATURITIES_1Y_STEP.length;
    int nPillars = PILLAR_CDSS.length;
    CdsAnalytic[] cds = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, MATURITIES_1Y_STEP);
    System.out.print("CDS maturities");
    for (int j = 0; j < nPillars; j++) {
      System.out.print("\t" + TENORS[j]);
    }
    System.out.print("\tSum\tParallel\n");
    for (int i = 0; i < nMat; i++) {
      double puf = PRICER.pv(cds[i], YIELD_CURVE, CREDIT_CURVE, COUPON);
      double qs = puf_con.pufToQuotedSpread(cds[i], COUPON, YIELD_CURVE, puf);
      double[] spreads = new double[nPillars];
      Arrays.fill(spreads, qs);
      double[] bCS01 = FD_SPREAD_SENSE_CAL.bucketedCS01FromQuotedSpreads(
          cds[i], COUPON, YIELD_CURVE, PILLAR_CDSS, SPREADS, ONE_BP, ShiftType.ABSOLUTE);
      double pCS01 = FD_SPREAD_SENSE_CAL.parallelCS01FromQuotedSpread(
          cds[i], COUPON, YIELD_CURVE, cds[i], qs, ONE_BP, ShiftType.ABSOLUTE);

      System.out.print(MATURITIES_1Y_STEP[i].format(DATE_FORMAT));
      double sum = 0;
      for (int j = 0; j < nPillars; j++) {
        sum += bCS01[j];
        System.out.print("\t" + bCS01[j]);
      }
      System.out.print("\t" + sum + "\t" + pCS01);
      System.out.print("\n");
    }
  }

  /**
   * The PV sensitivity to zero rates at the yield curve nodes for a set of standard CDSs with a coupon of 100bps and a
   *  recovery rate 40%. The trade date is 13-Jun-2011.
   */
  @Test(description = "Demo", enabled = false)
  public void yieldSenseTest() {
    String[] ycPoints = new String[] {"1M", "3M", "6M", "1Y", "3Y", "5Y", "7Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y"};
    String[] instuments = new String[] {"M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S"};
    double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525,
        0.02696, 0.02825, 0.02931, 0.03017};
    IsdaCompliantYieldCurve yc = makeYieldCurve(TRADE_DATE, SPOT_DATE, ycPoints, instuments, rates, ACT360, D30360,
        Period.ofYears(1));

    int nMat = MATURITIES_1Y_STEP.length;
    int nYCPoints = ycPoints.length;
    CdsAnalytic[] cds = CDS_FACTORY.makeCds(TRADE_DATE, STARTDATE, MATURITIES_1Y_STEP);
    for (int j = 0; j < nYCPoints; j++) {
      System.out.print("\t" + ycPoints[j]);
    }
    System.out.print("\n");
    for (int i = 0; i < nMat; i++) {
      System.out.print(MATURITIES_1Y_STEP[i].format(DATE_FORMAT));
      for (int j = 0; j < nYCPoints; j++) {
        double sense = PRICER_MARKIT_FIX.pvYieldSensitivity(cds[i], yc, CREDIT_CURVE, COUPON, j);
        System.out.print("\t" + sense);
      }
      System.out.print("\n");
    }
  }

  /*
   * Better tables can be produced using the Exce2LaTeX plugin
   */
  private String dumpLatexTable(String heading1, String heading2, String[] columnHeadings, String[] rowHeadings, double[][] data, int dp) {

    ArgChecker.noNulls(columnHeadings, "columnHeadings");
    ArgChecker.noNulls(rowHeadings, "rowHeadings");
    ArgChecker.noNulls(data, "data");
    int nColumns = columnHeadings.length;
    int nRows = rowHeadings.length;
    ArgChecker.isTrue(nColumns == data[0].length, nColumns + "column headings, but data has " + data[0].length + " columns");
    ArgChecker.isTrue(nRows == data.length, nRows + "row headings, but data has " + data.length + " rows");

    String format = "& %." + dp + "f";
    StringBuilder out = new StringBuilder();

    out.append("\\begin{tabular}{");
    for (int i = 0; i < nColumns + 1; i++) {
      out.append("c|");
    }
    out.append("}\n");
    out.append("\\cline{2-" + (nColumns + 1) + "}\n");
    out.append("& \\multicolumn{" + nColumns + "}{c|}{" + heading1 + "}\\\\\n");
    out.append("\\hline\n");
    out.append("\\multicolumn{1}{|c|}{" + heading2 + "}");
    for (int i = 0; i < nColumns; i++) {
      out.append("& " + columnHeadings[i]);
    }
    out.append("\\\\\n");
    out.append("\\hline\n");

    for (int i = 0; i < nRows; i++) {
      out.append("\\multicolumn{1}{|c|}{" + rowHeadings[i] + "}");
      for (int j = 0; j < nColumns; j++) {
        out.append(String.format(format, data[i][j]));
      }
      out.append("\\\\\n");
    }
    out.append("\\hline\n");
    out.append("\\end{tabular}\n");

    return out.toString();
  }

  private IsdaCompliantCreditCurve bumpCurve(IsdaCompliantCreditCurve curve, double amount) {
    double[] r = curve.getKnotZeroRates();
    int n = r.length;
    for (int i = 0; i < n; i++) {
      r[i] += amount;
    }
    return curve.withRates(r);
  }

  private IsdaCompliantCreditCurve tiltCurve(IsdaCompliantCreditCurve curve, double amount) {
    double[] r = curve.getKnotZeroRates();
    int n = r.length;
    for (int i = 0; i < n; i++) {
      r[i] += +(amount / (n / 2)) * (i - n / 2);
    }
    return curve.withRates(r);
  }
}
