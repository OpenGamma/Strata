/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.CompositeFunctionModelConfig;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.NonPortfolioOutput;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.DefaultImplementationProvider;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.NoOutputFunction;
import com.opengamma.util.ArgumentChecker;

/**
 * Builder for a graph.
 */
public final class GraphBuilder {

  /** Logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(GraphBuilder.class);

  private final AvailableOutputs _availableOutputs;
  private final FunctionModelConfig _defaultConfig;
  private final NodeDecorator _nodeDecorator;
  private final DefaultImplementationProvider _defaultImplProvider;
  private final Set<Class<?>> _availableComponents;

  public GraphBuilder(AvailableOutputs availableOutputs,
                      AvailableImplementations availableImplementations,
                      Set<Class<?>>  availableComponents,
                      FunctionModelConfig defaultConfig,
                      NodeDecorator nodeDecorator) {
    _availableOutputs = ArgumentChecker.notNull(availableOutputs, "functionRepo");
    _availableComponents = ArgumentChecker.notNull(availableComponents, "availableComponents");
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _nodeDecorator = ArgumentChecker.notNull(nodeDecorator, "nodeDecorator");
    // TODO should this be an argument?
    _defaultImplProvider = new DefaultImplementationProvider(availableImplementations);
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a model of the functions needed to calculate view outputs for a set of input types.
   * 
   * @param viewConfig  the configuration to use, not null
   * @param inputTypes  the types of the inputs to the calculations, e.g. trades, positions, securities
   * @return the model, not null
   */
  public GraphModel build(ViewConfig viewConfig, Set<Class<?>> inputTypes) {
    ArgumentChecker.notNull(viewConfig, "viewConfig");
    ArgumentChecker.notNull(inputTypes, "inputTypes");
    ImmutableMap.Builder<String, Map<Class<?>, FunctionModel>> builder = ImmutableMap.builder();
    // TODO each column could easily be done in parallel
    FunctionModelConfig modelConfig = viewConfig.getDefaultConfig();
    FunctionModelConfig defaultConfig = CompositeFunctionModelConfig.compose(modelConfig, _defaultConfig, _defaultImplProvider);

    for (ViewColumn column : viewConfig.getColumns()) {
      Map<Class<?>, FunctionModel> functions = Maps.newHashMap();

      for (Class<?> inputType : inputTypes) {
        // if we need to support stateful functions this is the place to do it.
        // the FunctionModel could flag if its tree contains any functions annotated as @Stateful and
        // it wouldn't be eligible for sharing with other inputs
        // would need to key on input ID instead of type. would need to assign ID for in-memory trades

        FunctionModel existingFunction = functions.get(inputType);
        OutputName outputName = column.getOutputName(inputType);
        FunctionMetadata function = outputName == null ?
                                    null :
                                    _availableOutputs.getOutputFunction(outputName, inputType);

        if (existingFunction == null && function != null) {
          FunctionModelConfig columnConfig = column.getFunctionConfig(inputType);
          FunctionModelConfig config = CompositeFunctionModelConfig.compose(columnConfig, defaultConfig);
          FunctionModel functionModel = FunctionModel.forFunction(function, config, _availableComponents, _nodeDecorator);
          functions.put(inputType, functionModel);
          s_logger.debug("created function for {}/{}\n{}",
                         column.getName(), inputType.getSimpleName(), functionModel.prettyPrint());
        } else {
          s_logger.warn("No function available for column '{}' for input type {}", column, inputType.getSimpleName());
          functions.put(inputType, FunctionModel.forFunction(NoOutputFunction.METADATA));
        }
      }
      builder.put(column.getName(), Collections.unmodifiableMap(functions));
    }
    // build the function models for non-portfolio outputs
    ImmutableMap.Builder<String, FunctionModel> nonPortfolioFunctionModels = ImmutableMap.builder();

    for (NonPortfolioOutput output : viewConfig.getNonPortfolioOutputs()) {
      OutputName outputName = output.getOutput().getOutputName();
      FunctionMetadata function = _availableOutputs.getOutputFunction(outputName);

      if (function != null) {
        FunctionModelConfig functionModelConfig = output.getOutput().getFunctionModelConfig();
        FunctionModelConfig config = CompositeFunctionModelConfig.compose(functionModelConfig, defaultConfig);
        FunctionModel functionModel = FunctionModel.forFunction(function, config, _availableComponents, _nodeDecorator);
        nonPortfolioFunctionModels.put(output.getName(), functionModel);
        s_logger.debug("created function for {}/{}\n{}", output.getName(), functionModel.prettyPrint());
      } else {
        nonPortfolioFunctionModels.put(output.getName(), FunctionModel.forFunction(NoOutputFunction.METADATA));
        s_logger.warn("Failed to find function to provide output named {}", outputName);
      }
    }
    return new GraphModel(builder.build(), nonPortfolioFunctionModels.build());
  }

}
