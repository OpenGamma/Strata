/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.util.ArgumentChecker;

/**
 * Metadata for a method or constructor parameter.
 */
public final class Parameter {

  private final String _name;
  private final Class<?> _type;
  private final int _ordinal;
  private final ImmutableMap<Class<?>, Annotation> _annotations;
  // TODO nullability flag

  public Parameter(String name, Class<?> type, int ordinal, Map<Class<?>, Annotation> annotations) {
    _name = name;
    _ordinal = ordinal;
    ArgumentChecker.notNull(type, "type");
    ArgumentChecker.notNull(annotations, "annotations");
    _type = type;
    _annotations = ImmutableMap.copyOf(annotations);
  }

  public String getName() {
    return _name;
  }

  public int getOrdinal() {
    return _ordinal;
  }

  public Class<?> getType() {
    return _type;
  }

  public ImmutableMap<Class<?>, Annotation> getAnnotations() {
    return _annotations;
  }
}
