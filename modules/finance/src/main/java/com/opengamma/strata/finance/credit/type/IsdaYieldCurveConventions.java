/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
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
      IsdaYieldCurveConvention
          .builder()
          .currency(Currency.EUR)
          .mmDayCount(DayCounts.ACT_360)
          .fixedDayCount(DayCounts.THIRTY_360_ISDA)
          .spotDays(2)
          .fixedPaymentFrequency(Frequency.P12M)
          .badDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
          .holidayCalendar(HolidayCalendars.SAT_SUN)
          .build();

  public static final IsdaYieldCurveConvention europeanGbp =
      IsdaYieldCurveConvention
          .builder()
          .currency(Currency.GBP)
          .mmDayCount(DayCounts.ACT_365F)
          .fixedDayCount(DayCounts.ACT_365F)
          .spotDays(2)
          .fixedPaymentFrequency(Frequency.P6M)
          .badDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
          .holidayCalendar(HolidayCalendars.SAT_SUN)
          .build();

  public static final IsdaYieldCurveConvention europeanChf =
      IsdaYieldCurveConvention
          .builder()
          .currency(Currency.CHF)
          .mmDayCount(DayCounts.ACT_360)
          .fixedDayCount(DayCounts.THIRTY_360_ISDA)
          .spotDays(2)
          .fixedPaymentFrequency(Frequency.P12M)
          .badDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
          .holidayCalendar(HolidayCalendars.SAT_SUN)
          .build();

  public static final IsdaYieldCurveConvention asianJPY =
      IsdaYieldCurveConvention
          .builder()
          .currency(Currency.JPY)
          .mmDayCount(DayCounts.ACT_360)
          .fixedDayCount(DayCounts.THIRTY_E_360)
          .spotDays(2)
          .fixedPaymentFrequency(Frequency.P6M)
          .badDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
          .holidayCalendar(HolidayCalendars.JPTO)
          .build();
}
