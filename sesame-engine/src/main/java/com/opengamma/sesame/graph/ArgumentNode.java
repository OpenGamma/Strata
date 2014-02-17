/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;
import java.util.Objects;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;

/**
*
*/
public final class ArgumentNode extends Node {

  private final Object _value;

  /* package */ ArgumentNode(Class<?> type, Object value, Parameter parameter) {
    super(type, parameter);
    _value = value;
  }

  @Override
  protected Object doCreate(ComponentMap componentMap, List<Object> dependencies) {
    return _value;
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + getType().getSimpleName() + " " + _value;
  }

  public Object getValue() {
    return _value;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(_value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final ArgumentNode other = (ArgumentNode) obj;
    return Objects.equals(this._value, other._value);
  }
}
