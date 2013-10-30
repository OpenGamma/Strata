/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

/**
 * Details that every function needs available which must be passed
 */
public interface ResultGenerator {

  <T> FunctionResult<T> generateSuccessResult(T resultValue);

  <T> FunctionResult<T> generateFailureResult(ResultStatus status, String message, Object... messageParams);

  ResultBuilder createBuilder();

  MarketDataResultBuilder marketDataResultBuilder();
}
