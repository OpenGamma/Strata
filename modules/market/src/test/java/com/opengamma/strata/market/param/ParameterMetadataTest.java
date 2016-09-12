/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

/**
 * Test {@link ParameterMetadata}.
 */
@Test
public class ParameterMetadataTest {

  public void test_empty() {
    ParameterMetadata test = ParameterMetadata.empty();
    assertEquals(test.getLabel(), "");
    assertEquals(test.getIdentifier(), "");
  }

  public void test_listOfEmpty() {
    List<ParameterMetadata> test = ParameterMetadata.listOfEmpty(2);
    assertEquals(test.size(), 2);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ParameterMetadata test = ParameterMetadata.empty();
    coverImmutableBean(test);
  }

  public void test_serialization() {
    ParameterMetadata test = ParameterMetadata.empty();
    assertSerialization(test);
  }

}
