/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class CompositeFunctionModelConfigTest {

  @Test
  public void compose2() {
    FunctionModelConfig config1 = config(implementations(Object.class, String.class,
                                                         Map.class, HashMap.class));
    FunctionModelConfig config2 = config(implementations(Object.class, Integer.class,
                                                         Number.class, Double.class));
    CompositeFunctionModelConfig config = new CompositeFunctionModelConfig(config1, config2);
    assertEquals(String.class, config.getFunctionImplementation(Object.class));
    assertEquals(Double.class, config.getFunctionImplementation(Number.class));
    assertEquals(HashMap.class, config.getFunctionImplementation(Map.class));
  }

  @Test
  public void composeMultiple() {
    FunctionModelConfig config1 = config(implementations(Object.class, String.class,
                                                         Map.class, HashMap.class));
    FunctionModelConfig config2 = config(implementations(Object.class, Integer.class,
                                                         Number.class, Double.class));
    FunctionModelConfig config3 = config(implementations(Set.class, HashSet.class,
                                                         Set.class, HashSet.class));
    FunctionModelConfig config = CompositeFunctionModelConfig.compose(config1, config2, config3);
    assertEquals(String.class, config.getFunctionImplementation(Object.class));
    assertEquals(Double.class, config.getFunctionImplementation(Number.class));
    assertEquals(HashMap.class, config.getFunctionImplementation(Map.class));
    assertEquals(HashSet.class, config.getFunctionImplementation(Set.class));
  }
}
