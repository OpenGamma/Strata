/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;

import java.time.LocalDate;

public class CdsCs01ParallelParFunction extends AbstractCdsFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate) {
    
    return pricer().cs01ParallelPar(product, yieldCurveParRates, creditCurveParRates, valuationDate);
  }

}
