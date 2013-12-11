/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.EnumSet;

/**
 * TODO is this a sensible name?
 * TODO should this be extensible rather than an enum so users can add their own? probably
 * or should users just be able to pass in decorators that the engine can compose with its own?
 */
public enum EngineService {

  CACHING,

  TRACING,

  TIMING;

  /**
   * Default services provided by the engine - caching and tracing of function calls.
   */
  public static final EnumSet<EngineService> DEFAULT_SERVICES = EnumSet.of(CACHING, TRACING);

  /**
   * Tells the engine to build the functions with no services.
   * Useful for debugging as there are no proxies between functions and every method call is direct.
   */
  public static final EnumSet<EngineService> NONE = EnumSet.noneOf(EngineService.class);
}
