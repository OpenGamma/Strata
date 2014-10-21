/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.trade;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;

import com.google.common.collect.ImmutableMap;

/**
 * A single trade against a particular counterparty.
 * <p>
 * A trade is a transaction that occurred at a specific instant in time.
 */
public interface Trade extends ImmutableBean {

  /**
   * Gets the trade type.
   * 
   * @return the trade type, not null
   */
  TradeType getTradeType();

  /**
   * Gets the asset class.
   * 
   * @return the asset class, not null
   */
  AssetClass getAssetClass();

  /**
   * Gets the trade date.
   * 
   * @return the trade date, not null
   */
  LocalDate getTradeDate();

  /**
   * Gets the entire set of additional attributes.
   * <p>
   * Most data in the trade is available as bean properties.
   * Attributes are used to tag the object with additional information.
   * 
   * @return the complete set of attributes, not null
   */
  ImmutableMap<String, String> getAttributes();

}
