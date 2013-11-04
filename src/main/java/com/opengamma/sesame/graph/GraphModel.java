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
import com.opengamma.sesame.config.CompositeFunctionConfig;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.DefaultImplementationProvider;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.FunctionRepo;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.function.NoOutputFunction;
import com.opengamma.sesame.function.SecurityAdapter;

/**
 * Lightweight model of the functions needed to generate the outputs for a view.
 */
public final class GraphModel {

  // TODO for this to be useful in the UI it needs to be map(column -> map((outputName,inputType) -> functionModel))
  // it will probably be better to have a real class than a rats' nest of generics
  private final Map<String, Map<ObjectId, FunctionModel>> _functionTrees;

  private GraphModel(Map<String, Map<ObjectId, FunctionModel>> functionTrees) {
    _functionTrees = functionTrees;
  }

  // TODO this is getting quite unwieldy and there are likely to be more parameters. create GraphBuilder with fields?
  // defaultConfig, components and functionRepo could all be fields of GraphBuilder
  // also proxy providers (when they exist)
  // if we need shared state for (e.g. singleton providers) then GraphBuilder would be sensible place to put it
  public static GraphModel forView(ViewDef viewDef,
                                   Collection<? extends PositionOrTrade> inputs,
                                   FunctionConfig defaultConfig,
                                   FunctionRepo functionRepo,
                                   ComponentMap components) {
    ImmutableMap.Builder<String, Map<ObjectId, FunctionModel>> builder = ImmutableMap.builder();
    for (ViewColumn column : viewDef.getColumns()) {
      ImmutableMap.Builder<ObjectId, FunctionModel> columnBuilder = ImmutableMap.builder();
      for (PositionOrTrade posOrTrade : inputs) {
        // TODO no need to create a FunctionTree for every target, cache on outputName/inputType

        // if we need to support stateful functions this is the place to do it.
        // the FunctionModel could flag if its tree contains any functions annotated as @Stateful and
        // it wouldn't be eligible for sharing with other inputs

        // look for an output for the position or trade
        String posOrTradeOutput = column.getOutputName(posOrTrade.getClass());
        if (posOrTradeOutput != null) {
          FunctionMetadata function = functionRepo.getOutputFunction(posOrTradeOutput, posOrTrade.getClass());
          if (function != null) {
            FunctionConfig config = configForInput(posOrTrade.getClass(), column, defaultConfig, functionRepo);
            GraphConfig graphConfig = new GraphConfig(config, components);
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
            FunctionConfig config = configForInput(security.getClass(), column, defaultConfig, functionRepo);
            GraphConfig graphConfig = new GraphConfig(config, components);
            FunctionModel securityModel = FunctionModel.forFunction(functionType, graphConfig);
            FunctionModel functionModel = SecurityAdapter.adapt(securityModel);
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

  private static FunctionConfig configForInput(Class<?> inputType,
                                               ViewColumn column,
                                               FunctionConfig defaultConfig,
                                               FunctionRepo functionRepo) {
    return CompositeFunctionConfig.compose(column.getFunctionConfig(inputType),
                                           defaultConfig,
                                           new DefaultImplementationProvider(functionRepo));
  }

  /**
   * TODO just return the map? function graph doesn't serve any purpose in its current form
   *
   * @return A graph containing the built function instances
   */
  public FunctionGraph build(ComponentMap components) {
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
    return new FunctionGraph(builder.build());
  }
}
