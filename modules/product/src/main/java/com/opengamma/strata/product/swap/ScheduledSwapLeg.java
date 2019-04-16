/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import com.opengamma.strata.basics.schedule.PeriodicSchedule;

/**
 * A swap leg that defines dates using a schedule.
 * <p>
 * The swap is formed from a number of accrual periods and payment periods, defined by schedules.
 */
public interface ScheduledSwapLeg extends SwapLeg {

  /**
   * Gets the accrual period schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the swap.
   * 
   * @return the accrual schedule
   */
  public abstract PeriodicSchedule getAccrualSchedule();

  /**
   * Gets the payment period schedule.
   * <p>
   * This is used to define the payment periods, including any compounding.
   * The payment period dates are based on the accrual schedule.
   * 
   * @return the payment schedule
   */
  public abstract PaymentSchedule getPaymentSchedule();

}
