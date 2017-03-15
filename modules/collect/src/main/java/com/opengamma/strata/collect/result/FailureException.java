/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import com.opengamma.strata.collect.ArgChecker;

/**
 * An exception thrown when a failure {@link Result} is encountered and the failure can't be handled.
 */
public class FailureException extends RuntimeException {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /** The details of the failure. */
  private final Failure failure;

  /**
   * Returns an exception wrapping a failure that couldn't be handled.
   *
   * @param failure  a failure that couldn't be handled
   */
  public FailureException(Failure failure) {
    super(failure.getMessage());
    this.failure = ArgChecker.notNull(failure, "failure");
  }

  /**
   * Returns the details of the failure.
   *
   * @return the details of the failure
   */
  public Failure getFailure() {
    return failure;
  }

}
