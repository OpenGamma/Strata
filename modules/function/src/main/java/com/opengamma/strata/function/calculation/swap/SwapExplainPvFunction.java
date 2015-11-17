/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swap.ExpandedSwap;

/**
 * Obtains the explain map for present value on a {@code SwapTrade}.
 */
public class SwapExplainPvFunction
    extends AbstractSwapFunction<ExplainMap> {

  @Override
  protected ExplainMap execute(ExpandedSwap product, RatesProvider provider) {
    return pricer().explainPresentValue(product, provider);
  }

}
