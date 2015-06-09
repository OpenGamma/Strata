package com.opengamma.strata.function.credit;

import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.finance.credit.ExpandedCdsTrade;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;

public class CdsBucketedPv01ParFunction extends AbstractCdsFunction<CurveCurrencyParameterSensitivities> {

  @Override
  protected CurveCurrencyParameterSensitivities execute(ExpandedCdsTrade trade, DefaultSingleCalculationMarketData provider) {
    return pricer().bucketedIr01Par(trade, provider);
  }

}
