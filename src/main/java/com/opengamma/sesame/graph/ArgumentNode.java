/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;

/**
*
*/
public final class ArgumentNode extends Node {

  private final Class<?> _type;
  private final Object _value;

  /* package */ ArgumentNode(Class<?> type, Object value, Parameter parameter) {
    super(parameter);
    _type = type;
    _value = value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object create(ComponentMap componentMap) {
    return _value;
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + _type.getSimpleName() + " " + _value;
  }

  public Class<?> getType() {
    return _type;
  }

  public Object getValue() {
    return _value;
  }
}
