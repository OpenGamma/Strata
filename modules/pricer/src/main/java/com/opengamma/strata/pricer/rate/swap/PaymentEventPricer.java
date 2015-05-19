/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.rate.swap.DispatchingPaymentEventPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for payment events.
 * <p>
 * This function provides the ability to price a {@link PaymentEvent}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of event
 */
public interface PaymentEventPricer<T extends PaymentEvent> {

  /**
   * Returns a default instance of the function.
   * <p>
   * Use this method to avoid a direct dependency on the implementation.
   * 
   * @return the payment event pricer
   */
  public static PaymentEventPricer<PaymentEvent> instance() {
    return DispatchingPaymentEventPricer.DEFAULT;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of a single payment event.
   * <p>
   * The amount is expressed in the currency of the event.
   * This returns the value of the event with discounting.
   * <p>
   * The payment date of the event should not be in the past.
   * The result of this method for payment dates in the past is undefined.
   * 
   * @param event  the event to price
   * @param provider  the rates provider
   * @return the present value of the event
   */
  public abstract double presentValue(T event, RatesProvider provider);

  /**
   * Calculates the present value sensitivity of a single payment event.
   * <p>
   * The present value sensitivity of the event is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param event  the event to price
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the event
   */
  public abstract PointSensitivityBuilder presentValueSensitivity(T event, RatesProvider provider);

  /**
   * Calculates the future value of a single payment event.
   * <p>
   * The amount is expressed in the currency of the event.
   * This returns the value of the event without discounting.
   * <p>
   * The payment date of the event should not be in the past.
   * The result of this method for payment dates in the past is undefined.
   * 
   * @param event  the event to price
   * @param provider  the rates provider
   * @return the future value of the event
   */
  public abstract double futureValue(T event, RatesProvider provider);

  /**
   * Calculates the future value sensitivity of a single payment event.
   * <p>
   * The future value sensitivity of the event is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param event  the event to price
   * @param provider  the rates provider
   * @return the future value curve sensitivity of the event
   */
  public abstract PointSensitivityBuilder futureValueSensitivity(T event, RatesProvider provider);

}
