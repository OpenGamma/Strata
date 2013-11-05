/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

import com.opengamma.DataNotFoundException;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.function.InvokableFunction;

/**
 * TODO this class seems to be pointless and nothing but a wrapper for a map. maybe Graph.build should return the map of fns
 */
public final class Graph {

  /** Map of column names -> map of target ID -> function. */
  private final Map<String, Map<ObjectId, InvokableFunction>> _functions;

  /* package */ Graph(Map<String, Map<ObjectId, InvokableFunction>> functions) {
    _functions = functions;
  }

  public Map<ObjectId, InvokableFunction> getFunctionsForColumn(String columnName) {
    Map<ObjectId, InvokableFunction> functions = _functions.get(columnName);
    if (functions == null) {
      throw new DataNotFoundException("No column found with name " + columnName);
    }
    return functions;
  }
}
