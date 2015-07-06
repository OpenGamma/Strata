/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Test {@link EmptySurfaceParameterMetadata}.
 */
@Test
public class EmptySurfaceParameterMetadataTest {

  public void test_empty() {
    EmptySurfaceParameterMetadata test = EmptySurfaceParameterMetadata.empty();
    assertThat(test.getLabel()).isEqualTo("Empty");
    assertThat(test.getIdentifier()).isEqualTo("Empty");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    EmptySurfaceParameterMetadata test = EmptySurfaceParameterMetadata.empty();
    coverImmutableBean(test);
  }

  public void test_serialization() {
    EmptySurfaceParameterMetadata test = EmptySurfaceParameterMetadata.empty();
    assertSerialization(test);
  }

}
