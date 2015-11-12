/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.credit.ReferenceInformationType.SINGLE_NAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;

/**
 * Test.
 */
@Test
public class CdsSingleNameTest {

  //-------------------------------------------------------------------------
  public void test_getStartDate() {
    assertEquals(sut().getStartDate(), date(2014, 3, 20));
  }

  public void test_getEndDate() {
    assertEquals(sut().getEndDate(), date(2019, 6, 20));
  }

  public void test_getBusinessDayAdjustment() {
    assertEquals(sut().getBusinessDayAdjustment().getConvention(), FOLLOWING);
    assertEquals(sut().getBusinessDayAdjustment().getCalendar(), USNY);
  }

  public void test_getReferenceInformation() {
    assertEquals(sut().getReferenceInformation().getType(), SINGLE_NAME);
  }

  public void test_isPayAccruedOnDefault() {
    assertTrue(sut().isPayAccruedOnDefault());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    assertEquals(ExpandedCdsTest.sut(), sut().expand());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  static Cds sut() {
    return Cds.builder()
        .buySellProtection(BUY)
        .startDate(date(2014, 3, 20))
        .endDate(date(2019, 6, 20))
        .businessDayAdjustment(
            BusinessDayAdjustment.of(FOLLOWING, USNY))
        .referenceInformation(SingleNameReferenceInformationTest.sut())
        .feeLeg(FeeLegTest.sut())
        .payAccruedOnDefault(true)
        .build();
  }

}
