/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

/**
 * Enumeration of the different methods for creating the stub on the premium leg of a credit default swap.
 * Note that the code does not calculate if the first coupon is long or short.
 * The first coupon is determined based on this switches setting.
 */
public enum CdsStubType {

  /**
   * Stub is at the start (front) of the cashflow schedule; first coupon is on the
   * first IMM date after the effective date (short stub)
   */
  FRONTSHORT,
  /**
   * Stub is at the start (front) of the cashflow schedule; first coupon is on the
   * first but one IMM date after the effective date (long stub)
   */
  FRONTLONG,
  /**
   * Stub is at the end (back) of the cashflow schedule; last but one coupon is on the
   * last scheduled coupon date before the maturity date (short stub)
   */
  BACKSHORT,
  /**
   * Stub is at the end (back) of the cashflow schedule; last but one coupon is on the
   * last but one scheduled coupon date before the maturity date (long stub)
   */
  BACKLONG,
  /**
   * No stub
   */
  NONE;

  // TODO : Double check these definitions
}
