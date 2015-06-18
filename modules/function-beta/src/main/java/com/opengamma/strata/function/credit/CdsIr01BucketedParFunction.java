package com.opengamma.strata.function.credit;

import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;

public class CdsIr01BucketedParFunction extends AbstractCdsFunction<CurveCurrencyParameterSensitivities> {

  @Override
  protected CurveCurrencyParameterSensitivities execute(ExpandedCds product, IsdaYieldCurveParRates parRates, DefaultSingleCalculationMarketData provider){
    return pricer().ir01BucketedPar(product, parRates, provider);
  }

}
