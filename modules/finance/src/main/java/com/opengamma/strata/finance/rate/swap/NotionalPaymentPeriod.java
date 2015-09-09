/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * A period over which interest is accrued with a single payment calculated using a notional.
 * <p>
 * This is a single payment period within a swap leg.
 * The amount of the payment is defined by implementations of this interface.
 * It is typically based on a rate of interest.
 * <p>
 * This interface imposes few restrictions on the payment periods.
 * It extends {@link PaymentPeriod} to require that the period is based on a notional amount.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface NotionalPaymentPeriod extends PaymentPeriod {

  /**
   * The notional amount, positive if receiving, negative if paying.
   * <p>
   * This is the notional amount applicable during the period.
   * The currency may differ from that returned by {@link #getCurrency()},
   * for example if the swap contains an FX reset.
   * 
   * @return the notional amount of the period
   */
  public abstract CurrencyAmount getNotionalAmount();

}
