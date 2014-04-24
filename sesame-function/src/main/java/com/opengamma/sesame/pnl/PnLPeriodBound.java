/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.pnl;

/**
 * Refers to a point in a PnL period.
 */
public enum PnLPeriodBound {
  
  /**
   * The start of the PnL period. e.g. yesterday.
   */
  START,
  
  /**
   * The end of the PnL period. e.g. today.
   */
  END;
}
