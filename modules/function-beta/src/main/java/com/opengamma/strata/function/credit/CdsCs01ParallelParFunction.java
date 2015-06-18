package com.opengamma.strata.function.credit;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;

public class CdsCs01ParallelParFunction extends AbstractCdsFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(ExpandedCds product, IsdaYieldCurveParRates parRates, DefaultSingleCalculationMarketData provider){
    return pricer().cs01ParallelPar(product, parRates, provider);
  }

}
