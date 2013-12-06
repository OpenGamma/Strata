/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class CompositeFailureFunctionResult<T> implements FunctionResult<T> {

  private final List<FunctionResult<?>> _failures;
  private final String _message;
  private final FailureStatus _status;

  /* package */ CompositeFailureFunctionResult(List<FunctionResult<?>> failures) {
    _failures = ImmutableList.copyOf(ArgumentChecker.notEmpty(failures, "failures"));
    List<String> messages = Lists.newArrayListWithCapacity(failures.size());
    FailureStatus compositeStatus = null;
    for (FunctionResult<?> failure : failures) {
      FailureStatus status = (FailureStatus) failure.getStatus();
      if (compositeStatus == null) {
        compositeStatus = status;
      } else if (compositeStatus != status) {
        compositeStatus = FailureStatus.MULTIPLE;
      }
      messages.add(failure.getFailureMessage());
    }
    _status = compositeStatus;
    _message = StringUtils.join(messages, "\n");
  }

  @Override
  public FailureStatus getStatus() {
    return _status;
  }

  @Override
  public T getResult() {
    throw new IllegalStateException("Unable to get a value from a failure result");
  }

  @Override
  public String getFailureMessage() {
    return _message;
  }

  @Override
  public boolean isResultAvailable() {
    return false;
  }

  public List<FunctionResult<?>> getFailures() {
    return _failures;
  }
}
