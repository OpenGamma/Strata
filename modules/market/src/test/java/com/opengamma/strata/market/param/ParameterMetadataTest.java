/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ParameterMetadata}.
 */
public class ParameterMetadataTest {

  @Test
  public void test_empty() {
    ParameterMetadata test = ParameterMetadata.empty();
    assertThat(test.getLabel()).isEmpty();
    assertThat(test.getIdentifier()).isEqualTo("");
  }

  @Test
  public void test_listOfEmpty() {
    List<ParameterMetadata> test = ParameterMetadata.listOfEmpty(2);
    assertThat(test).hasSize(2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ParameterMetadata test = ParameterMetadata.empty();
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    ParameterMetadata test = ParameterMetadata.empty();
    assertSerialization(test);
  }

}
