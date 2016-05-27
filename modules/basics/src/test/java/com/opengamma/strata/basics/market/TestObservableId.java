/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.io.Serializable;
import java.util.Objects;

/**
 * ObservableId implementation used in tests.
 */
public class TestObservableId
    implements ObservableId, ReferenceDataId<Number>, Serializable {

  private static final long serialVersionUID = 1L;

  private final StandardId id;

  private final ObservableSource observableSource;

  public static TestObservableId of(String id) {
    return new TestObservableId(id, ObservableSource.NONE);
  }

  public static TestObservableId of(String id, ObservableSource obsSource) {
    return new TestObservableId(id, obsSource);
  }

  public static TestObservableId of(StandardId id) {
    return new TestObservableId(id, ObservableSource.NONE);
  }

  public static TestObservableId of(StandardId id, ObservableSource obsSource) {
    return new TestObservableId(id, obsSource);
  }

  TestObservableId(String id, ObservableSource obsSource) {
    this(StandardId.of("test", id), obsSource);
  }

  TestObservableId(StandardId id, ObservableSource obsSource) {
    this.observableSource = obsSource;
    this.id = id;
  }

  @Override
  public StandardId getStandardId() {
    return id;
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
  public Class<Number> getReferenceDataType() {
    return Number.class;
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
    TestObservableId that = (TestObservableId) obj;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "TestObservableId [id=" + id + ", observableSource=" + observableSource + "]";
  }
}
