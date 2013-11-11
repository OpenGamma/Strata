/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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
import com.opengamma.sesame.function.NoOutputFunction;
import com.opengamma.sesame.function.SecurityFunctionAdapter;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public final class GraphBuilder {

  private static final Logger s_logger = LoggerFactory.getLogger(GraphBuilder.class);

  private final FunctionRepo _functionRepo;
  private final ComponentMap _componentMap;
  private final FunctionConfig _defaultConfig;
  private final NodeDecorator _nodeDecorator;
  private final DefaultImplementationProvider _defaultImplProvider;

  public GraphBuilder(FunctionRepo functionRepo,
                      ComponentMap componentMap,
                      FunctionConfig defaultConfig,
                      NodeDecorator nodeDecorator) {
    _functionRepo = ArgumentChecker.notNull(functionRepo, "functionRepo");
    _componentMap = ArgumentChecker.notNull(componentMap, "componentMap");
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _nodeDecorator = ArgumentChecker.notNull(nodeDecorator, "nodeDecorator");
    _defaultImplProvider = new DefaultImplementationProvider(functionRepo);
  }

  public GraphModel build(ViewDef viewDef, Collection<? extends PositionOrTrade> inputs) {
    ImmutableMap.Builder<String, Map<ObjectId, FunctionModel>> builder = ImmutableMap.builder();
    // TODO each column could easily be done in parallel
    for (ViewColumn column : viewDef.getColumns()) {
      Map<Class<?>, FunctionModel> functions = Maps.newHashMap();
      ImmutableMap.Builder<ObjectId, FunctionModel> columnBuilder = ImmutableMap.builder();
      for (PositionOrTrade posOrTrade : inputs) {
        // if we need to support stateful functions this is the place to do it.
        // the FunctionModel could flag if its tree contains any functions annotated as @Stateful and
        // it wouldn't be eligible for sharing with other inputs

        // TODO extract a method for the logic below. it's almost exactly the same twice except adapting the security function

        // look for an output for the position or trade
        String posOrTradeOutput = column.getOutputName(posOrTrade.getClass());
        if (posOrTradeOutput != null) {
          FunctionMetadata function = _functionRepo.getOutputFunction(posOrTradeOutput, posOrTrade.getClass());
          if (function != null) {
            FunctionModel functionModel;
            FunctionModel existingFunction = functions.get(posOrTrade.getClass());
            if (existingFunction != null) {
              functionModel = existingFunction;
              s_logger.debug("using existing function for {}/{}", column.getName(), posOrTrade.getClass().getSimpleName());
            } else {
              FunctionConfig columnConfig = column.getFunctionConfig(posOrTrade.getClass());
              FunctionConfig config = CompositeFunctionConfig.compose(columnConfig, _defaultConfig, _defaultImplProvider);
              GraphConfig graphConfig = new GraphConfig(config, _componentMap, _nodeDecorator);
              functionModel = FunctionModel.forFunction(function, graphConfig);
              functions.put(posOrTrade.getClass(), functionModel);
              s_logger.debug("created function for {}/{}", column.getName(), posOrTrade.getClass().getSimpleName());
            }
            // TODO see note in Graph about keying the functions by column/input type instead of ID. this is inefficient
            columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionModel);
            continue;
          }
        }

        // look for an output for the security type
        Security security = posOrTrade.getSecurity();
        String securityOutput = column.getOutputName(security.getClass());
        if (securityOutput != null) {
          FunctionMetadata function = _functionRepo.getOutputFunction(securityOutput, security.getClass());
          if (function != null) {
            FunctionModel functionModel;
            FunctionModel existingFunction = functions.get(security.getClass());
            if (existingFunction != null) {
              functionModel = existingFunction;
              s_logger.debug("using existing function for {}/{}", column.getName(), security.getClass().getSimpleName());
            } else {
              FunctionConfig columnConfig = column.getFunctionConfig(security.getClass());
              FunctionConfig config = CompositeFunctionConfig.compose(columnConfig, _defaultConfig, _defaultImplProvider);
              GraphConfig graphConfig = new GraphConfig(config, _componentMap, _nodeDecorator);
              FunctionModel securityModel = FunctionModel.forFunction(function, graphConfig);
              functionModel = SecurityFunctionAdapter.adapt(securityModel);
              functions.put(security.getClass(), functionModel);
              s_logger.debug("created function for {}/{}", column.getName(), security.getClass().getSimpleName());
            }
            // TODO see note in Graph about keying the functions by column/input type instead of ID. this is inefficient
            columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionModel);
            continue;
          }
        }
        FunctionModel functionModel = FunctionModel.forFunction(NoOutputFunction.METADATA);
        // TODO how will this work for in-memory trades? assign an ID? use object identity?
        // at least some of the analytics code assumes a unique ID (on securities) so maybe we have to assign them
        columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionModel);
      }
      builder.put(column.getName(), columnBuilder.build());
    }
    return new GraphModel(builder.build());
  }

}
