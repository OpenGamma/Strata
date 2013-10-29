/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.sesame.config.ConfigUtils;

/**
 * TODO would a type parameter for the underlying class actually be useful here?
 * TODO joda bean?
 */
public class TypeMetadata {

  private final Class<?> _type;
  private final Constructor<?> _constructor;
  private final List<FunctionMetadata> _functions;
  // TODO function metadata. in a map? keyed on? repo registration will want to query this for functions
  // need
  //   output names
  //   input types for each output

  /* package */ TypeMetadata(Class<?> type) {
    _type = type;
    _constructor = ConfigUtils.getConstructor(type);
    List<FunctionMetadata> functions = Lists.newArrayList();
    for (Method method : type.getMethods()) {
      if (method.isAnnotationPresent(Output.class)) {
        FunctionMetadata function = new FunctionMetadata(method, this);
        functions.add(function);
      }
    }
    // TODO check that there aren't any clashes between constructor and method param names
    // shouldn't matter if two methods have the same param names, the engine will only be calling one
    _functions = ImmutableList.copyOf(functions);
  }

  public Class<?> getType() {
    return _type;
  }

  public Constructor<?> getConstructor() {
    return _constructor;
  }

  public List<FunctionMetadata> getFunctions() {
    return _functions;
  }
}
