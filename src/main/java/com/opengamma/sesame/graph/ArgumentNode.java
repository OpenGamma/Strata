/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

/**
*
*/
public final class ArgumentNode extends Node {

  private final Class<?> _type;
  private final Object _value;
  private final Object _defaultValue;

  /* package */ ArgumentNode(Class<?> type, Object value, Object defaultValue) {
    _type = type;
    _value = value;
    _defaultValue = defaultValue;
  }

  @SuppressWarnings("unchecked")
  @Override
  Object create(Map<Class<?>, Object> infrastructure) {
    return _value;
  }

  public Class<?> getType() {
    return _type;
  }

  public Object getValue() {
    return _value;
  }

  public Object getDefaultValue() {
    return _defaultValue;
  }
}
