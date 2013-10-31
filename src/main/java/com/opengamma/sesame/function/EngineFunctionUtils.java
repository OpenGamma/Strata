/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Method;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * TODO does this need to exist any more?
 */
public final class EngineFunctionUtils {

  private EngineFunctionUtils() {
  }

  /**
   * Returns metadata for the outputs a type can produce.
   * The type's public methods are scanned looking for {@link Output} annotations
   * @param type A function or class that can produce output values for the engine
   * @return Metadata for each of the methods that can produce an output
   * TODO this is a big problem
   * we can't possibly know the implementations of an interface at this point and therefore can't provide a
   * constructor to FunctionMetadata. which is a bit of a snag
   */
  public static List<FunctionMetadata> getOutputFunctions(Class<?> type) {
    List<FunctionMetadata> functions = Lists.newArrayList();
    for (Method method : type.getMethods()) {
      if (method.isAnnotationPresent(Output.class)) {
        FunctionMetadata function = new FunctionMetadata(method);
        functions.add(function);
      }
    }
    // TODO check that there aren't any clashes between constructor and method param names
    // shouldn't matter if two methods have the same param names, the engine will only be calling one
    return functions;
  }
}
