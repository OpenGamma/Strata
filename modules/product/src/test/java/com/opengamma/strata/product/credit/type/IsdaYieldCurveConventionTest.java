/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_E_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link IsdaYieldCurveConvention}.
 */
@Test
public class IsdaYieldCurveConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_of() {
    IsdaYieldCurveConvention sut = IsdaYieldCurveConventions.USD_ISDA;
    assertEquals(sut, IsdaYieldCurveConvention.of("USD-ISDA"));
    assertEquals(sut.getName(), "USD-ISDA");
    assertEquals(sut.getCurrency(), USD);
    assertEquals(sut.getMoneyMarketDayCount(), ACT_360);
    assertEquals(sut.getFixedDayCount(), THIRTY_E_360);
    assertEquals(sut.getSpotDays(), 2);
    assertEquals(sut.getFixedPaymentFrequency(), P6M);
    assertEquals(sut.getBusinessDayConvention(), MODIFIED_FOLLOWING);
    assertEquals(sut.getHolidayCalendar(), SAT_SUN);
  }

  //-------------------------------------------------------------------------
  public void test_spot_date_from_valuation_date() {
    IsdaYieldCurveConvention sut = IsdaYieldCurveConventions.USD_ISDA;
    assertEquals(sut.calculateSpotDateFromTradeDate(date(2014, 9, 19), REF_DATA), date(2014, 9, 23));
  }

}
