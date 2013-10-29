/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.DataNotFoundException;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.function.Invoker;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public final class FunctionGraph {

  // TODO need a method (or invoker) for the root functions

  /** Map of column names -> map of target ID -> function. */
  private final Map<String, Map<ObjectId, Object>> _functions;

  /* package */ FunctionGraph(Map<String, Map<ObjectId, Object>> functions) {
    ImmutableMap.Builder<String, Map<ObjectId, Object>> builder = ImmutableMap.builder();
    for (Map.Entry<String, Map<ObjectId, Object>> entry : functions.entrySet()) {
      String columnName = entry.getKey();
      ImmutableMap.Builder<ObjectId, Object> targetBuilder = ImmutableMap.builder();
      for (Map.Entry<ObjectId, Object> targetEntry : entry.getValue().entrySet()) {
        ObjectId targetId = targetEntry.getKey();
        Object function = targetEntry.getValue();
        targetBuilder.put(targetId, function);
      }
      builder.put(columnName, targetBuilder.build());
    }
    _functions = builder.build();
  }

  public Map<ObjectId, Object> getFunctionsForColumn(String columnName) {
    ArgumentChecker.notEmpty(columnName, "columnName");
    if (!_functions.containsKey(columnName)) {
      throw new DataNotFoundException("Unknown column name " + columnName);
    }
    return _functions.get(columnName);
  }

  public Map<String, Map<ObjectId, Invoker>> getFunctions() {
    return _functions;
  }
}
