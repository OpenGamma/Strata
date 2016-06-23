/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.io.Serializable;
import java.util.Objects;

import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.NamedMarketDataId;

/**
 * NamedMarketDataId implementation used in tests.
 */
public class TestingNamedId implements NamedMarketDataId<String>, Serializable {

  private static final long serialVersionUID = 1L;

  private final String name;

  public TestingNamedId(String name) {
    this.name = name;
  }

  @Override
  public MarketDataName<String> getMarketDataName() {
    return new TestingName(name);
  }

  @Override
  public Class<String> getMarketDataType() {
    return String.class;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof TestingNamedId) {
      TestingNamedId other = (TestingNamedId) obj;
      return Objects.equals(name, other.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return name;
  }

}
