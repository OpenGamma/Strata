/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.credit.ReferenceInformationType.INDEX;
import static com.opengamma.strata.product.credit.ReferenceInformationType.SINGLE_NAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.market.ReferenceData;

/**
 * Test {@link Cds}.
 */
@Test
public class CdsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_getStartDate() {
    assertEquals(sutSingleName().getStartDate(), date(2014, 3, 20));
  }

  public void test_getEndDate() {
    assertEquals(sutSingleName().getEndDate(), date(2019, 6, 20));
  }

  public void test_getBusinessDayAdjustment() {
    assertEquals(sutSingleName().getBusinessDayAdjustment().getConvention(), FOLLOWING);
    assertEquals(sutSingleName().getBusinessDayAdjustment().getCalendar(), USNY);
  }

  public void test_getReferenceInformation() {
    assertEquals(sutSingleName().getReferenceInformation().getType(), SINGLE_NAME);
    assertEquals(sutIndex().getReferenceInformation().getType(), INDEX);
  }

  public void test_isPayAccruedOnDefault() {
    assertTrue(sutSingleName().isPayAccruedOnDefault());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    assertEquals(sutSingleName().resolve(REF_DATA), ResolvedCdsTest.sutSingleName());
    assertEquals(sutIndex().resolve(REF_DATA), ResolvedCdsTest.sutIndex());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sutSingleName());
    coverImmutableBean(sutIndex());
    coverBeanEquals(sutSingleName(), sutIndex());
  }

  public void test_serialization() {
    assertSerialization(sutSingleName());
    assertSerialization(sutIndex());
  }

  //-------------------------------------------------------------------------
  static Cds sutSingleName() {
    return Cds.builder()
        .buySellProtection(BUY)
        .startDate(date(2014, 3, 20))
        .endDate(date(2019, 6, 20))
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, USNY))
        .referenceInformation(SingleNameReferenceInformationTest.sut())
        .feeLeg(FeeLegTest.sut())
        .payAccruedOnDefault(true)
        .build();
  }

  static Cds sutIndex() {
    return Cds.builder()
        .buySellProtection(BUY)
        .startDate(date(2014, 3, 20))
        .endDate(date(2019, 6, 20))
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, USNY))
        .referenceInformation(IndexReferenceInformationTest.sut())
        .feeLeg(FeeLegTest.sut())
        .payAccruedOnDefault(true)
        .build();
  }

}
