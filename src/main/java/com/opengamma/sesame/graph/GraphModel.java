/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.function.AdaptingFunctionMetadata;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.FunctionRepo;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.function.NoOutputFunction;

/**
 * Lightweight model of the functions needed to generate the outputs for a view.
 */
public final class GraphModel {

  // TODO for this to be useful in the UI it needs to be map(column -> map((outputName,inputType) -> functionTree))
  // TODO probably better to have a real class that a rats' nest of generics
  private final Map<String, Map<ObjectId, FunctionModel>> _functionTrees;

  private GraphModel(Map<String, Map<ObjectId, FunctionModel>> functionTrees) {
    _functionTrees = functionTrees;
  }

  public static GraphModel forView(ViewDef viewDef,
                                   Collection<? extends PositionOrTrade> inputs,
                                   FunctionRepo functionRepo,
                                   Map<Class<?>, Object> singletons) {
    ImmutableMap.Builder<String, Map<ObjectId, FunctionModel>> builder = ImmutableMap.builder();
    for (ViewColumn column : viewDef.getColumns()) {
      ImmutableMap.Builder<ObjectId, FunctionModel> columnBuilder = ImmutableMap.builder();
      for (PositionOrTrade posOrTrade : inputs) {
        // TODO no need to create a FunctionTree for every target, cache on outputName/inputType

        // look for an output for the position or trade
        String posOrTradeOutput = column.getOutputName(posOrTrade.getClass());
        if (posOrTradeOutput != null) {
          FunctionMetadata function = functionRepo.getOutputFunction(posOrTradeOutput, posOrTrade.getClass());
          if (function != null) {
            GraphConfig graphConfig = new GraphConfig(posOrTrade, column, functionRepo, singletons);
            FunctionModel functionModel = FunctionModel.forFunction(function, graphConfig);
            columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionModel);
            continue;
          }
        }

        // look for an output for the security type
        Security security = posOrTrade.getSecurity();
        String securityOutput = column.getOutputName(security.getClass());
        if (securityOutput != null) {
          FunctionMetadata functionType = functionRepo.getOutputFunction(securityOutput, security.getClass());
          if (functionType != null) {
            GraphConfig graphConfig = new GraphConfig(security, column, functionRepo, singletons);
            FunctionModel securityModel = FunctionModel.forFunction(functionType, graphConfig);
            // TODO factory method on AdaptingFunctionMetadata?
            FunctionModel functionModel = new FunctionModel(securityModel.getRootFunction(),
                                                            new AdaptingFunctionMetadata(securityModel.getRootMetadata()));
            columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionModel);
            continue;
          }
        }
        FunctionModel functionModel = FunctionModel.forFunction(NoOutputFunction.METADATA);
        // TODO how will this work for in-memory trades? assign an ID? use object identity?
        columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionModel);
      }
      builder.put(column.getName(), columnBuilder.build());
    }
    return new GraphModel(builder.build());
  }

  /**
   * TODO just return the map? function graph doesn't serve any purpose in its current form
   *
   * @param infrastructure The engine infrastructure
   * @return A graph containing the built function instances
   */
  public FunctionGraph build(Map<Class<?>, Object> infrastructure) {
    ImmutableMap.Builder<String, Map<ObjectId, InvokableFunction>> builder = ImmutableMap.builder();
    for (Map.Entry<String, Map<ObjectId, FunctionModel>> entry : _functionTrees.entrySet()) {
      Map<ObjectId, FunctionModel> functionsByTargetId = entry.getValue();
      ImmutableMap.Builder<ObjectId, InvokableFunction> columnBuilder = ImmutableMap.builder();
      for (Map.Entry<ObjectId, FunctionModel> columnEntry : functionsByTargetId.entrySet()) {
        ObjectId targetId = columnEntry.getKey();
        FunctionModel functionModel = columnEntry.getValue();
        columnBuilder.put(targetId, functionModel.build(infrastructure));
      }
      String columnName = entry.getKey();
      builder.put(columnName, columnBuilder.build());
    }
    return new FunctionGraph(builder.build());
  }
}
