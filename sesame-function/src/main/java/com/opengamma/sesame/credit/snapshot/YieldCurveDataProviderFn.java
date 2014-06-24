/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.snapshot;

import com.opengamma.financial.analytics.isda.credit.YieldCurveData;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;

/**
 * A provider function which, given a currency, will return a valid {@link YieldCurveData} instance.
 */
public interface YieldCurveDataProviderFn {

  /**
   * Loads the {@link YieldCurveData} instance associated with the given currency.
   * @param key a currency
   * @return a valid {@link YieldCurveData} object or failure
   */
  Result<YieldCurveData> loadYieldCurveData(Currency key);

}
