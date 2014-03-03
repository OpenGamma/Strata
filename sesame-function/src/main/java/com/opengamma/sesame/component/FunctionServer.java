/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

import com.opengamma.sesame.engine.Results;

/**
 * Represents a running server capable of executing requests.
 */
public interface FunctionServer {

  /**
   * Execute the request and return the results. Executes a single cycle only.
   *
   * @param request the request to be executed, not null
   * @return the results of the execution
   */
  // todo - want execute (n), where the result will have all iterations in, and/or just the last
  // todo - want executeStreaming, where only an id and queue is returned but results are streamed to the queue
  Results executeOnce(FunctionServerRequest request);
}
