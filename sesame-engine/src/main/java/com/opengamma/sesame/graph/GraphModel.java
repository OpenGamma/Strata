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
import com.opengamma.sesame.function.ConfigurationErrorFunction;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.util.ArgumentChecker;

/**
 * Lightweight representation of the function models needed for a view.
 * <p>
 * A view is formed from a grid of cells, each of which generates a desired output.
 * The function for each cell is represented by a {@link FunctionModel}.
 * The complete model for all cells is represented by this class.
 */
public final class GraphModel {

  /** Logger. */
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
   * Builds the graph from the model using the specified set of components.
   * 
   * @param components  the component map, not null
   * @return a graph containing the built function instances, not null
   */
  public Graph build(ComponentMap components, FunctionBuilder functionBuilder) {
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
          // put in a placeholder function that produces no output
          FunctionModel noOutputFunctionModel = FunctionModel.forFunction(ConfigurationErrorFunction.METADATA);
          columnBuilder.put(inputType, noOutputFunctionModel.build(functionBuilder, components));
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
        // put in a placeholder function that produces no output
        FunctionModel noOutputFunctionModel = FunctionModel.forFunction(ConfigurationErrorFunction.METADATA);
        nonPortfolioFunctions.put(name, noOutputFunctionModel.build(functionBuilder, components));
      }
    }

    return new Graph(portfolioFunctions.build(), nonPortfolioFunctions.build());
  }

  /**
   * Returns the {@link FunctionModel} of the function used to calculate the value in a column.
   * @param columnName the name of the column
   * @param inputType type of input (i.e. the security, trade or position type) for the row
   * @return the function model or null if there isn't one for the specified input type
   * @throws IllegalArgumentException if the column name isn't found
   */
  public FunctionModel getFunctionModel(String columnName, Class<?> inputType) {
    ArgumentChecker.notEmpty(columnName, "columnName");
    Map<Class<?>, FunctionModel> columnFns = _portfolioFunctionModels.get(columnName);

    if (columnFns == null) {
      throw new IllegalArgumentException("There is no column named '" + columnName + "'");
    } else {
      return columnFns.get(inputType);
    }
  }

  /**
   * Returns the {@link FunctionModel} of the function used to calculate a non-portfolio output.
   * @param outputName the name of the output
   * @return the function model
   * @throws IllegalArgumentException if the output name isn't found
   */
  public FunctionModel getFunctionModel(String outputName) {
    FunctionModel model = _nonPortfolioFunctionModels.get(ArgumentChecker.notEmpty(outputName, "outputName"));

    if (model == null) {
      throw new IllegalArgumentException("There is no output named '" + outputName + "'");
    } else {
      return model;
    }
  }
}
