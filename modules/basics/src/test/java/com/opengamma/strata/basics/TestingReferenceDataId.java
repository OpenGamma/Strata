/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import java.io.Serializable;
import java.util.Objects;

/**
 * ReferenceDataId implementation used in tests.
 */
class TestingReferenceDataId
    implements ReferenceDataId<Number>, Serializable {

  private static final long serialVersionUID = 1L;

  private final String id;

  public TestingReferenceDataId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
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
    TestingReferenceDataId that = (TestingReferenceDataId) obj;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "TestingReferenceDataId [id=" + id + "]";
  }

}
