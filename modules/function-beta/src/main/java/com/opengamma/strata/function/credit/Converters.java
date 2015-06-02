package com.opengamma.strata.function.credit;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Convert some basic Strata types to corresponding analytics type
 */
public class Converters {

  public static com.opengamma.analytics.convention.daycount.DayCount translateDayCount(DayCount from) {
    switch (from.getName()) {
      case "Act/365F":
        return com.opengamma.analytics.convention.daycount.DayCounts.ACT_365F;
      case "30E/360":
        return com.opengamma.analytics.convention.daycount.DayCounts.THIRTY_E_360;
      case "Act/360":
        return com.opengamma.analytics.convention.daycount.DayCounts.ACT_360;
      default:
        throw new IllegalStateException("Converters: Unknown daycount " + from);
    }
  }

  public static com.opengamma.analytics.financial.credit.isdastandardmodel.StubType translateStubType(StubConvention from) {
    switch (from) {
      case SHORT_INITIAL:
        return com.opengamma.analytics.financial.credit.isdastandardmodel.StubType.FRONTSHORT;
      case SHORT_FINAL:
        return com.opengamma.analytics.financial.credit.isdastandardmodel.StubType.BACKSHORT;
      default:
        throw new IllegalStateException("Converters: Unknown stub convention: " + from);
    }
  }

}
