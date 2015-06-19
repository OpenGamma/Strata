package com.opengamma.strata.function.calculation.credit;

import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;

import java.time.LocalDate;

public class CdsCs01BucketedParFunction extends AbstractCdsFunction<CurveCurrencyParameterSensitivities> {

  @Override
  protected CurveCurrencyParameterSensitivities execute(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate
  ) {
    return pricer().cs01BucketedPar(product, yieldCurveParRates, creditCurveParRates, valuationDate);
  }

}
