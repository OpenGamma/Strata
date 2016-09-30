/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

/**
 * The accrual start for credit default swaps.
 * <p>
 * The accrual start is the next day or the previous IMM date.
 */
public enum AccrualStart {

  /**
   * The accrual starts on T+1, i.e., the next day.
   */
  NEXT_DAY,

  /**
   * The accrual starts on the previous IMM date.
   * <p>
   * The IMM date must be computed based on {@link CdsImmDateLogic}.
   */
  IMM_DATE;

}
