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
      IsdaYieldCurveConvention.of(
          Currency.USD,
          DayCounts.ACT_360,
          DayCounts.THIRTY_360_ISDA,
          DayCounts.ACT_365F,
          2,
          Frequency.P6M,
          BusinessDayConventions.MODIFIED_FOLLOWING,
          HolidayCalendars.SAT_SUN
      );
}
