/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesName;

/**
 * The specification of how to build FX option volatilities.
 * <p>
 * This is the specification for a single volatility object, {@link FxOptionVolatilities}. 
 * Each implementation of this interface must have the ability to create an instance of the respective implementation 
 * of {@link FxOptionVolatilities}.
 */
public interface FxOptionVolatilitiesSpecification {

  /**
   * Gets the name of a set of FX option volatilities.
   * 
   * @return the name
   */
  public abstract FxOptionVolatilitiesName getName();

  /**
   * Gets the currency pair.
   * 
   * @return the currency pair
   */
  public abstract CurrencyPair getCurrencyPair();

  /**
   * Gets the volatilities nodes.
   * 
   * @return the nodes
   */
  public abstract ImmutableList<FxOptionVolatilitiesNode> getNodes();

  //-------------------------------------------------------------------------
  /**
   * Creates FX option volatilities.
   * <p>
   * The number and ordering of {@code parameters} must be coherent to those of nodes, {@code #getNodes()}.
   * 
   * @param valuationDateTime  the valuation date time
   * @param parameters  the parameters
   * @param refData  the reference data
   * @return the volatilities
   */
  public abstract FxOptionVolatilities volatilities(
      ZonedDateTime valuationDateTime,
      DoubleArray parameters,
      ReferenceData refData);

  /**
   * Obtains the inputs required to create the FX option volatilities.
   * 
   * @return the inputs
   */
  public default ImmutableList<QuoteId> volatilitiesInputs() {
    return getNodes().stream()
        .map(FxOptionVolatilitiesNode::getQuoteId)
        .collect(toImmutableList());
  }

  /**
   * Gets the number of parameters.
   * 
   * @return the number of parameters
   */
  public default int getParameterCount() {
    return getNodes().size();
  }

}
