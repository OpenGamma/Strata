/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.FinancialSecurity;

public class MarketDataRequirementFactory {

  public static MarketDataRequirement of(FinancialSecurity security, String dataField) {
    return new SecurityMarketDataRequirement(security, dataField);
  }

  public static MarketDataRequirement of(CurrencyPair currencyPair) {
    return new CurrencyPairMarketDataRequirement(currencyPair);
  }

  /**
   * Return the market data requirements for a curve node. A set is used as
   * depending on the type of CurveNode, it may be that an underlying a spread
   * is returned.
   *
   * @param id the curve node to get data for
   * @return the market data requirements for the curve
   */
  public static Set<MarketDataRequirement> of(CurveNodeWithIdentifier id) {

    ImmutableSet.Builder<MarketDataRequirement> requirements = ImmutableSet.builder();

    requirements.add(new CurveNodeMarketDataRequirement(id.getIdentifier(), id.getDataField()));

    if (id instanceof PointsCurveNodeWithIdentifier) {
      final PointsCurveNodeWithIdentifier node = (PointsCurveNodeWithIdentifier) id;
      requirements.add(new CurveNodeMarketDataRequirement(node.getUnderlyingIdentifier(), node.getUnderlyingDataField()));
    }

    return requirements.build();
  }
}
