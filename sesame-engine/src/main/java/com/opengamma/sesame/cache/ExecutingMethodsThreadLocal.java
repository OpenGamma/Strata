/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.inject.Provider;

import com.opengamma.util.ArgumentChecker;

/**
 * Thread local stack of {@link MethodInvocationKey}s whose initial value is an empty stack.
 * When a cacheable method is executed the corresponding cache key is {@link #push}ed into this object.
 * This makes it available to {@link DefaultCacheInvalidator} to associate with any subscriptions that occur
 * while it's executing.
 */
public class ExecutingMethodsThreadLocal implements Provider<Collection<MethodInvocationKey>> {

  private final ThreadLocal<LinkedList<MethodInvocationKey>> _executingMethods = new ThreadLocal<LinkedList<MethodInvocationKey>>() {
    @Override
    protected LinkedList<MethodInvocationKey> initialValue() {
      return new LinkedList<>();
    }
  };

  @Override
  public Collection<MethodInvocationKey> get() {
    return Collections.unmodifiableList(_executingMethods.get());
  }

  /* package */ void push(MethodInvocationKey key) {
    _executingMethods.get().push(ArgumentChecker.notNull(key, "key"));
  }

  /* package */ void pop() {
    _executingMethods.get().pop();
  }
}
