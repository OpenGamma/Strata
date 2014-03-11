/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

import java.util.List;

import com.opengamma.sesame.engine.Results;

/**
 * Represents a running server capable of executing requests to
 * run views..
 */
public interface FunctionServer {

  /**
   * Execute the request and return the results. Executes a single cycle only.
   *
   * @param request the request to be executed, not null
   * @return the results of the execution, not null
   */
  Results executeSingleCycle(FunctionServerRequest<IndividualCycleOptions> request);

  /**
   * Execute the request and return the results. Executes multiple
   * cycles (potentially infinite).
   *
   * @param request the request to be executed, not null
   * @return the results of the execution, not null
   */
  List<Results> executeMultipleCycles(FunctionServerRequest<GlobalCycleOptions> request);
}
