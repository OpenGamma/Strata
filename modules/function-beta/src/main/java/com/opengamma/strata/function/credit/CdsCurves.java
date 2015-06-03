package com.opengamma.strata.function.credit;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.financial.credit.isdastandardmodel.AccrualOnDefaultFormulae;
import com.opengamma.analytics.financial.credit.isdastandardmodel.FastCreditCurveBuilder;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCreditCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCreditCurveBuilder;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurveBuild;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDAInstrumentTypes;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.finance.credit.CdsTrade;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

import static com.opengamma.analytics.convention.businessday.BusinessDayDateUtils.addWorkDays;
import static com.opengamma.strata.function.credit.Converters.translateDayCount;
import static com.opengamma.strata.function.credit.Converters.translateStubType;

public class CdsCurves {

  public static ISDACompliantYieldCurve yieldCurve(CdsTrade trade) {

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

    Period swapInterval = Period.ofMonths(3);
    DayCount mmDayCount = DayCounts.ACT_360;
    DayCount swapDayCount = DayCounts.THIRTY_E_360;
    DayCount curveDayCount = DayCounts.ACT_365F;

    BusinessDayConvention convention = trade.getProduct().getGeneralTerms().getDateAdjustments().getConvention();
    HolidayCalendar holidayCalendar = trade.getProduct().getGeneralTerms().getDateAdjustments().getCalendar();

    LocalDate tradeDate = trade.getTradeInfo().getTradeDate().get();
    LocalDate spotDate = addWorkDays(tradeDate.minusDays(1), 3, holidayCalendar);

    return new ISDACompliantYieldCurveBuild(
        tradeDate,
        spotDate,
        yieldCurveInstruments,
        yieldCurvePoints,
        translateDayCount(mmDayCount),
        translateDayCount(swapDayCount),
        swapInterval,
        translateDayCount(curveDayCount),
        convention,
        holidayCalendar
    ).build(rates);

  }

  public static ISDACompliantCreditCurve creditCurve(CdsTrade trade, ISDACompliantYieldCurve yieldCurve, double recoveryRate) {

    List<LocalDate> curvepoints = ImmutableList.of(trade.modelEndDate());
    LocalDate[] endDates = curvepoints.stream().toArray(LocalDate[]::new);

    double[] fractionalParSpreads =
        curvepoints.stream().mapToDouble(x -> 0.0110).toArray();

    return new FastCreditCurveBuilder(AccrualOnDefaultFormulae.OrignalISDA, ISDACompliantCreditCurveBuilder.ArbitrageHandling.Fail)
        .calibrateCreditCurve(
            trade.modelTradeDate(),
            trade.modelStepInDate(),
            trade.modelValueDate(),
            trade.modelAccStartDate(),
            endDates,
            fractionalParSpreads,
            trade.modelPayAccOnDefault(),
            trade.modelPaymentInterval(),
            translateStubType(trade.modelStubConvention()),
            trade.modelProtectStart(),
            yieldCurve,
            recoveryRate
        );

  }

}
