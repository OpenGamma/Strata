/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Builder to create {@code TradeInfo}.
 * <p>
 * This builder allows a {@link TradeInfo} to be created.
 */
public final class TradeInfoBuilder {

  /**
   * The primary identifier for the trade.
   * <p>
   * The identifier is used to identify the trade.
   */
  private StandardId id;
  /**
   * The counterparty identifier.
   * <p>
   * An identifier used to specify the counterparty of the trade.
   */
  private StandardId counterparty;
  /**
   * The trade date.
   */
  private LocalDate tradeDate;
  /**
   * The trade time.
   */
  private LocalTime tradeTime;
  /**
   * The trade time-zone.
   */
  private ZoneId zone;
  /**
   * The settlement date.
   */
  private LocalDate settlementDate;
  /**
   * The trade attributes.
   * <p>
   * Trade attributes, provide the ability to associate arbitrary information
   * with a trade in a key-value map.
   */
  private final Map<TradeAttributeType<?>, Object> attributes = new HashMap<>();

  // creates an empty instance
  TradeInfoBuilder() {
  }

  // creates a populated instance
  TradeInfoBuilder(
      StandardId id,
      StandardId counterparty,
      LocalDate tradeDate,
      LocalTime tradeTime,
      ZoneId zone,
      LocalDate settlementDate,
      Map<TradeAttributeType<?>, Object> attributes) {

    this.id = id;
    this.counterparty = counterparty;
    this.tradeDate = tradeDate;
    this.tradeTime = tradeTime;
    this.zone = zone;
    this.settlementDate = settlementDate;
    this.attributes.putAll(attributes);
  }

  //-----------------------------------------------------------------------
  /**
   * Sets the primary identifier for the trade, optional.
   * <p>
   * The identifier is used to identify the trade.
   * 
   * @param id  the identifier
   * @return this, for chaining
   */
  public TradeInfoBuilder id(StandardId id) {
    this.id = id;
    return this;
  }

  /**
   * Sets the counterparty identifier, optional.
   * <p>
   * An identifier used to specify the counterparty of the trade.
   * 
   * @param counterparty  the counterparty
   * @return this, for chaining
   */
  public TradeInfoBuilder counterparty(StandardId counterparty) {
    this.counterparty = counterparty;
    return this;
  }

  /**
   * Sets the trade date, optional.
   * 
   * @param tradeDate  the trade date
   * @return this, for chaining
   */
  public TradeInfoBuilder tradeDate(LocalDate tradeDate) {
    this.tradeDate = tradeDate;
    return this;
  }

  /**
   * Sets the trade time, optional.
   * 
   * @param tradeTime  the trade time
   * @return this, for chaining
   */
  public TradeInfoBuilder tradeTime(LocalTime tradeTime) {
    this.tradeTime = tradeTime;
    return this;
  }

  /**
   * Sets the trade time-zone, optional.
   * 
   * @param zone  the trade zone
   * @return this, for chaining
   */
  public TradeInfoBuilder zone(ZoneId zone) {
    this.zone = zone;
    return this;
  }

  /**
   * Sets the settlement date, optional.
   * 
   * @param settlementDate  the settlement date
   * @return this, for chaining
   */
  public TradeInfoBuilder settlementDate(LocalDate settlementDate) {
    this.settlementDate = settlementDate;
    return this;
  }

  /**
   * Adds a trade attribute to the map of attributes.
   * <p>
   * The attribute is added using {@code Map.put(type, value)} semantics.
   * 
   * @param <T> the type of the value
   * @param type  the type providing meaning to the value
   * @param value  the value
   * @return this, for chaining
   */
  @SuppressWarnings("unchecked")
  public <T> TradeInfoBuilder addAttribute(TradeAttributeType<T> type, T value) {
    ArgChecker.notNull(type, "type");
    ArgChecker.notNull(value, "value");
    // ImmutableMap.Builder would not provide Map.put semantics
    attributes.put(type, value);
    return this;
  }

  /**
   * Builds the trade information.
   * 
   * @return the trade information
   */
  public TradeInfo build() {
    return new TradeInfo(
        id,
        counterparty,
        tradeDate,
        tradeTime,
        zone,
        settlementDate,
        attributes);
  }

}
