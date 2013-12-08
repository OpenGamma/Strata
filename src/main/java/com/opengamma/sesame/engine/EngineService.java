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

  // TODO add TIMING to this once it can be selectively enabled for a specific target (SSM-66)
  public static final EnumSet<EngineService> DEFAULT_SERVICES = EnumSet.of(CACHING, TRACING);
}
