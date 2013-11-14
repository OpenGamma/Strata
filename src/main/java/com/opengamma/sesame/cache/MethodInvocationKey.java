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
 * Cache key containing encapsulating a method invocation including its arguments.
 * This class contains the receiver of the method so the method can be invoked to get the value if it isn't present
 * in the cache. The receiver isn't used in the hashCode() or equals() methods.
 *
 * TODO this doesn't account for functions of the same type with different constructor args and therefore different behaviour
 * need to include the actual receiver in the cache key if it has state? will need equals() and hashCode()
 * but what if it doesn't have state? don't want to use Object.hashCode() / equals() because we'll be unnecessarily
 * recalculating things that could be cached.
 * could the constructor args be used instead of the instance itself?
 * should I just bite the bullet and share fn instances in FunctionModel? only if annotated with @Cache?
 * put them in GraphModel? so it's mutable? not sure about that
 * what about stateful functions? if I predicate the cache behaviour on sharing is that a problem?
 * or would stateful functions be impossible to cache anyway?
 */
/* package */ class MethodInvocationKey {

  private final Class<?> _receiverType;
  private final Method _method;
  private final Object[] _args;
  // TODO this needs to be used in hashCode and equals using object identity / hashCode
  private final Object _receiver;

  /* package */ MethodInvocationKey(Class<?> receiverType, Method method, Object[] args, Object receiver) {
    _receiver = ArgumentChecker.notNull(receiver, "receiver");
    _receiverType = ArgumentChecker.notNull(receiverType, "receiverType");
    _method = ArgumentChecker.notNull(method, "method");
    _args = args;
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
