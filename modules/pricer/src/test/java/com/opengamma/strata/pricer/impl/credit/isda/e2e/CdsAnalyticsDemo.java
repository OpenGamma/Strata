/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda.e2e;

import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getNextIMMDate;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getPrevIMMDate;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.Locale;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.pricer.impl.credit.isda.AnalyticCdsPricer;
import com.opengamma.strata.pricer.impl.credit.isda.AnalyticSpreadSensitivityCalculator;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalytic;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalyticFactory;
import com.opengamma.strata.pricer.impl.credit.isda.CdsParSpread;
import com.opengamma.strata.pricer.impl.credit.isda.CdsPriceType;
import com.opengamma.strata.pricer.impl.credit.isda.CdsQuoteConvention;
import com.opengamma.strata.pricer.impl.credit.isda.CdsQuotedSpread;
import com.opengamma.strata.pricer.impl.credit.isda.FastCreditCurveBuilder;
import com.opengamma.strata.pricer.impl.credit.isda.FiniteDifferenceSpreadSensitivityCalculator;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaBaseTest;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurveBuilder;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurveBuild;
import com.opengamma.strata.pricer.impl.credit.isda.MarketQuoteConverter;
import com.opengamma.strata.pricer.impl.credit.isda.PointsUpFront;
import com.opengamma.strata.pricer.impl.credit.isda.SuperFastCreditCurveBuilder;
import com.opengamma.strata.pricer.impl.credit.isda.YieldCurveProvider;

/**
 * The purpose of this class is the demonstrate the API for OpenGamma's implementation of the ISDA standard and extensions
 * we have built. Each method (test) demonstrates a new feature and outputs some results to the console - ideally they should 
 * be read and executed in order.
 */
@Test(enabled = false)
public class CdsAnalyticsDemo extends IsdaBaseTest {

  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 6, 30);

  private static final double[] RATES = new double[] {
      0.001515, 0.001965, 0.002346, 0.003268, 0.005451, 0.005875, 0.010035,
      0.013935, 0.017125, 0.01973, 0.021855, 0.02364, 0.025135, 0.02638,
      0.028405, 0.030435, 0.032225, 0.033055, 0.033435};

  private static final IsdaCompliantYieldCurve YIELD_CURVE = YieldCurveProvider.makeUSDCurve(TRADE_DATE, RATES);

  private static final LocalDate[] TEST_DATES = new LocalDate[] {LocalDate.of(2014, 7, 2), LocalDate.of(2014, 7, 10),
      LocalDate.of(2014, 7, 30), LocalDate.of(2014, 9, 1), LocalDate.of(2014, 10, 1),
      LocalDate.of(2014, 11, 1), LocalDate.of(2014, 12, 1), LocalDate.of(2015, 1, 1), LocalDate.of(2016, 1, 1),
      LocalDate.of(2017, 1, 1), LocalDate.of(2018, 1, 1)};

  /**
   * Show the construction of a ISDA model yield curve. {@link IsdaCompliantYieldCurveBuild} is the class that does the actual work, but 
   * {@link YieldCurveProvider} provides tools to set up standard (i.e. USD, EUR, GBP & JPY) yield curves.<p>
   * The main  point of this example is to show what is meant by a <i>shifted</i> yield curve
   */
  public void yieldCurveDemo() {
    System.out.println("\nyieldCurveDemo");

    //The yield curve is snapped on 29-Jun-2014 and the spot date is 2-Jul-2014 (three working days on)
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(TRADE_DATE.minusDays(1), 3);

    //USD conventions 
    final String[] periods = new String[] {
        "1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "25Y", "30Y"};
    final String[] types = new String[] {
        "M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S"};
    final DayCount mmDCC = ACT360;
    final DayCount swapDCC = D30360;
    final Period swapInterval = Period.ofMonths(6);

    //build shifted yield curve, by specifying all parameters 
    final IsdaCompliantYieldCurve yc_shifted = YieldCurveProvider.makeYieldCurve(
        TRADE_DATE, spotDate, periods, types, RATES, mmDCC, swapDCC, swapInterval, DEFAULT_CALENDAR);
    //cannot use equals directly as the curves have different names 
    assertTrue(Arrays.equals(YIELD_CURVE.getKnotTimes(), yc_shifted.getKnotTimes()));
    assertTrue(Arrays.equals(YIELD_CURVE.getKnotZeroRates(), yc_shifted.getKnotZeroRates()));

    //build unshifted yield curve 
    final IsdaCompliantYieldCurve yc_unshifted = YieldCurveProvider.makeYieldCurve(
        spotDate, spotDate, periods, types, RATES, mmDCC, swapDCC, swapInterval, DEFAULT_CALENDAR);

    //check the shifted and unshifted curves are equivalent - po is the discount factor between the CDS trade date and
    //the spot date of the yield curve instruments 
    final double p0 = yc_shifted.getDiscountFactor(ACT365F.yearFraction(TRADE_DATE, spotDate));
    final int n = TEST_DATES.length;
    for (int i = 0; i < n; i++) {
      final double t1 = ACT365F.yearFraction(TRADE_DATE, TEST_DATES[i]);
      final double t2 = ACT365F.yearFraction(spotDate, TEST_DATES[i]);
      final double df1 = YIELD_CURVE.getDiscountFactor(t1);
      final double df2 = p0 * yc_unshifted.getDiscountFactor(t2);
      assertEquals(df1, df2, 1e-15);
    }

    //this should be cut and pasted into Excel to view the curve 
    final int nSamples = 100;
    System.out.println("Time\tDiscount Factor\tZero Rate\tForward Rate");
    for (int i = 0; i < nSamples; i++) {
      final double t = i / (nSamples - 1.0) * 12.0; //view the curve out to 12 years
      final double df = YIELD_CURVE.getDiscountFactor(t);
      final double r = YIELD_CURVE.getZeroRate(t);
      final double f = YIELD_CURVE.getForwardRate(t);
      System.out.println(t + "\t" + df + "\t" + r + "\t" + f);
    }
  }

  /**
   * Construct an object that represents a SNAC 5Y single-name CDS
   */
  public void cdsConstructionDemo() {
    System.out.println("\nConstructionDemo");

    //CDSAnalyticFactory defaults to SNAC setting (with recovery rate 40%). All of these can be change using 'with' methods.
    //In this case we change the recovery rate to 30%
    final double recoveryRate = 0.3;
    final CdsAnalyticFactory factory = new CdsAnalyticFactory().withRecoveryRate(recoveryRate);
    final Period cdsTerm = Period.ofYears(5);
    final CdsAnalytic cds = factory.makeImmCds(TRADE_DATE, cdsTerm);
    assertEquals(recoveryRate, 1 - cds.getLGD(), 1e-15);
    assertEquals(11, cds.getAccuredDays());

    //can change the recovery rate without recomputing all the date logic 
    final CdsAnalytic cds65 = cds.withRecoveryRate(0.65);
    assertEquals(0.65, 1 - cds65.getLGD(), 1e-15);
    assertEquals(11, cds65.getAccuredDays());

    //the full API is consistent with the ISDA C code (and the xll plugin) 

    //standard CDS settings 
    final LocalDate stepinDate = TRADE_DATE.plusDays(1); // 1-Jul-2014
    final LocalDate cashSettleDate = DEFAULT_CALENDAR.shift(TRADE_DATE, 3); // 3-Jul-2014
    final LocalDate accStartDate = FOLLOWING.adjust(getPrevIMMDate(TRADE_DATE), DEFAULT_CALENDAR); // 20-Jun-2014
    final LocalDate endDate = getNextIMMDate(TRADE_DATE).plus(cdsTerm); // 20-Sep-2019

    final boolean payAccOnDefault = true;
    final Period paymentInterval = Period.ofMonths(3);
    final StubConvention stub = StubConvention.SHORT_INITIAL; //Irrelevant for SNAC where accrual start is previous IMM date 
    final boolean protectionStart = true;

    final CdsAnalytic cds2 = new CdsAnalytic(
        TRADE_DATE, stepinDate, cashSettleDate, accStartDate, endDate, payAccOnDefault, paymentInterval,
        stub, protectionStart, recoveryRate, FOLLOWING, DEFAULT_CALENDAR, ACT360);
    assertTrue(cds.equals(cds2));

    //Note: Can use exactly the same code to setup 'legacy' CDS, i.e. accStartDate T+1, or some bespoke CDS
    //Nowhere has the coupon appeared - the coupon is an input to the pricing, or in the case of par instruments, the par spread
    //is a calculation output 
  }

  /**
   * Standard ISDA up-front model for pricing CDS. Conversion between Points-Up-Front (PUF) and Spread quotes 
   */
  public void upfrontModelDemo() {
    System.out.println("\nupfrontModelDemo");

    final CdsAnalyticFactory factory = new CdsAnalyticFactory();
    final Period cdsTerm = Period.ofYears(3);
    final CdsAnalytic cds = factory.makeImmCds(TRADE_DATE, cdsTerm);

    final double coupon = 100 * ONE_BP;
    final double lambda = 7e-3; //a hazard rate of 0.7%
    final IsdaCompliantCreditCurve constCreditCurve = new IsdaCompliantCreditCurve(1.0, lambda);

    final AnalyticCdsPricer pricer = new AnalyticCdsPricer();
    final double puf = pricer.pv(cds, YIELD_CURVE, constCreditCurve, coupon);
    final double spread = pricer.parSpread(cds, YIELD_CURVE, constCreditCurve); //quoted spread or trade level

    //In this case the credit quality is good (hazard rate of 0.7%), so a coupon of 100bps is too high; the result
    //is a negative Points-Up-Front of -1.87%, and a corresponding quoted spread of 41.5bps
    System.out.format(Locale.ENGLISH, "PUF: %.4f%%, Quoted Spread: %.3fbps\n", puf * ONE_HUNDRED, spread * TEN_THOUSAND);

    //a CDS has a market quoted given as PUF and an equivalent quoted spread is required - this requires solving for the
    //hazard rate
    final IsdaCompliantCreditCurveBuilder creditCurveBuilder = new FastCreditCurveBuilder();
    final IsdaCompliantCreditCurve fittedConstCreditCurve = creditCurveBuilder
        .calibrateCreditCurve(cds, coupon, YIELD_CURVE, puf);
    final double quotedSpread = pricer.parSpread(cds, YIELD_CURVE, fittedConstCreditCurve);
    assertEquals(spread, quotedSpread, 1e-15);

    //going the other way also involves solving for a constant hazard rate 
    final IsdaCompliantCreditCurve fittedConstCreditCurve2 = creditCurveBuilder.calibrateCreditCurve(cds, spread, YIELD_CURVE);
    final double puf2 = pricer.pv(cds, YIELD_CURVE, fittedConstCreditCurve2, coupon);
    assertEquals(puf, puf2, 1e-15);

    //we have some tools to hide the calibration step
    final MarketQuoteConverter converter = new MarketQuoteConverter();
    System.out.print("\n");
    for (int i = 0; i < 21; i++) {
      final PointsUpFront pufQuote = new PointsUpFront(coupon, (-2.0 + 4.0 * i / 20.0) * ONE_PC);
      final CdsQuotedSpread spreadQuote = converter.convert(cds, pufQuote, YIELD_CURVE);
      System.out
          .format(
              Locale.ENGLISH, "PUF: %.4f%%," + "Quoted Spread: %.3fbps\n",
              pufQuote.getPointsUpFront() * ONE_HUNDRED,
              spreadQuote.getQuotedSpread() * TEN_THOUSAND);
    }

  }

  /**
   * We start with a quoted spread (or trade level) and compute the various cash amounts that are shown on BBG CDSW and/or 
   * Markit calculator 
   */

  public void cashSettlementDemo() {
    System.out.println("\ncashSettlementDemo");

    final CdsAnalyticFactory factory = new CdsAnalyticFactory();
    final Period cdsTerm = Period.ofYears(5);
    final CdsAnalytic cds = factory.makeImmCds(TRADE_DATE, cdsTerm);
    final double notional = 1e7; //10MM
    final double coupon = 100 * ONE_BP;

    //accured for the buyer of protection is conventionally shown as negative amount (e.g. on BBG CDSW) 
    //Here we show it as an absolute value which is consistent with is ISDA C code and the Markit calculator 
    final double accruedAmount = notional * cds.getAccruedPremium(coupon);
    System.out.format(Locale.ENGLISH, "Accrued Amt: %.2f\n", accruedAmount);

    final double tradeLevel = 75 * ONE_BP;
    final MarketQuoteConverter converter = new MarketQuoteConverter();
    final double puf = converter.quotedSpreadToPUF(cds, coupon, YIELD_CURVE, tradeLevel);

    final double principle = notional * puf;
    System.out.format(Locale.ENGLISH, "Principle: %.2f\n", principle);

    //The cash settlement is the amount actually paid (to enter the CDS contract) on the cash settlement date (3-Jul_2014)
    //here we subtract a positive accrued amount rather than add a negative accrued 
    final double cashSettlement = principle - accruedAmount;
    System.out.format(Locale.ENGLISH, "cash Settlement: %.2f\n", cashSettlement);

    //The market value is the value of the CDS on the trade date
    final LocalDate cashSettleDate = DEFAULT_CALENDAR.shift(TRADE_DATE, 3); // 3-Jul-2014
    final double df = YIELD_CURVE.getDiscountFactor(ACT365F.yearFraction(TRADE_DATE, cashSettleDate));
    final double marketValue = df * cashSettlement;
    System.out.format(Locale.ENGLISH, "Market value: %.2f\n", marketValue);

    //can do all this explicitly with a credit curve and pricer 
    final IsdaCompliantCreditCurveBuilder creditCurveBuilder = new FastCreditCurveBuilder();
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer();
    final IsdaCompliantCreditCurve fittedConstCreditCurve = creditCurveBuilder.calibrateCreditCurve(cds, tradeLevel, YIELD_CURVE);

    final double principle2 = notional * pricer.pv(cds, YIELD_CURVE, fittedConstCreditCurve, coupon, CdsPriceType.CLEAN);
    final double cashSettlement2 = notional * pricer.pv(cds, YIELD_CURVE, fittedConstCreditCurve, coupon, CdsPriceType.DIRTY);
    final double marketValue2 = notional * pricer.pv(cds, YIELD_CURVE, fittedConstCreditCurve, coupon, CdsPriceType.DIRTY, 0.0);
    assertEquals(principle, principle2, 1e-15 * notional);
    assertEquals(cashSettlement, cashSettlement2, 1e-15 * notional);
    assertEquals(marketValue, marketValue2, 1e-15 * notional);

    //finally we can look at the individual legs 
    //premium leg per unit of coupon (and unit notional) - often quoted per basis point of coupon 
    //AKA RPV01 or duration (its value should be slightly less than the CDS time to maturity)  
    final double annuity = pricer.annuity(cds, YIELD_CURVE, fittedConstCreditCurve);
    System.out.format(Locale.ENGLISH, "annuity: %.3f, time to maturity: %.3f\n", annuity, cds.getProtectionEnd());

    final double protectionLeg = notional * pricer.protectionLeg(cds, YIELD_CURVE, fittedConstCreditCurve);
    System.out.format(Locale.ENGLISH, "Protection leg: %.2f\n", protectionLeg);

    //this can be a useful way of expressing the principle 
    final double principle3 = notional * (tradeLevel - coupon) * annuity;
    assertEquals(principle, principle3, 1e-15 * notional);
  }

  /**
   * Build a full credit curve (i.e. time dependent hazard rate) from par quoted CDS
   */
  public void creditCurveBuildDemo() {
    System.out.println("\ncreditCurveBuildDemo");

    final Period[] tenors = new Period[] {
        Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3),
        Period.ofYears(5), Period.ofYears(7), Period.ofYears(10)};
    final double[] parSpreads = new double[] {0.008, 0.0088, 0.013, 0.017, 0.018, 0.019};

    final CdsAnalyticFactory factory = new CdsAnalyticFactory(); //Default is 40% recovery

    //the calibration instruments have accrual starting on last IMM date rather than T+1 (we'll show an example of a 'legacy' CDS later)
    final CdsAnalytic[] calibrationCDS = factory.makeImmCds(TRADE_DATE, tenors);

    final IsdaCompliantCreditCurveBuilder creditCurveBuilder = new SuperFastCreditCurveBuilder(); //its faster than FastCreditCurveBuilder
    final IsdaCompliantCreditCurve creditCurve =
        creditCurveBuilder.calibrateCreditCurve(calibrationCDS, parSpreads, YIELD_CURVE);

    //check all the calibration instruments do indeed price back to zero (i.e. internally consistent) 
    final int n = tenors.length;
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer();
    for (int i = 0; i < n; i++) {
      final double p = pricer.pv(calibrationCDS[i], YIELD_CURVE, creditCurve, parSpreads[i]);
      assertEquals(0, p, 1e-15);
      final double s = pricer.parSpread(calibrationCDS[i], YIELD_CURVE, creditCurve);
      assertEquals(parSpreads[i], s, 1e-15);
    }

    //price a 4Y CDS off the credit curve (assume RR is also 40%)
    final CdsAnalytic cds4Y = factory.makeImmCds(TRADE_DATE, Period.ofYears(4));
    //A SNAC will have a coupon of 100bps 
    final double coupon = 100 * ONE_BP;
    final double annuity = pricer.annuity(cds4Y, YIELD_CURVE, creditCurve);
    final double protLeg = pricer.protectionLeg(cds4Y, YIELD_CURVE, creditCurve);
    final double parSpread = protLeg / annuity;
    final double puf1 = protLeg - coupon * annuity;
    final double puf2 = (parSpread - coupon) * annuity;
    assertEquals(puf1, puf2, 1e-15);
    System.out.format(Locale.ENGLISH, "4Y par spread: %.3f, PUF: %.3f%%\n", parSpread * TEN_THOUSAND, puf1 * ONE_HUNDRED);

    //this should be cut and pasted into Excel to view the curve 
    final int nSamples = 100;
    System.out.println("\nTime\tDiscount Factor\tZero Rate\tForward Rate");
    for (int i = 0; i < nSamples; i++) {
      final double t = i / (nSamples - 1.0) * 12.0; //view the curve out to 12 years
      final double df = creditCurve.getSurvivalProbability(t); //can also use getDiscountFactor - it's the same thing
      final double r = creditCurve.getHazardRate(t); // ditto  getZeroRate(t);
      final double f = creditCurve.getForwardRate(t);
      System.out.println(t + "\t" + df + "\t" + r + "\t" + f);
    }
  }

  /**
   * We can also build a full credit curve from PUF (the standard ISDA model can't do this)
   */
  public void creditCurveBuildFromPUFDemo() {
    System.out.println("\ncreditCurveBuildFromPUFDemo");

    final Period[] tenors = new Period[] {
        Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5),
        Period.ofYears(7), Period.ofYears(10)};
    final double[] puf = new double[] {-0.0, -0.003, -0.01, 0.01, 0.015, 0.019};
    final int n = puf.length;
    final double[] coupons = new double[n];
    Arrays.fill(coupons, 100 * ONE_BP);

    final CdsAnalyticFactory factory = new CdsAnalyticFactory(); //Default is 40% recovery
    final CdsAnalytic[] calibrationCDS = factory.makeImmCds(TRADE_DATE, tenors);

    final IsdaCompliantCreditCurveBuilder creditCurveBuilder = new SuperFastCreditCurveBuilder();
    final IsdaCompliantCreditCurve creditCurve =
        creditCurveBuilder.calibrateCreditCurve(calibrationCDS, coupons, YIELD_CURVE, puf);

    //check all the calibration instruments do indeed price back to the market PUF (i.e. internally consistent) 
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer();
    for (int i = 0; i < n; i++) {
      final double p = pricer.pv(calibrationCDS[i], YIELD_CURVE, creditCurve, coupons[i]);
      assertEquals(puf[i], p, 1e-14);
    }

    //this should be cut and pasted into Excel to view the curve 
    final int nSamples = 100;
    System.out.println("\nTime\tDiscount Factor\tZero Rate\tForward Rate");
    for (int i = 0; i < nSamples; i++) {
      final double t = i / (nSamples - 1.0) * 12.0; //view the curve out to 12 years
      final double df = creditCurve.getSurvivalProbability(t); //can also use getDiscountFactor - it's the same thing
      final double r = creditCurve.getHazardRate(t); // ditto  getZeroRate(t);
      final double f = creditCurve.getForwardRate(t);
      System.out.println(t + "\t" + df + "\t" + r + "\t" + f);
    }
  }

  /**
   * Understanding the difference between par and quoted spreads.
   * Typically par spread and quoted spreads will differ by a few basis points
   */
  public void parVsQuotedSpreadDemo() {
    System.out.println("\nparVsQuotedSpreadDemo");

    //build a credit curve from par spreads 
    final Period[] tenors = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5),
        Period.ofYears(7), Period.ofYears(10)};
    final double[] parSpreads = new double[] {0.008, 0.0088, 0.013, 0.017, 0.018, 0.019};

    final CdsAnalyticFactory factory = new CdsAnalyticFactory(); //Default is 40% recovery
    final CdsAnalytic[] calibrationCDS = factory.makeImmCds(TRADE_DATE, tenors);

    final IsdaCompliantCreditCurveBuilder creditCurveBuilder = new SuperFastCreditCurveBuilder(); //its faster than FastCreditCurveBuilder
    final IsdaCompliantCreditCurve creditCurve =
        creditCurveBuilder.calibrateCreditCurve(calibrationCDS, parSpreads, YIELD_CURVE);

    final double coupon = 100 * ONE_BP; //again use standard coupon of 100bps 
    final int n = tenors.length;
    final double[] puf = new double[n];
    // price standard CDS with a coupon of 100bps - these are different from the calibration instruments
    // which have coupons equal to their par spreads
    for (int i = 0; i < n; i++) {
      puf[i] = PRICER.pv(calibrationCDS[i], YIELD_CURVE, creditCurve, coupon);
    }

    final MarketQuoteConverter converter = new MarketQuoteConverter();
    //this does an instrument-by-instrument conversion using separate constant hazard rates  
    final double[] quotedSpreads = converter.pufToQuotedSpreads(calibrationCDS, coupon, YIELD_CURVE, puf);
    System.out.println("Par Spread\tPUF\tQuoted Spread");
    for (int i = 0; i < n; i++) {
      System.out.format(
          Locale.ENGLISH,
          "%.0fbps\t%.3f%%\t%.3fbps\n", parSpreads[i] * TEN_THOUSAND, puf[i] * ONE_HUNDRED, quotedSpreads[i] * TEN_THOUSAND);
    }
  }

  /**
   * To build a credit curve from quoted spreads, one should first convert the quoted spread to PUF. Systems tend to not do 
   * this, but just treat quoted spreads as par spreads. We can build curves from mixed quote types 
   */
  public void mixedQuoteCurveBuildDemo() {
    System.out.println("\nmixedQuoteCurveBuildDemo");

    final Period[] tenors = new Period[] {
        Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5),
        Period.ofYears(7), Period.ofYears(10)};
    final int n = tenors.length;
    final double coupon = 100 * ONE_BP;
    final CdsQuoteConvention[] quotes = new CdsQuoteConvention[n];
    quotes[0] = new CdsParSpread(80 * ONE_BP);
    quotes[1] = new PointsUpFront(coupon, -0.3 * ONE_PC);
    quotes[2] = new PointsUpFront(coupon, 0.5 * ONE_PC);
    quotes[3] = new CdsQuotedSpread(coupon, 130 * ONE_BP);
    quotes[4] = new CdsParSpread(150 * ONE_BP);
    quotes[5] = new CdsQuotedSpread(coupon, 225 * ONE_BP);

    final CdsAnalyticFactory factory = new CdsAnalyticFactory();
    final CdsAnalytic[] calibrationCDS = factory.makeImmCds(TRADE_DATE, tenors);

    //make the par spread quotes T+1 accrual
    calibrationCDS[0] = factory.makeCds(TRADE_DATE, TRADE_DATE.plusDays(1), LocalDate.of(2015, 1, 20));
    calibrationCDS[4] = factory.makeCds(TRADE_DATE, TRADE_DATE.plusDays(1), LocalDate.of(2021, 9, 20));
    assertEquals(0, calibrationCDS[0].getAccuredDays()); //check no accrual 

    //can have a recovery rate term structure 
    for (int i = 0; i < n; i++) {
      final double rr = 0.7 - 0.5 * i / (n - 1.0);
      calibrationCDS[i] = calibrationCDS[i].withRecoveryRate(rr);
    }

    //get out a horrible looking curve (because I have just made up some data)
    final IsdaCompliantCreditCurve creditCurve = CREDIT_CURVE_BUILDER.calibrateCreditCurve(calibrationCDS, quotes, YIELD_CURVE);

    //this should be cut and pasted into Excel to view the curve 
    final int nSamples = 100;
    System.out.println("\nTime\tDiscount Factor\tZero Rate\tForward Rate");
    for (int i = 0; i < nSamples; i++) {
      final double t = i / (nSamples - 1.0) * 12.0; //view the curve out to 12 years
      final double df = creditCurve.getSurvivalProbability(t); //can also use getDiscountFactor - it's the same thing
      final double r = creditCurve.getHazardRate(t); // ditto  getZeroRate(t);
      final double f = creditCurve.getForwardRate(t);
      System.out.println(t + "\t" + df + "\t" + r + "\t" + f);
    }
  }

  //**********************************************************************************
  // Greeks
  //**********************************************************************************

  /**
   * Credit spread sensitivity (CS01 or Credit DV01)
   */
  public void cs01Demo() {
    System.out.println("\ncs01Demo");

    final FiniteDifferenceSpreadSensitivityCalculator sensCal = new FiniteDifferenceSpreadSensitivityCalculator();
    final AnalyticSpreadSensitivityCalculator anSensCal = new AnalyticSpreadSensitivityCalculator();
    final CdsAnalyticFactory factory = new CdsAnalyticFactory().withRecoveryRate(0.25);
    final CdsAnalytic cds5Y = factory.makeImmCds(TRADE_DATE, Period.ofYears(5));

    final double coupon = 500 * ONE_BP;
    final double tradeLevel = 367 * ONE_BP;
    final CdsQuoteConvention quotedSpread = new CdsQuotedSpread(coupon, tradeLevel);
    final double notional = 1e7; //10MM

    //CS01 is defined as a change in the principle for a one basis point increase in the quoted spread.
    //We can also calculate analytic sensitivity of the principle to the quoted spread, however it is the first
    //number (a forward finite difference) which is quoted on CDSW and Markit calculator 
    final double cs01 = notional * ONE_BP * sensCal.parallelCS01(cds5Y, quotedSpread, YIELD_CURVE, ONE_BP);
    final double anCS01 = notional * ONE_BP * anSensCal.parallelCS01(cds5Y, quotedSpread, YIELD_CURVE);
    System.out.format(Locale.ENGLISH, "CS01: %.2f, analytic sense:  %.2f \n", cs01, anCS01);

    //we can also compute a CS01 directly from PUF
    final MarketQuoteConverter converter = new MarketQuoteConverter();
    final PointsUpFront puf = converter.convert(cds5Y, (CdsQuotedSpread) quotedSpread, YIELD_CURVE);
    System.out.format(Locale.ENGLISH, "PUF:  %.2f%%\n", puf.getPointsUpFront() * ONE_HUNDRED);

    //this converted the PUF to a quoted spread behind the
    final double cs01FromPUF = notional * ONE_BP * sensCal.parallelCS01(cds5Y, puf, YIELD_CURVE, ONE_BP);
    assertEquals(cs01, cs01FromPUF, 1e-15 * notional);

  }

}
