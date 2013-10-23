/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.security.FinancialSecurity;

public interface MarketDataProvider {

  MarketDataFunctionResult retrieveMarketData(MarketDataContext marketDataContext, FinancialSecurity security, String requiredData);
}
