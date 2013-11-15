/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.sesame.engine.ComponentMap;

/**
 *
 */
public final class FunctionBuilder {

  private final Map<Node, Object> _sharedNodes = Maps.newHashMap();

  /* package */ Object create(Node node, ComponentMap componentMap) {
    // TODO check if node is eligible for sharing and return existing copy
    // TODO detect cycles in the graph
    List<Object> dependencies = Lists.newArrayListWithCapacity(node.getDependencies().size());
    for (Node dependentNode : node.getDependencies()) {
      dependencies.add(create(dependentNode, componentMap));
    }
    return node.create(componentMap, dependencies);
  }
}
