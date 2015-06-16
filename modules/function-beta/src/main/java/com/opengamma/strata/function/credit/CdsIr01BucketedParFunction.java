package com.opengamma.strata.function.credit;

import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.finance.credit.ExpandedCdsTrade;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;

public class CdsIr01BucketedParFunction extends AbstractCdsFunction<CurveCurrencyParameterSensitivities> {

  @Override
  protected CurveCurrencyParameterSensitivities execute(ExpandedCds product, DefaultSingleCalculationMarketData provider) {
    return pricer().ir01BucketedPar(product, provider);
  }

}
