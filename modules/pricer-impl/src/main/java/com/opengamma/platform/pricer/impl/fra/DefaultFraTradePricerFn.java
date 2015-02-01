/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.fra;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.fra.Fra;
import com.opengamma.platform.finance.fra.FraProduct;
import com.opengamma.platform.finance.fra.FraTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.TradePricerFn;
import com.opengamma.platform.pricer.fra.FraProductPricerFn;

/**
 * Pricer implementation for forward rate agreement trades.
 * <p>
 * The forward rate agreement trade is priced by examining the embedded forward rate agreement.
 */
public class DefaultFraTradePricerFn
    implements TradePricerFn<FraTrade> {

  /**
   * Default implementation.
   */
  public static final DefaultFraTradePricerFn DEFAULT = new DefaultFraTradePricerFn(
      ExpandingFraProductPricerFn.DEFAULT);

  /**
   * Pricer for {@link FraProduct}.
   */
  private final FraProductPricerFn<? super Fra> fraPricerFn;

  /**
   * Creates an instance.
   * 
   * @param fraPricerFn  the FRA pricer
   */
  public DefaultFraTradePricerFn(
      FraProductPricerFn<? super Fra> fraPricerFn) {
    this.fraPricerFn = ArgChecker.notNull(fraPricerFn, "fraPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, FraTrade trade) {
    return fraPricerFn.presentValue(env, trade.getFra());
  }

}
