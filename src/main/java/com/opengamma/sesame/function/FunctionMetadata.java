/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Method;

import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.util.ArgumentChecker;

/**
 * Metadata for engine function interfaces.
 * TODO this is redundant
 * need FunctionUtils with
 * getTargetType for OutputFunction impls
 * getDefaultImplementation for others
 */
@Deprecated
public class FunctionMetadata {

  private final String _valueName;
  private final Class<?> _defaultImplementation;
  private final Class<?> _targetType;

  /* package */ FunctionMetadata(String valueName, Class<?> defaultImplementation, Class<?> targetType) {
    ArgumentChecker.notNull(defaultImplementation, "defaultImplementation");
    ArgumentChecker.notNull(targetType, "targetType");
    _targetType = targetType;
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

  /**
   * @return The type of the function method annotated with {@link Target}.
   */
  public Class<?> getTargetType() {
    return _targetType;
  }

  /**
   * Returns the metadata for an interface annotated with {@link OutputName}.
   * @param functionInterface The interface
   * @return Its metadata
   * @throws IllegalArgumentException If the interface doesn't have exactly 1 method annotated
   * with {@link OutputName}
   */
  public static FunctionMetadata forOutput(Class<? extends PortfolioOutputFunction<?, ?>> functionInterface) {
    if (functionInterface.isInterface()) {
      throw new IllegalArgumentException(functionInterface.getName() + " is an interface");
    }
    // TODO allow multiple engine functions with different value names on the same interface?
    Method functionMethod = null;
    for (Method method : functionInterface.getMethods()) {
      if (method.isAnnotationPresent(OutputName.class)) {
        if (functionMethod == null) {
          functionMethod = method;
        } else {
          throw new IllegalArgumentException("Exactly one method should be annotated with @EngineOutput on " +
                                                 functionInterface.getName());
        }
      }
    }
    if (functionMethod == null) {
      throw new IllegalArgumentException("Exactly one method should be annotated with @EngineOutput on " +
                                             functionInterface.getName());
    }
    OutputName outputNameAnnotation = functionMethod.getAnnotation(OutputName.class);
    String valueName = outputNameAnnotation.value();
    // TODO don't need this if all fns implement OutputFunction
    Parameter targetParam = ConfigUtils.getAnnotatedParameter(Target.class, functionMethod);
    Class<?> targetType = targetParam.getType();
    DefaultImplementation defaultImplementationAnnotation = functionInterface.getAnnotation(DefaultImplementation.class);
    Class defaultImpl;
    if (defaultImplementationAnnotation == null) {
      throw new IllegalArgumentException("Function interfaces must be annotated with @DefaultImplementation");
    }
    defaultImpl = defaultImplementationAnnotation.value();
    return new FunctionMetadata(valueName, defaultImpl, targetType);
  }
}
