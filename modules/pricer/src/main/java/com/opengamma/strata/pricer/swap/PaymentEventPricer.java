/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap;

import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.swap.DispatchingPaymentEventPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swap.PaymentEvent;

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

  //-------------------------------------------------------------------------
  /**
   * Calculates the forecast value of a single payment event.
   * <p>
   * The amount is expressed in the currency of the event.
   * This returns the value of the event without discounting.
   * <p>
   * The payment date of the event should not be in the past.
   * The result of this method for payment dates in the past is undefined.
   * 
   * @param event  the event to price
   * @param provider  the rates provider
   * @return the forecast value of the event
   */
  public abstract double forecastValue(T event, RatesProvider provider);

  /**
   * Calculates the forecast value sensitivity of a single payment event.
   * <p>
   * The forecast value sensitivity of the event is the sensitivity of the forecast value to
   * the underlying curves.
   * 
   * @param event  the event to price
   * @param provider  the rates provider
   * @return the forecast value curve sensitivity of the event
   */
  public abstract PointSensitivityBuilder forecastValueSensitivity(T event, RatesProvider provider);

  //-------------------------------------------------------------------------
  /**
   * Explains the present value of a single payment event.
   * <p>
   * This adds information to the {@link ExplainMapBuilder} to aid understanding of the calculation.
   * 
   * @param event  the event to price
   * @param provider  the rates provider
   * @param builder  the builder to populate
   */
  public abstract void explainPresentValue(
      T event,
      RatesProvider provider,
      ExplainMapBuilder builder);

}
