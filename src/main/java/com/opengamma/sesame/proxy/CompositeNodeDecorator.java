/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.sesame.graph.Node;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class CompositeNodeDecorator implements NodeDecorator {

  private final List<NodeDecorator> _decorators;

  public CompositeNodeDecorator(List<NodeDecorator> decorators) {
    ArgumentChecker.notNull(decorators, "decorators");
    // reverse the decorators so the first decorator's node is the first one presented with the argument
    // it's arguable which way round is least surprising. this makes most sense to me
    List<NodeDecorator> reversed = Lists.newArrayList(decorators);
    Collections.reverse(reversed);
    _decorators = ImmutableList.copyOf(reversed);
  }

  @Override
  public Node decorateNode(Node node) {
    Node wrappedNode = node;
    for (NodeDecorator decorator : _decorators) {
      wrappedNode = decorator.decorateNode(node);
    }
    return wrappedNode;
  }
}
