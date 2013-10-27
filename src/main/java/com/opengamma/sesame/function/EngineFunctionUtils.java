/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.opengamma.sesame.config.ConfigUtils;

/**
 *
 */
public final class EngineFunctionUtils {

  private EngineFunctionUtils() {
  }

  private static ConcurrentMap<Class<?>, Class<?>> s_targetTypes = Maps.newConcurrentMap();

  public static String getOutputName(Class<? extends OutputFunction<?, ?>> type) {
    OutputName annotation = type.getAnnotation(OutputName.class);
    if (annotation == null) {
      throw new IllegalArgumentException("All OutputFunction implementations should be annotated with OutputName. " +
                                             type.getName() + " isn't");
    }
    return annotation.value();
  }

  public static Class<?> getTargetType(Class<? extends OutputFunction<?, ?>> type) {
    if (s_targetTypes.containsKey(type)) {
      return s_targetTypes.get(type);
    }
    Set<Class<?>> supertypes = ConfigUtils.getSupertypes(type);
    for (Class<?> supertype : supertypes) {
      for (Type anInterface : supertype.getGenericInterfaces()) {
        if (anInterface instanceof ParameterizedType && ((ParameterizedType) anInterface).getRawType().equals(OutputFunction.class)) {
          Type targetType = ((ParameterizedType) anInterface).getActualTypeArguments()[0];
          // cache the result, it won't change and it will save walking up the type hierarchy every time
          s_targetTypes.put(type, (Class<?>) targetType);
          return (Class<?>) targetType;
        }
      }
    }
    throw new IllegalArgumentException("execute method not found on " + type.getName()); // shouldn't happen
  }

  public static Class<?> getDefaultImplementation(Class<?> type) {
    if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
      DefaultImplementation annotation = type.getAnnotation(DefaultImplementation.class);
      if (annotation == null) {
        throw new IllegalArgumentException(type.getName() + " isn't annotated with @DefaultImplementation");
      }
      return annotation.value();
    } else {
      return type;
    }
  }
}
