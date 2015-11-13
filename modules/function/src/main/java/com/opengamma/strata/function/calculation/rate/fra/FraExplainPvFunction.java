/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.fra;

import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.fra.ExpandedFra;

/**
 * Obtains the explain map for present value on a {@code FraTrade}.
 */
public class FraExplainPvFunction
    extends AbstractFraFunction<ExplainMap> {

  @Override
  protected ExplainMap execute(ExpandedFra product, RatesProvider provider) {
    return pricer().explainPresentValue(product, provider);
  }

}
