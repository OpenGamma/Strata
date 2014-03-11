/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server.streaming;

import com.opengamma.sesame.server.FunctionServer;
import com.opengamma.sesame.server.FunctionServerRequest;
import com.opengamma.sesame.server.GlobalCycleOptions;

/**
 * Represents a running server capable of executing streaming requests.
 */
public interface StreamingFunctionServer extends FunctionServer {

  /**
   * Creates a new streaming client which will stream the results
   * of the underlying view to a listener. The request contains the
   * details of the view to be run and how to run it.
   *
   * @param request the details of the view to be run and how to run it, not null
   * @return a new streaming client which will stream the results
   * of the underlying view, not null
   */
  StreamingClient createStreamingClient(FunctionServerRequest<GlobalCycleOptions> request);
}
