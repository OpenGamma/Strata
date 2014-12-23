/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import org.joda.beans.ImmutableBean;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.schedule.Schedule;

/**
 * The accrual calculation part of an interest rate swap leg.
 * <p>
 * An interest rate swap leg is defined by {@link RateCalculationSwapLeg}.
 * The rate to be paid is defined by the implementations of this interface.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface RateCalculation
    extends ImmutableBean {

  /**
   * Expands this calculation to accrual periods based on the specified schedule.
   * <p>
   * The specified accrual schedule defines the period dates to be created.
   * One instance of {@link RateAccrualPeriod} must be created for each period in the schedule.
   * 
   * @param accrualSchedule  the accrual schedule
   * @param paymentSchedule  the payment schedule
   * @return the expanded accrual periods
   * @throws RuntimeException if the swap is invalid
   */
  public abstract ImmutableList<RateAccrualPeriod> expand(Schedule accrualSchedule, Schedule paymentSchedule);

}
