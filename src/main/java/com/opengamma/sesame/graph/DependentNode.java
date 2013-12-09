/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public abstract class DependentNode extends Node {

  private final List<Node> _dependencies;
  private boolean _valid;

  protected DependentNode(Class<?> type, Parameter parameter, Node... dependencies) {
    this(type, parameter, Arrays.asList(dependencies));
  }

  protected DependentNode(Class<?> type, Parameter parameter, List<Node> dependencies) {
    super(type, parameter);
    _dependencies = ImmutableList.copyOf(ArgumentChecker.notNull(dependencies, "dependencies"));
    _valid = isValid(_dependencies);
  }

  @Override
  public List<Node> getDependencies() {
    return _dependencies;
  }

  @Override
  public boolean isValid() {
    return _valid;
  }

  private static boolean isValid(List<Node> dependencies) {
    for (Node dependency : dependencies) {
      if (!dependency.isValid()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(_dependencies);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final DependentNode other = (DependentNode) obj;
    return Objects.equals(this._dependencies, other._dependencies);
  }
}
