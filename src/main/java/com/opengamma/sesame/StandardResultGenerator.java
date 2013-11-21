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
import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.sesame.marketdata.CurveNodeMarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataResultBuilder;
import com.opengamma.sesame.marketdata.MarketDataSingleResult;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.sesame.marketdata.MarketDataValue;

public class StandardResultGenerator {

  public static MarketDataResultBuilder marketDataResultBuilder() {

    return new MarketDataResultBuilder() {
      private final Set<MarketDataRequirement> _missing = new HashSet<>();
      private SuccessStatus _status = SuccessStatus.AWAITING_MARKET_DATA;

      private final Map<MarketDataRequirement, MarketDataItem<?>> _requirementStatus = new HashMap<>();

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
        _requirementStatus.put(requirement, MarketDataItem.PENDING);
        return this;
      }

      @Override
      public MarketDataResultBuilder foundData(MarketDataRequirement requirement, MarketDataItem<?> item) {
         _requirementStatus.put(requirement, item);
        return this;
      }

      @Override
      public MarketDataResultBuilder foundData(Map<MarketDataRequirement, MarketDataItem<?>> data) {
        _requirementStatus.putAll(data);
        return this;
      }

      @Override
      public MarketDataSingleResult build() {
        if (_missing.isEmpty()) {
          return new MarketDataFunctionSuccessResult(SuccessStatus.SUCCESS, _requirementStatus);
        } else {
          return new MarketDataFunctionSuccessResult(_status, _requirementStatus);
        }
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

  public static MarketDataSingleResult marketDataFailure(FailureStatus status, String message, Object... messageArgs) {
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
    public String toString() {
      return "SuccessFunctionResult{_result=" + _result + '}';
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
    public String toString() {
      return "FailureFunctionResult{_errorMessage=" + _errorMessage + '}';
    }
  }

  @SuppressWarnings("unchecked")
  private static final class MarketDataFunctionSuccessResult extends SuccessFunctionResult<Map<MarketDataRequirement, MarketDataItem<?>>>
      implements MarketDataSingleResult {

    private MarketDataFunctionSuccessResult(SuccessStatus status, Map<MarketDataRequirement, MarketDataItem<?>> marketDataResults) {
      super(status, marketDataResults);
    }

    // todo this is probably not worth having?
    @Override
    public <T> MarketDataValue<T> getSingleValue() {
      return (MarketDataValue<T>) Iterables.getOnlyElement(getResult().values()).getValue();
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
      for (Map.Entry<MarketDataRequirement, MarketDataItem<?>> entry : getResult().entrySet()) {

        MarketDataRequirement key = entry.getKey();
        MarketDataItem<?> pair = entry.getValue();
        MarketDataStatus status = pair.getStatus();

        if (key instanceof CurveNodeMarketDataRequirement && status == MarketDataStatus.AVAILABLE) {
          snapshot.setDataPoint(((CurveNodeMarketDataRequirement) key).getExternalId(), (Double) pair.getValue());
        }

      }
      return snapshot;
    }


    private static final class MarketDataFunctionFailureResult extends FailureFunctionResult<Map<MarketDataRequirement, MarketDataItem<?>>>
        implements MarketDataSingleResult {

      private MarketDataFunctionFailureResult(FailureStatus failureStatus, String message, Object... messageArgs) {
        super(failureStatus, message, messageArgs);
      }

      @Override
      public <T> MarketDataValue<T> getSingleValue() {
        throw new IllegalStateException("Unable to get data from a failure result");
      }

      @Override
      public MarketDataStatus getStatus(MarketDataRequirement requirement) {
        throw new IllegalStateException("Unable to get data from a failure result");
      }

      @Override
      public <T> MarketDataValue<T> getValue(MarketDataRequirement requirement) {
        throw new IllegalStateException("Unable to get data from a failure result");
      }

      @Override
      public SnapshotDataBundle toSnapshot() {
        throw new IllegalStateException("Unable to get snapshot from a failure result");
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
    public boolean isResultAvailable() {
      return _status.isResultAvailable();
    }
  }
}
