/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;

import com.google.common.collect.ImmutableMap;
import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;

/**
 * A single trade against a particular counterparty.
 * <p>
 * A trade is a transaction that occurred on a specific date between two counterparties.
 * For example, an interest rate swap trade agreed on a particular date for
 * cash-flows in the future.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface Trade
    extends IdentifiableBean, Attributable, ImmutableBean {

  /**
   * The primary standard identifier for the trade.
   * <p>
   * The standard identifier is used to identify the trade.
   * It will typically be an identifier in an external data system.
   * <p>
   * A trade may have multiple active identifiers. Any identifier may be chosen here,
   * however it is strongly recommended to use an identifier that does not change over time.
   */
  @Override
  public abstract StandardId getStandardId();

  /**
   * Gets the entire set of additional attributes.
   * <p>
   * Attributes are typically used to tag the object with additional information.
   * 
   * @return the complete set of attributes
   */
  @Override
  public abstract ImmutableMap<String, String> getAttributes();

  /**
   * Gets the trade type.
   * 
   * @return the trade type
   */
  public abstract TradeType getTradeType();

  /**
   * Gets the trade date.
   * 
   * @return the trade date
   */
  public abstract LocalDate getTradeDate();

}
