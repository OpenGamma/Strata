package com.opengamma.strata.function.calculation.credit;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;

import java.time.LocalDate;

public class CdsPvFunction extends AbstractCdsFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate
  ) {
    return pricer().presentValue(product, yieldCurveParRates, creditCurveParRates, valuationDate);
  }

}
