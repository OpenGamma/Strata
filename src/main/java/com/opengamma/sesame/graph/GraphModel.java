/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.InvokableFunction;

/**
 * Lightweight model of the functions needed to generate the outputs for a view.
 */
public final class GraphModel {

  // TODO for this to be useful in the UI it needs to be map(column -> map((outputName,inputType) -> functionModel))
  // it will probably be better to have a real class than a rats' nest of generics
  private final Map<String, Map<ObjectId, FunctionModel>> _functionTrees;

  /* package */ GraphModel(Map<String, Map<ObjectId, FunctionModel>> functionTrees) {
    _functionTrees = functionTrees;
  }

  /**
   * TODO just return the map? function graph doesn't serve any purpose in its current form
   *
   * @return A graph containing the built function instances
   */
  public Graph build(ComponentMap components) {
    ImmutableMap.Builder<String, Map<ObjectId, InvokableFunction>> builder = ImmutableMap.builder();
    for (Map.Entry<String, Map<ObjectId, FunctionModel>> entry : _functionTrees.entrySet()) {
      Map<ObjectId, FunctionModel> functionsByTargetId = entry.getValue();
      ImmutableMap.Builder<ObjectId, InvokableFunction> columnBuilder = ImmutableMap.builder();
      for (Map.Entry<ObjectId, FunctionModel> columnEntry : functionsByTargetId.entrySet()) {
        ObjectId targetId = columnEntry.getKey();
        FunctionModel functionModel = columnEntry.getValue();
        columnBuilder.put(targetId, functionModel.build(components));
      }
      String columnName = entry.getKey();
      builder.put(columnName, columnBuilder.build());
    }
    return new Graph(builder.build());
  }
}
