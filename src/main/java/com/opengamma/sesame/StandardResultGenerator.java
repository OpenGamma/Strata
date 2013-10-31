/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import com.google.common.collect.Iterables;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

public class StandardResultGenerator {

  public static MarketDataResultBuilder marketDataResultBuilder() {

    return new MarketDataResultBuilder() {
      private final Set<MarketDataRequirement> _missing = new HashSet<>();
      private SuccessStatus _status = SuccessStatus.AWAITING_MARKET_DATA;

      private final Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> _requirementStatus = new HashMap<>();

      @Override
      public MarketDataResultBuilder missingData(Set<MarketDataRequirement> missing) {

        for (MarketDataRequirement requirement : missing) {
         missingData(requirement);
        }

        return this;
      }

      @Override
      public MarketDataResultBuilder missingData(MarketDataRequirement requirement) {
        _missing.add(requirement);
        _requirementStatus.put(requirement, Pairs.of(MarketDataStatus.PENDING, (MarketDataValue) null));
        return this;
      }

      @Override
      public MarketDataResultBuilder foundData(MarketDataRequirement requirement,
                                               Pair<MarketDataStatus, ? extends MarketDataValue> state) {
         _requirementStatus.put(requirement, state);
        return this;
      }

      @Override
      public MarketDataResultBuilder foundData(Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> result) {
        _requirementStatus.putAll(result);
        return this;
      }

      @Override
      public MarketDataFunctionResult build() {
        return new MarketDataFunctionSuccessResult(_missing.isEmpty() ? SuccessStatus.SUCCESS : _status, _requirementStatus);
      }
    };
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

  public static <T> FunctionResult<T> success(SuccessStatus status, T value) {
    return new SuccessFunctionResult<>(status, value);
  }

  public static MarketDataFunctionResult marketDataFailure(FailureStatus status, String message, Object... messageArgs) {
    return new MarketDataFunctionSuccessResult.MarketDataFunctionFailureResult(status, message, messageArgs);
  }

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
    public <N> FunctionResult<N> generateFailureResult() {
      throw new IllegalStateException("Can only generate a failure result from an existing failure");
    }
  }

  private static class FailureFunctionResult<T> extends AbstractFunctionResult<T>  {

    private final FormattingTuple _errorMessage;

    private FailureFunctionResult(FailureStatus failureStatus,
                                  String message,
                                  Object... messageArgs) {
      this(failureStatus, MessageFormatter.arrayFormat(message, messageArgs));
    }

    public FailureFunctionResult(FailureStatus failureStatus,
                                 FormattingTuple errorMessage) {

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
    public <N> FunctionResult<N> generateFailureResult() {
      // todo - remove cast
      return new FailureFunctionResult<>((FailureStatus) getStatus(), _errorMessage);
    }
  }

  private static final class MarketDataFunctionSuccessResult extends SuccessFunctionResult<Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>>>
      implements MarketDataFunctionResult {

    private MarketDataFunctionSuccessResult(SuccessStatus status,
                                             Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> marketDataResults) {
      super(status, marketDataResults);
    }

    // todo this is probably not worth having?
    @Override
    public MarketDataValue getSingleMarketDataValue() {
      return Iterables.getOnlyElement(getResult().values()).getValue();
    }

    @Override
    public MarketDataStatus getMarketDataState(MarketDataRequirement requirement) {

      return getResult().containsKey(requirement) ?
          getResult().get(requirement).getKey() :
          MarketDataStatus.NOT_REQUESTED;
    }

    @Override
    public MarketDataValue getMarketDataValue(MarketDataRequirement requirement) {
      if (getResult().containsKey(requirement) &&
          getResult().get(requirement).getKey() == MarketDataStatus.AVAILABLE) {
          return getResult().get(requirement).getValue();
      } else {
        throw new IllegalStateException("Market data value for requirement: " + requirement + " is not available");
      }
    }

    private static final class MarketDataFunctionFailureResult extends FailureFunctionResult<Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>>>
        implements MarketDataFunctionResult {

      private MarketDataFunctionFailureResult(FailureStatus failureStatus, String message, Object... messageArgs) {
        super(failureStatus, message, messageArgs);
      }

      @Override
      public MarketDataValue getSingleMarketDataValue() {
        throw new IllegalStateException("Unable to get data from a failure result");
      }

      @Override
      public MarketDataStatus getMarketDataState(MarketDataRequirement requirement) {
        throw new IllegalStateException("Unable to get data from a failure result");
      }

      @Override
      public MarketDataValue getMarketDataValue(MarketDataRequirement requirement) {
        throw new IllegalStateException("Unable to get data from a failure result");
      }
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
    public <N> FunctionResult<N> generateSuccessResult(SuccessStatus status, N newResult) {
      return new SuccessFunctionResult<>(newResult);
    }

    @Override
    public <N> FunctionResult<N> generateFailureResult(FailureStatus status, String message, Object... args) {
      return new FailureFunctionResult<>(status, message, args);
    }
  }
}
