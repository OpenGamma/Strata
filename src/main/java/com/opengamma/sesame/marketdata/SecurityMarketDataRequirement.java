/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.util.ArgumentChecker;

/**
 * A market data requirement for a security / data field combination.
 */
public class SecurityMarketDataRequirement implements MarketDataRequirement {

  /**
   * THe security to get the market data for.
   */
  private final FinancialSecurity _security;

  /**
   * The market data field being requested.
   */
  private final String _dataField;

  /* package */ SecurityMarketDataRequirement(FinancialSecurity security, String dataField) {
    _security = ArgumentChecker.notNull(security, "security");
    _dataField = ArgumentChecker.notNull(dataField, "dataField");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SecurityMarketDataRequirement that = (SecurityMarketDataRequirement) o;
    return _dataField.equals(that._dataField) && _security.equals(that._security);
  }

  @Override
  public int hashCode() {
    return 31 * _security.hashCode() + _dataField.hashCode();
  }
}
