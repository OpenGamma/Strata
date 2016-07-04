/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloor;

/**
 * Pricer for cap/floor trades in normal or Bachelier model.
 */
public class NormalIborCapFloorTradePricer
    extends VolatilityIborCapFloorTradePricer {

  /**
   * Default implementation.
   */
  public static final NormalIborCapFloorTradePricer DEFAULT =
      new NormalIborCapFloorTradePricer(NormalIborCapFloorProductPricer.DEFAULT, DiscountingPaymentPricer.DEFAULT);

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedIborCapFloor}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public NormalIborCapFloorTradePricer(
      NormalIborCapFloorProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {

    super(productPricer, paymentPricer);
  }

}
