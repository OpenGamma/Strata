package com.opengamma.platform.pricer.impl.future;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.future.ExpandedIborFuture;
import com.opengamma.platform.finance.future.ExpandedIborFutureOption;
import com.opengamma.platform.finance.future.IborFutureOption;
import com.opengamma.platform.finance.future.IborFutureOptionSecurityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.future.IborFutureOptionProductPricerFn;

/**
 * Pricer implementation for ibor future option products.
 * <p>
 * The ibor future option product is priced by expanding it.
 */
public class ExpandingIborFutureOptionProductPricerFn
    implements IborFutureOptionProductPricerFn<IborFutureOption> {

  /**
   * Pricer for {@link ExpandedIborFuture}.
   */
  private final IborFutureOptionProductPricerFn<ExpandedIborFutureOption> expandedIborFutureOptionPricerFn;

  /**
   * Creates an instance.
   * 
   * @param expandedIborFutureOptionPricerFn  the pricer for {@link ExpandedIborFutureOption}
   */
  public ExpandingIborFutureOptionProductPricerFn(
      IborFutureOptionProductPricerFn<ExpandedIborFutureOption> expandedIborFutureOptionPricerFn) {
    this.expandedIborFutureOptionPricerFn = ArgChecker.notNull(expandedIborFutureOptionPricerFn,
        "expandedIborFutureOptionPricerFn");
  }

  @Override
  public double price(PricingEnvironment env, IborFutureOption iborFutureOptionProduct,
      Object surface) {
    return expandedIborFutureOptionPricerFn.price(env, iborFutureOptionProduct.expand(), surface);
  }

  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, IborFutureOption iborFutureOptionProduct,
      IborFutureOptionSecurityTrade trade, double lastClosingPrice, Object surface) {
    return expandedIborFutureOptionPricerFn.presentValue(env, iborFutureOptionProduct.expand(), trade,
        lastClosingPrice, surface);
  }
}
