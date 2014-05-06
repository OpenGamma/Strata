/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang.ClassUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opengamma.core.link.Link;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.sesame.config.CompositeFunctionModelConfig;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * A node in the function model that can build a function and its dependencies.
 */
public abstract class FunctionModelNode {

  /** A set of ineligible types. */
  private static final Set<Class<?>> INELIGIBLE_TYPES =
      Sets.<Class<?>>newHashSet(UniqueId.class, ExternalId.class, ExternalIdBundle.class);

  /** The expected type of the object created by this node, not null. */
  private final Class<?> _type;

  /** The parameter this node satisfies, null if it's the root node. */
  private final Parameter _parameter;

  /**
   * Creates an instance.
   * 
   * @param type  the expected type of the object created by this node, not null
   * @param parameter  the parameter this node satisfies, null if it's the root node
   */
  FunctionModelNode(Class<?> type, Parameter parameter) {
    _type = ArgumentChecker.notNull(type, "type");
    _parameter = parameter;
  }

  /**
   * Creates a node for building an object of the specified type.
   * The node and its dependencies are built using the provided config.
   *
   * @param type the type of the object built by the node
   * @param config configuration used to build the node and any dependent nodes
   * @param availableComponents the types of components that the engine can provide to satisfy node dependencies
   * @param nodeDecorator inserts proxies between functions to provide engine services, e.g. caching, tracing
   * @return a node that can build an object
   */
  public static FunctionModelNode create(Class<?> type,
                                         FunctionModelConfig config,
                                         Set<Class<?>> availableComponents,
                                         NodeDecorator nodeDecorator) {
    return createNode(type, config, availableComponents, nodeDecorator, Lists.<Parameter>newArrayList(), null);
  }

  @Nullable
  private static FunctionModelNode createNode(Class<?> type,
                                              FunctionModelConfig config,
                                              Set<Class<?>> availableComponents,
                                              NodeDecorator nodeDecorator,
                                              List<Parameter> path,
                                              @Nullable Parameter parameter) {
    if (!isEligibleForBuilding(type)) {
      return null;
    }

    Class<?> implType;
    try {
      implType = getImplementationType(type, config, path, parameter);
    } catch (InvalidGraphException e) {
      return new ErrorNode(type, e, parameter);
    }

    Constructor<?> constructor;
    try {
      constructor = getConstructor(implType, path);
    } catch (NoSuitableConstructorException e) {
      return new ErrorNode(type, e, parameter);
    }

    List<Parameter> constructorParameters = EngineUtils.getParameters(constructor);
    List<FunctionModelNode> constructorArguments = Lists.newArrayListWithCapacity(constructorParameters.size());

    for (Parameter constructorParameter : constructorParameters) {
      // this isn't terribly efficient but unlikely to be a problem. could use a stack but that's nasty and mutable
      List<Parameter> newPath = Lists.newArrayList(path);
      newPath.add(constructorParameter);
      FunctionModelNode argNode = createArgumentNode(type, config, availableComponents, nodeDecorator, implType, constructorParameter, newPath);
      constructorArguments.add(argNode);
    }
    FunctionModelNode node;

    if (type.isInterface()) {
      node = new InterfaceNode(type, implType, constructorArguments, parameter);
    } else {
      node = new ClassNode(type, implType, constructorArguments, parameter);
    }
    return nodeDecorator.decorateNode(node);
  }

  /**
   * Returns the implementation that should be used for creating instances of a type.
   * <p>
   * The result can be:
   * <ul>
   * <li>An implementation of the specified interface</li>
   * <li>A {@link Provider} that provides the implementation</li>
   * <li>The input type itself, if it is a concrete class</li>
   * </ul>
   * @param type  the type to find the implementation type for, not null
   * @param parameter the constructor parameter the implementation must satisfy
   * @return the implementation type that should be used, not null
   * @throws NoImplementationException if there is no implementation available
   */
  private static Class<?> getImplementationType(Class<?> type,
                                                FunctionModelConfig config,
                                                List<Parameter> path,
                                                @Nullable Parameter parameter) {
    Class<?> implType = null;

    if (parameter != null) {
      implType = config.getFunctionImplementation(parameter);
    }
    if (implType == null) {
      implType = config.getFunctionImplementation(type);
    }
    if (implType == null) {
      if (type.isInterface()) {
        throw new NoImplementationException(type, path, "No implementation or provider found: " + type.getSimpleName());
      }
      implType = type;
    }
    if (!isValidImplementationType(implType)) {
      throw new InvalidImplementationException(path, "Function implementation is invalid: " + type.getSimpleName());
    }
    return implType;
  }

  /**
   * Returns a constructor for the engine to build instances of type.
   * <p>
   * Only public constructors are considered.
   * If there is only one constructor it is used. If there are multiple constructors
   * then one, and only one, must be annotated with {@link Inject}.
   *
   * @param type  the type to find the constructor for, not null
   * @return the constructor the engine should use for building instances, not null
   * @throws NoSuitableConstructorException if there isn't a valid constructor
   */
  private static Constructor<?> getConstructor(Class<?> type, List<Parameter> path) {
    try {
      return EngineUtils.getConstructor(type);
    } catch (IllegalArgumentException ex) {
      throw new NoSuitableConstructorException(path, "No suitable constructor: " + type.getName());
    }
  }

  /**
   * <p>Creates a node in the function model representing a single constructor argument.
   * If the object has dependencies this method is called recursively and descends down the dependency tree
   * creating child nodes until all dependencies are satisfied. If a dependency can't be satisfied an error node is
   * created.</p>
   *
   * <p>There are 3 ways a dependency can be satisfied. They are tried in order:
   * <ol>
   *   <li>A component provided by the configuration. The component is matched to the type of the parameter</li>
   *   <li>A value provided by the user in the configuration. This is specified in {@link FunctionArguments} by
   *   the implementation class of the function and the parameter name</li>
   *   <li>A function built by recursively calling
   *   {@link #createNode(Class, FunctionModelConfig, Set, NodeDecorator, List, Parameter)}</li>
   * </ol>
   * </p>
   * @param type The type of the parent object into which the argument will be injected
   * @param config The configuration for building the model
   * @param parameter The constructor parameter this node must satisfy.
   * @param path The chain of dependencies from the root of the tree of functions to this parameter. In the event of a
   * failure this allows the exact location in the graph to be identified
   * @return A node in the function graph, not null
   */
  private static FunctionModelNode createArgumentNode(Class<?> type,
                                                      FunctionModelConfig config,
                                                      Set<Class<?>> availableComponents,
                                                      NodeDecorator nodeDecorator,
                                                      Class<?> implType,
                                                      Parameter parameter,
                                                      List<Parameter> path) {
    try {
      if (availableComponents.contains(parameter.getType())) {
        // the parameter can be satisfied by an existing component, no need to build it or look up a user argument
        return nodeDecorator.decorateNode(new ComponentNode(parameter));
      }
      // has the user explicitly provided an argument?
      Object argument = getConstructorArgument(config, implType, path, parameter);

      // can we use this argument directly?
      if (canInjectArgument(argument, parameter.getType())) {
        return new ArgumentNode(parameter.getType(), argument, parameter);
      }
      // if the user has specified function config as the argument we use it to build the subtree
      // otherwise we use the existing config to build it
      FunctionModelConfig subtreeConfig =
          argument != null ?
              CompositeFunctionModelConfig.compose(((FunctionModelConfig) argument), config) :
              config;
      FunctionModelNode createdNode = createNode(parameter.getType(), subtreeConfig, availableComponents, nodeDecorator, path, parameter);

      if (createdNode != null) {
        return createdNode;
      }
      if (parameter.isNullable()) {
        return nodeDecorator.decorateNode(new ArgumentNode(parameter.getType(), null, parameter));
      }
      throw new NoConstructorArgumentException(path, "No value available for non-nullable parameter " +
                                                   parameter.getFullName());
    } catch (InvalidGraphException e) {
      return new ErrorNode(type, e, parameter);
    }
  }

  /**
   * Checks whether the argument can be used to satisfy a parameter of the specified type.
   * Returns false if the argument is null or the argument type is {@link FunctionModelConfig}
   * but the parameter type isn't.
   *
   * @param argument the argument
   * @param parameterType the type of the parameter
   * @return true if the argument can be used to satisfy the parameter
   */
  private static boolean canInjectArgument(@Nullable Object argument, Class<?> parameterType) {
    if (argument == null) {
      return false;
    }
    if (argument instanceof FunctionModelConfig) {
      if (parameterType.isAssignableFrom(FunctionModelConfig.class)) {
        return true;
      } else {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the type is eligible for building.
   *
   * @param type  the type, not null
   * @return true if valid
   */
  private static boolean isEligibleForBuilding(Class<?> type) {
    if (INELIGIBLE_TYPES.contains(type)) {
      return false;
    }
    if (type.isPrimitive()) {
      return false;
    }
    Package pkg = type.getPackage();
    if (pkg != null) {
      String packageName = pkg.getName();
      if (packageName.startsWith("java") || packageName.startsWith("javax") || packageName.startsWith("org.threeten")) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the type is invalid and cannot be constructed.
   *
   * @param type  the type, not null
   * @return true if valid
   */
  private static boolean isValidImplementationType(Class<?> type) {
    if (type.isInterface() || type.isAnnotation() || type.isPrimitive() || type.isArray() || type.isEnum() || type.isSynthetic() ||
        Modifier.isAbstract(type.getModifiers()) || type.isAnonymousClass() || type.isLocalClass()) {
      return false;
    }
    if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers())) {  // inner class (vs nested class)
      return false;
    }
    return true;
  }

  /**
   * Checks the type of the constructor argument matches the expected type and returns it.
   * Handles {@link Link}s and {@link Provider}s by checking the parameter type is a
   * {@link Link} or {@link Provider} or that its type is compatible with the linked / provided object.
   */
  private static Object getConstructorArgument(FunctionModelConfig functionModelConfig,
                                               Class<?> objectType,
                                               List<Parameter> path,
                                               Parameter parameter) {
    FunctionArguments args = functionModelConfig.getFunctionArguments(objectType);
    Object arg = args.getArgument(parameter.getName());

    if (arg == null || arg instanceof Provider || arg instanceof FunctionModelConfig) {
      return arg;
      // this takes into account boxing of primitives which Class.isAssignableFrom() doesn't
    } else if (ClassUtils.isAssignable(arg.getClass(), parameter.getType(), true)) {
      return arg;
    } else if (arg instanceof Link) {
      if (ClassUtils.isAssignable(((Link<?>) arg).getTargetType(), parameter.getType(), true)) {
        return arg;
      } else {
        throw new IncompatibleTypeException(path, "Link argument (" + arg + ") doesn't resolve to the " +
            "required type for " + parameter.getFullName());
      }
    } else {
      throw new IncompatibleTypeException(path, "Argument (" + arg + ": " + arg.getClass().getSimpleName() + ") isn't of the " +
          "required type for " + parameter.getFullName());
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the expected type of the object created by this node.
   * 
   * @return the expected type, not null
   */
  public Class<?> getType() {
    return _type;
  }

  /**
   * Gets the parameter that this node satisfies.
   * 
   * @return the parameter, not null
   */
  public Parameter getParameter() {
    return _parameter;
  }

  /**
   * Gets the concrete, non-proxy, node.
   * <p>
   * This is used to access the concrete node that has been proxied.
   * Most nodes simply return {@code this}.
   * 
   * @return the parameter, not null
   */
  public FunctionModelNode getConcreteNode() {
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the object represented by this node.
   * <p>
   * Implementations should override {@link #doCreate}, not this method.
   * 
   * @param componentMap  the map of infrastructure components, not null
   * @param dependencies  the dependencies of this node, not null
   * @return the object represented by this node, may be null
   */
  public Object create(ComponentMap componentMap, List<Object> dependencies) {
    Object object = doCreate(componentMap, dependencies);
    if (object instanceof Provider) {
      // TODO some slightly more robust checking of compatibility of types
      // TODO what's the logic I actually need here?
      return Provider.class.isAssignableFrom(_type) ? object : ((Provider<?>) object).get();
    } else if (object instanceof Link) {
      return Link.class.isAssignableFrom(_type) ? object : ((Link<?>) object).resolve();
    } else {
      return object;
    }
  }

  /**
   * Returns the object represented by this node, creating if necessary.
   * <p>
   * If this node's object is a {@link Provider} this method should return it,
   * not the results of calling {@link Provider#get()}. This class will use the
   * expected type to decide whether to call {@code get()} or
   * to inject the provider instance directly.
   * 
   * @param componentMap  the map of infrastructure components, not null
   * @param dependencies  the dependencies of this node, not null
   * @return the object represented by this node, may be null
   */
  protected abstract Object doCreate(ComponentMap componentMap, List<Object> dependencies);

  //-------------------------------------------------------------------------
  /**
   * Gets the dependencies of this node.
   * 
   * @return the dependencies, not null
   */
  public List<FunctionModelNode> getDependencies() {
    return Collections.emptyList();
  }

  /**
   * Gets the complete set of exceptions in the tree of this node.
   * 
   * @return the list of exceptions, not null
   */
  public List<InvalidGraphException> getExceptions() {
    return Collections.emptyList();
  }

  /**
   * Checks if this node represents a valid object that can be constructed.
   * <p>
   * A true result implies that this node and all nodes below it in the dependency tree are valid.
   * 
   * @return true if this node and all its dependencies are valid
   */
  public boolean isValid() {
    return true;
  }

  /**
   * Checks if this node is an error node.
   * 
   * @return true if this node represents an object that is the source of an error
   */
  public boolean isError() {
    return false;
  }

  //-------------------------------------------------------------------------
  /**
   * Pretty prints this node and its descendants.
   *
   * @param showProxies  true to include proxy nodes
   * @return the node structure, not null
   */
  public String prettyPrint(boolean showProxies) {
    return prettyPrint(new StringBuilder(), "", "", showProxies).toString();
  }

  /**
   * Pretty prints this node and its descendants without including proxy nodes inserted to provide engine services.
   *
   * @return the node structure, not null
   */
  public String prettyPrint() {
    return prettyPrint(false);
  }

  /**
   * Provides the name of the parameter being satisfied ready for pretty printing.
   * 
   * @return a description of the node, not null
   */
  protected abstract String prettyPrintLine();

  /**
   * Performs the pretty print.
   * 
   * @param builder  the builder to add to, not null
   * @param indent  the current indent, not null
   * @param childIndent  the child indent, not null
   * @param showProxies  true to include proxy nodes
   * @return the node structure, not null
   */
  private StringBuilder prettyPrint(StringBuilder builder, String indent, String childIndent, boolean showProxies) {
    FunctionModelNode realNode = (showProxies ? this : getConcreteNode());
    // prefix the line with an indicator if the node is an error node for easier debugging
    String errorPrefix = isError() ? "->" : "  ";
    // prefix the line with the parameter name
    String paramPrefix = (realNode.getParameter() != null ? realNode.getParameter().getName() + ": " : "");
    builder.append('\n').append(errorPrefix).append(indent).append(paramPrefix).append(realNode.prettyPrintLine());
    for (Iterator<FunctionModelNode> it = realNode.getDependencies().iterator(); it.hasNext();) {
      FunctionModelNode child = it.next();
      String newIndent;
      String newChildIndent;
      boolean isFinalChild = !it.hasNext();
      if (!isFinalChild) {
        newIndent = childIndent + " |--";  // Unicode boxes: \u251c\u2500\u2500
        newChildIndent = childIndent + " |  ";  // Unicode boxes: \u2502
      } else {
        newIndent = childIndent + " `--";  // Unicode boxes: \u2514\u2500\u2500
        newChildIndent = childIndent + "    ";
      }
      child.prettyPrint(builder, newIndent, newChildIndent, showProxies);
    }
    return builder;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final FunctionModelNode other = (FunctionModelNode) obj;
    return Objects.equals(this._type, other._type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_type);
  }

  @Override
  public String toString() {
    String paramPrefix = (getParameter() != null ? getParameter().getName() + ": " : "");
    return paramPrefix + prettyPrintLine();
  }

}
