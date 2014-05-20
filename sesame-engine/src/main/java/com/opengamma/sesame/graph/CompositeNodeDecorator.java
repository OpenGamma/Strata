/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.opengamma.util.ArgumentChecker;

/**
 * Node decorator that composes other decorators.
 */
public final class CompositeNodeDecorator extends NodeDecorator {

  /** Logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(CompositeNodeDecorator.class);

  /**
   * The underlying decorators.
   */
  private final ImmutableList<NodeDecorator> _decorators;

  //-------------------------------------------------------------------------
  /**
   * Composes a list of decorators.
   * 
   * @param decorators  the array of decorators, not null
   * @return the composite decorators, not null
   */
  public static NodeDecorator compose(NodeDecorator... decorators) {
    ArgumentChecker.noNulls(decorators, "decorators");
    switch (decorators.length) {
      case 0:
        return NodeDecorator.IDENTITY;
      case 1:
        return decorators[0];
      default:
        return new CompositeNodeDecorator(ImmutableList.copyOf(decorators));
    }
  }

  /**
   * Composes a list of decorators.
   * 
   * @param decorators  the list of decorators, not null
   * @return the composite decorators, not null
   */
  public static NodeDecorator compose(List<NodeDecorator> decorators) {
    ArgumentChecker.noNulls(decorators, "decorators");
    switch (decorators.size()) {
      case 0:
        return NodeDecorator.IDENTITY;
      case 1:
        return decorators.get(0);
      default:
        return new CompositeNodeDecorator(ImmutableList.copyOf(decorators));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param decorators  the decorators, not null
   */
  private CompositeNodeDecorator(ImmutableList<NodeDecorator> decorators) {
    _decorators = decorators.reverse();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionModelNode decorateNode(FunctionModelNode node) {
    FunctionModelNode wrappedNode = node;
    for (NodeDecorator decorator : _decorators) {
      wrappedNode = decorator.decorateNode(wrappedNode);
    }
    return wrappedNode;
  }

}
