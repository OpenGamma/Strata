/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.OpenGammaRuntimeException;

/**
 * TODO this needs a less ambiguous name
 * TODO will this also work for providers? should be able to build anything with a suitable constructor. different name?
 */
public final class FunctionNode extends Node {

  // TODO if this needs to be serializable this might have to be stored as class + args. or just class
  private final Constructor<?> _constructor;
  private final List<Node> _arguments;

  public FunctionNode(Constructor<?> constructor, List<Node> arguments) {
    _constructor = constructor;
    _arguments = ImmutableList.copyOf(arguments);
  }

  @Override
  Object create(Map<Class<?>, Object> infrastructure) {
    try {
      List<Object> arguments = Lists.newArrayListWithCapacity(_arguments.size());
      for (Node argument : _arguments) {
        arguments.add(argument.create(infrastructure));
      }
      // TODO provider support. what about singletons? need some hook into state shared across the build process
      // if instance is a provider return get(), otherwise return the instance. or have separate ProviderNode?
      return _constructor.newInstance(arguments.toArray());
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new OpenGammaRuntimeException("Failed to create of " + _constructor.getDeclaringClass().getName(), e);
    }
  }
}
