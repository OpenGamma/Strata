/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.lang.reflect.Method;

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
    // TODO allow multiple engine functions with different value names on the same interface?
    String valueName = null;
    for (Method method : functionInterface.getMethods()) {
      EngineFunction engineFunctionAnnotation = method.getAnnotation(EngineFunction.class);
      if (engineFunctionAnnotation != null) {
        if (valueName == null) {
          valueName = engineFunctionAnnotation.value();
        } else {
          throw new IllegalArgumentException("Only one method should be annotated with @EngineFunction on " +
                                                 functionInterface.getName());
        }
      }
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
