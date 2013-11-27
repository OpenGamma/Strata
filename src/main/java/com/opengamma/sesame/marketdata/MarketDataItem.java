/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Objects;

import com.opengamma.util.ArgumentChecker;

/**
 * TODO should this have a type param or not?
 * TODO maybe there should be 2 similar classes for single values and time series
 */
public class MarketDataItem {

  public static final MarketDataItem UNAVAILBLE = new MarketDataItem(MarketDataStatus.UNAVAILABLE, null);
  public static final MarketDataItem PENDING = new MarketDataItem(MarketDataStatus.PENDING, null);
  public static final MarketDataItem NOT_REQUESTED = new MarketDataItem(MarketDataStatus.NOT_REQUESTED, null);

  private final MarketDataStatus _status;
  private final Object _value;

  private MarketDataItem(MarketDataStatus status, Object value) {
    _status = ArgumentChecker.notNull(status, "status");
    _value = value;
  }

  public MarketDataStatus getStatus() {
    return _status;
  }

  public Object getValue() {
    if (_value == null) {
      throw new IllegalStateException("No value available when status is " + _status);
    }
    return _value;
  }

  public static MarketDataItem available(Object value) {
    return new MarketDataItem(MarketDataStatus.AVAILABLE, ArgumentChecker.notNull(value, "value"));
  }

  public static MarketDataItem missing(MarketDataStatus status) {
    if (status == MarketDataStatus.AVAILABLE) {
      throw new IllegalArgumentException("Missing data can't have status AVAILABLE");
    }
    return new MarketDataItem(status, null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_status, _value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MarketDataItem other = (MarketDataItem) obj;
    return Objects.equals(this._status, other._status) && Objects.equals(this._value, other._value);
  }

  public boolean isAvailable() {
    return _status == MarketDataStatus.AVAILABLE;
  }

  @Override
  public String toString() {
    return "MarketDataItem [" + _status + ", " + _value + "]";
  }
}
