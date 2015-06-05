/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Test {@link EmptyCurveParameterMetadata}.
 */
@Test
public class EmptyCurveParameterMetadataTest {

  public void test_empty() {
    EmptyCurveParameterMetadata test = EmptyCurveParameterMetadata.empty();
    assertThat(test.getLabel()).isEqualTo("Empty");
    assertThat(test.getIdentifier()).isEqualTo("Empty");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    EmptyCurveParameterMetadata test = EmptyCurveParameterMetadata.empty();
    coverImmutableBean(test);
  }

  public void test_serialization() {
    EmptyCurveParameterMetadata test = EmptyCurveParameterMetadata.empty();
    assertSerialization(test);
  }

}
