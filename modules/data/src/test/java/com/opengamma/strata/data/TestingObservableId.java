/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import java.io.Serializable;
import java.util.Objects;

import com.opengamma.strata.basics.StandardId;

/**
 * MarketDataId implementation used in tests.
 */
public class TestingObservableId
    implements ObservableId, Serializable {

  private static final long serialVersionUID = 1L;

  private final String id;

  public TestingObservableId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public Class<Double> getMarketDataType() {
    return Double.class;
  }

  @Override
  public StandardId getStandardId() {
    return StandardId.of("Test", id);
  }

  @Override
  public FieldName getFieldName() {
    return FieldName.MARKET_VALUE;
  }

  @Override
  public ObservableSource getObservableSource() {
    return ObservableSource.NONE;
  }

  @Override
  public ObservableId withObservableSource(ObservableSource obsSource) {
    return this;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    TestingObservableId that = (TestingObservableId) obj;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "TestingMarketDataId [id=" + id + "]";
  }

}
