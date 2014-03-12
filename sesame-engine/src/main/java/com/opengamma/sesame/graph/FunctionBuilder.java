/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.engine.ComponentMap;

/**
 * Builds function objects from the {@link Node} instances representing them in the function model.
 */
public final class FunctionBuilder {

  private static final Logger s_logger = LoggerFactory.getLogger(FunctionBuilder.class);

  /**
   * {@link Cacheable} functions are shared if their nodes are identical.
   * This allows the function object's identity to be used as part of the cache key.
   */
  private final Map<Node, Object> _sharedNodeObjects = Maps.newHashMap();

  /* package */ Object create(Node node, ComponentMap componentMap) {
    checkValid(node);
    // TODO detect cycles in the graph
    // TODO cache this info if it proves expensive to do it over and over for the same classes
    boolean cacheable =
        node instanceof InterfaceNode &&
            (EngineUtils.hasMethodAnnotation(((InterfaceNode) node).getImplementationType(), Cacheable.class) ||
                (EngineUtils.hasMethodAnnotation(((InterfaceNode) node).getType(), Cacheable.class)));
    if (cacheable) {
      Object existing = _sharedNodeObjects.get(node);
      if (existing != null) {
        s_logger.debug("Returning existing function for node {}, {}", existing, node.prettyPrint(false));
        return existing;
      } else {
        s_logger.debug("No existing function found for node {}", node.prettyPrint(false));
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
      throw new GraphBuildException("Can't build functions from an invalid graph\n" + node.prettyPrint(false) + "\n",
                                    node.getExceptions());
    }
  }
}
