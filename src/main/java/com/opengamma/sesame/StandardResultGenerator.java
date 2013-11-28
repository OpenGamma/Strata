/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.sesame.marketdata.MarketDataSeriesResultBuilder;
import com.opengamma.sesame.marketdata.MarketDataValuesResultBuilder;

public class StandardResultGenerator {

  public static MarketDataValuesResultBuilder marketDataValuesBuilder() {
    return new MarketDataValuesResultBuilder();
  }

  public static MarketDataSeriesResultBuilder marketDataSeriesBuilder() {
    return new MarketDataSeriesResultBuilder();
  }

  public static <T> FunctionResult<T> propagateFailure(FunctionResult result) {
    // todo remove the cast
    return new FailureFunctionResult<>((FailureStatus) result.getStatus(), result.getFailureMessage());
  }

  public static <T> FunctionResult<T> failure(FailureStatus status, String message, Object... messageArgs) {
    return new FailureFunctionResult<>(status, message, messageArgs);
  }

  public static <T> FunctionResult<T> success(T value) {
    return new SuccessFunctionResult<>(value);
  }

  /*public static <T> FunctionResult<T> success(SuccessStatus status, T value) {
    return new SuccessFunctionResult<>(status, value);
  }*/

}
