/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.opengamma.financial.security.equity.EquitySecurity;

/**
 * Details that every function needs available which must be passed
 */
public interface FunctionContext {

  MarketDataFunctionResult retrieveMarketData(Set<MarketDataRequirement> requiredMarketData);

  MarketDataRequirement generateMarketDataRequirement(EquitySecurity security,
                                                      String requirementType);

  <T> FunctionResult<T> generateFailureResult(FunctionResult<?> functionResult);

  <T> FunctionResult<T> generateSuccessResult(T resultValue);
}
