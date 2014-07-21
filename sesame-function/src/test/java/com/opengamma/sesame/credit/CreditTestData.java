/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit;

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.primitives.Doubles;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurveBuild;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDAInstrumentTypes;
import com.opengamma.financial.analytics.isda.credit.YieldCurveData;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.Tenor;

/**
 * Helper class to produce standard objects for credit testing.
 * 
 * Objects here are based on those in /sesame-function/src/test/resources/credit/YC Test Data.xls
 */
public final class CreditTestData {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 3, 27);
  private static final LocalDate SPOT_DATE = LocalDate.of(2014, 4, 1);

  private static int MONEY_MARKET_INSTS = 6;
  private static List<Period> TENORS = ImmutableList.of(Period.ofMonths(1), 
                                                        Period.ofMonths(2), 
                                                        Period.ofMonths(3), 
                                                        Period.ofMonths(6), 
                                                        Period.ofMonths(9), 
                                                        Period.ofYears(1), 
                                                        Period.ofYears(2), 
                                                        Period.ofYears(3), 
                                                        Period.ofYears(4), 
                                                        Period.ofYears(5), 
                                                        Period.ofYears(6), 
                                                        Period.ofYears(7), 
                                                        Period.ofYears(8), 
                                                        Period.ofYears(9), 
                                                        Period.ofYears(10), 
                                                        Period.ofYears(11), 
                                                        Period.ofYears(12), 
                                                        Period.ofYears(15), 
                                                        Period.ofYears(20), 
                                                        Period.ofYears(25), 
                                                        Period.ofYears(30));
  private static List<Double> RATES = ImmutableList.of(0.00445,
                                                       0.009488,
                                                       0.012337,
                                                       0.017762,
                                                       0.01935,
                                                       0.020838,
                                                       0.01652,
                                                       0.02018,
                                                       0.023033,
                                                       0.02525,
                                                       0.02696,
                                                       0.02825,
                                                       0.02931,
                                                       0.03017,
                                                       0.03092,
                                                       0.0316,
                                                       0.03231,
                                                       0.03367,
                                                       0.03419,
                                                       0.03411,
                                                       0.03412);

  private CreditTestData() {}
  
  public static ISDACompliantYieldCurve createYieldCurve() {
    
    ISDAInstrumentTypes[] instrumentTypes = new ISDAInstrumentTypes[TENORS.size()];
    
    Arrays.fill(instrumentTypes, 0, MONEY_MARKET_INSTS, ISDAInstrumentTypes.MoneyMarket);
    Arrays.fill(instrumentTypes, MONEY_MARKET_INSTS, TENORS.size(), ISDAInstrumentTypes.Swap);
    
    ISDACompliantYieldCurveBuild builder = new ISDACompliantYieldCurveBuild(
        VALUATION_DATE,
        SPOT_DATE,
        instrumentTypes, 
        TENORS.toArray(new Period[TENORS.size()]),
        DayCounts.ACT_360,
        DayCounts.THIRTY_360,
        Period.ofYears(1),
        DayCounts.ACT_365,
        BusinessDayConventions.MODIFIED_FOLLOWING,
        new MondayToFridayCalendar("test")
    );
    
    return builder.build(Doubles.toArray(RATES));
    
  }

  public static YieldCurveData createYieldCurveData() {
    SortedMap<Tenor, Double> cashData = ImmutableSortedMap.<Tenor, Double>naturalOrder()
        .put(Tenor.ONE_MONTH, 0.00445)
        .put(Tenor.TWO_MONTHS, 0.009488)
        .put(Tenor.THREE_MONTHS, 0.012337)
        .put(Tenor.SIX_MONTHS, 0.017762)
        .put(Tenor.NINE_MONTHS, 0.01935)
        .put(Tenor.ONE_YEAR, 0.020838)
        .build();

    @SuppressWarnings("deprecation")
    SortedMap<Tenor, Double> swapData = ImmutableSortedMap.<Tenor, Double>naturalOrder()
        .put(Tenor.TWO_YEARS, 0.01652)
        .put(Tenor.THREE_YEARS, 0.02018)
        .put(Tenor.FOUR_YEARS, 0.023033)
        .put(Tenor.FIVE_YEARS, 0.02525)
        .put(Tenor.SIX_YEARS, 0.02696)
        .put(Tenor.SEVEN_YEARS, 0.02825)
        .put(Tenor.EIGHT_YEARS, 0.02931)
        .put(Tenor.NINE_YEARS, 0.03017)
        .put(Tenor.TEN_YEARS, 0.03092)
        .put(new Tenor(Period.ofYears(11)), 0.0316)
        .put(new Tenor(Period.ofYears(12)), 0.03231)
        .put(new Tenor(Period.ofYears(15)), 0.03367)
        .put(new Tenor(Period.ofYears(20)), 0.03419)
        .put(new Tenor(Period.ofYears(25)), 0.03411)
        .put(new Tenor(Period.ofYears(30)), 0.03412)
        .build();

    YieldCurveData ycData = YieldCurveData.builder()
        .cashData(cashData)
        .swapData(swapData)
        .cashDayCount(DayCounts.ACT_360)
        .currency(Currency.USD)
        .curveBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
        .curveDayCount(DayCounts.ACT_365)
        .regionId(null) //weekend only calendar
        .spotDate(SPOT_DATE)
        .swapDayCount(DayCounts.THIRTY_360)
        .swapFixedLegInterval(Tenor.ONE_YEAR)
        .build();
    return ycData;
  }

}
