/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.FinancialSecurity;

public class StandardMarketDataRequirement implements MarketDataRequirement {


  private final FinancialSecurity _security;
  private final String _requirement;

  public StandardMarketDataRequirement(FinancialSecurity security, String requirement) {
    //To change body of created methods use File | Settings | File Templates.
    _security = security;
    _requirement = requirement;
  }

  public static MarketDataRequirement of(FinancialSecurity security, String marketValue) {

    return new StandardMarketDataRequirement(security, marketValue);
  }

  public static MarketDataRequirement of(CurrencyPair currencyPair) {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  public static Set<MarketDataRequirement> of(CurveNodeWithIdentifier id) {

    /*Set<MarketDataRequirement> requirements = new HashSet<>();
    if (id.getDataField() != null) {

      id.getDataField(), ComputationTargetType.PRIMITIVE, id.getIdentifier()));
      if (id instanceof PointsCurveNodeWithIdentifier) {
        final PointsCurveNodeWithIdentifier node = (PointsCurveNodeWithIdentifier) id;
        requirements.add(new ValueRequirement(node.getUnderlyingDataField(), ComputationTargetType.PRIMITIVE, node.getUnderlyingIdentifier()));
      }
    } else {
      requirements.add(new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE, ComputationTargetType.PRIMITIVE, id.getIdentifier()));
    }*/
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StandardMarketDataRequirement that = (StandardMarketDataRequirement) o;
    return _requirement.equals(that._requirement) && _security.equals(that._security);

  }

  @Override
  public int hashCode() {
    int result = _security.hashCode();
    result = 31 * result + _requirement.hashCode();
    return result;
  }

}
