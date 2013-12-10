/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.opengamma.sesame.marketdata.MarketDataSeriesResultBuilder;
import com.opengamma.sesame.marketdata.MarketDataValuesResultBuilder;

public class FunctionResultGenerator {

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

  public static boolean anyFailures(FunctionResult<?>... results) {
    for (FunctionResult<?> result : results) {
      if (result.getStatus() != SuccessStatus.SUCCESS) {
        return true;
      }
    }
    return false;
  }

  // results can include successes which are ignored
  public static <T> FunctionResult<T> propagateFailures(FunctionResult<?> result1,
                                                        FunctionResult<?> result2,
                                                        FunctionResult<?>... results) {
    List<FunctionResult<?>> resultList = Lists.newArrayListWithCapacity(results.length + 2);
    resultList.add(result1);
    resultList.add(result2);
    resultList.addAll(Arrays.asList(results));
    List<FunctionResult<?>> failures = Lists.newArrayList();
    for (FunctionResult<?> result : resultList) {
      if (result instanceof FailureFunctionResult) {
        failures.add((FailureFunctionResult) result);
      }
    }
    if (failures.isEmpty()) {
      throw new IllegalArgumentException("No failures found in " + failures);
    }
    return new MultipleFailureFunctionResult<>(failures);
  }
}
