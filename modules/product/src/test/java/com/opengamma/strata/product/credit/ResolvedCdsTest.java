/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_INITIAL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;

import java.util.Optional;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Test.
 */
@Test
public class ResolvedCdsTest {

  //-------------------------------------------------------------------------
  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> ResolvedCds.builder().build());
  }

  //-------------------------------------------------------------------------
  public void test_getters() {
    assertEquals(sutSingleName().getUpfrontFeeAmount(), OptionalDouble.of(1_000_000d));
    assertEquals(sutSingleName().getUpfrontFeePaymentDate(), Optional.of(date(2014, 3, 23)));
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
  static ResolvedCds sutSingleName() {
    return ResolvedCds.builder()
        .buySellProtection(BUY)
        .currency(USD)
        .notional(100_000_000d)
        .coupon(.00100)
        .startDate(date(2014, 3, 20))
        .endDate(date(2019, 6, 20))
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, USNY))
        .referenceInformation(SingleNameReferenceInformationTest.sut())
        .payAccruedOnDefault(true)
        .paymentInterval(Frequency.P3M.getPeriod())
        .stubConvention(SHORT_INITIAL)
        .accrualDayCount(ACT_360)
        .upfrontFeeAmount(1_000_000d)
        .upfrontFeePaymentDate(date(2014, 3, 23))
        .build();
  }

  static ResolvedCds sutIndex() {
    return ResolvedCds.builder()
        .buySellProtection(BUY)
        .currency(USD)
        .notional(100_000_000d)
        .coupon(.00100)
        .startDate(date(2014, 3, 20))
        .endDate(date(2019, 6, 20))
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, USNY))
        .referenceInformation(IndexReferenceInformationTest.sut())
        .payAccruedOnDefault(true)
        .paymentInterval(Frequency.P3M.getPeriod())
        .stubConvention(SHORT_INITIAL)
        .accrualDayCount(ACT_360)
        .upfrontFeeAmount(1_000_000d)
        .upfrontFeePaymentDate(date(2014, 3, 23))
        .build();
  }

}
