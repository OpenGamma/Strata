/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.convert.StringConvert;

import com.google.common.collect.Lists;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.ConstructorParameter;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.UserParam;

/**
 * A lightweight representation of the dependency graph for a single function.
 * TODO joda bean? needs to be serializable along with all Node subclasses
 */
public final class Graph<T> {

  private final Function<T> _root;

  /* package */ Graph(Function<T> root) {
    _root = root;
  }

  public Function<T> getRootFunction() {
    return _root;
  }

  public static <T> Graph<T> forFunction(Class<T> functionType, FunctionConfig config, Set<Class<?>> infrastructure) {
    return new Graph<>(createNode(functionType, config, infrastructure));
  }

  public static <T> Graph<T> forFunction(Class<T> functionType, FunctionConfig config) {
    return new Graph<>(createNode(functionType, config, Collections.<Class<?>>emptySet()));
  }

  public static <T> Graph<T> forFunction(Class<T> functionType) {
    return new Graph<>(createNode(functionType, FunctionConfig.EMPTY, Collections.<Class<?>>emptySet()));
  }

  @SuppressWarnings("unchecked")
  private static <T> Function<T> createNode(Class<T> functionType, FunctionConfig config, Set<Class<?>> infrastructureTypes) {
    Class<? extends T> implType = (Class<? extends T>) config.getFunctionImplementation(functionType);
    Constructor<? extends T> constructor = ConfigUtils.getConstructor(implType);
    List<ConstructorParameter> parameters = ConfigUtils.getParameters(constructor);
    FunctionArguments functionArguments = config.getFunctionArguments(implType);
    List<Node> constructorArguments = Lists.newArrayListWithCapacity(parameters.size());
    for (ConstructorParameter parameter : parameters) {
      Node argument = getArgument(parameter, functionArguments, infrastructureTypes);
      if (argument != null) {
        constructorArguments.add(argument);
      } else {
        // TODO this assumes anything that isn't infrastructure or an argument is a parameter
        // is that right? do we need to create regular objects?
        // TODO cyclic dependencies
        constructorArguments.add(createNode(parameter.getType(), config, infrastructureTypes));
      }
    }
    return new Function<>(constructor, constructorArguments);
  }

  private static Node getArgument(ConstructorParameter parameter,
                                  FunctionArguments functionArguments,
                                  Set<Class<?>> infrastructureTypes) {
      /*
      there are 4 types of argument:
        infrastructure components
        other functions (return null for these)
        user arguments
        user arguments with defaults generated from @UserParam
      */
    if (infrastructureTypes.contains(parameter.getType())) {
      return new Infrastructure(parameter.getType());
    } else if (parameter.getAnnotations().containsKey(UserParam.class)) {
      UserParam paramAnnotation = (UserParam) parameter.getAnnotations().get(UserParam.class);
      String paramName = paramAnnotation.name();
      Object value;
      Object defaultValue = StringConvert.INSTANCE.convertFromString(parameter.getType(), paramAnnotation.defaultValue());
      if (functionArguments.hasArgument(paramName)) {
        value = functionArguments.getArgument(paramName);
      } else {
        value = defaultValue;
      }
      return new Argument(parameter.getType(), value, defaultValue);
    } else {
      return null;
    }
  }

  public T build(Map<Class<?>, Object> infrastructure) {
    return _root.create(infrastructure);
  }

  // TODO pretty print method
}
