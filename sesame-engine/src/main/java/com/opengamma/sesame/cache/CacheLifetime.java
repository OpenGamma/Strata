/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

/**
 * The length of time a cached value is valid before it needs to be recalculated.
 */
public enum CacheLifetime {

  /**
   * The value is only valid at the instant it's calculated.
   */
  INSTANT,
  /**
   * The value is valid up to midnight of the day it's calculated (using the system time zone).
   */
  DAY,
  /**
   * The value is valid until the next future roll date (3rd Wednesday of the last month of the quarter).
   */
  NEXT_FUTURE_ROLL,
  /**
   * The value is valid forever.
   */
  FOREVER,

}
