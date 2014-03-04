/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Objects;

import com.opengamma.util.ArgumentChecker;

/**
 * A piece of market data and its status.
 * @param <T> the type of the data
 * TODO would it make more sense to use Result?
 */
public class MarketDataItem<T> {

  public static final MarketDataItem UNAVAILABLE = new MarketDataItem(MarketDataStatus.UNAVAILABLE, null);
  public static final MarketDataItem PENDING = new MarketDataItem(MarketDataStatus.PENDING, null);

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

  public boolean isAvailable() {
    return _status == MarketDataStatus.AVAILABLE;
  }

  @Override
  public String toString() {
    return "MarketDataItem [_status=" + _status + ", _value=" + _value + "]";
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
}
