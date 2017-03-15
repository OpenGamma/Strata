/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link LabelParameterMetadata}.
 */
@Test
public class LabelParameterMetadataTest {

  public void test_of() {
    LabelParameterMetadata test = LabelParameterMetadata.of("Label");
    assertEquals(test.getLabel(), "Label");
    assertEquals(test.getIdentifier(), "Label");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    LabelParameterMetadata test = LabelParameterMetadata.of("Label");
    coverImmutableBean(test);
    LabelParameterMetadata test2 = LabelParameterMetadata.of("Label2");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    LabelParameterMetadata test = LabelParameterMetadata.of("Label");
    assertSerialization(test);
  }

}
