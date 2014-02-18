/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.apache.commons.lang.ClassUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opengamma.core.link.Link;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.sesame.config.EngineFunctionUtils;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.sesame.proxy.ProxyNode;

/**
 * A lightweight representation of the tree of functions for a single output.
 * TODO joda bean? needs to be serializable along with all Node subclasses.
 */
public final class FunctionModel {

  private static final Set<Class<?>> INELIGIBLE_TYPES =
      Sets.<Class<?>>newHashSet(UniqueId.class, ExternalId.class, ExternalIdBundle.class);

  private final Node _root;
  private final FunctionMetadata _rootMetadata;

  private FunctionModel(Node root, FunctionMetadata rootMetadata) {
    _root = root;
    _rootMetadata = rootMetadata;
  }

  public InvokableFunction build(FunctionBuilder builder, ComponentMap components) {
    Object receiver = builder.create(_root, components);
    return _rootMetadata.getInvokableFunction(receiver);
  }

  public String prettyPrint(boolean showProxies) {
    return prettyPrint(new StringBuilder(), _root, "", "", showProxies).toString();
  }

  public String prettyPrint() {
    return prettyPrint(false);
  }

  public boolean isValid() {
    return _root.isValid();
  }

  /* package */ List<AbstractGraphBuildException> getExceptions() {
    return _root.getExceptions();
  }

  private static NodeDecorator getNodeDecorator(NodeDecorator[] nodeDecorators) {
    switch (nodeDecorators.length) {
      case 0:
        return NodeDecorator.IDENTITY;
      case 1:
        return nodeDecorators[0];
      default:
        return new CompositeNodeDecorator(nodeDecorators);
    }
  }

  public static FunctionModel forFunction(FunctionMetadata function,
                                          FunctionModelConfig config,
                                          Set<Class<?>> availableComponents,
                                          NodeDecorator... nodeDecorators) {
    NodeDecorator nodeDecorator = getNodeDecorator(nodeDecorators);
    Node node = createNode(function.getDeclaringType(), config, availableComponents, nodeDecorator);
    return new FunctionModel(node, function);
  }

  public static FunctionModel forFunction(FunctionMetadata function, FunctionModelConfig config) {
    Node node = createNode(function.getDeclaringType(), config, Collections.<Class<?>>emptySet(), NodeDecorator.IDENTITY);
    return new FunctionModel(node, function);
  }

  public static FunctionModel forFunction(FunctionMetadata function) {
    Node node = createNode(function.getDeclaringType(),
                           FunctionModelConfig.EMPTY,
                           Collections.<Class<?>>emptySet(),
                           NodeDecorator.IDENTITY);
    return new FunctionModel(node, function);
  }

  // TODO make it clear this is for one-off building, testing etc, not for the engine (because FunctionBuilder isn't shared)
  public static <T> T build(Class<T> functionType,
                            FunctionModelConfig config,
                            ComponentMap componentMap,
                            NodeDecorator... nodeDecorators) {
    FunctionBuilder functionBuilder = new FunctionBuilder();
    NodeDecorator nodeDecorator = getNodeDecorator(nodeDecorators);
    Node node = createNode(functionType, config, componentMap.getComponentTypes(), nodeDecorator);
    Object function = functionBuilder.create(node, componentMap);
    return functionType.cast(function);
  }

  public static <T> T build(Class<T> functionType, FunctionModelConfig config) {
    return build(functionType, config, ComponentMap.EMPTY);
  }

  public static <T> T build(Class<T> functionType) {
    return build(functionType, FunctionModelConfig.EMPTY);
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

  private static Node createNode(Class<?> type,
                                 FunctionModelConfig config,
                                 Set<Class<?>> availableComponents,
                                 NodeDecorator nodeDecorator) {
    return createNode(type, config, availableComponents, nodeDecorator, Lists.<Parameter>newArrayList(), null);
  }

  // TODO I don't think this will work if the root is an infrastructure component. is that important?
  // TODO this is ugly, simplify / beautify
  /**
   * <p>Creates a node in the function model representing a single object.
   * If the object has dependencies this method is called recursively and descends down the dependency tree
   * creating child nodes until all dependencies are satisfied. If a dependency can't be satisfied an error node is
   * created.</p>
   *
   * <p>There are 3 ways a dependency can be satisfied. They are tried in order:
   * <ol>
   *   <li>A component provided by the engine. The component is matched to the type of the parameter</li>
   *   <li>A value provided by the user in the configuration. This is specified in {@link FunctionArguments} by
   *   the implementation class of the function and the parameter name</li>
   *   <li>A function built by recursively calling this method</li>
   * </ol>
   * </p>
   * @param type The type of the object
   * @param config The configuration for building the graph
   * @param path The chain of dependencies from the root of the tree of functions to this node. In the event of a
   * failure this allows the exact location in the graph to be identified
   * @param parentParameter The constructor parameter this object must satisfy.
   * @return A node in the function graph, not null
   */
  @SuppressWarnings("unchecked")
  private static Node createNode(Class<?> type,
                                 FunctionModelConfig config,
                                 Set<Class<?>> availableComponents,
                                 NodeDecorator nodeDecorator,
                                 List<Parameter> path,
                                 Parameter parentParameter) {
    if (!isEligibleForBuilding(type)) {
      return null;
    }
    Class<?> implType = config.getFunctionImplementation(type);
    if (implType == null && !type.isInterface()) {
      implType = type;
    }
    if (implType == null) {
      // TODO this isn't very nice. rename ExceptionNode->FailureNode and create directly without exceptions
      NoImplementationException e = new NoImplementationException(path, "No implementation or provider available for " +
                                                                    type.getSimpleName());
      return new ExceptionNode(type, e, parentParameter);
    }
    Constructor<?> constructor;
    try {
      constructor = EngineFunctionUtils.getConstructor(implType);
    } catch (IllegalArgumentException e) {
      // TODO this isn't very nice. rename ExceptionNode->FailureNode and create directly without exceptions
      NoSuitableConstructorException exception =
          new NoSuitableConstructorException(path, implType.getName() + " has no suitable constructors");
      return new ExceptionNode(type, exception, parentParameter);
    }
    List<Parameter> parameters = EngineFunctionUtils.getParameters(constructor);
    List<Node> constructorArguments = Lists.newArrayListWithCapacity(parameters.size());
    // create a node to satisfy each of the constructor parameters. this can come from 3 sources:
    //   1) a component provided by the engine. this is looked up by type
    //   2) a value provided by the user. this is specified in FunctionArguments by implementation class and parameter name
    //   3) a function built by recursively calling this method
    for (Parameter parameter : parameters) {
      // this isn't terribly efficient but unlikely to be a problem. alternatively could use a stack but nasty and mutable
      List<Parameter> newPath = Lists.newArrayList(path);
      newPath.add(parameter);
      Node argNode;
      try {
        if (availableComponents.contains(parameter.getType())) {
          // the parameter can be satisfied by a component supplied by the engine
          argNode = nodeDecorator.decorateNode(new ComponentNode(parameter));
        } else {
          // check if the user has provided a value in the config for this argument
          Object argument = getConstructorArgument(config, implType, parameter);
          if (argument == null) {
            // TODO don't ever return null. if it's eligible for building it's a failure if it doesn't?
            // there's no argument, try to build an instance by recursing
            Node createdNode = createNode(parameter.getType(), config, availableComponents, nodeDecorator, newPath, parameter);
            if (createdNode != null) {
              argNode = createdNode;
            } else if (parameter.isNullable()) {
              argNode = nodeDecorator.decorateNode(new ArgumentNode(parameter.getType(), null, parameter));
            } else {
              throw new NoConstructorArgumentException(newPath, "No value available for non-nullable parameter " +
                                                           parameter.getFullName());
            }
          } else {
            // there's a value in the config for this argument, use it
            argNode = nodeDecorator.decorateNode(new ArgumentNode(parameter.getType(), argument, parameter));
          }
        }
      } catch (AbstractGraphBuildException e) {
        argNode = new ExceptionNode(type, e, parameter);
      }
      constructorArguments.add(argNode);
    }
    Node node;
    if (type.isInterface()) {
      node = new InterfaceNode(type, implType, constructorArguments, parentParameter);
    } else {
      node = new ClassNode(type, implType, constructorArguments, parentParameter);
    }
    return nodeDecorator.decorateNode(node);
  }

  /**
   * @param type A type
   * @return true If the system should try to build an instance
   */
  private static boolean isEligibleForBuilding(Class<?> type) {
    if (INELIGIBLE_TYPES.contains(type)) {
      return false;
    }
    if (type.isPrimitive()) {
      return false;
    }
    Package pkg = type.getPackage();
    String packageName = pkg.getName();
    if (packageName.startsWith("java")) {
      return false;
    }
    if (packageName.startsWith("org.threeten")) {
      return false;
    }
    return true;
  }

  /**
   * Checks the type of the constructor argument matches the expected type. Handles {@link Link}s and {@link Provider}s
   */
  private static Object getConstructorArgument(FunctionModelConfig functionModelConfig,
                                               Class<?> objectType,
                                               Parameter parameter) {
    FunctionArguments args = functionModelConfig.getFunctionArguments(objectType);
    Object arg = args.getArgument(parameter.getName());
    if (arg == null) {
      return null;
      // this takes into account boxing of primitives which Class.isAssignableFrom() doesn't
    } else if (ClassUtils.isAssignable(arg.getClass(), parameter.getType(), true)) {
      return arg;
    } else if (arg instanceof Provider) {
      return arg;
    } else if (arg instanceof Link) {
      if (ClassUtils.isAssignable(((Link) arg).getType(), parameter.getType(), true)) {
        return arg;
      } else {
        throw new IllegalArgumentException("Link argument (" + arg + ") doesn't resolve to the " +
                                               "required type for " + parameter.getFullName());
      }
    } else {
      throw new IllegalArgumentException("Argument (" + arg + ": " + arg.getClass().getSimpleName() + ") isn't of the " +
                                             "required type for " + parameter.getFullName());
    }
  }

  private static StringBuilder prettyPrint(StringBuilder builder,
                                           Node node,
                                           String indent,
                                           String childIndent,
                                           boolean showProxies) {
    Node realNode = getRealNode(node, showProxies);
    // prefix the line with an asterisk if the node is an error node for easier debugging
    String errorPrefix = node.isError() ? "->" : "  ";
    // TODO can this method deal with prepending the property name so Node doesn't need to know the parameter?
    builder.append('\n').append(errorPrefix).append(indent).append(realNode.prettyPrint());
    for (Iterator<Node> itr = realNode.getDependencies().iterator(); itr.hasNext(); ) {
      Node child = itr.next();
      String newIndent;
      String newChildIndent;
      boolean isFinalChild = !itr.hasNext();
      if (!isFinalChild) {
        newIndent = childIndent + " |--";
        newChildIndent = childIndent + " |  ";
      } else {
        newIndent = childIndent + " `--";
        newChildIndent = childIndent + "    ";
      }
      // these are unicode characters for box drawing
      /*if (!isFinalChild) {
        newIndent = childIndent + " \u251c\u2500\u2500";
        newChildIndent = childIndent + " \u2502  ";
      } else {
        newIndent = childIndent + " \u2514\u2500\u2500";
        newChildIndent = childIndent + "    ";
      }*/
      prettyPrint(builder, child, newIndent, newChildIndent, showProxies);
    }
    return builder;
  }

  private static Node getRealNode(Node node, boolean showProxies) {
    if (showProxies) {
      return node;
    }
    if (node instanceof ProxyNode) {
      return getRealNode(((ProxyNode) node).getDelegate(), false);
    } else {
      return node;
    }
  }
}
