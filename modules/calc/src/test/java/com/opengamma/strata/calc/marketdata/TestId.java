/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.Objects;

import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableSource;

/**
 * MarketDataId implementation used in tests.
 */
public class TestId implements MarketDataId<String> {

  private final String id;
  private final ObservableSource observableSource;

  public static TestId of(String id) {
    return new TestId(id);
  }

  public TestId(String id, ObservableSource obsSource) {
    this.id = id;
    this.observableSource = obsSource;
  }

  public TestId(String id) {
    this(id, ObservableSource.NONE);
  }

  @Override
  public Class<String> getMarketDataType() {
    return String.class;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestId testId = (TestId) o;
    return Objects.equals(id, testId.id) &&
        Objects.equals(observableSource, testId.observableSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, observableSource);
  }

  @Override
  public String toString() {
    return "TestId [id='" + id + "', observableSource=" + observableSource + "]";
  }
}
