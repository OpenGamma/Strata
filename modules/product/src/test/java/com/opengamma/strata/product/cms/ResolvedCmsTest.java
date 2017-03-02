/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;

/**
 * Test {@link ResolvedCms}.
 */
@Test
public class ResolvedCmsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  static final ResolvedCmsLeg CMS_LEG = CmsTest.sutCap().getCmsLeg().resolve(REF_DATA);
  static final ResolvedSwapLeg PAY_LEG = CmsTest.sutCap().getPayLeg().get().resolve(REF_DATA);

  //-------------------------------------------------------------------------
  public void test_of_twoLegs() {
    ResolvedCms test = sut();
    assertEquals(test.getCmsLeg(), CMS_LEG);
    assertEquals(test.getPayLeg().get(), PAY_LEG);
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(CMS_LEG.getCurrency()));
  }

  public void test_of_oneLeg() {
    ResolvedCms test = ResolvedCms.of(CMS_LEG);
    assertEquals(test.getCmsLeg(), CMS_LEG);
    assertFalse(test.getPayLeg().isPresent());
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(CMS_LEG.getCurrency()));
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
  static ResolvedCms sut() {
    return ResolvedCms.of(CMS_LEG, PAY_LEG);
  }

  static ResolvedCms sut2() {
    return ResolvedCms.of(CmsTest.sutFloor().getCmsLeg().resolve(REF_DATA));
  }

}
