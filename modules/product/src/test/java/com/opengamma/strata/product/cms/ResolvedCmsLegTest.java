/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link ResolvedCmsLeg}.
 */
public class ResolvedCmsLegTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final CmsPeriod PERIOD_1 = CmsLegTest.sutCap().resolve(REF_DATA).getCmsPeriods().get(0);
  private static final CmsPeriod PERIOD_2 = CmsLegTest.sutCap().resolve(REF_DATA).getCmsPeriods().get(1);

  @Test
  public void test_builder() {
    ResolvedCmsLeg test = sut();
    assertThat(test.getCmsPeriods()).hasSize(2);
    assertThat(test.getCmsPeriods().get(0)).isEqualTo(PERIOD_1);
    assertThat(test.getCmsPeriods().get(1)).isEqualTo(PERIOD_2);
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getStartDate()).isEqualTo(PERIOD_1.getStartDate());
    assertThat(test.getEndDate()).isEqualTo(PERIOD_2.getEndDate());
    assertThat(test.getIndex()).isEqualTo(PERIOD_1.getIndex());
    assertThat(test.getUnderlyingIndex())
        .isEqualTo(PERIOD_1.getIndex().getTemplate().getConvention().getFloatingLeg().getIndex());
    assertThat(test.getPayReceive()).isEqualTo(RECEIVE);
  }

  @Test
  public void test_builder_multiCurrencyIndex() {
    CmsPeriod period3 = CmsPeriodTest.sut2();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedCmsLeg.builder().payReceive(RECEIVE).cmsPeriods(PERIOD_1, period3).build());
    CmsPeriod period4 = CmsPeriodTest.sutCoupon();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedCmsLeg.builder().payReceive(RECEIVE).cmsPeriods(PERIOD_1, period4).build());
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
