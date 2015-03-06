/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import org.joda.beans.ImmutableBean;

import com.opengamma.collect.id.IdentifiableBean;
import com.opengamma.collect.id.StandardId;

/**
 * A single trade.
 * <p>
 * A trade is a transaction that occurred on a specific date between two counterparties.
 * For example, an interest rate swap trade agreed on a particular date for
 * cash-flows in the future.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface Trade
    extends IdentifiableBean, ImmutableBean {

  /**
   * The primary standard identifier for the trade.
   * <p>
   * The standard identifier is used to identify the trade.
   * It will typically be an identifier in an external data system.
   * <p>
   * A trade may have multiple active identifiers. Any identifier may be chosen here.
   * Certain uses of the identifier, such as storage in a database, require that the
   * identifier does not change over time, and this should be considered best practice.
   */
  @Override
  public abstract StandardId getStandardId();

  /**
   * The additional trade information.
   * <p>
   * This allows additional information to be attached to the trade.
   * 
   * @return the additional trade info
   */
  public abstract TradeInfo getTradeInfo();

}
