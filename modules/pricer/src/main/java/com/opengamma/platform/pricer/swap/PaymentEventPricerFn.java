/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.swap;

import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.swap.PaymentEvent;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;

/**
 * Pricer for payment events.
 * <p>
 * This function provides the ability to price a {@link PaymentEvent}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of event
 */
public interface PaymentEventPricerFn<T extends PaymentEvent> {

  /**
   * Calculates the present value of a single payment event.
   * <p>
   * The amount is expressed in the currency of the event.
   * This returns the value of the event with discounting.
   * 
   * @param env  the pricing environment
   * @param event  the event to price
   * @return the present value of the event
   */
  public abstract double presentValue(PricingEnvironment env, T event);

  /**
   * Calculates the future value of a single payment event.
   * <p>
   * The amount is expressed in the currency of the event.
   * This returns the value of the event without discounting.
   * 
   * @param env  the pricing environment
   * @param event  the event to price
   * @return the future value of the event
   */
  public abstract double futureValue(PricingEnvironment env, T event);

  public abstract Pair<Double, MulticurveSensitivity3LD> presentValueCurveSensitivity3LD(
      PricingEnvironment env, T event);
}
