/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server;

/**
 * Provides a check to a {@link CycleRunner} so that it can
 * determine whether to terminate the sequence of cycles it
 * is responsible for executing. The primary use case will
 * be to allow termination of an infinite series of cycles.
 */
public interface CycleTerminator {

  /**
   * Indicates to the {@link CycleRunner} whether execution
   * of cycles should continue.
   *
   * @return true if execution of cycles should continue
   */
  boolean shouldContinue();
}
