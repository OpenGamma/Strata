/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.currency;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.collect.ImmutableSet;
import com.opengamma.collect.ArgChecker;

/**
 * A unit of currency.
 * <p>
 * This class represents a unit of currency such as the British Pound, Euro or US Dollar.
 * Any three letter code may be used, however it is intended to use codes based on ISO-4217.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class Currency
    implements Comparable<Currency>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * A cache of instances.
   */
  private static final ConcurrentMap<String, Currency> CACHE = new ConcurrentHashMap<>();
  /**
   * The valid regex for schemes.
   * Three ASCII upper case letters.
   */
  private static final Pattern REGEX_CODE = Pattern.compile("[A-Z]{3}");

  // a selection of commonly traded, stable currencies
  /**
   * The currency 'USD' - United States Dollar.
   */
  public static final Currency USD = of("USD");
  /**
   * The currency 'EUR' - Euro.
   */
  public static final Currency EUR = of("EUR");
  /**
   * The currency 'JPY' - Japanese Yen.
   */
  public static final Currency JPY = of("JPY");
  /**
   * The currency 'GBP' - British pound.
   */
  public static final Currency GBP = of("GBP");
  /**
   * The currency 'EUR' - Swiss Franc.
   */
  public static final Currency CHF = of("CHF");
  /**
   * The currency 'AUD' - Australian Dollar.
   */
  public static final Currency AUD = of("AUD");
  /**
   * The currency 'CAD' - Canadian Dollar.
   */
  public static final Currency CAD = of("CAD");

  // a selection of other currencies
  /**
   * The currency 'ARS' - Argentine Peso.
   */
  public static final Currency ARS = of("ARS");
  /**
   * The currency 'BRL' - Brazil Dollar.
   */
  public static final Currency BRL = of("BRL");
  /**
   * The currency 'CLP' - Chilean Peso.
   */
  public static final Currency CLP = of("CLP");
  /**
   * The currency 'CNY' - Chinese Yuan.
   */
  public static final Currency CNY = of("CNY");
  /**
   * The currency 'CZK' - Czeck Krona.
   */
  public static final Currency CZK = of("CZK");
  /**
   * The currency 'DKK' - Danish Krone.
   */
  public static final Currency DKK = of("DKK");
  /**
   * The currency 'EGP' - Egyptian Pound.
   */
  public static final Currency EGP = of("EGP");
  /**
   * The currency 'HKD' - Hong Kong Dollar.
   */
  public static final Currency HKD = of("HKD");
  /**
   * The currency 'HUF' = Hugarian Forint.
   */
  public static final Currency HUF = of("HUF");
  /**
   * The currency 'IDR' = Indonesian Rupiah.
   */
  public static final Currency IDR = of("IDR");
  /**
   * The currency 'ILS' = Israeli Shekel.
   */
  public static final Currency ILS = of("ILS");
  /**
   * The currency 'KRW' = South Korean Won.
   */
  public static final Currency KRW = of("KRW");
  /**
   * The currency 'INR' = Indian Rupee.
   */
  public static final Currency INR = of("INR");
  /**
   * The currency 'ISK' = Icelandic Krone.
   */
  public static final Currency ISK = of("ISK");
  /**
   * The currency 'MXN' - Mexican Peso.
   */
  public static final Currency MXN = of("MXN");
  /**
   * The currency 'MYR' - Malaysian Ringgit.
   */
  public static final Currency MYR = of("MYR");
  /**
   * The currency 'NOK' - Norwegian Krone.
   */
  public static final Currency NOK = of("NOK");
  /**
   * The currency 'NZD' - New Zealand Dollar.
   */
  public static final Currency NZD = of("NZD");
  /**
   * The currency 'PLN' - Polish Zloty.
   */
  public static final Currency PLN = of("PLN");
  /**
   * The currency 'RUB' - Russian Ruble.
   */
  public static final Currency RUB = of("RUB");
  /**
   * The currency 'SAR' - Saudi Riyal.
   */
  public static final Currency SAR = of("SAR");
  /**
   * The currency 'SEK' - Swedish Krona.
   */
  public static final Currency SEK = of("SEK");
  /**
   * The currency 'SGD' - Singapore Dollar.
   */
  public static final Currency SGD = of("SGD");
  /**
   * The currency 'SKK' - Slovak Korona.
   */
  public static final Currency SKK = of("SKK"); 
  /**
   * The currency 'THB' - Thai Baht.
   */
  public static final Currency THB = of("THB"); 
  /**
   * The currency 'TRY' - Turkish Lira.
   */
  public static final Currency TRY = of("TRY"); 
  /**
   * The currency 'ZAR' - South African Rand.
   */
  public static final Currency ZAR = of("ZAR");

  // a selection of historic currencies
  /**
   * The historic currency 'BEF' - Belgian Franc.
   */
  public static final Currency BEF = of("BEF");
  /**
   * The historic currency 'DEM' - Deutsche Mark.
   */
  public static final Currency DEM = of("DEM");
  /**
   * The historic currency 'ESP' - Spanish Peseta.
   */
  public static final Currency ESP = of("ESP");
  /**
   * The historic currency 'FRF' - French Franc.
   */
  public static final Currency FRF = of("FRF");
  /**
   * The historic currency 'GRD' - Greek Drachma.
   */
  public static final Currency GRD = of("GRD");
  /**
   * The historic currency 'IEP' - Irish Pound.
   */
  public static final Currency IEP = of("IEP");
  /**
   * The historic currency 'ITL' - Italian Lira.
   */
  public static final Currency ITL = of("ITL");
  /**
   * The historic currency 'NLG' - Dutch Guilder.
   */
  public static final Currency NLG = of("NLG");
  /**
   * The historic currency 'PTE' - Portuguese Escudo.
   */
  public static final Currency PTE = of("PTE");

  /**
   * The currency code.
   */
  private final String code;

  //-----------------------------------------------------------------------
  /**
   * Obtains the set of available currencies.
   * <p>
   * This contains all the currencies that have been defined at the point
   * that the method is called.
   * 
   * @return an immutable set containing all registered currencies
   */
  public static Set<Currency> getAvailableCurrencies() {
    return ImmutableSet.copyOf(CACHE.values());
  }

  //-----------------------------------------------------------------------
  /**
   * Obtains an instance of {@code Currency} for the specified ISO-4217
   * three letter currency code dynamically creating a currency if necessary.
   * <p>
   * A currency is uniquely identified by ISO-4217 three letter code.
   * This method creates the currency if it is not known.
   *
   * @param currencyCode  the three letter currency code, ASCII and upper case
   * @return the singleton instance
   * @throws IllegalArgumentException if the currency code is invalid
   */
  @FromString
  public static Currency of(String currencyCode) {
    ArgChecker.matches(REGEX_CODE, currencyCode, "currencyCode");
    return CACHE.computeIfAbsent(currencyCode, Currency::new);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of {@code Currency} matching the specified JDK currency.
   * <p>
   * This converts the JDK currency instance to a currency unit using the code.
   *
   * @param currency  the currency
   * @return the singleton instance
   * @throws IllegalArgumentException if the currency cannot be converted
   */
  public static Currency fromJdk(java.util.Currency currency) {
    ArgChecker.notNull(currency, "currency");
    return Currency.of(currency.getCurrencyCode());
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a string to obtain a {@code Currency}.
   * <p>
   * The parse is identical to {@link #of(String)} except that it will convert
   * letters to upper case first.
   *
   * @param currencyCode  the three letter currency code, ASCII
   * @return the singleton instance
   * @throws IllegalArgumentException if the currency code is invalid
   */
  public static Currency parse(String currencyCode) {
    ArgChecker.notNull(currencyCode, "currencyCode");
    return of(currencyCode.toUpperCase(Locale.ENGLISH));
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   * 
   * @param code  the three letter currency code
   */
  private Currency(String code) {
    this.code = code;
  }

  /**
   * Ensure singleton on deserialization.
   * 
   * @return the singleton
   */
  private Object readResolve() {
    return Currency.of(code);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the three letter ISO code.
   * 
   * @return the three letter ISO code
   */
  public String getCode() {
    return code;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the JDK currency instance equivalent to this currency.
   * <p>
   * This attempts to convert a {@code Currency} to a JDK {@code Currency}.
   * 
   * @return the JDK currency instance
   * @throws IllegalArgumentException if no matching currency exists in the JDK
   */
  public java.util.Currency toJdk() {
    return java.util.Currency.getInstance(code);
  }

  //-----------------------------------------------------------------------
  /**
   * Compares this currency to another.
   * <p>
   * The comparison sorts alphabetically by the three letter currency code.
   * 
   * @param other  the other currency
   * @return negative if less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(Currency other) {
    return code.compareTo(other.code);
  }

  /**
   * Checks if this currency equals another currency.
   * <p>
   * The comparison checks the three letter currency code.
   * 
   * @param obj  the other currency, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Currency) {
      return code.equals(((Currency) obj).code);
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
    return code.hashCode();
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a string representation of the currency, which is the three letter code.
   * 
   * @return the three letter currency code
   */
  @Override
  @ToString
  public String toString() {
    return code;
  }

}
