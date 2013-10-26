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
import com.opengamma.sesame.config.ColumnRequirement;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.function.FunctionRepo;
import com.opengamma.sesame.function.NoPortfolioOutputFunction;
import com.opengamma.sesame.function.PortfolioOutputFunction;

/**
 * Lightweight model of the functions needed to generate the outputs for a view.
 */
public final class Graph {

  // TODO should this be a lightweight model like Tree? will we ever want to see a lightweight model of the whole graph?
  /** Map of column names -> map of target ID -> function. */
  //private final Map<String, Map<ObjectId, PortfolioOutputFunction<?, ?>>> _functions;
  private final Map<String, Map<ObjectId, Tree<?>>> _functionTrees;

  private Graph(Map<String, Map<ObjectId, Tree<?>>> functionTrees) {
    _functionTrees = functionTrees;
  }

  // TODO separate GraphBuilder with infrastructure and function repo fields? doesn't match Tree if I do that
  public static Graph forView(ViewDef viewDef,
                              Collection<PositionOrTrade> targets,
                              Map<Class<?>, Object> infrastructure,
                              FunctionRepo functionRepo) {
    ImmutableMap.Builder<String, Map<ObjectId, Tree<?>>> builder = ImmutableMap.builder();
    for (ViewColumn column : viewDef.getColumns()) {
      ImmutableMap.Builder<ObjectId, Tree<?>> columnBuilder = ImmutableMap.builder();
      Map<Class<?>, ColumnRequirement> requirements = column.getRequirements();
      for (PositionOrTrade positionOrTrade : targets) {
        Tree<?> tree;
        if (requirements.containsKey(positionOrTrade.getClass())) {
          ColumnRequirement requirement = requirements.get(positionOrTrade.getClass());
          Class<?> outputFunctionType = functionRepo.getFunctionType(requirement.getOutputName(), positionOrTrade.getClass());
          tree = Tree.forFunction(outputFunctionType, requirement.getFunctionConfig(), infrastructure.keySet());
          columnBuilder.put(positionOrTrade.getUniqueId().getObjectId(), tree);
        } else if (requirements.containsKey(positionOrTrade.getSecurity().getClass())) {
          Security security = positionOrTrade.getSecurity();
          ColumnRequirement requirement = requirements.get(security.getClass());
          Class<?> outputFunctionType = functionRepo.getFunctionType(requirement.getOutputName(), security.getClass());
          Tree<?> securityTree = Tree.forFunction(outputFunctionType, requirement.getFunctionConfig(), infrastructure.keySet());
          tree = SecurityFunctionDecorator.decorateRoot(securityTree);
        } else {
          tree = Tree.forFunction(NoPortfolioOutputFunction.class);
        }
        columnBuilder.put(positionOrTrade.getUniqueId().getObjectId(), tree);
      }
      builder.put(column.getName(), columnBuilder.build());
    }
    return new Graph(builder.build());
  }

  // TODO this return type is too much, create a class. FunctionGraph?
  /* package */ Map<String, Map<ObjectId, PortfolioOutputFunction<?, ?>>> build(Map<Class<?>, Object> infrastructure) {
    throw new UnsupportedOperationException();
  }
}
