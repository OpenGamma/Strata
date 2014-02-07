/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.util.ArgumentChecker;

/**
 * Lightweight model of the functions needed to generate the outputs for a view.
 */
public final class GraphModel {

  private static final Logger s_logger = LoggerFactory.getLogger(GraphModel.class);

  // TODO for this to be useful in the UI it needs to be map(column -> map((outputName,inputType) -> functionModel))
  // it will probably be better to have a real class than a rats' nest of generics

  /** Function models for portfolio output. The outer map is keyed by column name, the inner map by input type. */
  private final Map<String, Map<Class<?>, FunctionModel>> _portfolioFunctionModels;

  /** Function models for non-portfolio outputs, keyed by name */
  private final Map<String, FunctionModel> _nonPortfolioFunctionModels;

  /* package */ GraphModel(Map<String, Map<Class<?>, FunctionModel>> portfolioFunctionModels,
                           Map<String, FunctionModel> nonPortfolioFunctionModels) {
    _portfolioFunctionModels = ArgumentChecker.notNull(portfolioFunctionModels, "portfolioFunctionModels");
    _nonPortfolioFunctionModels = ArgumentChecker.notNull(nonPortfolioFunctionModels, "nonPortfolioFunctionModels");
  }

  /**
   * @return A graph containing the built function instances
   */
  public Graph build(ComponentMap components) {
    FunctionBuilder functionBuilder = new FunctionBuilder();

    // build the functions for the portfolio outputs
    ImmutableMap.Builder<String, Map<Class<?>, InvokableFunction>> portfolioFunctions = ImmutableMap.builder();
    for (Map.Entry<String, Map<Class<?>, FunctionModel>> entry : _portfolioFunctionModels.entrySet()) {
      Map<Class<?>, FunctionModel> functionsByTargetId = entry.getValue();
      ImmutableMap.Builder<Class<?>, InvokableFunction> columnBuilder = ImmutableMap.builder();
      for (Map.Entry<Class<?>, FunctionModel> columnEntry : functionsByTargetId.entrySet()) {
        Class<?> inputType = columnEntry.getKey();
        FunctionModel functionModel = columnEntry.getValue();
        if (functionModel.isValid()) {
          columnBuilder.put(inputType, functionModel.build(functionBuilder, components));
        } else {
          s_logger.warn("Can't build invalid function model{}", functionModel.prettyPrint());
        }
      }
      String columnName = entry.getKey();
      portfolioFunctions.put(columnName, columnBuilder.build());
    }

    // build the functions for the non-portfolio outputs
    ImmutableMap.Builder<String, InvokableFunction> nonPortfolioFunctions = ImmutableMap.builder();
    for (Map.Entry<String, FunctionModel> entry : _nonPortfolioFunctionModels.entrySet()) {
      String name = entry.getKey();
      FunctionModel functionModel = entry.getValue();
      if (functionModel.isValid()) {
        nonPortfolioFunctions.put(name, functionModel.build(functionBuilder, components));
      } else {
        s_logger.warn("Can't build invalid function model{}", functionModel.prettyPrint());
      }
    }

    return new Graph(portfolioFunctions.build(), nonPortfolioFunctions.build());
  }
}
