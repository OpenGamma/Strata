/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Test.
 */
@Test
public class UpfrontFlatTest {
  private static final HolidayCalendar DEFAULT_CALENDAR = HolidayCalendars.SAT_SUN;

  private static final DayCount ACT365 = DayCounts.ACT_365F;
  private static final DayCount ACT360 = DayCounts.ACT_360;
  private static final double NOTIONAL = 1e7;

  private static final BusinessDayConvention FOLLOWING = BusinessDayConventions.FOLLOWING;

  private static final LocalDate TODAY = LocalDate.of(2008, Month.SEPTEMBER, 19);
  private static final LocalDate STEPIN_DATE = TODAY.plusDays(1);
  private static final LocalDate CASH_SETTLE_DATE = DEFAULT_CALENDAR.shift(TODAY, 3); // AKA valuation date
  private static final LocalDate START_DATE = LocalDate.of(2007, Month.MARCH, 20);
  private static final LocalDate END_DATE = LocalDate.of(2013, Month.JUNE, 20);

  // yield curve
  private static final LocalDate SPOT_DATE = DEFAULT_CALENDAR.shift(TODAY, 2);
  private static final IsdaCompliantYieldCurve YIELD_CURVE;

  static {
    final int nMoneyMarket = 6;
    final int nSwaps = 15;
    final int nInstruments = nMoneyMarket + nSwaps;

    final IsdaInstrumentTypes[] types = new IsdaInstrumentTypes[nInstruments];
    final Period[] tenors = new Period[nInstruments];
    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12 };
    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30 };
    // check
    ArgChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
    ArgChecker.isTrue(swapYears.length == nSwaps, "swapYears");

    for (int i = 0; i < nMoneyMarket; i++) {
      types[i] = IsdaInstrumentTypes.MONEY_MARKET;
      tenors[i] = Period.ofMonths(mmMonths[i]);
    }
    for (int i = nMoneyMarket; i < nInstruments; i++) {
      types[i] = IsdaInstrumentTypes.SWAP;
      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
    }

    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
      0.03367, 0.03419, 0.03411, 0.03412 };

    final DayCount moneyMarketDCC = ACT360;
    final DayCount swapDCC = ACT360;
    final DayCount curveDCC = ACT365;
    final Period swapInterval = Period.ofMonths(6);

    YIELD_CURVE = IsdaCompliantYieldCurveBuild.build(TODAY, SPOT_DATE, types, tenors, rates, moneyMarketDCC, swapDCC, swapInterval, curveDCC, FOLLOWING);
  }

  public void Test() {
    final Period tenor = Period.ofMonths(3);
    final boolean payAccOnDefault = true;
    final StubConvention stubType = StubConvention.SHORT_INITIAL;
    final boolean protectionStart = true;

    final double quotedSpread = 550. / 10000;
    final double coupon = 500. / 10000;
    final double recovery = 0.4;

    final IsdaCompliantCreditCurveBuilder builder = new FastCreditCurveBuilder();
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer();

    final IsdaCompliantCreditCurve creditCurve = builder.calibrateCreditCurve(TODAY, STEPIN_DATE, CASH_SETTLE_DATE, START_DATE, END_DATE, quotedSpread, payAccOnDefault, tenor, stubType,
        protectionStart, YIELD_CURVE, recovery);
    final CdsAnalytic cds = new CdsAnalytic(TODAY, STEPIN_DATE, CASH_SETTLE_DATE, START_DATE, END_DATE, payAccOnDefault, tenor, stubType, protectionStart, recovery);

    final double clean = pricer.pv(cds, YIELD_CURVE, creditCurve, coupon, CdsPriceType.CLEAN);
    final double dirty = pricer.pv(cds, YIELD_CURVE, creditCurve, coupon, CdsPriceType.DIRTY);

    //Numbers from Excel
    assertEquals("upfrount", 0.018566047, clean, 1e-9);
    assertEquals("cashSettled", 57882.69024, dirty * NOTIONAL, 1e-9 * NOTIONAL);

  }

}
