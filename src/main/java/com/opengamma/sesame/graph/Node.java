/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collections;
import java.util.List;

import com.opengamma.sesame.engine.ComponentMap;

/**
 * TODO isValid()? all impls except exceptionNode return true?
 */
public abstract class Node {

  /* package */ public abstract Object create(ComponentMap componentMap);

  public List<Node> getDependencies() {
    return Collections.emptyList();
  }
}
