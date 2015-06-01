package com.opengamma.strata.examples.finance.credit;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurveBuild;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDAInstrumentTypes;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.Period;

import static com.opengamma.analytics.convention.businessday.BusinessDayDateUtils.addWorkDays;

@Test
public class CdsBuildACurve {

  @Test
  public void test_sss() {

    ImmutableList<String> raytheon20141020 = ImmutableList.of(
        "1M,M,0.001535",
        "2M,M,0.001954",
        "3M,M,0.002281",
        "6M,M,0.003217",
        "1Y,M,0.005444",
        "2Y,S,0.005905",
        "3Y,S,0.009555",
        "4Y,S,0.012775",
        "5Y,S,0.015395",
        "6Y,S,0.017445",
        "7Y,S,0.019205",
        "8Y,S,0.020660",
        "9Y,S,0.021885",
        "10Y,S,0.022940",
        "12Y,S,0.024615",
        "15Y,S,0.026300",
        "20Y,S,0.027950",
        "25Y,S,0.028715",
        "30Y,S,0.029160"
    );

    Period[] yieldCurvePoints = raytheon20141020
        .stream()
        .map(s -> Tenor.parse(s.split(",")[0]).getPeriod())
        .toArray(Period[]::new);
    ISDAInstrumentTypes[] yieldCurveInstruments = raytheon20141020
        .stream()
        .map(s -> (s.split(",")[1].equals("M") ? ISDAInstrumentTypes.MoneyMarket : ISDAInstrumentTypes.Swap))
        .toArray(ISDAInstrumentTypes[]::new);
    double[] rates = raytheon20141020
        .stream()
        .mapToDouble(s -> Double.valueOf(s.split(",")[2]))
        .toArray();

    HolidayCalendar holidayCalendar = HolidayCalendars.NO_HOLIDAYS;
    LocalDate tradeDate = LocalDate.of(2014, 10, 16);
    LocalDate spotDate = addWorkDays(tradeDate.minusDays(1), 3, holidayCalendar);
    DayCount mmDayCount = DayCounts.ACT_360;
    DayCount swapDayCount = DayCounts.THIRTY_E_360;
    DayCount curveDayCount = DayCounts.ACT_365F;
    Period swapInterval = Period.ofMonths(3);
    BusinessDayConvention convention = BusinessDayConventions.MODIFIED_FOLLOWING;

    ISDACompliantYieldCurve discountCurve = new ISDACompliantYieldCurveBuild(
        tradeDate,
        spotDate,
        yieldCurveInstruments,
        yieldCurvePoints,
        conv(mmDayCount),
        conv(swapDayCount),
        swapInterval,
        conv(curveDayCount),
        convention,
        holidayCalendar
    ).build(rates);

    System.out.println(discountCurve);
  }

  public static com.opengamma.analytics.convention.daycount.DayCount conv(DayCount from) {
    switch (from.getName()) {
      case "Act/365F":
        return com.opengamma.analytics.convention.daycount.DayCounts.ACT_365F;
      case "30E/360":
        return com.opengamma.analytics.convention.daycount.DayCounts.THIRTY_E_360;
      case "Act/360":
        return com.opengamma.analytics.convention.daycount.DayCounts.ACT_360;
      default:
        throw new RuntimeException("unknown daycount " + from);
    }
  }

}
