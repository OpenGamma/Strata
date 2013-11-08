/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trace;

import java.lang.reflect.Method;
import java.util.Deque;
import java.util.LinkedList;

/**
 *
 */
public class FullTracer implements Tracer {

  private final Deque<Call> _stack = new LinkedList<>();

  private Call _root;

  @Override
  public void called(Method method, Object[] args) {
    Call call = new Call(method, args);
    if (_root == null) {
      _root = call;
    } else {
      _stack.peek().called(call);
    }
    _stack.push(call);
  }

  @Override
  public void returned(Object returnValue) {
    _stack.pop().returned(returnValue);
  }

  @Override
  public void threw(Throwable e) {
    _stack.pop().threw(e);
  }

  @Override
  public Call getRoot() {
    return _root;
  }
}
