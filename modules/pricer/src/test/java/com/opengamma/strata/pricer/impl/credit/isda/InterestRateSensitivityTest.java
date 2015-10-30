/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * 
 */
@Test
public class InterestRateSensitivityTest {

  private static final IsdaCompliantCreditCurveBuilder BUILDER = new FastCreditCurveBuilder();
  private static final HolidayCalendar DEFAULT_CALENDAR = HolidayCalendars.SAT_SUN;
  private static final DayCount ACT_365 = DayCounts.ACT_365F;
  private static final DayCount ACT360 = DayCounts.ACT_360;

  private static final LocalDate TODAY = LocalDate.of(2011, 5, 11);
  private static final LocalDate EFFECTIVE_DATE = TODAY.plusDays(1);
  private static final LocalDate CASH_SETTLE_DATE = DEFAULT_CALENDAR.shift(TODAY, 3);
  private static final double RECOVERY_RATE = 0.4;

  private static final LocalDate PROTECTION_STATE_DATE = LocalDate.of(2013, 2, 3); // Seasoned CDS
  private static final LocalDate PROTECTION_END_DATE = LocalDate.of(2018, 3, 20);
  private static final double DEAL_SPREAD = 112;
  private static final CdsAnalytic CDS;

  private static final LocalDate[] PAR_SPD_DATES = new LocalDate[] {LocalDate.of(2013, 6, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2015, 3, 20),
    LocalDate.of(2016, 3, 20), LocalDate.of(2018, 3, 20), LocalDate.of(2023, 3, 20) };
  private static final double[] PAR_SPREADS = new double[] {60, 75, 100, 115, 110, 95, 80 };
  private static final int NUM_MARKET_CDS = PAR_SPD_DATES.length;
  private static final CdsAnalytic[] MARKET_CDS = new CdsAnalytic[NUM_MARKET_CDS];

  private static IsdaCompliantYieldCurve YIELD_CURVE;
  private static IsdaCompliantCreditCurve CREDIT_CURVE;

  private static final double ONE_BPS = 1e-4;

  static {
    final double[] zeroRate = new double[] {0.06, 0.05, 0.065, 0.07, 0.06, 0.055, 0.055 };
    final double[] time = new double[] {0.1, 1., 2., 3., 5., 9., 20. };
    YIELD_CURVE = new IsdaCompliantYieldCurve(time, zeroRate);

    final boolean payAccOndefault = true;
    final Period tenor = Period.ofMonths(3);
    final StubConvention stubType = StubConvention.SHORT_INITIAL;
    final boolean protectionStart = true;

    CDS = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, PROTECTION_STATE_DATE, PROTECTION_END_DATE, payAccOndefault, tenor, stubType, protectionStart, RECOVERY_RATE);

    for (int i = 0; i < NUM_MARKET_CDS; i++) {
      MARKET_CDS[i] = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, TODAY, PAR_SPD_DATES[i], payAccOndefault, tenor, stubType, protectionStart, RECOVERY_RATE);
    }

    final double[] mrkSpreads = new double[NUM_MARKET_CDS];
    for (int i = 0; i < NUM_MARKET_CDS; i++) {
      mrkSpreads[i] = PAR_SPREADS[i] * ONE_BPS;
    }
    CREDIT_CURVE = BUILDER.calibrateCreditCurve(MARKET_CDS, mrkSpreads, YIELD_CURVE);
  }

  private static IsdaCompliantYieldCurveBuild YIELD_CURVE_BUILDER;
  private static int NUM_INSTRUMENTS;
  static {
    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12 };
    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30 };
    final int nMoneyMarket = mmMonths.length;
    final int nSwaps = swapYears.length;
    NUM_INSTRUMENTS = nMoneyMarket + nSwaps;

    final IsdaInstrumentTypes[] instrumentTypes = new IsdaInstrumentTypes[NUM_INSTRUMENTS];
    final Period[] tenors = new Period[NUM_INSTRUMENTS];
    for (int i = 0; i < nMoneyMarket; i++) {
      instrumentTypes[i] = IsdaInstrumentTypes.MONEY_MARKET;
      tenors[i] = Period.ofMonths(mmMonths[i]);
    }
    for (int i = nMoneyMarket; i < NUM_INSTRUMENTS; i++) {
      instrumentTypes[i] = IsdaInstrumentTypes.SWAP;
      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
    }

    YIELD_CURVE_BUILDER = new IsdaCompliantYieldCurveBuild(TODAY, TODAY.plusDays(2), instrumentTypes, tenors, ACT360, ACT360, Period.ofMonths(6), ACT_365, BusinessDayConventions.MODIFIED_FOLLOWING,
        DEFAULT_CALENDAR);
  }

  /**
   * 
   */
  @Test
  public void parallelIR01Test() {
    /*
     * Not needed if exactly the same path taken
     */
    final double tol = 1.e-12;

    final InterestRateSensitivityCalculator calc = new InterestRateSensitivityCalculator();
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer();
    final double pv = pricer.pv(CDS, YIELD_CURVE, CREDIT_CURVE, DEAL_SPREAD * ONE_BPS, CdsPriceType.DIRTY);
    final double res = calc.parallelIR01(CDS, DEAL_SPREAD * ONE_BPS, CREDIT_CURVE, YIELD_CURVE);

    final double[] zeroRate = YIELD_CURVE.getKnotZeroRates();
    final double[] time = YIELD_CURVE.getKnotTimes();

    final int nKnots = time.length;
    final double[] bumpedRate = new double[nKnots];
    for (int i = 0; i < nKnots; ++i) {
      bumpedRate[i] = zeroRate[i] + ONE_BPS;
    }
    final IsdaCompliantYieldCurve bumpedYield = new IsdaCompliantYieldCurve(time, bumpedRate);
    final double bumpedPv = pricer.pv(CDS, bumpedYield, CREDIT_CURVE, DEAL_SPREAD * ONE_BPS, CdsPriceType.DIRTY);

    assertEquals(bumpedPv - pv, res, tol);

    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
      0.03367, 0.03419, 0.03411, 0.03412 };
    final double res1 = calc.parallelIR01(CDS, DEAL_SPREAD * ONE_BPS, CREDIT_CURVE, YIELD_CURVE_BUILDER, rates);

    final double[] bumpedRates = new double[NUM_INSTRUMENTS];
    for (int i = 0; i < NUM_INSTRUMENTS; ++i) {
      bumpedRates[i] = rates[i] + ONE_BPS;
    }
    final IsdaCompliantYieldCurve baseYC = YIELD_CURVE_BUILDER.build(rates);
    final IsdaCompliantYieldCurve bumpedYC = YIELD_CURVE_BUILDER.build(bumpedRates);
    final double base = pricer.pv(CDS, baseYC, CREDIT_CURVE, DEAL_SPREAD * ONE_BPS, CdsPriceType.CLEAN);
    final double bumped = pricer.pv(CDS, bumpedYC, CREDIT_CURVE, DEAL_SPREAD * ONE_BPS, CdsPriceType.CLEAN);

    assertEquals(bumped - base, res1, tol);
  }

  /**
   * 
   */
  @Test
  public void bucketedIR01Test() {
    /*
     * Not needed if exactly the same path taken
     */
    final double tol = 1.e-12;

    final AccrualOnDefaultFormulae form = AccrualOnDefaultFormulae.CORRECT;
    final InterestRateSensitivityCalculator calc = new InterestRateSensitivityCalculator(form);
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer(form);
    final double pv = pricer.pv(CDS, YIELD_CURVE, CREDIT_CURVE, DEAL_SPREAD * ONE_BPS, CdsPriceType.DIRTY);
    final double[] res = calc.bucketedIR01(CDS, DEAL_SPREAD * ONE_BPS, CREDIT_CURVE, YIELD_CURVE);

    final double[] zeroRate = YIELD_CURVE.getKnotZeroRates();
    final double[] time = YIELD_CURVE.getKnotTimes();

    final int nKnots = time.length;
    for (int i = 0; i < nKnots; ++i) {
      final double[] bumpedRate = Arrays.copyOf(zeroRate, nKnots);
      bumpedRate[i] += ONE_BPS;
      final IsdaCompliantYieldCurve bumpedYield = new IsdaCompliantYieldCurve(time, bumpedRate);
      final double bumpedPv = pricer.pv(CDS, bumpedYield, CREDIT_CURVE, DEAL_SPREAD * ONE_BPS, CdsPriceType.DIRTY);
      assertEquals(bumpedPv - pv, res[i], tol);
    }

    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
      0.03367, 0.03419, 0.03411, 0.03412 };
    final double[] res1 = calc.bucketedIR01(CDS, DEAL_SPREAD * ONE_BPS, CREDIT_CURVE, YIELD_CURVE_BUILDER, rates);
    final IsdaCompliantYieldCurve baseYC = YIELD_CURVE_BUILDER.build(rates);
    final double base = pricer.pv(CDS, baseYC, CREDIT_CURVE, DEAL_SPREAD * ONE_BPS, CdsPriceType.CLEAN);
    for (int i = 0; i < NUM_INSTRUMENTS; ++i) {
      final double[] bumpedRates = Arrays.copyOf(rates, NUM_INSTRUMENTS);
      bumpedRates[i] += ONE_BPS;
      final IsdaCompliantYieldCurve bumpedYC = YIELD_CURVE_BUILDER.build(bumpedRates);
      final double bumped = pricer.pv(CDS, bumpedYC, CREDIT_CURVE, DEAL_SPREAD * ONE_BPS, CdsPriceType.CLEAN);
      assertEquals(bumped - base, res1[i], tol);
    }
  }

}
