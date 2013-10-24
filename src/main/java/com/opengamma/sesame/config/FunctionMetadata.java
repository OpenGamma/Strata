/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.util.ArgumentChecker;

/**
 *
 */
/* package */ class FunctionMetadata {

  private final String _valueName;
  private final Class<?> _defaultImplementation;

  /* package */ FunctionMetadata(String valueName, Class<?> defaultImplementation) {
    ArgumentChecker.notNull(defaultImplementation, "defaultImplementation");
    _valueName = valueName;
    _defaultImplementation = defaultImplementation;
  }

  /**
   * @return The value name, null if the function can't be used as an output
   */
  public String getValueName() {
    return _valueName;
  }

  /**
   * @return The default implementation, null if the function doesn't know it
   */
  public Class<?> getDefaultImplementation() {
    return _defaultImplementation;
  }

  /* package */ static FunctionMetadata forFunctionInterface(Class<?> functionInterface) {
    if (!functionInterface.isInterface()) {
      throw new IllegalArgumentException(functionInterface.getName() + " isn't an interface");
    }
    EngineFunction engineFunctionAnnotation = functionInterface.getAnnotation(EngineFunction.class);
    String valueName;
    if (engineFunctionAnnotation == null) {
      valueName = null;
    } else {
      valueName = engineFunctionAnnotation.value();
    }
    DefaultImplementation defaultImplementationAnnotation = functionInterface.getAnnotation(DefaultImplementation.class);
    Class defaultImpl;
    if (defaultImplementationAnnotation == null) {
      defaultImpl = null;
    } else {
      defaultImpl = defaultImplementationAnnotation.value();

    }
    return new FunctionMetadata(valueName, defaultImpl);
  }
}
