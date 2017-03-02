/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.ZonedDateTime;

import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.Swap;

/**
 * Pricer for swaption with physical settlement in a log-normal or Black model on the swap rate.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed.
 * The underlying swap must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * <p>
 * The value of the swaption after expiry is 0.
 * For a swaption which has already expired, a negative number is returned by
 * {@link SwaptionVolatilities#relativeTime(ZonedDateTime)}.
 */
public class BlackSwaptionPhysicalProductPricer
    extends VolatilitySwaptionPhysicalProductPricer {

  /**
   * Default implementation.
   */
  public static final BlackSwaptionPhysicalProductPricer DEFAULT =
      new BlackSwaptionPhysicalProductPricer(DiscountingSwapProductPricer.DEFAULT);

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public BlackSwaptionPhysicalProductPricer(DiscountingSwapProductPricer swapPricer) {
    super(swapPricer);
  }

}
