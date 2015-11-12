/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_INITIAL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Test.
 */
@Test
public class ExpandedCdsTest {

  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> ExpandedCds.builder().build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    assertEquals(CdsSingleNameTest.sut().expand(), sut());
    assertEquals(sut().expand(), sut());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  static ExpandedCds sut() {
    return ExpandedCds.builder()
        .buySellProtection(BUY)
        .currency(USD)
        .notional(100_000_000d)
        .coupon(.00100)
        .startDate(date(2014, 3, 20))
        .endDate(date(2019, 6, 20))
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, USNY))
        .payAccruedOnDefault(true)
        .paymentInterval(Frequency.P3M.getPeriod())
        .stubConvention(SHORT_INITIAL)
        .accrualDayCount(ACT_360)
        .upfrontFeeAmount(1_000_000d)
        .upfrontFeePaymentDate(date(2014, 3, 23))
        .build();
  }

}
