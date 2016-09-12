/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Pricer for cap/floor products in log-normal or Black model.
 */
public class BlackIborCapFloorProductPricer
    extends VolatilityIborCapFloorProductPricer {

  /**
   * Default implementation.
   */
  public static final BlackIborCapFloorProductPricer DEFAULT =
      new BlackIborCapFloorProductPricer(BlackIborCapFloorLegPricer.DEFAULT, DiscountingSwapLegPricer.DEFAULT);

  /**
   * Creates an instance.
   * 
   * @param capFloorLegPricer  the pricer for {@link IborCapFloorLeg}
   * @param payLegPricer  the pricer for {@link SwapLeg}
   */
  public BlackIborCapFloorProductPricer(
      BlackIborCapFloorLegPricer capFloorLegPricer,
      DiscountingSwapLegPricer payLegPricer) {

    super(capFloorLegPricer, payLegPricer);
  }

}
