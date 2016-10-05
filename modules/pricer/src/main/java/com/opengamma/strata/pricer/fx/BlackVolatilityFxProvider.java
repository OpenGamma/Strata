/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;

/**
 * Data provider of volatility for FX options in the lognormal or Black-Scholes model.
 */
public interface BlackVolatilityFxProvider
    extends FxProvider, ImmutableBean {

  /**
   * Gets the valuation date-time.
   * 
   * @return the valuation date-time
   */
  public abstract ZonedDateTime getValuationDateTime();

  /**
   * Returns the Black volatility.
   * 
   * @param currencyPair the currency pair
   * @param expiryDate  the option expiry
   * @param strike  the option strike
   * @param forward  the underling forward
   * @return the volatility
   */
  public abstract double getVolatility(CurrencyPair currencyPair, LocalDate expiryDate, double strike, double forward);

  /**
   * Returns the index on which the underlying FX is based.
   * @return the index
   */
  public abstract CurrencyPair getCurrencyPair();

  /**
   * Converts a date to a relative {@code double} time.
   * 
   * @param date  the date to find the relative time of
   * @param time  the time to find the relative time of
   * @param zone  the time zone
   * @return the relative time
   */
  public abstract double relativeTime(LocalDate date, LocalTime time, ZoneId zone);

  /**
   * Computes the sensitivity to the nodes used in the description of the Black volatility from a point sensitivity.
   * <p>
   * The returned value is a map between the parameter of a node (for example, {@code DoublesPair} specifying 
   * expiry time and strike for a surface) and the sensitivity value to the node as {@code Double}.  
   * 
   * @param point  the point sensitivity at a given key
   * @return the sensitivity to the nodes
   */
  public abstract Map<?, Double> nodeSensitivity(FxOptionSensitivity point);
}
