package com.opengamma.strata.function.credit;

import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.finance.credit.ExpandedCdsTrade;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;

public class CdsBucketedCs01ParFunction extends AbstractCdsFunction<CurveCurrencyParameterSensitivities> {

  @Override
  protected CurveCurrencyParameterSensitivities execute(ExpandedCdsTrade trade, DefaultSingleCalculationMarketData provider) {
    return pricer().bucketedCr01Par(trade, provider);
  }

}
