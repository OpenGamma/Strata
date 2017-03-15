/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableSource;

/**
 * A test market data ID.
 */
public class TestSimpleId implements MarketDataId<String> {

  private final String id;
  private final ObservableSource observableSource;

  public TestSimpleId(String id, ObservableSource obsSource) {
    this.id = id;
    this.observableSource = obsSource;
  }

  @Override
  public Class<String> getMarketDataType() {
    return String.class;
  }

  public ObservableSource getObservableSource() {
    return observableSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestSimpleId that = (TestSimpleId) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(observableSource, that.observableSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, observableSource);
  }
}
