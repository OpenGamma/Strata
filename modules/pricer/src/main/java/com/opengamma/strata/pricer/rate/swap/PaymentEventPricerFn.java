/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.pricer.PricingEnvironment;

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
   * <p>
   * The payment date of the event should not be in the past.
   * The result of this method for payment dates in the past is undefined.
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
   * <p>
   * The payment date of the event should not be in the past.
   * The result of this method for payment dates in the past is undefined.
   * 
   * @param env  the pricing environment
   * @param event  the event to price
   * @return the future value of the event
   */
  public abstract double futureValue(PricingEnvironment env, T event);

}
