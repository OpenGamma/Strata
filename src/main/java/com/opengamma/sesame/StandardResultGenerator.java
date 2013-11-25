/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import com.opengamma.sesame.marketdata.MarketDataSeriesResultBuilder;
import com.opengamma.sesame.marketdata.MarketDataValuesResultBuilder;

public class StandardResultGenerator {

  public static MarketDataValuesResultBuilder marketDataBuilder() {
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

  private static class SuccessFunctionResult<T> extends AbstractFunctionResult<T> {

    private final T _result;

    private SuccessFunctionResult(T result) {
      this(SuccessStatus.SUCCESS, result);
    }

    public SuccessFunctionResult(SuccessStatus status, T result) {
      super(status);
      this._result = result;
    }

    @Override
    public T getResult() {
      return _result;
    }

    @Override
    public String getFailureMessage() {
      throw new IllegalStateException("Unable to get an error message from a success result");
    }

    @Override
    public String toString() {
      return "SuccessFunctionResult{_result=" + _result + '}';
    }
  }

  private static class FailureFunctionResult<T> extends AbstractFunctionResult<T> {

    private final FormattingTuple _errorMessage;

    private FailureFunctionResult(FailureStatus failureStatus, String message, Object... messageArgs) {
      this(failureStatus, MessageFormatter.arrayFormat(message, messageArgs));
    }

    public FailureFunctionResult(FailureStatus failureStatus, FormattingTuple errorMessage) {

      super(failureStatus);
      _errorMessage = errorMessage;
    }

    @Override
    public T getResult() {
      throw new IllegalStateException("Unable to get an error message from a success result");
    }

    @Override
    public String getFailureMessage() {
      return _errorMessage.getMessage();
    }

    @Override
    public String toString() {
      return "FailureFunctionResult{_errorMessage=" + _errorMessage.getMessage() + '}';
    }
  }

  private abstract static class AbstractFunctionResult<T> implements FunctionResult<T> {

    private final ResultStatus _status;

    private AbstractFunctionResult(ResultStatus status) {
      _status = status;
    }

    @Override
    public ResultStatus getStatus() {
      return _status;
    }

    @Override
    public boolean isResultAvailable() {
      return _status.isResultAvailable();
    }
  }
}
