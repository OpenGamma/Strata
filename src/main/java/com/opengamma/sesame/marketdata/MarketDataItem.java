/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Objects;

import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class MarketDataItem<T> {

  public static final MarketDataItem<Void> UNAVAILBLE = new MarketDataItem<>(MarketDataStatus.UNAVAILABLE, null);
  public static final MarketDataItem<Void> PENDING = new MarketDataItem<>(MarketDataStatus.PENDING, null);
  public static final MarketDataItem<Void> NOT_REQUESTED = new MarketDataItem<>(MarketDataStatus.NOT_REQUESTED, null);

  private final MarketDataStatus _status;
  private final T _value;

  private MarketDataItem(MarketDataStatus status, T value) {
    _status = ArgumentChecker.notNull(status, "status");
    _value = value;
  }

  public MarketDataStatus getStatus() {
    return _status;
  }

  public T getValue() {
    if (_value == null) {
      throw new IllegalStateException("No value available when status is " + _status);
    }
    return _value;
  }

  public static <U> MarketDataItem<U> available(U value) {
    return new MarketDataItem<>(MarketDataStatus.AVAILABLE, ArgumentChecker.notNull(value, "value"));
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
