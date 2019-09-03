/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.data.ObservableSource;

/**
 * Test {@link LegalEntityCurveGroupId}.
 */
public class LegalEntityCurveGroupIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final ObservableSource OBS_SOURCE2 = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  @Test
  public void test_of_String() {
    LegalEntityCurveGroupId test = LegalEntityCurveGroupId.of(GROUP1.toString());
    assertThat(test.getCurveGroupName()).isEqualTo(GROUP1);
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getMarketDataType()).isEqualTo(LegalEntityCurveGroup.class);
    assertThat(test.toString()).isEqualTo("LegalEntityCurveGroupId:Group1");
  }

  @Test
  public void test_of_Type() {
    LegalEntityCurveGroupId test = LegalEntityCurveGroupId.of(GROUP1);
    assertThat(test.getCurveGroupName()).isEqualTo(GROUP1);
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getMarketDataType()).isEqualTo(LegalEntityCurveGroup.class);
    assertThat(test.toString()).isEqualTo("LegalEntityCurveGroupId:Group1");
  }

  @Test
  public void test_of_TypeSource() {
    LegalEntityCurveGroupId test = LegalEntityCurveGroupId.of(GROUP1, OBS_SOURCE2);
    assertThat(test.getCurveGroupName()).isEqualTo(GROUP1);
    assertThat(test.getObservableSource()).isEqualTo(OBS_SOURCE2);
    assertThat(test.getMarketDataType()).isEqualTo(LegalEntityCurveGroup.class);
    assertThat(test.toString()).isEqualTo("LegalEntityCurveGroupId:Group1/Vendor");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    LegalEntityCurveGroupId test = LegalEntityCurveGroupId.of(GROUP1);
    coverImmutableBean(test);
    LegalEntityCurveGroupId test2 = LegalEntityCurveGroupId.of(GROUP2, OBS_SOURCE2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    LegalEntityCurveGroupId test = LegalEntityCurveGroupId.of(GROUP1);
    assertSerialization(test);
  }

}
