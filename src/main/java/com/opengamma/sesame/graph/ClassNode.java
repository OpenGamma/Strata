/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.inject.Provider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.util.ArgumentChecker;

/**
 * A node in the dependency model an object referred to by its concrete class that must be created by the injection framework.
 * TODO separate class ProviderNode? it would only differ by a line or two
 */
public class ClassNode extends Node {

  private final List<Node> _arguments;
  private final Class<?> _type;

  public ClassNode(Class<?> type, List<Node> arguments) {
    _type = ArgumentChecker.notNull(type, "type");
    _arguments = ImmutableList.copyOf(ArgumentChecker.notNull(arguments, "arguments"));
  }

  @Override
  public Object create(ComponentMap components) {
    Constructor<?> constructor = ConfigUtils.getConstructor(_type);
    try {
      List<Object> arguments = Lists.newArrayListWithCapacity(_arguments.size());
      for (Node argument : _arguments) {
        arguments.add(argument.create(components));
      }
      Object instance = constructor.newInstance(arguments.toArray());
      if (instance instanceof Provider) {
        return ((Provider) instance).get();
      } else {
        return instance;
      }
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new OpenGammaRuntimeException("Failed to create of " + constructor.getDeclaringClass().getName(), e);
    }
  }

  @Override
  public List<Node> getDependencies() {
    return _arguments;
  }

  public Class<?> getType() {
    return _type;
  }
}
