/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

/**
 * An immutable version of LiveDataResults.
 * <p>
 * Marker interface to ensure that a MutableLiveDataResults
 * cannot be returned where an immutable one is wanted.
 */
public interface ImmutableLiveDataResults extends LiveDataResults {
}
