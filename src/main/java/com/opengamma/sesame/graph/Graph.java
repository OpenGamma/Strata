/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.config.ColumnOutput;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.function.AdaptingFunctionMetadata;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.FunctionRepo;
import com.opengamma.sesame.function.Invoker;
import com.opengamma.sesame.function.NoOutputFunction;

/**
 * Lightweight model of the functions needed to generate the outputs for a view.
 */
public final class Graph {

  private final Map<String, Map<ObjectId, FunctionTree>> _functionTrees;

  private Graph(Map<String, Map<ObjectId, FunctionTree>> functionTrees) {
    _functionTrees = functionTrees;
  }

  // TODO this method is ugly, find a neater way
  public static Graph forView(ViewDef viewDef,
                              Collection<? extends PositionOrTrade> inputs,
                              Map<Class<?>, Object> infrastructure,
                              FunctionRepo functionRepo) {
    ImmutableMap.Builder<String, Map<ObjectId, FunctionTree>> builder = ImmutableMap.builder();
    for (ViewColumn column : viewDef.getColumns()) {
      ImmutableMap.Builder<ObjectId, FunctionTree> columnBuilder = ImmutableMap.builder();
      for (PositionOrTrade posOrTrade : inputs) {
        // TODO no need to create a FunctionTree for every target, cache on outputName/inputType

        // TODO at the bare minimum this could be moved into 2 helper methods that return a function tree
        // one each for position and security. or one to create the function tree and one to wrap it for securities

        // look for an output for the position or trade
        ColumnOutput posOrTradeOutput = column.getOutput(posOrTrade.getClass());
        if (posOrTradeOutput != null) {
          FunctionMetadata function = functionRepo.getOutputFunction(posOrTradeOutput.getOutputName(), posOrTrade.getClass());
          if (function != null) {
            FunctionTree functionTree = FunctionTree.forFunction(function, posOrTradeOutput.getFunctionConfig(), infrastructure.keySet());
            columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionTree);
            continue;
          }
        }

        // look for an output for the security type
        Security security = posOrTrade.getSecurity();
        ColumnOutput securityOutput = column.getOutput(security.getClass());
        if (securityOutput != null) {
          FunctionMetadata functionType = functionRepo.getOutputFunction(securityOutput.getOutputName(), security.getClass());
          if (functionType != null) {
            FunctionTree securityTree = FunctionTree.forFunction(functionType,
                                                                 securityOutput.getFunctionConfig(),
                                                                 infrastructure.keySet());
            FunctionTree functionTree = new FunctionTree(securityTree.getRootFunction(),
                                                         new AdaptingFunctionMetadata(securityTree.getRootMetadata()));
            columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionTree);
            continue;
          }
        }

        // try the default output
        ColumnOutput defaultOutput = column.getDefaultOutput();
        if (defaultOutput != null) {
          // is there a function providing the default output for a position / trade?
          FunctionMetadata defaultPosOrTradeFunction = functionRepo.getOutputFunction(defaultOutput.getOutputName(),
                                                                                      posOrTrade.getClass());
          if (defaultPosOrTradeFunction != null) {
            FunctionTree functionTree = FunctionTree.forFunction(defaultPosOrTradeFunction,
                                                                 defaultOutput.getFunctionConfig(),
                                                                 infrastructure.keySet());
            columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionTree);
            continue;
          }
          // is there a default output for the security
          FunctionMetadata defaultSecFunction = functionRepo.getOutputFunction(defaultOutput.getOutputName(),
                                                                               posOrTrade.getSecurity().getClass());
          if (defaultSecFunction != null) {
            FunctionTree securityTree = FunctionTree.forFunction(defaultSecFunction,
                                                                 defaultOutput.getFunctionConfig(),
                                                                 infrastructure.keySet());
            FunctionTree functionTree = new FunctionTree(securityTree.getRootFunction(),
                                                         new AdaptingFunctionMetadata(securityTree.getRootMetadata()));
            columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionTree);
            continue;
          }
        }
        FunctionTree functionTree = FunctionTree.forFunction(NoOutputFunction.METADATA,
                                                             FunctionConfig.EMPTY,
                                                             Collections.<Class<?>>emptySet());
        // TODO how will this work for in-memory trades? assign an ID? use object identity?
        columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionTree);
      }
      builder.put(column.getName(), columnBuilder.build());
    }
    return new Graph(builder.build());
  }

  /**
   * TODO just return the map? function graph doesn't serve any purpose in its current form
   *
   * @param infrastructure The engine infrastructure
   * @return A graph containing the built function instances
   */
  public FunctionGraph build(Map<Class<?>, Object> infrastructure) {
    ImmutableMap.Builder<String, Map<ObjectId, Invoker>> builder = ImmutableMap.builder();
    for (Map.Entry<String, Map<ObjectId, FunctionTree>> entry : _functionTrees.entrySet()) {
      Map<ObjectId, FunctionTree> functionsByTargetId = entry.getValue();
      ImmutableMap.Builder<ObjectId, Invoker> columnBuilder = ImmutableMap.builder();
      for (Map.Entry<ObjectId, FunctionTree> columnEntry : functionsByTargetId.entrySet()) {
        ObjectId targetId = columnEntry.getKey();
        FunctionTree functionTree = columnEntry.getValue();
        columnBuilder.put(targetId, (Invoker) functionTree.build(infrastructure));
      }
      String columnName = entry.getKey();
      builder.put(columnName, columnBuilder.build());
    }
    return new FunctionGraph(builder.build());
  }
}
