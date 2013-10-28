/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.lang.reflect.Constructor;
import java.util.List;

import com.google.common.collect.Lists;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.sesame.function.EngineFunctionUtils;
import com.opengamma.sesame.function.OutputFunction;

/**
 * Wraps a function taking a security argument in one that takes a position or trade argument.
 * It gets the security from the {@link PositionOrTrade} and calls the decorated function.
 * This allows all portfolio output functions in a graph to take {@link PositionOrTrade} as an input so the
 * engine doesn't have to check the target type of every function before invoking it.
 */
/* package */ final class SecurityFunctionDecorator<TResult> implements OutputFunction<PositionOrTrade, TResult> {

  private final OutputFunction<? super Security, TResult> _delegate;

  /**
   * @param delegate A function that takes a security argument
   */
  /* package */ SecurityFunctionDecorator(OutputFunction<? super Security, TResult> delegate) {
    _delegate = delegate;
  }

  @Override
  public TResult execute(PositionOrTrade target) {
    return _delegate.execute(target.getSecurity());
  }

  /**
   * Decorates the root function of the tree with a {@link SecurityFunctionDecorator}.
   * The type signature doesn't constrain the types of tree and function required. It is possible to create a
   * signature that does but the generics are too hair-raising to be worth it.
   * @param functionTree Tree whose root function is a {@link OutputFunction} that takes a security argument
   * @return A Tree whose root function is a {@link OutputFunction} that takes a {@link PositionOrTrade} argument
   * @throws IllegalArgumentException If the tree's root function isn't a {@link OutputFunction} that
   * takes a {@link PositionOrTrade} argument
   */
  @SuppressWarnings("unchecked")
  /* package */ static <T> FunctionTree<T> decorateRoot(FunctionTree<T> functionTree) {
    Function<?> rootFunction = functionTree.getRootFunction();
    if (!OutputFunction.class.isAssignableFrom(rootFunction.getType())) {
      throw new IllegalArgumentException("The tree's root function type " + rootFunction.getType().getName() +
                                             "doesn't implement PortfolioOutputFunction");
    }
    Class<? extends OutputFunction<?, ?>> rootType =
        (Class<? extends OutputFunction<?, ?>>) rootFunction.getType();
    Class<?> targetType = EngineFunctionUtils.getTargetType(rootType);
    if (!Security.class.isAssignableFrom(targetType)) {
      throw new IllegalArgumentException("The tree's root target type " + targetType.getName() + " isn't a " +
                                             "position or trade");
    }
    List<Node> args = Lists.<Node>newArrayList(rootFunction);
    Constructor<?> constructor = SecurityFunctionDecorator.class.getDeclaredConstructors()[0];
    Function<?> decorator = new Function<>(constructor, args);
    return (FunctionTree<T>) new FunctionTree<>(decorator);
  }
}
