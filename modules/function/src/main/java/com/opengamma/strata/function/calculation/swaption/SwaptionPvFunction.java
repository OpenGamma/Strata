/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swaption;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.view.SwaptionVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swaption.ExpandedSwaption;
import com.opengamma.strata.product.swaption.SettlementType;

/**
 * Calculates the present value of a {@code SwaptionTrade} for each of a set of scenarios.
 */
public class SwaptionPvFunction extends AbstractSwaptionFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(ExpandedSwaption product, RatesProvider provider, SwaptionVolatilities volatilities) {
    if (product.getSwaptionSettlement().getSettlementType() == SettlementType.CASH) {
      return cashParYieldPricer().presentValue(product, provider, volatilities);
    } else {
      return physicalPricer().presentValue(product, provider, volatilities);
    }
  }

}
