/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.lang.reflect.Constructor;
import java.util.List;

import com.google.common.collect.Lists;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.function.Parameter;

/**
 * A lightweight representation of the dependency tree for a single function.
 * TODO joda bean? needs to be serializable along with all Node subclasses.
 * need support for final fields or immutable inheritance
 * in the mean time could make Node.getDependencies() abstract and put the field in every subclass
 */
public final class FunctionModel {

  private final ClassNode _root;
  private final FunctionMetadata _rootMetadata;

  public FunctionModel(ClassNode root, FunctionMetadata rootMetadata) {
    _root = root;
    _rootMetadata = rootMetadata;
  }

  public ClassNode getRootFunction() {
    return _root;
  }

  public FunctionMetadata getRootMetadata() {
    return _rootMetadata;
  }

  public static FunctionModel forFunction(FunctionMetadata function, GraphConfig config) {
    return new FunctionModel(createNode(function.getDeclaringType(), config), function);
  }

  public static FunctionModel forFunction(FunctionMetadata function, FunctionConfig config) {
    return new FunctionModel(createNode(function.getDeclaringType(), new GraphConfig(config)), function);
  }

  public static FunctionModel forFunction(FunctionMetadata function) {
    return new FunctionModel(createNode(function.getDeclaringType(), GraphConfig.EMPTY), function);
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
  private static ClassNode createNode(Class<?> type, GraphConfig config) {
    Class<?> implType = config.getImplementationType(type);
    if (implType == null && !type.isInterface()) {
      implType = type;
    }
    if (implType == null) {
      throw new IllegalArgumentException("No implementation or provider available for " + type.getName());
    }
    Constructor<?> constructor = ConfigUtils.getConstructor(implType);
    List<Parameter> parameters = ConfigUtils.getParameters(constructor);
    List<Node> constructorArguments = Lists.newArrayListWithCapacity(parameters.size());
    for (Parameter parameter : parameters) {
      Node argument = getArgument(implType, parameter, config);
      if (argument != null) {
        constructorArguments.add(argument);
      } else {
        // TODO this assumes anything that isn't infrastructure or an argument is a parameter
        // is that right? do we need to create regular objects?
        // TODO cyclic dependencies
        // TODO this is where proxies will be inserted
        constructorArguments.add(createNode(parameter.getType(), config));
      }
    }
    return new ClassNode(constructor, constructorArguments);
  }

  private static Node getArgument(Class<?> implType, Parameter parameter, GraphConfig config) {
    if (config.getObject(parameter.getType()) != null) {
      return new ObjectNode(parameter.getType());
    }
    // TODO can we handle missing nullable constructor parameters and return a null node instead of null?
    Object argument = config.getConstructorArgument(implType, parameter.getType(), parameter.getName());
    if (argument != null) {
      // TODO check full a null value and nullability of the parameter
      return new ArgumentNode(parameter.getType(), argument);
    }
    return null;
  }

  public InvokableFunction build(ComponentMap components) {
    Object receiver = _root.create(components);
    return _rootMetadata.getInvokableFunction(receiver);
  }

  // TODO pretty print method
}
