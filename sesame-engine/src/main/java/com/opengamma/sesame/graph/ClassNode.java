/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * A node in the function model defined as a concrete class.
 * <p>
 * This is used for an object that is referred to by its concrete class
 * The implementation is created by the injection framework.
 */
public class ClassNode extends DependentNode {

  /**
   * The implementation type.
   */
  private final Class<?> _implementationType;
  /**
   * The constructor.
   */
  private Constructor<?> _constructor;

  /**
   * Creates an instance.
   * 
   * @param type  the expected type of the object created by this node, not null
   * @param parameter  the parameter this node satisfies, null if it's the root node
   * @param implementationType  the implementation type to create, may be null
   * @param arguments  the list of nodes representing the arguments to the constructor, not null
   */
  ClassNode(Class<?> type, Class<?> implementationType, List<FunctionModelNode> arguments, Parameter parameter) {
    super(type, parameter, arguments);
    _implementationType = ArgumentChecker.notNull(implementationType, "implementationType");
    _constructor = EngineUtils.getConstructor(_implementationType);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the implementation type.
   * 
   * @return the implementation type, not null
   */
  public Class<?> getImplementationType() {
    return _implementationType;
  }

  //-------------------------------------------------------------------------
  @Override
  protected Object doCreate(ComponentMap componentMap, List<Object> dependencies) {
    try {
      return _constructor.newInstance(dependencies.toArray());
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new OpenGammaRuntimeException("Failed to create of " + _constructor.getDeclaringClass().getName(), e);
    }
  }

  @Override
  protected String prettyPrintLine() {
    return "new " + _implementationType.getSimpleName();
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
    final ClassNode other = (ClassNode) obj;
    return Objects.equals(this._implementationType, other._implementationType);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(_implementationType);
  }

}
