/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collections;
import java.util.Set;

import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.graph.convert.ArgumentConverter;
import com.opengamma.util.ArgumentChecker;

/**
 * Lightweight representation of the tree of functions for a single output.
 * <p>
 * Each node in the tree is represented by {@link FunctionModelNode}.
 * There are nodes for each function that must be created and for each
 * parameter passed to the constructor of the function.
 * Proxy nodes may be added to insert additional behaviour.
 * An error node can be added if there is a problem.
 * TODO this class has static and instance methods called build(). that's not good
 */
public final class FunctionModel {

  // TODO replace Set<Class<?>> component types with AvailableComponents
  // TODO wrap FunctionModelConfig and AvailableComponents in a class for easier decoration by scenarios. name?
  // TODO wrap FunctionModelConfig and ComponentMap in a class for easier decoration by scenarios. name?

  /** The root node of the tree of functions and dependencies. */
  private final FunctionModelNode _root;

  /** The function meta-data for the root function. */
  private final FunctionMetadata _rootMetadata;

  /**
   * @param root  the root node, not null
   * @param rootMetadata  the meta-data, not null
   */
  private FunctionModel(FunctionModelNode root, FunctionMetadata rootMetadata) {
    _root = root;
    _rootMetadata = rootMetadata;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the function defined by this model.
   * 
   * @param builder  the builder to use, not null
   * @param components  the map of components, not null
   * @return the function created from this model, not null
   * @throws GraphBuildException if the function cannot be built
   */
  public InvokableFunction build(FunctionBuilder builder, ComponentMap components) {
    Object receiver = builder.create(_root, components);
    return _rootMetadata.getInvokableFunction(receiver);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the root node of the function model tree.
   * 
   * @return the root node
   */
  public FunctionModelNode getRoot() {
    return _root;
  }

  /**
   * Checks if this model is valid such that a function can be constructed.
   * 
   * @return true if this model is valid
   */
  public boolean isValid() {
    return _root.isValid();
  }

  //-------------------------------------------------------------------------
  /**
   * Pretty prints this model without proxies.
   * 
   * @return the tree structure, not null
   */
  public String prettyPrint() {
    return prettyPrint(false);
  }

  /**
   * Pretty prints this model.
   * 
   * @param showProxies  true to include proxy nodes
   * @return the tree structure, not null
   */
  public String prettyPrint(boolean showProxies) {
    return _root.prettyPrint(showProxies);
  }

  //-------------------------------------------------------------------------

  /**
   * Builds a {@link FunctionModel} representing a function implementation and its dependencies.
   *
   * @param function The function's metadata
   * @param config Configuration specifying the implementations and arguments for building the function and its dependencies
   * @param availableComponents Component types available for injecting into function implementations
   * @param nodeDecorators For inserting nodes in the model between functions to add functionality, e.g. caching,
   *   logging, tracing
   * @return A model of the function implementation and its dependencies
   */
  public static FunctionModel forFunction(FunctionMetadata function,
                                          FunctionModelConfig config,
                                          Set<Class<?>> availableComponents,
                                          NodeDecorator... nodeDecorators) {
    NodeDecorator nodeDecorator = CompositeNodeDecorator.compose(nodeDecorators);
    FunctionModelNode node = FunctionModelNode.create(function.getDeclaringType(), config, availableComponents, nodeDecorator);
    return new FunctionModel(node, function);
  }

  /**
   * Builds a {@link FunctionModel} representing a function implementation and its dependencies.
   *
   * @param function The function's metadata
   * @param config Configuration specifying the implementations and arguments for building the function and its dependencies
   * @param availableComponents Component types available for injecting into function implementations
   * @param nodeDecorator For inserting nodes in the model between functions to add functionality, e.g. caching,
   *   logging, tracing
   * @return A model of the function implementation and its dependencies
   */
  public static FunctionModel forFunction(FunctionMetadata function,
                                          FunctionModelConfig config,
                                          Set<Class<?>> availableComponents,
                                          NodeDecorator nodeDecorator,
                                          ArgumentConverter argumentConverter) {
    FunctionModelNode node =
        FunctionModelNode.create(function.getDeclaringType(), config, availableComponents, nodeDecorator, argumentConverter);
    return new FunctionModel(node, function);
  }

  /**
   * Builds a {@link FunctionModel} representing a function implementation and its dependencies.
   * This is suitable for building functions that require user specified constructor arguments but don't require
   * components provided by the system.
   * @param function The function's metadata
   * @param config Configuration specifying the implementations and arguments for building the function and its
   * dependencies
   * logging, tracing
   * @return A model of the function implementation and its dependencies
   */
  public static FunctionModel forFunction(FunctionMetadata function, FunctionModelConfig config) {
    FunctionModelNode node = FunctionModelNode.create(function.getDeclaringType(),
                                                      config,
                                                      Collections.<Class<?>>emptySet(),
                                                      NodeDecorator.IDENTITY);
    return new FunctionModel(node, function);
  }

  /**
   * Builds a {@link FunctionModel} representing a function implementation and its dependencies.
   * This is only suitable for building the simplest of functions that require no arguments or components.
   * @param function The function's metadata
   * @return A model of the function implementation and its dependencies
   */
  public static FunctionModel forFunction(FunctionMetadata function) {
    FunctionModelNode node = FunctionModelNode.create(function.getDeclaringType(),
                                                      FunctionModelConfig.EMPTY,
                                                      Collections.<Class<?>>emptySet(),
                                                      NodeDecorator.IDENTITY);
    return new FunctionModel(node, function);
  }

  /**
   * Builds a function model from metadata and an existing model node.
   * This is useful for the case where
   *
   * @param function the function's metadata
   * @param functionNode model node for building the function instance
   * @return a model built using the metadata and node
   */
  public static FunctionModel forFunction(FunctionMetadata function, FunctionModelNode functionNode) {
    ArgumentChecker.notNull(function, "function");
    ArgumentChecker.notNull(functionNode, "functionNode");

    Class<?> functionType = function.getDeclaringType();
    Class<?> nodeType = functionNode.getType();

    if (!functionType.isAssignableFrom(nodeType)) {
      throw new IllegalArgumentException("Function type " + functionType.getName() + " is not compatible with node type " +
                                             nodeType.getName());
    }
    return new FunctionModel(functionNode, function);
  }

  // functions built with this method will have a cache that's not shared with any other functions
  // if that's required an overloaded version will be needed with a FunctionBuilder parameter
  /**
   * Creates a {@link FunctionModelNode} for a function and its dependencies, builds it and returns the constructed function object.
   * @param functionType The type of the function, can be an interface or implementation class
   * @param config Configuration for building the function and its dependencies
   * @param componentMap Components available for injecting into the function and its dependencies
   * @param nodeDecorators For inserting nodes between functions to add functionality, e.g. caching,
   * logging, tracing
   * @param <T> The type of the function
   * @return The constructed function, not null
   */
  public static <T> T build(Class<T> functionType,
                            FunctionModelConfig config,
                            ComponentMap componentMap,
                            NodeDecorator... nodeDecorators) {
    FunctionBuilder functionBuilder = new FunctionBuilder();
    NodeDecorator nodeDecorator = CompositeNodeDecorator.compose(nodeDecorators);
    FunctionModelNode node = FunctionModelNode.create(functionType,
                                                      config,
                                                      componentMap.getComponentTypes(),
                                                      nodeDecorator);
    Object function = functionBuilder.create(node, componentMap);
    return functionType.cast(function);
  }

  /**
   * Creates a {@link FunctionModelNode} for a function and its dependencies, builds it and returns the constructed function object.
   * This is suitable for building functions that require user specified constructor arguments but don't require
   * components provided by the system.
   * @param functionType The type of the function, can be an interface or implementation class
   * @param config Configuration for building the function and its dependencies
   * @param <T> The type of the function
   * @return The constructed function, not null
   */
  public static <T> T build(Class<T> functionType, FunctionModelConfig config) {
    return build(functionType, config, ComponentMap.EMPTY);
  }

  /**
   * Creates a {@link FunctionModelNode} for a function and its dependencies, builds it and returns the constructed function object.
   * This is only suitable for building the simplest of functions that require no arguments or components.
   * @param functionType The type of the function, can be an interface or implementation class
   * @param <T> The type of the function
   * @return The constructed function, not null
   */
  public static <T> T build(Class<T> functionType) {
    return build(functionType, FunctionModelConfig.EMPTY);
  }
}
