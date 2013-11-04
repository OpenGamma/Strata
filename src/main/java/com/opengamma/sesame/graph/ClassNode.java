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
import com.opengamma.sesame.engine.ComponentMap;

/**
 * A node in the dependency model representing an object that must be created by the injection framework.
 * TODO separate class ProviderNode? it would only differ by a line or two
 */
public final class ClassNode extends Node {

  // TODO if this needs to be serializable this might have to be stored as class + arg types. or just class
  private final Constructor<?> _constructor;
  private final List<Node> _arguments;

  public ClassNode(Constructor<?> constructor, List<Node> arguments) {
    _constructor = constructor;
    _arguments = ImmutableList.copyOf(arguments);
  }

  @Override
  Object create(ComponentMap components) {
    try {
      List<Object> arguments = Lists.newArrayListWithCapacity(_arguments.size());
      for (Node argument : _arguments) {
        arguments.add(argument.create(components));
      }
      Object instance = _constructor.newInstance(arguments.toArray());
      if (instance instanceof Provider) {
        return ((Provider) instance).get();
      } else {
        return instance;
      }
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new OpenGammaRuntimeException("Failed to create of " + _constructor.getDeclaringClass().getName(), e);
    }
  }
}
