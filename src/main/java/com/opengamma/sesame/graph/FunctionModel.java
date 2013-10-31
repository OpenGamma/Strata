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

  public FunctionNode getRootFunction() {
    return _root;
  }

  public FunctionMetadata getRootMetadata() {
    return _rootMetadata;
  }

  public static FunctionModel forFunction(FunctionMetadata function, FunctionConfig config, Set<Class<?>> infrastructure) {
    return new FunctionModel(createNode(function.getDeclaringType(), config, infrastructure), function);
  }

  public static FunctionModel forFunction(FunctionMetadata function, FunctionConfig config) {
    return new FunctionModel(createNode(function.getDeclaringType(), config, Collections.<Class<?>>emptySet()), function);
  }

  public static FunctionModel forFunction(FunctionMetadata function) {
    return new FunctionModel(createNode(function.getDeclaringType(), FunctionConfig.EMPTY, Collections.<Class<?>>emptySet()), function);
  }

  // TODO need to pass in config that merges default from system, view, (calc config), column, input type
  // it needs to know about the view def, (calc config), column because this class doesn't
  // it needs to merge arguments from everything going up the type hierarchy
  // when searching for args should we go up the type hierarchy at each level of config or go up the levels of config
  // for each type? do we only need to look for the implementation type and the function type?
  // constructor args only apply to the impl and method args apply to the fn method which is on all subtypes of
  // the fn interface. so only need to look at the superclasses that impl the fn interface
  // so in this case we don't need to go up the hierarchy because we're only dealing with constructor args
  // in the engine we'll need to go up the class hierarchy and check everything
  // for implementation class it just needs to go up the set of defaults looking for the first one that matches

  @SuppressWarnings("unchecked")
  private static FunctionNode createNode(Class<?> type, FunctionConfig config, Set<Class<?>> infrastructureTypes) {
    Class<?> implType;
    // TODO if there's a provider for type then implType is the provider type
    // then the provider will be built and have its dependencies built and injected
    if (!type.isInterface()) {
      implType = type;
    } else {
      implType = config.getFunctionImplementation(type);
    }
    Constructor<?> constructor = ConfigUtils.getConstructor(implType);
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
        // TODO providers
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
      // TODO providers
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
