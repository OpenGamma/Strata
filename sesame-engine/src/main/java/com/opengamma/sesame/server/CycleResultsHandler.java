/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server;

import com.opengamma.sesame.engine.Results;

/**
 * Responsible for handling the results produced by a {@link CycleRunner}.
 * This may range from simply collecting results as they are received,
 * to streaming them out to remote listeners.
 */
public interface CycleResultsHandler {

  /**
   * Handle the latest set of Results produced by the engine.
   *
   * @param results the results to process
   */
  void handleResults(Results results);
}
