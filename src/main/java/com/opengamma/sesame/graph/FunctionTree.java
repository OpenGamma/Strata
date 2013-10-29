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
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.sesame.function.UserParam;

/**
 * A lightweight representation of the dependency tree for a single function.
 * TODO joda bean? needs to be serializable along with all Node subclasses.
 * need support for final fields or immutable inheritance
 * in the mean time could make Node.getDependencies() abstract and put the field in every subclass
 */
public final class FunctionTree<T> {

  // TODO need to know which method to call on the root function
  // TODO if the Function knows about the function outputs and corresponding methods we can create invokers. but where?
  // only need to do it at the root
  // TODO need to know the actual output that this tree was built to satisfy
  // TODO wrap the return value from createNode in forFunction in something that knows about the output
  private final Function<T> _root;

  // TODO this shouldn't be public but is used by SecurityFunctionDecorator
  public FunctionTree(Function<T> root) {
    _root = root;
  }

  public Function<T> getRootFunction() {
    return _root;
  }

  public static <T> FunctionTree<T> forFunction(Class<T> functionType, FunctionConfig config, Set<Class<?>> infrastructure) {
    return new FunctionTree<>(createNode(functionType, config, infrastructure));
  }

  public static <T> FunctionTree<T> forFunction(Class<T> functionType, FunctionConfig config) {
    return new FunctionTree<>(createNode(functionType, config, Collections.<Class<?>>emptySet()));
  }

  public static <T> FunctionTree<T> forFunction(Class<T> functionType) {
    return new FunctionTree<>(createNode(functionType, FunctionConfig.EMPTY, Collections.<Class<?>>emptySet()));
  }

  // TODO does this need TypeMetadata? better to figure out the constructor and params here or there?
  // will they ever get reused either way?
  @SuppressWarnings("unchecked")
  private static <T> Function<T> createNode(Class<T> functionType, FunctionConfig config, Set<Class<?>> infrastructureTypes) {
    Class<? extends T> implType;
    if (!functionType.isInterface()) {
      implType = functionType;
    } else {
      implType = (Class<? extends T>) config.getFunctionImplementation(functionType);
    }
    Constructor<? extends T> constructor = ConfigUtils.getConstructor(implType);
    List<Parameter> parameters = ConfigUtils.getParameters(constructor);
    FunctionArguments functionArguments = config.getFunctionArguments(implType);
    List<Node> constructorArguments = Lists.newArrayListWithCapacity(parameters.size());
    for (Parameter parameter : parameters) {
      Node argument = getArgument(parameter, functionArguments, infrastructureTypes);
      if (argument != null) {
        constructorArguments.add(argument);
      } else {
        // TODO this assumes anything that isn't infrastructure or an argument is a parameter
        // is that right? do we need to create regular objects?
        // TODO cyclic dependencies
        // TODO this is where proxies will be inserted
        constructorArguments.add(createNode(parameter.getType(), config, infrastructureTypes));
      }
    }
    return new Function<>(constructor, constructorArguments);
  }

  private static Node getArgument(Parameter parameter,
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
      Object fallbackValue = StringConvert.INSTANCE.convertFromString(parameter.getType(), paramAnnotation.fallbackValue());
      if (functionArguments.hasArgument(paramName)) {
        // TODO allow the value to be a string and convert if the expected type isn't?
        value = functionArguments.getArgument(paramName);
      } else if (fallbackValue != null) {
        value = fallbackValue;
      } else {
        throw new IllegalArgumentException("No argument or fallback value available for parameter " + parameter);
      }
      return new Argument(parameter.getType(), value, fallbackValue);
    } else {
      return null;
    }
  }

  public T build(Map<Class<?>, Object> infrastructure) {
    return _root.create(infrastructure);
  }

  // TODO pretty print method
}
