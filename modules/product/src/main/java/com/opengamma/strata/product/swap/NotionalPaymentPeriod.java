/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.util.Optional;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.FxIndexObservation;

/**
 * A period over which interest is accrued with a single payment calculated using a notional.
 * <p>
 * This is a single payment period within a swap leg.
 * The amount of the payment is defined by implementations of this interface.
 * It is typically based on a rate of interest.
 * <p>
 * This interface imposes few restrictions on the payment periods.
 * It extends {@link SwapPaymentPeriod} to require that the period is based on a notional amount.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface NotionalPaymentPeriod extends SwapPaymentPeriod {

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

  /**
   * Gets the FX reset observation, optional.
   * <p>
   * This property is used when the defined amount of the notional is specified in
   * a currency other than the currency of the swap leg. When this occurs, the notional
   * amount has to be converted using an FX rate to the swap leg currency.
   * <p>
   * The FX reset definition must be valid. The currency of the period and the currency
   * of the notional must differ, and the currency pair must be that of the observation.
   * 
   * @return the optional FX reset observation
   */
  public abstract Optional<FxIndexObservation> getFxResetObservation();

}
