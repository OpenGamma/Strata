/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server;

import java.util.List;

import com.opengamma.sesame.engine.Results;

/**
 * Represents a running server capable of executing requests to run views.
 * <p>
 * This is a convenient entry point that wraps up desirable single and multiple cycle behavior.
 * For single cycles, this includes waiting for market data to become available.
 */
public interface FunctionServer {

  /**
   * Execute the request and return the results.
   * <p>
   * This executes a single cycle only.
   *
   * @param request  the request to be executed
   * @return the results of the execution
   */
  Results executeSingleCycle(FunctionServerRequest<IndividualCycleOptions> request);

  /**
   * Execute the request and return the results.
   * <p>
   * This executes multiple cycles, potentially for an infinite amount of time.
   *
   * @param request  the request to be executed
   * @return the results of the execution
   */
  List<Results> executeMultipleCycles(FunctionServerRequest<GlobalCycleOptions> request);

}
