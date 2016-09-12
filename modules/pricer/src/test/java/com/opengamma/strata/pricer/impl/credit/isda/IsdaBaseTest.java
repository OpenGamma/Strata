/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;

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
public class IsdaBaseTest {

  protected static final AccrualOnDefaultFormulae ORIGINAL_ISDA = AccrualOnDefaultFormulae.ORIGINAL_ISDA;
  protected static final AccrualOnDefaultFormulae MARKIT_FIX = AccrualOnDefaultFormulae.MARKIT_FIX;
  protected static final AccrualOnDefaultFormulae OG_FIX = AccrualOnDefaultFormulae.CORRECT;

  protected static final AnalyticCdsPricer PRICER = new AnalyticCdsPricer();
  protected static final AnalyticCdsPricer PRICER_MARKIT_FIX = new AnalyticCdsPricer(MARKIT_FIX);
  protected static final AnalyticCdsPricer PRICER_OG_FIX = new AnalyticCdsPricer(OG_FIX);
  protected static final IsdaCompliantCreditCurveBuilder CREDIT_CURVE_BUILDER = new FastCreditCurveBuilder();
  protected static final FiniteDifferenceSpreadSensitivityCalculator CS01_CAL = new FiniteDifferenceSpreadSensitivityCalculator();

  protected static final double ONE_PC = 1e-2;
  protected static final double ONE_BP = 1e-4;
  protected static final double ONE_HUNDRED = 100.;
  protected static final double TEN_THOUSAND = 10000.;

  protected static final HolidayCalendar DEFAULT_CALENDAR = HolidayCalendars.SAT_SUN;
  protected static final HolidayCalendar NO_HOLIDAY_CALENDAR = HolidayCalendars.NO_HOLIDAYS;
  protected static final DayCount ACT365F = DayCounts.ACT_365F;
  protected static final DayCount ACT360 = DayCounts.ACT_360;
  protected static final DayCount D30360 = DayCounts.THIRTY_360_ISDA;  // THIRTY_U_360 minus the EOM rule
  protected static final DayCount ACT_ACT_ISDA = DayCounts.ACT_ACT_ISDA;

  protected static final BusinessDayConvention FOLLOWING = BusinessDayConventions.FOLLOWING;
  protected static final BusinessDayConvention MOD_FOLLOWING = BusinessDayConventions.MODIFIED_FOLLOWING;

  //standard CDS settings 
  protected static final boolean PAY_ACC_ON_DEFAULT = true;
  protected static final Period PAYMENT_INTERVAL = Period.ofMonths(3);
  protected static final StubConvention STUB = StubConvention.SHORT_INITIAL;
  protected static final boolean PROCTECTION_START = true;
  protected static final double RECOVERY_RATE = 0.4;

  protected static IsdaCompliantYieldCurveBuild makeYieldCurveBuilder(final LocalDate today, final LocalDate spotDate, final String[] maturities, final String[] type, final DayCount moneyMarketDCC,
      final DayCount swapDCC, final Period swapInterval) {
    return makeYieldCurveBuilder(today, spotDate, maturities, type, moneyMarketDCC, swapDCC, swapInterval, DEFAULT_CALENDAR);
  }

  protected static IsdaCompliantYieldCurveBuild makeYieldCurveBuilder(final LocalDate today, final LocalDate spotDate, final String[] maturities, final String[] type, final DayCount moneyMarketDCC,
      final DayCount swapDCC, final Period swapInterval, final HolidayCalendar calendar) {
    final DayCount curveDCC = ACT365F;
    final int nInstruments = maturities.length;
    ArgChecker.isTrue(nInstruments == type.length, "type length {} does not match maturities length {}", type.length, nInstruments);
    final Period[] tenors = new Period[nInstruments];
    final IsdaInstrumentTypes[] types = new IsdaInstrumentTypes[nInstruments];
    for (int i = 0; i < nInstruments; i++) {
      String temp = maturities[i];
      if (temp.endsWith("M")) {
        temp = temp.split("M")[0];
        tenors[i] = Period.ofMonths(Integer.valueOf(temp));
      } else if (temp.endsWith("Y")) {
        temp = temp.split("Y")[0];
        tenors[i] = Period.ofYears(Integer.valueOf(temp));
      } else {
        throw new IllegalArgumentException("cannot parse " + temp);
      }

      temp = type[i];
      if (temp.equalsIgnoreCase("M")) {
        types[i] = IsdaInstrumentTypes.MONEY_MARKET;
      } else if (temp.equalsIgnoreCase("S")) {
        types[i] = IsdaInstrumentTypes.SWAP;
      } else {
        throw new IllegalArgumentException("cannot parse " + temp);
      }
    }
    final IsdaCompliantYieldCurveBuild builder = new IsdaCompliantYieldCurveBuild(today, spotDate, types, tenors, moneyMarketDCC, swapDCC, swapInterval, curveDCC, MOD_FOLLOWING, calendar);
    return builder;
  }

  protected static IsdaCompliantYieldCurve makeYieldCurve(final LocalDate today, final LocalDate spotDate, final String[] maturities, final String[] type, final double[] rates,
      final DayCount moneyMarketDCC, final DayCount swapDCC, final Period swapInterval) {
    return makeYieldCurve(today, spotDate, maturities, type, rates, moneyMarketDCC, swapDCC, swapInterval, DEFAULT_CALENDAR);
  }

  protected static IsdaCompliantYieldCurve makeYieldCurve(final LocalDate today, final LocalDate spotDate, final String[] maturities, final String[] type, final double[] rates,
      final DayCount moneyMarketDCC, final DayCount swapDCC, final Period swapInterval, final HolidayCalendar calendar) {

    final IsdaCompliantYieldCurveBuild builder = makeYieldCurveBuilder(today, spotDate, maturities, type, moneyMarketDCC, swapDCC, swapInterval, calendar);
    return builder.build(rates);
  }

  protected static IsdaCompliantYieldCurve makeYieldCurve(final LocalDate today, final LocalDate spotDate, final String[] maturities, final String[] type, final double[] rates) {
    final DayCount moneyMarketDCC = ACT360;
    final DayCount swapDCC = D30360;
    final Period swapInterval = Period.ofMonths(6);
    return makeYieldCurve(today, spotDate, maturities, type, rates, moneyMarketDCC, swapDCC, swapInterval);
  }

  protected static LocalDate parseDateString(final String ddmmyyyy) {
    ArgChecker.notNull(ddmmyyyy, "ddmmyyyy");
    final String[] temp = ddmmyyyy.split("/");
    ArgChecker.isTrue(temp.length == 3, "date formatt wrong: length");
    final int day = Integer.valueOf(temp[0]);
    final int month = Integer.valueOf(temp[1]);
    final int year = Integer.valueOf(temp[2]);
    ArgChecker.isTrue(year > 1900 && year < 2500, "date formatt wrong: year out of range - {}", year);
    return LocalDate.of(year, month, day);
  }

  protected static LocalDate[] parseDateStrings(final String[] ddmmyyyy) {
    ArgChecker.notNull(ddmmyyyy, "ddmmyyyy");
    final int n = ddmmyyyy.length;
    final LocalDate[] res = new LocalDate[n];
    for (int i = 0; i < n; i++) {
      res[i] = parseDateString(ddmmyyyy[i]);
    }
    return res;
  }
}
