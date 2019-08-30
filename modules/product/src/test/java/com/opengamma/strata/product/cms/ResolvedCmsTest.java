/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;

/**
 * Test {@link ResolvedCms}.
 */
public class ResolvedCmsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  static final ResolvedCmsLeg CMS_LEG = CmsTest.sutCap().getCmsLeg().resolve(REF_DATA);
  static final ResolvedSwapLeg PAY_LEG = CmsTest.sutCap().getPayLeg().get().resolve(REF_DATA);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_twoLegs() {
    ResolvedCms test = sut();
    assertThat(test.getCmsLeg()).isEqualTo(CMS_LEG);
    assertThat(test.getPayLeg().get()).isEqualTo(PAY_LEG);
    assertThat(test.allPaymentCurrencies()).containsOnly(CMS_LEG.getCurrency());
  }

  @Test
  public void test_of_oneLeg() {
    ResolvedCms test = ResolvedCms.of(CMS_LEG);
    assertThat(test.getCmsLeg()).isEqualTo(CMS_LEG);
    assertThat(test.getPayLeg().isPresent()).isFalse();
    assertThat(test.allPaymentCurrencies()).containsOnly(CMS_LEG.getCurrency());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
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
