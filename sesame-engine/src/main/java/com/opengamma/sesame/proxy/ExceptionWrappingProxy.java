/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.proxy;

import java.lang.reflect.Method;

import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.util.result.Result;

/**
 * Proxy implementation that wraps each function node and catches
 * any exceptions thrown on method invocations. These are then
 * wrapped in FailureResult objects.
 * <p>
 * Using this class means that exceptions are trapped as close as possible
 * to where they occur - without it stack traces can become long
 * as the stack is extended due to other proxies. Additionally catching
 * the exceptions means that other function code is potentially able to
 * perform useful work.
 */
public class ExceptionWrappingProxy extends ProxyNodeDecorator {

  /**
   * Singleton instance of the proxy.
   */
  public static final ExceptionWrappingProxy INSTANCE = new ExceptionWrappingProxy();

  /**
   * Private constructor.
   */
  private ExceptionWrappingProxy() {
  }

  /**
   * Indicates whether a node should be wrapped in a proxy.
   * <p>
   * We want all nodes where at least one method returns {@link Result} wrapped
   * in a proxy as potentially any of them could throw an exception.
   *
   * @param interfaceType the type of the interface being decorated, not null
   * @param implementationType the implementation type being decorated, not null
   * @return true
   */
  @Override
  protected boolean decorate(Class<?> interfaceType, Class<?> implementationType) {
    for (Method method : interfaceType.getMethods()) {
      if (methodHasResultReturnType(method)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Called when a method on the proxy is invoked.
   *
   * @param proxy the proxy whose method was invoked, not null
   * @param delegate the object being proxied, not null
   * @param method the method that was invoked, not null
   * @param args the method arguments, not null
   * @return the return value of the call
   * @throws Throwable if something goes wrong with the underlying call
   */
  @Override
  protected Object invoke(Object proxy, Object delegate, Method method, Object[] args) throws Throwable {
    try {
      return method.invoke(delegate, args);
    } catch (Exception ex) {
      Exception cause = EngineUtils.getCause(ex);
      if (methodHasResultReturnType(method)) {
        return Result.failure(cause);
      } else {
        throw cause;
      }
    }
  }

  private boolean methodHasResultReturnType(Method method) {
    return Result.class.isAssignableFrom(method.getReturnType());
  }
}
