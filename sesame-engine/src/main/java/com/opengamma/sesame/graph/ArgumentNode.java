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
 * A node in the function model that supplies a fixed object value.
 * <p>
 * This represents an argument value.
 */
public final class ArgumentNode extends Node {

  /**
   * The argument value.
   */
  private final Object _value;

  /**
   * Creates an instance.
   * 
   * @param type  the expected type of the object created by this node, not null
   * @param parameter  the parameter this node satisfies, null if it's the root node
   * @param value  the argument value, may be null
   */
  /* package */ ArgumentNode(Class<?> type, Object value, Parameter parameter) {
    super(type, parameter);
    _value = value;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the argument value.
   * 
   * @return the argument value, may be null
   */
  public Object getValue() {
    return _value;
  }

  //-------------------------------------------------------------------------
  @Override
  protected Object doCreate(ComponentMap componentMap, List<Object> dependencies) {
    return _value;
  }

  @Override
  protected String prettyPrintLine() {
    return getType().getSimpleName() + " " + _value;
  }

  //-------------------------------------------------------------------------
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

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(_value);
  }

}
