/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.function.Invoker;

/**
 * TODO this class seems to be pointless and nothing but a wrapper for a map. maybe Graph.build should return the map of fns
 */
public final class FunctionGraph {

  // TODO need a method (or invoker) for the root functions

  /** Map of column names -> map of target ID -> function. */
  private final Map<String, Map<ObjectId, Invoker>> _functions;

  /* package */ FunctionGraph(Map<String, Map<ObjectId, Object>> functions) {
    ImmutableMap.Builder<String, Map<ObjectId, Invoker>> builder = ImmutableMap.builder();
    for (Map.Entry<String, Map<ObjectId, Object>> entry : functions.entrySet()) {
      String columnName = entry.getKey();
      ImmutableMap.Builder<ObjectId, Invoker> columnBuilder = ImmutableMap.builder();
      for (Map.Entry<ObjectId, Object> inputEntry : entry.getValue().entrySet()) {
        ObjectId inputId = inputEntry.getKey();
        Object function = inputEntry.getValue();
        // TODO function needs to be wrapped in an invoker. need the FunctionMetadata
        columnBuilder.put(inputId, function);
      }
      builder.put(columnName, columnBuilder.build());
    }
    _functions = builder.build();
  }

  public Map<String, Map<ObjectId, Invoker>> getFunctions() {
    return _functions;
  }
}
