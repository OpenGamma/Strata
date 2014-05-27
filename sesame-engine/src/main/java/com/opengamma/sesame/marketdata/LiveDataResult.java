/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.util.result.Result;

/**
 * An immutable collection of market data values for a single ticker, or the reason
 * why the data is not available.
 * <p>
 * All implementations of this interface must be immutable.
 */
public interface LiveDataResult {

  /**
   * Indicates if this instance represents a ticker where we are still waiting
   * for market data to be delivered.
   *
   * @return true if data for this ticker is still pending
   */
  boolean isPending();

  /**
   * Checks whether the current user has permission to see the data held in the result.
   * <p>
   * If the user is authorized, the same instance will be returned unaltered.
   * If not, a new instance will be returned indicating the denied permissions.
   *
   * @return a permission checked version of the values
   */
  LiveDataResult permissionCheck();

  /**
   * Returns the value held for the specified field name.
   * <p>
   * Note that data may be available for a ticker but a particular field may be unknown.
   * In this situation an {@code FailureResult} indicating this will be returned.
   *
   * @param name  the name of the field to get
   * @return a result either holding the data for the field, or indicating why it is not available
   */
  Result<?> getValue(FieldName name);

  /**
   * Returns a new result based on this one updated with data from the specified {@code LiveDataUpdate}.
   * <p>
   * This is used to record updated market data values.
   *
   * @param updatedValues  the new values to update the current values with
   * @return a new result object representing the updated values
   */
  LiveDataResult update(LiveDataUpdate updatedValues);

}
