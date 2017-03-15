/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloor;

/**
 * Pricer for cap/floor trades in log-normal or Black model.
 */
public class BlackIborCapFloorTradePricer
    extends VolatilityIborCapFloorTradePricer {

  /**
   * Default implementation.
   */
  public static final BlackIborCapFloorTradePricer DEFAULT =
      new BlackIborCapFloorTradePricer(BlackIborCapFloorProductPricer.DEFAULT, DiscountingPaymentPricer.DEFAULT);

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedIborCapFloor}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public BlackIborCapFloorTradePricer(
      BlackIborCapFloorProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {

    super(productPricer, paymentPricer);
  }

}
