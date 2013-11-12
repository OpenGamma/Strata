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
 *
 */
/* package */ class CacheKey {

  private final Class<?> _receiverType;
  private final Method _method;
  private final Object[] _args;
  /** This is deliberately not used in hashCode() and equals(). */
  private final Object _receiver;

  // TODO I really don't like having to include the receiver. is there a better way?
  /* package */ CacheKey(Class<?> receiverType, Method method, Object[] args, Object receiver) {
    _receiver = ArgumentChecker.notNull(receiver, "receiver");
    _receiverType = ArgumentChecker.notNull(receiverType, "receiverType");
    _method = ArgumentChecker.notNull(method, "method");
    _args = args;
  }

  /* package */ Method getMethod() {
    return _method;
  }

  /* package */ Object[] getArgs() {
    return _args;
  }

  /* package */ Object getReceiver() {
    return _receiver;
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
    final CacheKey other = (CacheKey) obj;
    return
        Objects.equals(this._receiverType, other._receiverType) &&
        Objects.equals(this._method, other._method) &&
        Arrays.deepEquals(this._args, other._args);
  }

  @Override
  public String toString() {
    return "CacheKey [" +
        "_receiverType=" + _receiverType +
        ", _method=" + _method +
        ", _args=" + Arrays.deepToString(_args) +
        "]";
  }
}
