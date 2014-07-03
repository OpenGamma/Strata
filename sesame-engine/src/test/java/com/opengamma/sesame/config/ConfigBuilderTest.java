/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.sesame.config;

import org.testng.annotations.Test;

import com.opengamma.util.test.TestGroup;

@Test(groups= TestGroup.UNIT)
public class ConfigBuilderTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testImplementationsInherit() throws Exception {
    ConfigBuilder.implementations(TestFunctionInterface.class, TestFunction.class);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testImplementationsInPairs() throws Exception {
    ConfigBuilder.implementations(TestFunction.class);
  }

  private interface TestFunctionInterface{
  }

  // doesn't implement interface
  private class TestFunction {
  }
}
