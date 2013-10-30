/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.util.Map;

import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;

/**
 * Wraps an {@link Invoker} that expects a {@link Security} input in one that expects a {@link PositionOrTrade}
 * input. When the invoker is called it gets the security from the {@link PositionOrTrade} and uses it when
 * invoking the wrapped invoker.
 */
public class AdaptingFunctionMetadata extends FunctionMetadata {

  public AdaptingFunctionMetadata(FunctionMetadata function) {
    super(function);
  }

  @Override
  public Invoker getInvoker(Object receiver) {
    return new AdaptingInvoker(super.getInvoker(receiver));
  }

  private static class AdaptingInvoker implements Invoker {

    private final Invoker _delegate;

    private AdaptingInvoker(Invoker delegate) {
      _delegate = delegate;
    }

    @Override
    public Object invoke(Object input, Map<String, Object> args) {
      Security security = ((PositionOrTrade) input).getSecurity();
      return _delegate.invoke(security, args);
    }
  }
}
