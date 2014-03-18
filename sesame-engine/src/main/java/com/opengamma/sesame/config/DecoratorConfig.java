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

  /** Underlying config to which we're adding decorators. */
  private final FunctionModelConfig _delegate;

  /** Function implementation classes keyed by the implementation and the parameter where their delegate is injected. */
  private final Map<Parameter, Class<?>> _implByParameter = new HashMap<>();

  /** Function implementation classes keyed by function interface. */
  private final Map<Class<?>, Class<?>> _implByFnInterface = new HashMap<>();

  /** Function arguments keyed by function implementation type. */
  private final Map<Class<?>, FunctionArguments> _functionArguments;

  /**
   * Creates an instance that wraps an underlying {@link FunctionModelConfig} instance and decorates its functions.
   * The decorators parameter has a type of {@link LinkedHashSet} because the order of the decorators is significant
   * where there are multiple decorators applied to the same function. The decorators are called in the same order
   * they appear in the input set.
   *
   * @param delegate the underlying configuration to which decorator functions are added
   * @param decorators types of decorator to add to the underlying configuration
   */
  public DecoratorConfig(FunctionModelConfig delegate, LinkedHashSet<Class<?>> decorators) {
    this(delegate, decorators, Collections.<Class<?>, FunctionArguments>emptyMap());
  }

  /**
   * Creates an instance that wraps an underlying {@link FunctionModelConfig} instance and decorates a single function.
   *
   * @param delegate the underlying configuration to which decorator functions are added
   * @param decorator type of decorator to add to the underlying configuration
   */
  public DecoratorConfig(FunctionModelConfig delegate, Class<?> decorator) {
    this(delegate, linkedSetOf(decorator), Collections.<Class<?>, FunctionArguments>emptyMap());
  }

  /**
   * Creates an instance that wraps an underlying {@link FunctionModelConfig} instance and decorates a single function.
   *
   * @param delegate the underlying configuration to which decorator functions are added
   * @param decorator type of decorator to add to the underlying configuration
   * @param functionArguments arguments for constructing the decorator function
   */
  public DecoratorConfig(FunctionModelConfig delegate, Class<?> decorator, FunctionArguments functionArguments) {
    this(delegate, linkedSetOf(decorator), ImmutableMap.<Class<?>, FunctionArguments>of(decorator, functionArguments));
  }

  /**
   * Creates an instance that wraps an underlying {@link FunctionModelConfig} instance and decorates its functions.
   * The decorators parameter has a type of {@link LinkedHashSet} because the order of the decorators is significant
   * where there are multiple decorators applied to the same function. The decorators are called in the same order
   * they appear in the input set.
   *
   * @param delegate the underlying configuration to which decorator functions are added
   * @param decorators types of decorator to add to the underlying configuration
   * @param functionArguments arguments for constructing the decorator functions
   */
  public DecoratorConfig(FunctionModelConfig delegate,
                         LinkedHashSet<Class<?>> decorators,
                         Map<Class<?>, FunctionArguments> functionArguments) {
    _functionArguments = ImmutableMap.copyOf(ArgumentChecker.notNull(functionArguments, "functionArguments"));
    _delegate = ArgumentChecker.notNull(delegate, "delegate");

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
      Class<?> implementation = getImplementation(interfaceType, delegateParameter);
      if (implementation == null) {
        throw new IllegalArgumentException("No delegate available of type " + interfaceType.getName() + " for " +
                                               "decorator " + decorator.getName());
      }
      _implByParameter.put(delegateParameter, implementation);
      _implByFnInterface.put(interfaceType, decorator);
    }
  }

  private static LinkedHashSet<Class<?>> linkedSetOf(Class<?> decorator) {
    LinkedHashSet<Class<?>> decorators = new LinkedHashSet<>();
    decorators.add(decorator);
    return decorators;
  }

  private Class<?> getImplementation(Class<?> interfaceType, Parameter parameter) {
    Class<?> impl = _implByFnInterface.get(interfaceType);

    if (impl != null) {
      return impl;
    }
    impl = _delegate.getFunctionImplementation(interfaceType, parameter);

    if (impl != null) {
      return impl;
    }
    return _delegate.getFunctionImplementation(interfaceType);
  }

  @Override
  public Class<?> getFunctionImplementation(Class<?> functionType) {
    Class<?> impl = _implByFnInterface.get(functionType);

    return impl == null ?
        _delegate.getFunctionImplementation(functionType) :
        impl;
  }

  public Class<?> getFunctionImplementation(Class<?> functionType, Parameter parameter) {
    Class<?> implByParam = _implByParameter.get(parameter);

    if (implByParam != null) {
      return implByParam;
    }
    return _delegate.getFunctionImplementation(functionType, parameter);
  }

  @Override
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    FunctionArguments args = _functionArguments.get(ArgumentChecker.notNull(functionType, "functionType"));

    return args == null ?
        _delegate.getFunctionArguments(functionType) :
        args;
  }
}
