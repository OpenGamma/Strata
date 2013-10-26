/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *
 */
public final class EngineFunctionUtils {

  private EngineFunctionUtils() {
  }

  public static String getOutputName(Class<? extends PortfolioOutputFunction<?, ?>> type) {
    OutputName annotation = type.getAnnotation(OutputName.class);
    if (annotation == null) {
      throw new IllegalArgumentException("All OutputFunction implementations should be annotated with OutputName. " +
                                             type.getName() + " isn't");
    }
    return annotation.value();
  }

  // TODO this will fail if the class doesn't directly implement OutputFunction
  public static Class<?> getTargetType(Class<? extends PortfolioOutputFunction<?, ?>> type) {
    for (Type anInterface : type.getGenericInterfaces()) {
      if (anInterface instanceof ParameterizedType && ((ParameterizedType) anInterface).getRawType().equals(PortfolioOutputFunction.class)) {
        Type targetType = ((ParameterizedType) anInterface).getActualTypeArguments()[0];
        return (Class<?>) targetType;
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
