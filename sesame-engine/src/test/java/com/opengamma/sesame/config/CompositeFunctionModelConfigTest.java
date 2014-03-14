/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class CompositeFunctionModelConfigTest {

  @Test
  public void compose2() {
    FunctionModelConfig config1 = config(implementations(Object.class, String.class,
                                                         Float.class, Character.class));
    FunctionModelConfig config2 = config(implementations(Object.class, Integer.class,
                                                         Long.class, Double.class));
    CompositeFunctionModelConfig config = new CompositeFunctionModelConfig(config1, config2);
    Map<Class<?>, Annotation> annotations = Collections.emptyMap();
    Parameter parameter = new Parameter(Object.class, "notUsed", Object.class, 0, annotations);
    assertEquals(String.class, config.getFunctionImplementation(Object.class, parameter));
    assertEquals(Double.class, config.getFunctionImplementation(Long.class, parameter));
    assertEquals(Character.class, config.getFunctionImplementation(Float.class, parameter));
  }

  @Test
  public void composeMultiple() {
    FunctionModelConfig config1 = config(implementations(Object.class, String.class,
                                                         Float.class, Character.class));
    FunctionModelConfig config2 = config(implementations(Object.class, Integer.class,
                                                         Long.class, Double.class));
    FunctionModelConfig config3 = config(implementations(Number.class, Integer.class,
                                                         Long.class, Short.class));
    FunctionModelConfig config = CompositeFunctionModelConfig.compose(config1, config2, config3);
    Map<Class<?>, Annotation> annotations = Collections.emptyMap();
    Parameter parameter = new Parameter(Object.class, "notUsed", Object.class, 0, annotations);
    assertEquals(String.class, config.getFunctionImplementation(Object.class, parameter));
    assertEquals(Double.class, config.getFunctionImplementation(Long.class, parameter));
    assertEquals(Character.class, config.getFunctionImplementation(Float.class, parameter));
    assertEquals(Integer.class, config.getFunctionImplementation(Number.class, parameter));
  }
}
