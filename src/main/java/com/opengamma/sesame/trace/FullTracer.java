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
 * Tracer implementation that creates a full call graph.
 */
public class FullTracer implements Tracer {

  /**
   * The stack of calls.
   */
  private final Deque<CallGraph> _stack = new LinkedList<>();
  /**
   * The root of the call graph.
   */
  private CallGraph _root;

  //-------------------------------------------------------------------------
  @Override
  public void called(Method method, Object[] args) {
    CallGraph callGraph = new CallGraph(method, args);
    if (_root == null) {
      _root = callGraph;
    } else {
      _stack.peek().called(callGraph);
    }
    _stack.push(callGraph);
  }

  @Override
  public void returned(Object returnValue) {
    _stack.pop().returned(returnValue);
  }

  @Override
  public void threw(Throwable ex) {
    _stack.pop().threw(ex);
  }

  @Override
  public CallGraph getRoot() {
    return _root;
  }

}
