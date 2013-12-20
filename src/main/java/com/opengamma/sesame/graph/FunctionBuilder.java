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
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.engine.ComponentMap;

/**
 *
 */
public final class FunctionBuilder {

  private final Map<Node, Object> _sharedNodeObjects = Maps.newHashMap();

  /* package */ Object create(Node node, ComponentMap componentMap) {
    checkValid(node);
    // TODO detect cycles in the graph
    // TODO cache this info if it proves expensive to do it over and over for the same classes
    boolean cacheable =
        node instanceof InterfaceNode &&
            (ConfigUtils.hasMethodAnnotation(((InterfaceNode) node).getImplementationType(), Cacheable.class) ||
                (ConfigUtils.hasMethodAnnotation(((InterfaceNode) node).getType(), Cacheable.class)));
    if (cacheable) {
      Object existing = _sharedNodeObjects.get(node);
      if (existing != null) {
        return existing;
      }
    }
    List<Object> dependencies = Lists.newArrayListWithCapacity(node.getDependencies().size());
    for (Node dependentNode : node.getDependencies()) {
      dependencies.add(create(dependentNode, componentMap));
    }
    Object nodeObject = node.create(componentMap, dependencies);
    if (cacheable) {
      _sharedNodeObjects.put(node, nodeObject);
    }
    return nodeObject;
  }

  private static void checkValid(Node node) {
    if (!node.isValid()) {
      throw new GraphBuildException("Can't build functions from an invalid graph", node.getExceptions());
    }
  }
}
