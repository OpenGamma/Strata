/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import com.opengamma.util.ArgumentChecker;

/**
 * Cache key containing encapsulating a method invocation including its arguments and receiver.
 */
public class MethodInvocationKey {

  private final Object _receiver;
  private final Method _method;
  private final Object[] _args;

  /* package */ MethodInvocationKey(Object receiver, Method method, Object[] args) {
    _receiver = ArgumentChecker.notNull(receiver, "receiver");
    _method = ArgumentChecker.notNull(method, "method");
    _args = args;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_method, Arrays.deepHashCode(_args), System.identityHashCode(_receiver));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MethodInvocationKey other = (MethodInvocationKey) obj;
    return
        Objects.equals(this._method, other._method) &&
        Arrays.deepEquals(this._args, other._args) &&
        _receiver == other._receiver;
  }

  @Override
  public String toString() {
    return "MethodInvocationKey [" +
        ", _receiver=" + _receiver +
        ", _method=" + _method +
        ", _args=" + Arrays.deepToString(_args) +
        "]";
  }
}
