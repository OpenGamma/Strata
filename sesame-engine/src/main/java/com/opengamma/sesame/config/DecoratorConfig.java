/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * Configuration that adds decorator functions to another {@link FunctionModelConfig} instance.
 * Decorator config instances can be chained.
 * A decorator function is a normal function with the following properties:
 * <ol>
 *   <li>It implements a single interface (this restriction might be lifted in future)</li>
 *   <li>It has a constructor parameter of the same type as the interface it implements</li>
 * </ol>
 * Decorator functions allow behaviour to be added to existing functions, e.g. for implementing scenarios where
 * data from the underlying function is shocked. Decorator functions can be chained.
 */
public class DecoratorConfig implements FunctionModelConfig {

  /** Sentinel value for decorator that should get its delegate type from the next config in the chain. */
  /* package */ static final class UnknownImplementation { }

  /**
   * Function implementation classes keyed by the parameter where they are injected.
   * The parameter is a constructor parameter of a decorator and the class is the type being decorated.
   */
  private final Map<Parameter, Class<?>> _decoratorImplByParameter = new HashMap<>();

  /**
   * Decorator function implementation classes keyed by function interface.
   * The key is an interface type that is being decorated and the value is the decorator type.
   */
  private final Map<Class<?>, Class<?>> _decoratorImplByInterface = new HashMap<>();

  /**
   * Function arguments keyed by function implementation type.
   * Typically these will be the arguments for building the decorator instances, although they can be for any function.
   */
  private final Map<Class<?>, FunctionArguments> _functionArguments;

  /**
   * Creates an instance that wraps an underlying {@link FunctionModelConfig} instance and decorates its functions.
   * The decorators are called in the same order they appear in the arguments.
   *
   * @param decorators types of decorator to add to the underlying configuration
   * @param functionArguments arguments for constructing the decorator functions
   */
  private DecoratorConfig(Map<Class<?>, FunctionArguments> functionArguments, Class<?> decorator, Class<?>... decorators) {
    this(linkedSetOf(decorator, decorators), functionArguments);
  }

  /**
   * Creates an instance that wraps an underlying {@link FunctionModelConfig} instance and decorates its functions.
   * The decorators parameter has a type of {@link LinkedHashSet} because the order of the decorators is significant
   * where there are multiple decorators applied to the same function. The decorators are called in the same order
   * they appear in the input set.
   *
   * @param decorators types of decorator to add to the underlying configuration
   * @param functionArguments arguments for constructing the decorator functions
   * @throws IllegalArgumentException If a decorator is included for a function not in the underlying configuration
   */
  private DecoratorConfig(LinkedHashSet<Class<?>> decorators, Map<Class<?>, FunctionArguments> functionArguments) {
    _functionArguments = ImmutableMap.copyOf(ArgumentChecker.notNull(functionArguments, "functionArguments"));

    List<Class<?>> reversedDecorators = Lists.newArrayList(decorators);
    Collections.reverse(reversedDecorators);

    for (Class<?> decorator : reversedDecorators) {
      Set<Class<?>> interfaces = EngineUtils.getInterfaces(decorator);

      if (interfaces.size() != 1) {
        throw new IllegalArgumentException("Decorator class " + decorator.getName() + " must implement exactly one interface");
      }
      Class<?> interfaceType = interfaces.iterator().next();
      Constructor<?> constructor = EngineUtils.getConstructor(decorator);
      Parameter delegateParameter = Parameter.ofType(interfaceType, constructor);
      Class<?> implementation = getImplementation(interfaceType);
      _decoratorImplByParameter.put(delegateParameter, implementation);
      _decoratorImplByInterface.put(interfaceType, decorator);
    }
  }

  private static LinkedHashSet<Class<?>> linkedSetOf(Class<?> decorator, Class<?>... decorators) {
    LinkedHashSet<Class<?>> decoratorSet = new LinkedHashSet<>();
    decoratorSet.add(decorator);
    Collections.addAll(decoratorSet, decorators);
    return decoratorSet;
  }

  private Class<?> getImplementation(Class<?> interfaceType) {
    Class<?> implType = _decoratorImplByInterface.get(interfaceType);

    if (implType != null) {
      return implType;
    } else {
      return UnknownImplementation.class;
    }
  }

  @Override
  public Class<?> getFunctionImplementation(Class<?> functionType) {
    return _decoratorImplByInterface.get(functionType);
  }

  public Class<?> getFunctionImplementation(Parameter parameter) {
    return _decoratorImplByParameter.get(parameter);
  }

  @Override
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    FunctionArguments args = _functionArguments.get(ArgumentChecker.notNull(functionType, "functionType"));

    if (args != null) {
      return args;
    } else {
      return FunctionArguments.EMPTY;
    }
  }

  /**
   * Creates new configuration derived from config but with its functions decorated with the specified decorators.
   *
   * @param config the configuration to be decorated
   * @param decorator a decorator class
   * @param decorators other decorator classes
   * @return decorated configuration derived from config
   */
  public static FunctionModelConfig decorate(FunctionModelConfig config, Class<?> decorator, Class<?>... decorators) {
    return decorate(config, Collections.<Class<?>, FunctionArguments>emptyMap(), decorator, decorators);
  }


  /**
   * Creates new configuration derived from config but with its functions decorated with the specified decorators.
   *
   * @param config the configuration to be decorated
   * @param functionArguments arguments for constructing the decorator functions
   * @param decorator a decorator class
   * @param decorators other decorator classes
   * @return decorated configuration derived from config
   */
  public static FunctionModelConfig decorate(FunctionModelConfig config,
                                             Map<Class<?>, FunctionArguments> functionArguments,
                                             Class<?> decorator,
                                             Class<?>... decorators) {
    ArgumentChecker.notNull(config, "config");
    ArgumentChecker.notNull(decorator, "decorator");
    DecoratorConfig decoratorConfig = new DecoratorConfig(functionArguments, decorator, decorators);
    return CompositeFunctionModelConfig.compose(decoratorConfig, config);

  }
}
