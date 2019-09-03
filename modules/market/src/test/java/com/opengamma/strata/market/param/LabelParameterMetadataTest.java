/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link LabelParameterMetadata}.
 */
public class LabelParameterMetadataTest {

  @Test
  public void test_of() {
    LabelParameterMetadata test = LabelParameterMetadata.of("Label");
    assertThat(test.getLabel()).isEqualTo("Label");
    assertThat(test.getIdentifier()).isEqualTo("Label");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    LabelParameterMetadata test = LabelParameterMetadata.of("Label");
    coverImmutableBean(test);
    LabelParameterMetadata test2 = LabelParameterMetadata.of("Label2");
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    LabelParameterMetadata test = LabelParameterMetadata.of("Label");
    assertSerialization(test);
  }

}
