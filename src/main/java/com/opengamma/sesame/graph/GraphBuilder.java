/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.sesame.config.CompositeFunctionConfig;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.config.NonPortfolioOutput;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.DefaultImplementationProvider;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.NoOutputFunction;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public final class GraphBuilder {

  private static final Logger s_logger = LoggerFactory.getLogger(GraphBuilder.class);

  private final AvailableOutputs _availableOutputs;
  private final ComponentMap _componentMap;
  private final FunctionConfig _defaultConfig;
  private final NodeDecorator _nodeDecorator;
  private final DefaultImplementationProvider _defaultImplProvider;

  public GraphBuilder(AvailableOutputs availableOutputs,
                      AvailableImplementations availableImplementations,
                      ComponentMap componentMap,
                      FunctionConfig defaultConfig,
                      NodeDecorator nodeDecorator) {
    _availableOutputs = ArgumentChecker.notNull(availableOutputs, "functionRepo");
    _componentMap = ArgumentChecker.notNull(componentMap, "componentMap");
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _nodeDecorator = ArgumentChecker.notNull(nodeDecorator, "nodeDecorator");
    // TODO should this be an argument?
    _defaultImplProvider = new DefaultImplementationProvider(availableImplementations);
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}. This will be relaxed
   * in future.
   */
  public GraphModel build(ViewDef viewDef, Collection<?> inputs) {
    ArgumentChecker.notNull(viewDef, "viewDef");
    ArgumentChecker.notNull(inputs, "inputs");
    ImmutableMap.Builder<String, Map<Class<?>, FunctionModel>> builder = ImmutableMap.builder();
    // TODO each column could easily be done in parallel
    FunctionConfig viewConfig = viewDef.getDefaultConfig();
    FunctionConfig defaultConfig = CompositeFunctionConfig.compose(viewConfig, _defaultConfig, _defaultImplProvider);
    for (ViewColumn column : viewDef.getColumns()) {
      Map<Class<?>, FunctionModel> functions = Maps.newHashMap();
      for (Object input : inputs) {
        // if we need to support stateful functions this is the place to do it.
        // the FunctionModel could flag if its tree contains any functions annotated as @Stateful and
        // it wouldn't be eligible for sharing with other inputs
        // would need to key on input ID instead of type. would need to assign ID for in-memory trades

        // TODO extract a method for the logic below. it's almost exactly the same twice except adapting the security function

        // look for an output for the position or trade
        String outputName = column.getOutputName(input.getClass());
        if (outputName != null) {
          FunctionMetadata function = _availableOutputs.getOutputFunction(outputName, input.getClass());
          if (function != null) {
            FunctionModel existingFunction = functions.get(input.getClass());
            if (existingFunction == null) {
              FunctionConfig columnConfig = column.getFunctionConfig(input.getClass());
              FunctionConfig config = CompositeFunctionConfig.compose(columnConfig, defaultConfig);
              GraphConfig graphConfig = new GraphConfig(config, _componentMap, _nodeDecorator);
              FunctionModel functionModel = FunctionModel.forFunction(function, graphConfig);
              functions.put(input.getClass(), functionModel);
              s_logger.debug("created function for {}/{}", column.getName(), input.getClass().getSimpleName());
            }
            continue;
          }
        }

        // look for an output for the security type
        if (input instanceof PositionOrTrade) {
          Security security = ((PositionOrTrade) input).getSecurity();
          String securityOutput = column.getOutputName(security.getClass());
          if (securityOutput != null) {
            FunctionMetadata function = _availableOutputs.getOutputFunction(securityOutput, security.getClass());
            if (function != null) {
              FunctionModel existingFunction = functions.get(security.getClass());
              if (existingFunction == null) {
                FunctionConfig columnConfig = column.getFunctionConfig(security.getClass());
                FunctionConfig config = CompositeFunctionConfig.compose(columnConfig, defaultConfig);
                GraphConfig graphConfig = new GraphConfig(config, _componentMap, _nodeDecorator);
                FunctionModel functionModel = FunctionModel.forFunction(function, graphConfig);
                functions.put(security.getClass(), functionModel);
                s_logger.debug("created function for {}/{}", column.getName(), security.getClass().getSimpleName());
              }
              continue;
            }
          }
        }
        s_logger.warn("Failed to find function to provide output for {} for {}", column, input.getClass().getSimpleName());
        FunctionModel functionModel = FunctionModel.forFunction(NoOutputFunction.METADATA);
        functions.put(input.getClass(), functionModel);
      }
      builder.put(column.getName(), Collections.unmodifiableMap(functions));
    }

    // build the function models for non-portfolio outputs
    ImmutableMap.Builder<String, FunctionModel> nonPortfolioFunctionModels = ImmutableMap.builder();
    for (NonPortfolioOutput output : viewDef.getNonPortfolioOutputs()) {
      String outputName = output.getOutput().getOutputName();
      FunctionMetadata function = _availableOutputs.getOutputFunction(outputName);
      FunctionModel functionModel;
      if (function != null) {
        FunctionConfig functionConfig = output.getOutput().getFunctionConfig();
        FunctionConfig config = CompositeFunctionConfig.compose(functionConfig, defaultConfig);
        GraphConfig graphConfig = new GraphConfig(config, _componentMap, _nodeDecorator);
        functionModel = FunctionModel.forFunction(function, graphConfig);
      } else {
        s_logger.warn("Failed to find function to provide output named {}", outputName);
        functionModel = FunctionModel.forFunction(NoOutputFunction.METADATA);
      }
      nonPortfolioFunctionModels.put(output.getName(), functionModel);
    }

    return new GraphModel(builder.build(), nonPortfolioFunctionModels.build());
  }

}
