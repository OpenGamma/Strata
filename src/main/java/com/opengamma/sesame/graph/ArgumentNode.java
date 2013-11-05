/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import com.opengamma.sesame.engine.ComponentMap;

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
  public Object create(ComponentMap components) {
    return _value;
  }

  public Class<?> getType() {
    return _type;
  }

  public Object getValue() {
    return _value;
  }
}
