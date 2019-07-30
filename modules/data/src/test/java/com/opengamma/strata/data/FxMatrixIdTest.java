/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.FxMatrix;

/**
 * Test {@link FxMatrixId}.
 */
public class FxMatrixIdTest {

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  @Test
  public void test_standard() {
    FxMatrixId test = FxMatrixId.standard();
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getMarketDataType()).isEqualTo(FxMatrix.class);
    assertThat(test.toString()).isEqualTo("FxMatrixId");
  }

  @Test
  public void test_of() {
    FxMatrixId test = FxMatrixId.of(OBS_SOURCE);
    assertThat(test.getObservableSource()).isEqualTo(OBS_SOURCE);
    assertThat(test.getMarketDataType()).isEqualTo(FxMatrix.class);
    assertThat(test.toString()).isEqualTo("FxMatrixId:Vendor");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxMatrixId test = FxMatrixId.standard();
    coverImmutableBean(test);
    FxMatrixId test2 = FxMatrixId.of(OBS_SOURCE);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FxMatrixId test = FxMatrixId.standard();
    assertSerialization(test);
  }

}
