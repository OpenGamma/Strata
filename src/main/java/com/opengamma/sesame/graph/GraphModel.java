/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.InvokableFunction;

/**
 * Lightweight model of the functions needed to generate the outputs for a view.
 */
public final class GraphModel {

  // TODO for this to be useful in the UI it needs to be map(column -> map((outputName,inputType) -> functionModel))
  // it will probably be better to have a real class than a rats' nest of generics
  private final Map<String, Map<Class<?>, FunctionModel>> _functionTrees;

  /* package */ GraphModel(Map<String, Map<Class<?>, FunctionModel>> functionTrees) {
    _functionTrees = functionTrees;
  }

  /**
   * @return A graph containing the built function instances
   */
  public Graph build(ComponentMap components) {
    ImmutableMap.Builder<String, Map<Class<?>, InvokableFunction>> builder = ImmutableMap.builder();
    FunctionBuilder functionBuilder = new FunctionBuilder();
    for (Map.Entry<String, Map<Class<?>, FunctionModel>> entry : _functionTrees.entrySet()) {
      Map<Class<?>, FunctionModel> functionsByTargetId = entry.getValue();
      ImmutableMap.Builder<Class<?>, InvokableFunction> columnBuilder = ImmutableMap.builder();
      for (Map.Entry<Class<?>, FunctionModel> columnEntry : functionsByTargetId.entrySet()) {
        Class<?> inputType = columnEntry.getKey();
        FunctionModel functionModel = columnEntry.getValue();
        columnBuilder.put(inputType, functionModel.build(functionBuilder, components));
      }
      String columnName = entry.getKey();
      builder.put(columnName, columnBuilder.build());
    }
    return new Graph(builder.build());
  }
}
