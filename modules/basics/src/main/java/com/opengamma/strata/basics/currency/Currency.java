/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A unit of currency.
 * <p>
 * This class represents a unit of currency such as the British Pound, Euro or US Dollar.
 * The currency is represented by a three letter code, intended to be ISO-4217.
 * <p>
 * It is recommended to define currencies in advance using the {@code Currency.ini} file.
 * Standard configuration includes many commonly used currencies.
 * <p>
 * Only currencies listed in configuration will be returned by {@link #getAvailableCurrencies()}.
 * If a currency is requested that is not defined in configuration, it will still be created,
 * however it will have the default value of zero for the minor units and 'USD' for
 * the triangulation currency.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class Currency
    implements Comparable<Currency>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The matcher for the code.
   */
  static final CharMatcher CODE_MATCHER = CharMatcher.inRange('A', 'Z');
  /**
   * The configured instances.
   */
  private static final ImmutableMap<String, Currency> CONFIGURED =
      CurrencyDataLoader.loadCurrencies(false);
  /**
   * A cache of dynamically created instances, initialized with some historic currencies.
   */
  private static final ConcurrentMap<String, Currency> DYNAMIC =
      new ConcurrentHashMap<>(CurrencyDataLoader.loadCurrencies(true));

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
   * The currency 'CHF' - Swiss Franc.
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
  /**
   * The currency 'NZD' - New Zealand Dollar.
   */
  public static final Currency NZD = of("NZD");

  // a selection of other currencies
  /**
   * The currency 'AED' - UAE Dirham.
   */
  public static final Currency AED = of("AED");
  /**
   * The currency 'ARS' - Argentine Peso.
   */
  public static final Currency ARS = of("ARS");
  /**
   * The currency 'BGN' - Bulgarian Lev.
   */
  public static final Currency BGN = of("BGN");
  /**
   * The currency 'BHD' - Bahraini Dinar.
   */
  public static final Currency BHD = of("BHD");
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
   * The currency 'COP' - Colombian Peso.
   */
  public static final Currency COP = of("COP");
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
   * The currency 'HRK' - Croatian Kuna.
   */
  public static final Currency HRK = of("HRK");
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
   * The currency 'INR' = Indian Rupee.
   */
  public static final Currency INR = of("INR");
  /**
   * The currency 'ISK' = Icelandic Krone.
   */
  public static final Currency ISK = of("ISK");
  /**
   * The currency 'KRW' = South Korean Won.
   */
  public static final Currency KRW = of("KRW");
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
   * The currency 'PEN' - Peruvian Nuevo Sol.
   */
  public static final Currency PEN = of("PEN");
  /**
   * The currency 'PHP' - Philippine Peso.
   */
  public static final Currency PHP = of("PHP");
  /**
   * The currency 'PKR' - Pakistani Rupee.
   */
  public static final Currency PKR = of("PKR");
  /**
   * The currency 'PLN' - Polish Zloty.
   */
  public static final Currency PLN = of("PLN");
  /**
   * The currency 'RON' - Romanian New Leu.
   */
  public static final Currency RON = of("RON");
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
   * The currency 'THB' - Thai Baht.
   */
  public static final Currency THB = of("THB");
  /**
   * The currency 'TRY' - Turkish Lira.
   */
  public static final Currency TRY = of("TRY");
  /**
   * The currency 'TWD' - New Taiwan Dollar.
   */
  public static final Currency TWD = of("TWD");
  /**
   * The currency 'UAH' - Ukrainian Hryvnia.
   */
  public static final Currency UAH = of("UAH");
  /**
   * The currency 'ZAR' - South African Rand.
   */
  public static final Currency ZAR = of("ZAR");

  // special cases
  /**
   * The currency 'XXX' - No applicable currency.
   */
  public static final Currency XXX = of("XXX");
  /**
   * The currency 'XAG' - Silver (troy ounce).
   */
  public static final Currency XAG = of("XAG");
  /**
   * The currency 'XAU' - Gold (troy ounce).
   */
  public static final Currency XAU = of("XAU");
  /**
   * The currency 'XPD' - Paladium (troy ounce).
   */
  public static final Currency XPD = of("XPD");
  /**
   * The currency 'XPT' - Platinum (troy ounce).
   */
  public static final Currency XPT = of("XPT");

  /**
   * The currency code.
   */
  private final String code;
  /**
   * The number of fraction digits, such as 2 for cents in the dollar.
   */
  private final transient int minorUnitDigits;
  /**
   * The triangulation currency.
   * Due to initialization ordering, cannot guarantee that USD/EUR is loaded first, so this must be a string.
   */
  private final transient String triangulationCurrency;
  /**
   * The cached hash code.
   */
  private final transient int cachedHashCode;

  //-------------------------------------------------------------------------
  /**
   * Obtains the set of configured currencies.
   * <p>
   * This contains all the currencies that have been defined in configuration.
   * Any currency instances that have been dynamically created are not included.
   * 
   * @return an immutable set containing all registered currencies
   */
  public static Set<Currency> getAvailableCurrencies() {
    return ImmutableSet.copyOf(CONFIGURED.values());
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance for the specified ISO-4217 three letter currency code.
   * <p>
   * A currency is uniquely identified by ISO-4217 three letter code.
   * Currencies should be defined in configuration before they can be used.
   * If the requested currency is not defined in configuration, it will still be created,
   * however it will have the default value of zero for the minor units and 'USD' for
   * the triangulation currency.
   *
   * @param currencyCode  the three letter currency code, ASCII and upper case
   * @return the singleton instance
   * @throws IllegalArgumentException if the currency code is invalid
   */
  @FromString
  public static Currency of(String currencyCode) {
    ArgChecker.notNull(currencyCode, "currencyCode");
    Currency currency = CONFIGURED.get(currencyCode);
    if (currency == null) {
      return addCode(currencyCode);
    }
    return currency;
  }

  // add code
  private static Currency addCode(String currencyCode) {
    ArgChecker.matches(CODE_MATCHER, 3, 3, currencyCode, "currencyCode", "[A-Z][A-Z][A-Z]");
    return DYNAMIC.computeIfAbsent(currencyCode, code -> new Currency(code, 0, "USD"));
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a string to obtain a {@code Currency}.
   * <p>
   * The parse is identical to {@link #of(String)} except that it will convert
   * letters to upper case first.
   * If the requested currency is not defined in configuration, it will still be created,
   * however it will have the default value of zero for the minor units and 'USD' for
   * the triangulation currency.
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
   * Restricted constructor, called only by {@code CurrencyProperties}.
   * 
   * @param code  the three letter currency code, validated
   * @param fractionDigits  the number of fraction digits, validated
   * @param triangulationCurrency  the triangulation currency
   */
  Currency(String code, int fractionDigits, String triangulationCurrency) {
    this.code = code;
    this.minorUnitDigits = fractionDigits;
    this.triangulationCurrency = triangulationCurrency;
    // total universe is (26 * 26 * 26) codes, which can provide a unique hash code
    this.cachedHashCode = ((code.charAt(0) - 64) << 16) + ((code.charAt(1) - 64) << 8) + (code.charAt(2) - 64);
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

  /**
   * Gets the number of digits in the minor unit.
   * <p>
   * For example, 'USD' will return 2, indicating that there are two digits,
   * corresponding to cents in the dollar.
   * 
   * @return the number of fraction digits
   */
  public int getMinorUnitDigits() {
    return minorUnitDigits;
  }

  /**
   * Gets the preferred triangulation currency.
   * <p>
   * When obtaining a market quote for a currency, the triangulation currency
   * is used if no direct rate can be found.
   * For example, there is no direct rate for 'CZK/SGD'. Instead 'CZK' might be defined to
   * triangulate via 'EUR' and 'SGD' with 'USD'. Since the three rates, 'CZK/EUR', 'EUR/USD'
   * and 'USD/SGD' can be obtained, a rate can be determined for 'CZK/SGD'.
   * Note that most currencies triangulate via 'USD'.
   * 
   * @return the triangulation currency
   */
  public Currency getTriangulationCurrency() {
    return Currency.of(triangulationCurrency);
  }

  //-------------------------------------------------------------------------
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
    // hash code is unique and ordered so can be used for compareTo
    return cachedHashCode - other.cachedHashCode;
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
      return equals((Currency) obj);
    }
    return false;
  }

  // called by CurrencyAmount
  boolean equals(Currency other) {
    // hash code is unique so can be used for equals
    return other.cachedHashCode == cachedHashCode;
  }

  /**
   * Returns a suitable hash code for the currency.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return cachedHashCode;
  }

  //-------------------------------------------------------------------------
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
