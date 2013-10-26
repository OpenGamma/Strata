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
 *
 */
public final class Parameter {

  private final Class<?> _type;
  private final ImmutableMap<Class<?>, Annotation> _annotations;

  public Parameter(Class<?> type, Map<Class<?>, Annotation> annotations) {
    ArgumentChecker.notNull(type, "type");
    ArgumentChecker.notNull(annotations, "annotations");
    _type = type;
    _annotations = ImmutableMap.copyOf(annotations);
  }

  public Class<?> getType() {
    return _type;
  }

  public ImmutableMap<Class<?>, Annotation> getAnnotations() {
    return _annotations;
  }
}
