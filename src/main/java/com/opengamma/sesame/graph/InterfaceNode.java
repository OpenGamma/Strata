/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.List;

import com.opengamma.util.ArgumentChecker;

/**
 * A node in the dependency model for an object referred to via an interface that must be created by the injection framework.
 */
public final class InterfaceNode extends ClassNode {

  private final Class<?> _interfaceType;

  public InterfaceNode(Class<?> interfaceType, Class<?> implementationType, List<Node> arguments) {
    super(implementationType, arguments);
    _interfaceType = ArgumentChecker.notNull(interfaceType, "interfaceType");
  }

  public Class<?> getInterfaceType() {
    return _interfaceType;
  }
}
