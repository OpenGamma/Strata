/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.io.Serializable;
import java.util.Objects;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableSource;

/**
 * An observable ID implementation used in tests.
 */
public class TestObservableId implements ObservableId, Serializable {

  private static final long serialVersionUID = 1L;

  private final String id;
  private final ObservableSource observableSource;

  public static TestObservableId of(String id) {
    return new TestObservableId(id, ObservableSource.NONE);
  }

  public TestObservableId(String id, ObservableSource obsSource) {
    this.id = id;
    this.observableSource = obsSource;
  }

  @Override
  public StandardId getStandardId() {
    return StandardId.of("test", id);
  }

  @Override
  public FieldName getFieldName() {
    return FieldName.MARKET_VALUE;
  }

  @Override
  public ObservableSource getObservableSource() {
    return observableSource;
  }

  @Override
  public ObservableId withObservableSource(ObservableSource obsSource) {
    return new TestObservableId(id, obsSource);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestObservableId that = (TestObservableId) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(observableSource, that.observableSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, observableSource);
  }
}
