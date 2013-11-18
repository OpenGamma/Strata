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

  protected DependentNode(Parameter parameter, Node... dependencies) {
    this(parameter, Arrays.asList(dependencies));
  }

  protected DependentNode(Parameter parameter, List<Node> dependencies) {
    super(parameter);
    _dependencies = ImmutableList.copyOf(ArgumentChecker.notNull(dependencies, "dependencies"));
  }

  @Override
  public List<Node> getDependencies() {
    return _dependencies;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_dependencies);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DependentNode other = (DependentNode) obj;
    return Objects.equals(this._dependencies, other._dependencies);
  }
}
