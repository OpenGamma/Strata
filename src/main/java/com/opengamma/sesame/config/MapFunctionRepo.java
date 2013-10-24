/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Map;

import com.google.common.collect.Maps;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;
import com.opengamma.util.tuple.Triple;

/**
 * Extremely simple {@link FunctionRepo} backed by a map.
 */
/* package */ class MapFunctionRepo implements FunctionRepo {

  /** The concrete class implementing a function keyed by value name and target type. */
  private final Map<Pair<String, Class<?>>, Class<?>> _defaultFunctionTypes = Maps.newHashMap();

  /** The concrete class implementing a function keyed by value name, target type and implementation name. */
  private final Map<Triple<String, Class<?>, String>, Class<?>> _overrideFunctionTypes = Maps.newHashMap();

  @Override
  public Class<?> getDefaultFunctionImplementation(String valueName, Class<?> targetType) {
    return _defaultFunctionTypes.get(Pairs.<String, Class<?>>of(valueName, targetType));
  }

  @Override
  public Class<?> getFunctionImplementation(String valueName, Class<?> targetType, String implName) {
    return _overrideFunctionTypes.get(Triple.<String, Class<?>, String>of(valueName, targetType, implName));
  }

  /**
   * If it's an EngineFunction interface register the default type
   * If it's an impl register an override type
   * @param functionType
   * TODO this is only for functions with a target. what about other outputs?
   */
  /* package */ void register(Class<?> functionType) {
    if (functionType.isInterface()) {
      registerDefaultImplementation(functionType);
    } else {
      // TODO functionType must be an non-default implementation
    }
  }

  // TODO this shares a lot of logic with Injector, most of it in ConfigUtils
  private void registerDefaultImplementation(Class<?> functionType) {
    FunctionMetadata functionMeta = FunctionMetadata.forFunctionInterface(functionType);
    Class<?> defaultImplementation = functionMeta.getDefaultImplementation();
    String valueName = functionMeta.getValueName();
    Constructor[] constructors = defaultImplementation.getConstructors();
    // TODO relax this constraint but insist exactly one constructor is annotated (@Inject?)
    if (constructors.length != 1) {
      throw new IllegalArgumentException("Engine function implementations must have one constructor, " +
                                             defaultImplementation + " has " + constructors.length);
    }
    // find @Target on the constructor params
    Constructor constructor = constructors[0];
    Class[] parameterTypes = constructor.getParameterTypes();
    Class<?> targetType = null;
    for (int i = 0; i < constructor.getParameterAnnotations().length; i++) {
      Annotation[] annotations = constructor.getParameterAnnotations()[i];
      for (Annotation annotation : annotations) {
        if (annotation.getClass().equals(Target.class)) {
          if (targetType == null) {
            targetType = parameterTypes[i];
          } else {
            throw new IllegalArgumentException("Only one constructor parameter should be annotated with @Target in " +
                                                   defaultImplementation);
          }
        }
      }
    }
    if (targetType == null) {
      throw new IllegalArgumentException("No constructor parameter found with a @Target annotation in " + defaultImplementation);
    }
    _defaultFunctionTypes.put(Pairs.<String, Class<?>>of(valueName, targetType), defaultImplementation);
  }
}
