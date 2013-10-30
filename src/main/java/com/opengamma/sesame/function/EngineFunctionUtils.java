/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.collect.Lists;
import com.opengamma.sesame.config.ConfigUtils;

/**
 * TODO does this need to exist any more?
 */
public final class EngineFunctionUtils {

  private EngineFunctionUtils() {
  }

  // TODO this only has one called. inline?
  /**
   * Returns metadata for the outputs a type can produce.
   * The type's public methods are scanned looking for {@link Output} annotations
   * @param type A function or class that can produce output values for the engine
   * @return Metadata for each of the methods that can produce an output
   */
  public static List<FunctionMetadata> getOutputFunctions(Class<?> type) {
    Constructor constructor = ConfigUtils.getConstructor(type);
    List<FunctionMetadata> functions = Lists.newArrayList();
    for (Method method : type.getMethods()) {
      if (method.isAnnotationPresent(Output.class)) {
        FunctionMetadata function = new FunctionMetadata(method, constructor);
        functions.add(function);
      }
    }
    // TODO check that there aren't any clashes between constructor and method param names
    // shouldn't matter if two methods have the same param names, the engine will only be calling one
    return functions;
  }
}
