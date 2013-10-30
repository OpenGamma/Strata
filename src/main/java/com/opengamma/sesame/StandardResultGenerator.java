/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.ResultStatus.AWAITING_MARKET_DATA;
import static com.opengamma.sesame.ResultStatus.SUCCESS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

public class StandardResultGenerator implements MarketDataResultGenerator {

  @Override
  public <T> FunctionResult<T> generateSuccessResult(final T resultValue) {
    return new SuccessFunctionResult<>(ImmutableSet.<MarketDataRequirement>of(), resultValue);
  }

  @Override
  public <T> FunctionResult<T> generateFailureResult(final ResultStatus failureStatus,
                                                     String message,
                                                     Object... messageParams) {
    return new FailureFunctionResult<>(failureStatus, ImmutableSet.<MarketDataRequirement>of(), message, messageParams);
  }

  @Override
  public ResultBuilder createBuilder() {
    return new ResultBuilder();
  }

  @Override
  public MarketDataResultBuilder marketDataResultBuilder() {
    return new MarketDataResultBuilder() {
      private final Set<MarketDataRequirement> _missing = new HashSet<>();
      private ResultStatus _status = AWAITING_MARKET_DATA;

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
        return new StandardMarketDataFunctionResult(_missing.isEmpty() ? SUCCESS : _status, _missing, _requirementStatus);
      }
    };
  }

  private static final class SuccessFunctionResult<T> extends AbstractFunctionResult<T> {

    private final T _result;

    private SuccessFunctionResult(Set<MarketDataRequirement> requiredMarketData, T result) {
      super(SUCCESS, requiredMarketData);
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

  private static final class FailureFunctionResult<T> extends AbstractFunctionResult<T>  {

    private final FormattingTuple _errorMessage;

    private FailureFunctionResult(ResultStatus failureStatus,
                                  Set<MarketDataRequirement> requiredMarketData,
                                  String message,
                                  Object... messageArgs) {
      this(failureStatus, requiredMarketData, MessageFormatter.arrayFormat(message, messageArgs));
    }

    public FailureFunctionResult(ResultStatus failureStatus,
                                 Set<MarketDataRequirement> requiredMarketData,
                                 FormattingTuple errorMessage) {

      super(failureStatus, requiredMarketData);

      ArgumentChecker.isTrue(failureStatus != SUCCESS, "Failure result cannot have a SUCCESS status");
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
      return new FailureFunctionResult<>(getStatus(), getRequiredMarketData(), _errorMessage);
    }
  }

  private static final class StandardMarketDataFunctionResult extends AbstractFunctionResult<Map<MarketDataRequirement, ? extends MarketDataValue>>
      implements MarketDataFunctionResult {

    private final Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> _marketDataResults;

    private StandardMarketDataFunctionResult(ResultStatus status,
                                             Set<MarketDataRequirement> missingMarketData,
                                             Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> marketDataResults) {
      super(status, missingMarketData);
      _marketDataResults = marketDataResults;
    }

    @Override
    public boolean isFullyAvailable() {
      return getRequiredMarketData().isEmpty();
    }

    // todo this is probably not worth having?
    @Override
    public MarketDataValue getSingleMarketDataValue() {
      return Iterables.getOnlyElement(_marketDataResults.values()).getValue();
    }

    @Override
    public MarketDataStatus getMarketDataState(MarketDataRequirement requirement) {

      return _marketDataResults.containsKey(requirement) ?
          _marketDataResults.get(requirement).getKey() :
          MarketDataStatus.NOT_REQUESTED;
    }

    @Override
    public MarketDataValue getMarketDataValue(MarketDataRequirement requirement) {
      if (_marketDataResults.containsKey(requirement) &&
          _marketDataResults.get(requirement).getKey() == MarketDataStatus.AVAILABLE) {
          return _marketDataResults.get(requirement).getValue();
      } else {
        throw new IllegalStateException("Market data value for requirement: " + requirement + " is not available");
      }
    }

    @Override
    public Map<MarketDataRequirement, MarketDataValue> getResult() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <N> FunctionResult<N> generateFailureResult() {
      return new FailureFunctionResult<>(getStatus(), getRequiredMarketData(), "some error");
    }

    @Override
    public String getFailureMessage() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
  }


  private abstract static class AbstractFunctionResult<T> implements FunctionResult<T> {

    private final ResultStatus _status;
    private final Set<MarketDataRequirement> _requiredMarketData;
    private final MarketDataResult _marketDataResult;

    private AbstractFunctionResult(ResultStatus status,
                                   Set<MarketDataRequirement> requiredMarketData) {
      _status = status;
      _requiredMarketData = requiredMarketData;
      _marketDataResult = EmptyMarketDataResult.INSTANCE;
    }

    @Override
    public FunctionResult combine(FunctionResult... functionResults) {

      Set<MarketDataRequirement> marketData = new HashSet<>(_requiredMarketData);
      ResultStatus status = _status;

      for (FunctionResult result : functionResults) {
        marketData.addAll(result.getRequiredMarketData());
        if (status == SUCCESS) {
          status = result.getStatus();
        }
      }

      return status == SUCCESS ?
          new SuccessFunctionResult(marketData, getResult()) : new FailureFunctionResult(status, marketData, "something wrong");
    }

    @Override
    public Set<MarketDataRequirement> getRequiredMarketData() {
      return _requiredMarketData;
    }

    @Override
    public ResultStatus getStatus() {
      return _status;
    }

    @Override
    public <N> FunctionResult<N> generateSuccessResult(N newResult) {
      return new SuccessFunctionResult<>(_requiredMarketData, newResult);
    }

    @Override
    public <N> FunctionResult<N> generateFailureResult(ResultStatus status, String message, Object... args) {
      return new FailureFunctionResult<>(status, _requiredMarketData, message, args);
    }
  }
}
