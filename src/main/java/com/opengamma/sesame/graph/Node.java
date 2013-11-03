/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.sesame.engine.ComponentMap;

/**
 *
 */
public abstract class Node {

  private final ImmutableList<Node> _dependencies;

  /* package */ Node(List<Node> dependencies) {
    _dependencies = ImmutableList.copyOf(dependencies);
  }

  /* package */ Node() {
    this(Collections.<Node>emptyList());
  }

  /* package */ abstract Object create(ComponentMap components);

  public ImmutableList<Node> getDependencies() {
    return _dependencies;
  }
}
