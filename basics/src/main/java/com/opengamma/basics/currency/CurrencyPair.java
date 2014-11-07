/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.currency;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;

/**
 * An ordered pair of currencies, such as 'EUR/USD'.
 * <p>
 * This could be used to identify a pair of currencies for quoting rates in FX deals.
 * See {@link FxRate} for the representation that contains a rate.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class CurrencyPair
    implements Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * Regular expression to parse the textual format.
   */
  private static final Pattern REGEX_FORMAT = Pattern.compile("([A-Z]{3})[/]([A-Z]{3})");

  /**
   * The base currency of the pair.
   * In the pair 'AAA/BBB' the base is 'AAA'.
   */
  private final Currency base;
  /**
   * The counter currency of the pair.
   * In the pair 'AAA/BBB' the counter is 'BBB'.
   */
  private final Currency counter;

  //-------------------------------------------------------------------------
  /**
   * Obtains a currency pair from two currencies.
   * <p>
   * The first currency is the base and the second is the counter.
   * The two currencies may be the same.
   * 
   * @param base  the base currency
   * @param counter  the counter currency
   * @return the currency pair
   */
  public static CurrencyPair of(Currency base, Currency counter) {
    ArgChecker.notNull(base, "base");
    ArgChecker.notNull(counter, "counter");
    return new CurrencyPair(base, counter);
  }

  /**
   * Parses a currency pair from a string with format AAA/BBB.
   * <p>
   * The parsed format is '${baseCurrency}/${counterCurrency}'.
   * Currency parsing is case insensitive.
   * 
   * @param pairStr  the currency pair as a string AAA/BBB
   * @return the currency pair
   * @throws IllegalArgumentException if the pair cannot be parsed
   */
  @FromString
  public static CurrencyPair parse(String pairStr) {
    ArgChecker.notNull(pairStr, "pairStr");
    Matcher matcher = REGEX_FORMAT.matcher(pairStr.toUpperCase(Locale.ENGLISH));
    if (matcher.matches() == false) {
      throw new IllegalArgumentException("Invalid currency pair: " + pairStr);
    }
    Currency base = Currency.parse(matcher.group(1));
    Currency counter = Currency.parse(matcher.group(2));
    return new CurrencyPair(base, counter);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a currency pair.
   * 
   * @param base  the base currency, validated not null
   * @param counter  the counter currency, validated not null
   */
  private CurrencyPair(Currency base, Currency counter) {
    this.base = base;
    this.counter = counter;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the inverse currency pair.
   * <p>
   * The inverse pair has the same currencies but in reverse order.
   * 
   * @return the inverse pair
   */
  public CurrencyPair inverse() {
    return new CurrencyPair(counter, base);
  }

  /**
   * Checks if the currency pair contains the supplied currency as either its base or counter.
   * 
   * @param currency  the currency to check against the pair
   * @return true if the currency is either the base or counter currency in the pair
   */
  public boolean contains(Currency currency) {
    ArgChecker.notNull(currency, "currency");
    return base.equals(currency) || counter.equals(currency);
  }

  /**
   * Checks if this currency pair is the inverse of the specified pair.
   * <p>
   * This could be used to check if an FX rate specified in one currency pair needs inverting.
   * 
   * @param other  the other currency pair
   * @return true if the currency is the inverse of the specified pair
   */
  public boolean isInverse(CurrencyPair other) {
    ArgChecker.notNull(other, "currencyPair");
    return base.equals(other.counter) && counter.equals(other.base);
  }

  /**
   * Checks if this currency pair is related to the specified pair.
   * <p>
   * Two pairs are related if they have at least one currency in common.
   * This could be used to check if two FX rates can be combined into a single rate.
   * 
   * @param other  the other currency pair
   * @return true if this pair contains either the base or counter currency of the specified pair
   */
  public boolean isRelated(CurrencyPair other) {
    ArgChecker.notNull(other, "currencyPair");
    return contains(other.base) || contains(other.counter);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the base currency of the pair.
   * <p>
   * In the pair 'AAA/BBB' the base is 'AAA'.
   * 
   * @return the base currency
   */
  public Currency getBase() {
    return base;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the counter currency of the pair.
   * <p>
   * In the pair 'AAA/BBB' the counter is 'BBB'.
   * <p>
   * The counter currency is also known as the <i>quote currency</i> or the <i>variable currency</i>.
   * 
   * @return the counter currency
   */
  public Currency getCounter() {
    return counter;
  }

  //-----------------------------------------------------------------------
  /**
   * Checks if this currency pair equals another.
   * <p>
   * The comparison checks the two currencies.
   * 
   * @param obj  the other currency pair, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurrencyPair other = (CurrencyPair) obj;
      return base.equals(other.base) &&
          counter.equals(other.counter);
    }
    return false;
  }

  /**
   * Returns a suitable hash code for the currency.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return base.hashCode() ^ counter.hashCode();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted string version of the currency pair.
   * <p>
   * The format is '${baseCurrency}/${counterCurrency}'.
   * 
   * @return the formatted string
   */
  @Override
  @ToString
  public String toString() {
    return base.getCode() + "/" + counter.getCode();
  }

}
