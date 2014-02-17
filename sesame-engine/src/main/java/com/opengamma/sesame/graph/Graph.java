/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

import com.opengamma.DataNotFoundException;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.util.ArgumentChecker;

/**
 * TODO this class seems to be pointless and nothing but a wrapper for a map. maybe Graph.build should return the map of fns
 * TODO would be a lot more efficient for graph building to key functions by column and input type
 * but this wouldn't work for stateful functions where we need to key the function by the input ID so it's not shared.
 * have a strategy for linking inputs to function? could use more efficient type based strategy for the normal case
 * and degenerate to the slower ID based strategy for (the hopefully rare case of) stateful functions?
 */
public final class Graph {

  /** Map of column names -> map of input type -> function. */
  private final Map<String, Map<Class<?>, InvokableFunction>> _functions;

  /** Functions for non-portfolio outputs, keyed by name */
  private final Map<String, InvokableFunction> _nonPortfolioFunctions;

  /* package */ Graph(Map<String, Map<Class<?>, InvokableFunction>> functions,
                      Map<String, InvokableFunction> nonPortfolioFunctions) {
    _functions = ArgumentChecker.notNull(functions, "functions");
    _nonPortfolioFunctions = ArgumentChecker.notNull(nonPortfolioFunctions, "nonPortfolioFunctions");
  }

  public Map<Class<?>, InvokableFunction> getFunctionsForColumn(String columnName) {
    Map<Class<?>, InvokableFunction> functions = _functions.get(columnName);
    if (functions == null) {
      // TODO IllegalArgumentException?
      throw new DataNotFoundException("No column found with name " + columnName);
    }
    return functions;
  }

  public InvokableFunction getNonPortfolioFunction(String name) {
    InvokableFunction function = _nonPortfolioFunctions.get(name);
    if (function == null) {
      throw new IllegalArgumentException("No function found for output named '" + name + "'");
    }
    return function;
  }
}
