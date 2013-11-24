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

  /*// TODO can the same implementation work for values and series without the generics going insane?
  @SuppressWarnings("unchecked")
  private static final class MarketDataFunctionSuccessResult extends SuccessFunctionResult<Map<MarketDataRequirement, MarketDataValue<?>>>
      implements MarketDataResult {

    private MarketDataFunctionSuccessResult(SuccessStatus status,
                                            Map<MarketDataRequirement, MarketDataItem<?>> marketDataResults) {
      super(status, marketDataResults);
    }

    // todo this is probably not worth having?
    @Override
    public <T> MarketDataItem<T> getSingleValue() {
      return (MarketDataItem<T>) Iterables.getOnlyElement(getResult().values());
    }

    @Override
    public MarketDataStatus getStatus(MarketDataRequirement requirement) {

      if (getResult().containsKey(requirement)) {
        return getResult().get(requirement).getStatus();
      } else {
        return MarketDataStatus.NOT_REQUESTED;
      }
    }

    @Override
    public <T> MarketDataValue<T> getValue(MarketDataRequirement requirement) {
      if (getResult().containsKey(requirement) &&
          getResult().get(requirement).getStatus() == MarketDataStatus.AVAILABLE) {
        return (MarketDataValue<T>) getResult().get(requirement).getValue();
      } else {
        throw new IllegalStateException("Market data value for requirement: " + requirement + " is not available");
      }
    }

    // todo - temporary implementation only!
    @Override
    public SnapshotDataBundle toSnapshot() {
      SnapshotDataBundle snapshot = new SnapshotDataBundle();
      for (Map.Entry<MarketDataRequirement, MarketDataValue<?>> entry : getResult().entrySet()) {

        MarketDataRequirement key = entry.getKey();
        MarketDataItem<?> item = entry.getValue();
        MarketDataStatus status = item.getStatus();
        MarketDataValue<?> marketDataValue = (MarketDataValue<?>) item.getValue();

        if (key instanceof CurveNodeMarketDataRequirement && status == MarketDataStatus.AVAILABLE) {
          snapshot.setDataPoint(((CurveNodeMarketDataRequirement) key).getExternalId(),
                                (Double) marketDataValue.getValue());
        }

      }
      return snapshot;
    }
  }*/

  // TODO is this even necessary? just use failure function result<marketDataResult<T>>
  /*private static final class MarketDataFunctionFailureResult<T> extends FailureFunctionResult<MarketDataResult<T>> {

    private MarketDataFunctionFailureResult(FailureStatus failureStatus, String message, Object... messageArgs) {
      super(failureStatus, message, messageArgs);
    }

    @Override
    public MarketDataStatus getStatus(MarketDataRequirement requirement) {
      throw new IllegalStateException("Unable to get data from a failure result");
    }

    @Override
    public T getOnlyValue() {
      // TODO implement getOnlyValue()
      throw new UnsupportedOperationException("getOnlyValue not implemented");
    }

    @Override
    public <T> MarketDataValue<T> getValue(MarketDataRequirement requirement) {
      throw new IllegalStateException("Unable to get data from a failure result");
    }

    @Override
    public SnapshotDataBundle toSnapshot() {
      throw new IllegalStateException("Unable to get snapshot from a failure result");
    }
  }*/


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
