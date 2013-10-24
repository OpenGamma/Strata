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
public final class Infrastructure extends Node {

  private final Class<?> _type;

  /* package */ Infrastructure(Class<?> type) {
    _type = type;
  }

  @SuppressWarnings("unchecked")
  @Override
  Object create(Map<Class<?>, Object> infrastructure) {
    return infrastructure.get(_type);
  }

  public Class<?> getType() {
    return _type;
  }
}
