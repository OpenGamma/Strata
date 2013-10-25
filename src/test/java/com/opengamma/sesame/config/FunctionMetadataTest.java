/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.util.test.TestGroup;

@SuppressWarnings("ALL")
@Test(groups = TestGroup.UNIT)
public class FunctionMetadataTest {

  /* package */ static final String VALUE_NAME = "ValueName";

  @DefaultImplementation(Long.class)
  interface I {

    @EngineFunction(VALUE_NAME)
    void fn(@Target String s);
  }

  @Test
  public void metadata() {
    FunctionMetadata metadata = FunctionMetadata.forFunctionInterface(I.class);
    assertEquals(String.class, metadata.getTargetType());
    assertEquals(Long.class, metadata.getDefaultImplementation());
    assertEquals(VALUE_NAME, metadata.getValueName());
  }
}
