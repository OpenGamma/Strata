/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class CompositeFunctionConfigTest {

  @Test
  public void compose2() {
    FunctionConfig config1 = config(implementations(Object.class, String.class,
                                                    Float.class, Character.class));
    FunctionConfig config2 = config(implementations(Object.class, Integer.class,
                                                    Long.class, Double.class));
    CompositeFunctionConfig config = new CompositeFunctionConfig(config1, config2);
    assertEquals(String.class, config.getFunctionImplementation(Object.class));
    assertEquals(Double.class, config.getFunctionImplementation(Long.class));
    assertEquals(Character.class, config.getFunctionImplementation(Float.class));
  }

  @Test
  public void composeMultiple() {
    FunctionConfig config1 = config(implementations(Object.class, String.class,
                                                    Float.class, Character.class));
    FunctionConfig config2 = config(implementations(Object.class, Integer.class,
                                                    Long.class, Double.class));
    FunctionConfig config3 = config(implementations(Number.class, Integer.class,
                                                    Long.class, Short.class));
    FunctionConfig config = CompositeFunctionConfig.compose(config1, config2, config3);
    assertEquals(String.class, config.getFunctionImplementation(Object.class));
    assertEquals(Double.class, config.getFunctionImplementation(Long.class));
    assertEquals(Character.class, config.getFunctionImplementation(Float.class));
    assertEquals(Integer.class, config.getFunctionImplementation(Number.class));
  }
}
