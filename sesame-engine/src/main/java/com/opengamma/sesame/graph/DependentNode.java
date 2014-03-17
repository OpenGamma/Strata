/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * A node in the function model with dependencies.
 */
public abstract class DependentNode extends FunctionModelNode {

  /**
   * The list of dependent nodes.
   */
  private final List<FunctionModelNode> _dependencies;
  /**
   * Whether the node if valid.
   */
  private boolean _valid;

  /**
   * Creates an instance.
   * 
   * @param type  the expected type of the object created by this node, not null
   * @param parameter  the parameter this node satisfies, null if it's the root node
   * @param dependencies  the array of dependencies, not null
   */
  DependentNode(Class<?> type, Parameter parameter, FunctionModelNode... dependencies) {
    this(type, parameter, Arrays.asList(dependencies));
  }

  /**
   * Creates an instance.
   * 
   * @param type  the expected type of the object created by this node, not null
   * @param parameter  the parameter this node satisfies, null if it's the root node
   * @param dependencies  the list of dependencies, not null
   */
  DependentNode(Class<?> type, Parameter parameter, List<FunctionModelNode> dependencies) {
    super(type, parameter);
    _dependencies = ImmutableList.copyOf(ArgumentChecker.notNull(dependencies, "dependencies"));
    _valid = isValid(_dependencies);
  }

  private static boolean isValid(List<FunctionModelNode> dependencies) {
    for (FunctionModelNode dependency : dependencies) {
      if (!dependency.isValid()) {
        return false;
      }
    }
    return true;
  }

  //-------------------------------------------------------------------------
  @Override
  public List<FunctionModelNode> getDependencies() {
    return _dependencies;
  }

  @Override
  public boolean isValid() {
    return _valid;
  }

  @Override
  public List<InvalidGraphException> getExceptions() {
    List<InvalidGraphException> list = new ArrayList<>();
    for (FunctionModelNode childNode : getDependencies()) {
      list.addAll(childNode.getExceptions());
    }
    return list;
  }

  //-------------------------------------------------------------------------
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

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(_dependencies);
  }

}
