/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

import com.opengamma.id.ObjectId;
import com.opengamma.sesame.function.Invoker;

/**
 * TODO this class seems to be pointless and nothing but a wrapper for a map. maybe Graph.build should return the map of fns
 */
public final class FunctionGraph {

  /** Map of column names -> map of target ID -> function. */
  private final Map<String, Map<ObjectId, Invoker>> _functions;

  /* package */ FunctionGraph(Map<String, Map<ObjectId, Invoker>> functions) {
    _functions = functions;
  }

  public Map<String, Map<ObjectId, Invoker>> getFunctions() {
    return _functions;
  }
}
