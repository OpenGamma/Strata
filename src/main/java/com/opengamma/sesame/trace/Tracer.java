/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trace;

import java.lang.reflect.Method;

/**
 * Provides the ability to handle tracing of function calls.
 * <p>
 * When processing functions, tracing can be added to monitor what is occurring.
 */
public interface Tracer {
//  possible implementations
//  AggregatingTracer - counts invocations to the same method. linked map impl
//  MergingTracer - merges adjacent invocations that are the same (e.g. loops)
//  might need an incremental stateful version if we need to show live updating call graphs in the UI.
//  need to assign a stable ID to nodes so the UI can keep track of node state as the graph is updated

  /**
   * Handles the tracing event when a method is about to be called.
   * <p>
   * This method must not throw an exception.
   * 
   * @param method  the method being called, not null
   * @param args  the method arguments, not null
   */
  void called(Method method, Object[] args);

  /**
   * Handles the tracing event when a method call completed.
   * <p>
   * This callback is invoked when no exception is thrown.
   * The return value may however represent a known failure case.
   * <p>
   * This method must not throw an exception.
   * 
   * @param returnValue  the return value of the method, may be null
   */
  void returned(Object returnValue);

  /**
   * Handles the tracing event when a method call threw an exception.
   * <p>
   * This method must not throw an exception.
   * 
   * @param ex  the exception that was thrown
   */
  void threw(Throwable ex);

  /**
   * Gets the root of the call graph.
   * <p>
   * This method must not throw an exception.
   * 
   * @return the call graph root, null if not providing a call graph
   */
  CallGraph getRoot();

}
