/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

/**
 * Provides access to a {@code FailureItem}.
 * <p>
 * This is intended to be implemented by exception classes
 */
public interface FailureItemProvider {

  /**
   * Gets the failure item.
   *
   * @return the failure item
   */
  public abstract FailureItem getFailureItem();

}
