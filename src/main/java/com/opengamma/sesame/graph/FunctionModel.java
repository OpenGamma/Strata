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
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.sesame.function.UserParam;

/**
 * A lightweight representation of the dependency tree for a single function.
 * TODO joda bean? needs to be serializable along with all Node subclasses.
 * need support for final fields or immutable inheritance
 * in the mean time could make Node.getDependencies() abstract and put the field in every subclass
 */
public final class FunctionModel {

  // TODO wrap the return value from createNode in forFunction in something that knows about the invoker
  // TODO or have a different node/function subtype that contains the invoker and any other metadata about the fn
  private final FunctionNode _root;
  private final FunctionMetadata _rootMetadata;

  /* package */ FunctionModel(FunctionNode root, FunctionMetadata rootMetadata) {
    _root = root;
    _rootMetadata = rootMetadata;
  }

  /* package */ FunctionModel(FunctionNode root) {
    this(root, null);
  }

  public FunctionNode getRootFunction() {
    return _root;
  }

  public FunctionMetadata getRootMetadata() {
    return _rootMetadata;
  }

  public static FunctionModel forFunction(Class<?> functionType, FunctionConfig config, Set<Class<?>> infrastructure) {
    return new FunctionModel(createNode(functionType, config, infrastructure));
  }

  public static FunctionModel forFunction(Class<?> functionType, FunctionConfig config) {
    return new FunctionModel(createNode(functionType, config, Collections.<Class<?>>emptySet()));
  }

  public static FunctionModel forFunction(Class<?> functionType) {
    return new FunctionModel(createNode(functionType, FunctionConfig.EMPTY, Collections.<Class<?>>emptySet()));
  }

  public static FunctionModel forFunction(FunctionMetadata function, FunctionConfig config, Set<Class<?>> infrastructure) {
    return new FunctionModel(createNode(function.getDeclaringType(), config, infrastructure), function);
  }

/*  public static FunctionTree forFunction(FunctionMetadata function, FunctionConfig config) {
    return new FunctionTree(createNode(function.getDeclaringType(), config, Collections.<Class<?>>emptySet()));
  }

  public static FunctionTree forFunction(FunctionMetadata function) {
    return new FunctionTree(createNode(function.getDeclaringType(), FunctionConfig.EMPTY, Collections.<Class<?>>emptySet()));
  }*/

  @SuppressWarnings("unchecked")
  private static FunctionNode createNode(Class<?> functionType, FunctionConfig config, Set<Class<?>> infrastructureTypes) {
    Class<?> implType;
    if (!functionType.isInterface()) {
      implType = functionType;
    } else {
      implType = config.getFunctionImplementation(functionType);
    }
    Constructor<?> constructor = ConfigUtils.getConstructor(functionType);
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
    return new FunctionNode(constructor, constructorArguments);
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
      return new InfrastructureNode(parameter.getType());
    } else if (parameter.getAnnotations().containsKey(UserParam.class)) {
      // TODO do we still want to use UserParam? NO - get defaults from somewhere else or not at all
      // TODO replace UserParam with an optional @DefaultValue which will be helpful for the UI?
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
      return new ArgumentNode(parameter.getType(), value, fallbackValue);
    } else {
      return null;
    }
  }

  public InvokableFunction build(Map<Class<?>, Object> infrastructure) {
    Object receiver = _root.create(infrastructure);
    return _rootMetadata.getInvokableFunction(receiver);
  }

  // TODO pretty print method
}
