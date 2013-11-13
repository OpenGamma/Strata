/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import com.opengamma.util.ArgumentChecker;

/**
 * Cache key containing encapsulating a method invocation including its arguments.
 * This class contains the receiver of the method so the method can be invoked to get the value if it isn't present
 * in the cache. The receiver isn't used in the hashCode() or equals() methods.
 */
/* package */ class MethodInvocationKey {

  private final Class<?> _receiverType;
  private final Method _method;
  private final Object[] _args;
  /** This is deliberately not used in hashCode() and equals(). */
  private final Object _receiver;

  /* package */ MethodInvocationKey(Class<?> receiverType, Method method, Object[] args, Object receiver) {
    _receiver = ArgumentChecker.notNull(receiver, "receiver");
    _receiverType = ArgumentChecker.notNull(receiverType, "receiverType");
    _method = ArgumentChecker.notNull(method, "method");
    _args = args;
  }

  /* package */ Object invoke() throws Exception {
    try {
      return _method.invoke(_receiver, _args);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Error) {
        throw ((Error) cause);
      } else {
        throw ((Exception) cause);
      }
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(_receiverType, _method, Arrays.deepHashCode(_args));
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
        Objects.equals(this._receiverType, other._receiverType) &&
        Objects.equals(this._method, other._method) &&
        Arrays.deepEquals(this._args, other._args);
  }

  @Override
  public String toString() {
    return "MethodInvocationKey [" +
        "_receiverType=" + _receiverType +
        ", _method=" + _method +
        ", _args=" + Arrays.deepToString(_args) +
        ", _receiver=" + _receiver +
        "]";
  }
}
