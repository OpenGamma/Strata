/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.interp;

public enum InterpMethod {

  /**
   * Linear interpolation
   */
  LINEAR,

  /**
   * PCHIP with Hyman filter
   */
  LOG_PCHIP_HYMAN

}
