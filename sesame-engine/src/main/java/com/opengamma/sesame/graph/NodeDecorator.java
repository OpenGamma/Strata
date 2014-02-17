/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

/**
 * Interface for classes that can decorate nodes in the graph.
 */
public interface NodeDecorator {

  /**
   * Returns its argument.
   */
  NodeDecorator IDENTITY = new NodeDecorator() {
    @Override
    public Node decorateNode(Node node) {
      return node;
    }
  };

  //-------------------------------------------------------------------------
  /**
   * Returns a node after optionally wrapping it in a proxy node.
   * <p>
   * If the factory doesn't insert a proxy it must return the original node.
   * 
   * @param node  a node, not null
   * @return a node, possibly wrapped in a proxy, not null
   */
  Node decorateNode(Node node);

}
