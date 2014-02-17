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
public abstract class Tracer {
//  possible implementations
//  AggregatingTracer - counts invocations to the same method. linked map impl
//  MergingTracer - merges adjacent invocations that are the same (e.g. loops)
//  might need an incremental stateful version if we need to show live updating call graphs in the UI.
//  need to assign a stable ID to nodes so the UI can keep track of node state as the graph is updated
//  possible change to have an 'empty' call graph rather than null

  /**
   * Creates a tracer, which is either active or inactive.
   * <p>
   * An active tracer will trace the call graph.
   * An inactive tracer will do nothing and return no call graph.
   * 
   * @param active  whether tracing should be activated
   * @return the tracer, not null
   */
  public static Tracer create(boolean active) {
    if (active) {
      return new FullTracer();
    } else {
      return NoOpTracer.INSTANCE;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  Tracer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Handles the tracing event when a method is about to be called.
   * <p>
   * This method must not throw an exception.
   * 
   * @param method  the method being called, not null
   * @param args  the method arguments, not null
   */
  abstract void called(Method method, Object[] args);

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
  abstract void returned(Object returnValue);

  /**
   * Handles the tracing event when a method call threw an exception.
   * <p>
   * This method must not throw an exception.
   * 
   * @param ex  the exception that was thrown
   */
  abstract void threw(Throwable ex);

  /**
   * Gets the root of the call graph.
   * <p>
   * This method must not throw an exception.
   * 
   * @return the call graph root, null if not providing a call graph
   */
  public abstract CallGraph getRoot();

}
