/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collections;
import java.util.List;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;

/**
 * TODO isValid()? all impls except exceptionNode return true?
 * TODO should every node have a (possibly null) Parameter?
 */
public abstract class Node {

  // TODO this doesn't really belong here, I just put it here for convenience. can it be moved?
  /** the parameter this node satisfies, null if it's the root node */
  private final Parameter _parameter;

  protected Node(Parameter parameter) {
    _parameter = parameter;
  }

  /* package */ public abstract Object create(ComponentMap componentMap);

  public List<Node> getDependencies() {
    return Collections.emptyList();
  }

  public String prettyPrint() {
    return toString();
  }

  public Parameter getParameter() {
    return _parameter;
  }

  protected final String getParameterName() {
    if (getParameter() == null) {
      return "";
    } else {
      return getParameter().getName() + ": ";
    }
  }
}
