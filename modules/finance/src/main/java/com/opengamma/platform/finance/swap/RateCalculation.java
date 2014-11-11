/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import org.joda.beans.ImmutableBean;

import com.opengamma.basics.PayReceive;

/**
 * Part of a swap leg. TODO
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface RateCalculation
    extends ImmutableBean {

  /**
   * Gets the pay or receive flag.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   * 
   * @return the pay/receive flag
   */
  public abstract PayReceive getPayReceive();
  /**
   * Gets the notional amount, always positive.
   * <p>
   * The notional amount of the swap leg, which can vary during the lifetime of the swap.
   * In most cases, the notional amount is not exchanged, with only the net difference being exchanged.
   * However, in certain cases, initial, final or intermediate amounts are exchanged.
   * <p>
   * The notional expressed here is always positive, see {@code payReceive}.
   * 
   * @return the notional
   */
  public abstract NotionalAmount getNotional();

}
