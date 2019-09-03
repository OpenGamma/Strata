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
 * Test {@link RepoCurveInputsId}.
 */
public class RepoCurveInputsIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final CurveName NAME1 = CurveName.of("Name1");
  private static final CurveName NAME2 = CurveName.of("Name2");
  private static final ObservableSource SOURCE2 = ObservableSource.of("Vendor2");

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    RepoCurveInputsId test = RepoCurveInputsId.of(GROUP1, NAME1, ObservableSource.NONE);
    assertThat(test.getCurveGroupName()).isEqualTo(GROUP1);
    assertThat(test.getCurveName()).isEqualTo(NAME1);
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getMarketDataType()).isEqualTo(RatesCurveInputs.class);
    assertThat(test.toString()).isEqualTo("RepoCurveInputsId:Group1/Name1");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    RepoCurveInputsId test = RepoCurveInputsId.of(GROUP1, NAME1, ObservableSource.NONE);
    coverImmutableBean(test);
    RepoCurveInputsId test2 = RepoCurveInputsId.of(GROUP2, NAME2, SOURCE2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    RepoCurveInputsId test = RepoCurveInputsId.of(GROUP1, NAME1, ObservableSource.NONE);
    assertSerialization(test);
  }

}
