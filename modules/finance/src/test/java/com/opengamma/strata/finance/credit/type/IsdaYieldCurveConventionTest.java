/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.type;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_E_360;
import static com.opengamma.strata.basics.date.HolidayCalendars.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link IsdaYieldCurveConvention}.
 */
@Test
public class IsdaYieldCurveConventionTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    IsdaYieldCurveConvention sut = IsdaYieldCurveConventions.ISDA_USD;
    assertEquals(sut, IsdaYieldCurveConvention.of("ISDA_USD"));
    assertEquals(sut.getName(), "ISDA_USD");
    assertEquals(sut.getCurrency(), USD);
    assertEquals(sut.getMmDayCount(), ACT_360);
    assertEquals(sut.getFixedDayCount(), THIRTY_E_360);
    assertEquals(sut.getSpotDays(), 2);
    assertEquals(sut.getFixedPaymentFrequency(), P6M);
    assertEquals(sut.getBadDayConvention(), MODIFIED_FOLLOWING);
    assertEquals(sut.getHolidayCalendar(), SAT_SUN);
  }

  //-------------------------------------------------------------------------
  public void test_spot_date_from_valuation_date() {
    IsdaYieldCurveConvention sut = IsdaYieldCurveConventions.ISDA_USD;
    assertEquals(sut.getSpotDateAsOf(date(2014, 9, 19)), date(2014, 9, 23));
  }

}
