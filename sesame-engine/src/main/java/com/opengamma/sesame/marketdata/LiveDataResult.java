/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.util.result.Result;

/**
 * Represents either a collection of market data values for a single
 * ticker or the reason why the data is not available.
 */
public interface LiveDataResult {

  /**
   * Indicates if this instance represents a ticker where we are
   * still waiting for market data to be delivered.
   *
   * @return true if data for this ticker is still pending
   */
  boolean isPending();

  /**
   * Permission check this instance to ensure that the current user
   * is authorized to see the data. If the user is authorized, the
   * same instance will be returned unaltered. If not, a new instance
   * will be returned indicating the denied permissions.
   *
   * @return a permission checked version of the values
   */
  LiveDataResult permissionCheck();

  /**
   * Return the value held for the specified field name. Note that
   * data may be available for a ticker but a particular field
   * may be unknown. In this situation an FailureResult indicating
   * this will be returned.
   *
   * @param name  the name of the field to get, not null
   * @return a result either holding the data for the field, or
   * indicating why it is not available
   */
  Result<?> getValue(FieldName name);

  /**
   * Create a new LiveDataResult object, updating any fields
   * with values from the supplied Fudge message.
   *
   * @param updatedValues  the new values to update the current values
   * with, not null
   * @return a new LiveDataResult object representing the updated
   * values, not null
   */
  LiveDataResult update(LiveDataUpdate updatedValues);

}
