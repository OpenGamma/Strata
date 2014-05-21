/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

/**
 * Records all the inputs and outputs for a single cycle
 * such they can be used later for regression testing
 * or other purposes.
 */
public interface CycleRecorder {

  /**
   * Indicates that the cycle has completed with the supplied
   * results and that all resources used should cleaned up.
   *
   * @param results the results of the cycle run
   */
  void complete(Results results);
}
