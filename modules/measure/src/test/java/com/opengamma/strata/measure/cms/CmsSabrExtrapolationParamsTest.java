/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.cms;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link CmsSabrExtrapolationParams}.
 */
public class CmsSabrExtrapolationParamsTest {

  @Test
  public void test_of() {
    CmsSabrExtrapolationParams test = CmsSabrExtrapolationParams.of(1d, 2d);
    assertThat(test.getCutOffStrike()).isEqualTo(1d);
    assertThat(test.getMu()).isEqualTo(2d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CmsSabrExtrapolationParams test = CmsSabrExtrapolationParams.of(1d, 2d);
    coverImmutableBean(test);
    CmsSabrExtrapolationParams test2 = CmsSabrExtrapolationParams.of(3d, 4d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    CmsSabrExtrapolationParams test = CmsSabrExtrapolationParams.of(1d, 2d);
    assertSerialization(test);
  }

}
