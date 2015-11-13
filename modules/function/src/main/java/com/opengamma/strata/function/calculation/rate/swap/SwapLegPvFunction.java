/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.swap;

import java.util.List;
import java.util.stream.Collectors;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.amount.LegAmount;
import com.opengamma.strata.market.amount.LegAmounts;
import com.opengamma.strata.market.amount.SwapLegAmount;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.product.rate.swap.ExpandedSwap;
import com.opengamma.strata.product.rate.swap.ExpandedSwapLeg;

/**
 * Calculates the present value of each leg of an interest rate swap.
 * <p>
 * The resulting {@linkplain LegAmounts leg amounts} contains of a value for each leg,
 * each expressed as a {@linkplain SwapLegAmount swap leg amount} with leg details.
 */
public class SwapLegPvFunction extends AbstractSwapFunction<LegAmounts> {

  @Override
  protected LegAmounts execute(ExpandedSwap product, RatesProvider provider) {
    List<LegAmount> legAmounts = product.getLegs().stream()
        .map(leg -> legAmount(leg, provider))
        .collect(Collectors.toList());
    return LegAmounts.of(legAmounts);
  }

  private SwapLegAmount legAmount(ExpandedSwapLeg leg, RatesProvider provider) {
    CurrencyAmount amount = DiscountingSwapLegPricer.DEFAULT.presentValue(leg, provider);
    return SwapLegAmount.of(leg, amount);
  }

}
