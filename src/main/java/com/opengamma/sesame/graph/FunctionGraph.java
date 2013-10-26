/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.DataNotFoundException;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.function.OutputFunction;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public final class FunctionGraph {

  /** Map of column names -> map of target ID -> function. */
  private final Map<String, Map<ObjectId, OutputFunction<PositionOrTrade, ?>>> _functions;

  /* package */ FunctionGraph(Map<String, Map<ObjectId, OutputFunction<?, ?>>> functions) {
    ImmutableMap.Builder<String, Map<ObjectId, OutputFunction<PositionOrTrade, ?>>> builder = ImmutableMap.builder();
    for (Map.Entry<String, Map<ObjectId, OutputFunction<?, ?>>> entry : functions.entrySet()) {
      String columnName = entry.getKey();
      ImmutableMap.Builder<ObjectId, OutputFunction<PositionOrTrade, ?>> targetBuilder = ImmutableMap.builder();
      for (Map.Entry<ObjectId, OutputFunction<?, ?>> targetEntry : entry.getValue().entrySet()) {
        ObjectId targetId = targetEntry.getKey();
        @SuppressWarnings("unchecked")
        OutputFunction<PositionOrTrade, ?> function = (OutputFunction<PositionOrTrade, ?>) targetEntry.getValue();
        targetBuilder.put(targetId, function);
      }
      builder.put(columnName, targetBuilder.build());
    }
    _functions = builder.build();
  }

  public Map<ObjectId, OutputFunction<PositionOrTrade, ?>> getFunctionsForColumn(String columnName) {
    ArgumentChecker.notEmpty(columnName, "columnName");
    if (!_functions.containsKey(columnName)) {
      throw new DataNotFoundException("Unknown column name " + columnName);
    }
    return _functions.get(columnName);
  }

  public Map<String, Map<ObjectId, OutputFunction<PositionOrTrade, ?>>> getFunctions() {
    return _functions;
  }
}
