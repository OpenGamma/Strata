/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalId;

/**
 * @deprecated use {@link MarketDataFn2}
 */
@Deprecated
public class CurveNodeMarketDataRequirement implements MarketDataRequirement {

  private final ExternalId _identifier;
  private final String _dataField;

  public CurveNodeMarketDataRequirement(ExternalId identifier, String dataField) {
    _identifier = identifier;
    _dataField = dataField;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CurveNodeMarketDataRequirement that = (CurveNodeMarketDataRequirement) o;
    return _dataField.equals(that._dataField) && _identifier.equals(that._identifier);
  }

  @Override
  public int hashCode() {
    return 31 * _identifier.hashCode() + _dataField.hashCode();
  }

  public ExternalId getExternalId() {
    return _identifier;
  }

  public String getDataField() {
    return _dataField;
  }

  public ExternalId getId() {
    return _identifier;
  }

  @Override
  public String toString() {
    return "CurveNodeMarketDataRequirement [_identifier=" + _identifier + ", _dataField='" + _dataField + "']";
  }
}
