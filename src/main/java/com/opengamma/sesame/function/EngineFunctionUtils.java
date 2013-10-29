/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 *
 */
public final class EngineFunctionUtils {

  private EngineFunctionUtils() {
  }

  private static ConcurrentMap<Class<?>, Class<?>> s_inputTypes = Maps.newConcurrentMap();

  public static String getOutputName(Class<? extends OutputFunction<?, ?>> type) {
    Output annotation = type.getAnnotation(Output.class);
    if (annotation == null) {
      throw new IllegalArgumentException("All OutputFunction implementations should be annotated with OutputName. " +
                                             type.getName() + " isn't");
    }
    return annotation.value();
  }

  // TODO do I need a high level type (maybe FunctionMetadata) will metadata for a class, its constructor params
  // and each of its output methods and their params

  // TODO does this logic belong in the constructor of FunctionMetadata? or TypeMetadata?
  // TODO this should return method metadata that can build an invoker
  // i.e. knows about the method and how to map the target and other args onto the parameters
  public static Set<Pair<String, Class<?>>> getOutputs(Class<?> functionType) {
    Set<Pair<String, Class<?>>> outputs = Sets.newHashSet();
    for (Method method : functionType.getMethods()) {
      if (method.isAnnotationPresent(Output.class)) {
        String outputName = method.getAnnotation(Output.class).value();
        Parameter targetParam = ConfigUtils.getAnnotatedParameter(Target.class, method);
        // TODO targetParam can be null for non-target outputs (where the fn has no params or they're in the args)
        Class<?> targetType = targetParam.getType();
        outputs.add(Pairs.<String, Class<?>>of(outputName, targetType));
      }
    }
    return outputs;
  }

  // TODO this needs to take a method or maybe FunctionMetadata
  public static Class<?> getInputType(Class<?> type) {
    if (s_inputTypes.containsKey(type)) {
      return s_inputTypes.get(type);
    }
    Set<Class<?>> supertypes = ConfigUtils.getSupertypes(type);
    for (Class<?> supertype : supertypes) {
      for (Type anInterface : supertype.getGenericInterfaces()) {
        if (anInterface instanceof ParameterizedType && ((ParameterizedType) anInterface).getRawType().equals(OutputFunction.class)) {
          Type targetType = ((ParameterizedType) anInterface).getActualTypeArguments()[0];
          // cache the result, it won't change and it will save walking up the type hierarchy every time
          s_inputTypes.put(type, (Class<?>) targetType);
          return (Class<?>) targetType;
        }
      }
    }
    throw new IllegalArgumentException("execute method not found on " + type.getName()); // shouldn't happen
  }

  public static Class<?> getDefaultImplementation(Class<?> type) {
    // TODO override mechanism
    if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
      FallbackImplementation annotation = type.getAnnotation(FallbackImplementation.class);
      if (annotation == null) {
        throw new IllegalArgumentException(type.getName() + " isn't annotated with @FallbackImplementation");
      }
      return annotation.value();
    } else {
      return type;
    }
  }
}
