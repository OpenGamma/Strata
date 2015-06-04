package com.opengamma.strata.finance.credit.type;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * http://www.cdsmodel.com/assets/cds-model/docs/Interest%20Rate%20Curve%20-%20XML%20Specifications.pdf
 */
public class IsdaYieldCurveConventions {

  public static final IsdaYieldCurveConvention northAmericanUsd =
      IsdaYieldCurveConvention
          .builder()
          .currency(Currency.USD)
          .mmDayCount(DayCounts.ACT_360)
          .fixedDayCount(DayCounts.THIRTY_E_360)
          .spotDays(2)
          .fixedPaymentFrequency(Frequency.P6M)
          .badDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
          .holidayCalendar(HolidayCalendars.SAT_SUN)
          .build();

  public static final IsdaYieldCurveConvention europeanEur =
      IsdaYieldCurveConvention.of(
          Currency.EUR,
          DayCounts.ACT_360,
          DayCounts.THIRTY_360_ISDA,
          2,
          Frequency.P12M,
          BusinessDayConventions.MODIFIED_FOLLOWING,
          HolidayCalendars.SAT_SUN
      );

  public static final IsdaYieldCurveConvention europeanGbp =
      IsdaYieldCurveConvention.of(
          Currency.GBP,
          DayCounts.ACT_365F,
          DayCounts.ACT_365F,
          2,
          Frequency.P6M,
          BusinessDayConventions.MODIFIED_FOLLOWING,
          HolidayCalendars.SAT_SUN
      );

  public static final IsdaYieldCurveConvention europeanChf =
      IsdaYieldCurveConvention.of(
          Currency.CHF,
          DayCounts.ACT_360,
          DayCounts.THIRTY_E_360,
          2,
          Frequency.P12M,
          BusinessDayConventions.MODIFIED_FOLLOWING,
          HolidayCalendars.SAT_SUN
      );


  public static final IsdaYieldCurveConvention asianJPY =
      IsdaYieldCurveConvention.of(
          Currency.JPY,
          DayCounts.ACT_360,
          DayCounts.THIRTY_E_360,
          2,
          Frequency.P6M,
          BusinessDayConventions.MODIFIED_FOLLOWING,
          HolidayCalendars.JPTO
      );

}
