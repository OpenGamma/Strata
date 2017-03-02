/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link ResolvedCmsLeg}.
 */
@Test
public class ResolvedCmsLegTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final CmsPeriod PERIOD_1 = CmsLegTest.sutCap().resolve(REF_DATA).getCmsPeriods().get(0);
  private static final CmsPeriod PERIOD_2 = CmsLegTest.sutCap().resolve(REF_DATA).getCmsPeriods().get(1);

  public void test_builder() {
    ResolvedCmsLeg test = sut();
    assertEquals(test.getCmsPeriods().size(), 2);
    assertEquals(test.getCmsPeriods().get(0), PERIOD_1);
    assertEquals(test.getCmsPeriods().get(1), PERIOD_2);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getStartDate(), PERIOD_1.getStartDate());
    assertEquals(test.getEndDate(), PERIOD_2.getEndDate());
    assertEquals(test.getIndex(), PERIOD_1.getIndex());
    assertEquals(test.getUnderlyingIndex(), PERIOD_1.getIndex().getTemplate().getConvention().getFloatingLeg().getIndex());
    assertEquals(test.getPayReceive(), RECEIVE);
  }

  public void test_builder_multiCurrencyIndex() {
    CmsPeriod period3 = CmsPeriodTest.sut2();
    assertThrowsIllegalArg(() -> ResolvedCmsLeg.builder().payReceive(RECEIVE).cmsPeriods(PERIOD_1, period3).build());
    CmsPeriod period4 = CmsPeriodTest.sutCoupon();
    assertThrowsIllegalArg(() -> ResolvedCmsLeg.builder().payReceive(RECEIVE).cmsPeriods(PERIOD_1, period4).build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedCmsLeg sut() {
    return ResolvedCmsLeg.builder()
        .payReceive(RECEIVE)
        .cmsPeriods(PERIOD_1, PERIOD_2)
        .build();
  }

  static ResolvedCmsLeg sut2() {
    return ResolvedCmsLeg.builder()
        .payReceive(PAY)
        .cmsPeriods(CmsLegTest.sutFloor().resolve(REF_DATA).getCmsPeriods())
        .build();
  }

}
