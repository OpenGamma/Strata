/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

/**
 * An immutable representation of the results of live market data.
 * <p>
 * This is a marker interface used to ensure that a {@code MutableLiveDataResults}
 * cannot be returned where an immutable one is wanted.
 * <p>
 * All implementations of this interface must be immutable.
 */
public interface ImmutableLiveDataResults extends LiveDataResults {

}
