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

  /* package */ ArgumentNode(Class<?> type, Object value) {
    _type = type;
    _value = value;
  }

  @SuppressWarnings("unchecked")
  @Override
  Object create(Map<Class<?>, Object> singletons) {
    return _value;
  }

  public Class<?> getType() {
    return _type;
  }

  public Object getValue() {
    return _value;
  }
}
