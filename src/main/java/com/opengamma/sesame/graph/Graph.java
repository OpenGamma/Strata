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
 * TODO would be a lot more efficient for graph building to key functions by column and input type
 * but this wouldn't work for stateful functions where we need to key the function by the input ID so it's not shared.
 * have a strategy for linking inputs to function? could use more efficient type based strategy for the normal case
 * and degenerate to the slower ID based strategy for (the hopefully rare case of) stateful functions?
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
