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

import javax.inject.Provider;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * A node in the dependency model an object referred to by its concrete class that must be created by the injection framework.
 */
public class ClassNode extends DependentNode {

  private final Class<?> _type;

  public ClassNode(Class<?> type, List<Node> arguments, Parameter parameter) {
    super(parameter, arguments);
    _type = ArgumentChecker.notNull(type, "type");
  }

  @Override
  public Object create(ComponentMap componentMap, List<Object> dependencies) {
    Constructor<?> constructor = ConfigUtils.getConstructor(_type);
    try {
      Object instance = constructor.newInstance(dependencies.toArray());
      if (instance instanceof Provider) {
        // TODO check for @Provides
        return ((Provider) instance).get();
      } else {
        return instance;
      }
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new OpenGammaRuntimeException("Failed to create of " + constructor.getDeclaringClass().getName(), e);
    }
  }

  public Class<?> getType() {
    return _type;
  }

  @Override
  public String prettyPrint() {
    return getParameterName() + "new " + _type.getSimpleName();
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(_type);
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
    final ClassNode other = (ClassNode) obj;
    return Objects.equals(this._type, other._type);
  }
}
