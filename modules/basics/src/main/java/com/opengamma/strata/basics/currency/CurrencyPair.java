/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An ordered pair of currencies, such as 'EUR/USD'.
 * <p>
 * This could be used to identify a pair of currencies for quoting rates in FX deals.
 * See {@link FxRate} for the representation that contains a rate.
 * <p>
 * It is recommended to define currencies in advance using the {@code CurrencyPair.ini} file.
 * Standard configuration includes many commonly used currency pairs.
 * <p>
 * Only currencies listed in configuration will be returned by {@link #getAvailablePairs()}.
 * If a pair is requested that is not defined in configuration, it will still be created,
 * however the market convention information will be generated.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class CurrencyPair
    implements Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Regular expression to parse the textual format.
   * Three ASCII upper case letters, a slash, and another three ASCII upper case letters.
   */
  static final Pattern REGEX_FORMAT = Pattern.compile("([A-Z]{3})/([A-Z]{3})");
  /**
   * The configured instances and associated rate digits.
   */
  private static final ImmutableMap<CurrencyPair, Integer> CONFIGURED = CurrencyDataLoader.loadPairs();
  /**
   * Ordering of each currency, used when choosing a market convention pair when there is no configuration.
   * The currency closer to the start of the list (with the lower ordering) is the base currency.
   */
  private static final ImmutableMap<Currency, Integer> CURRENCY_ORDERING = CurrencyDataLoader.loadOrdering();

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
   * Obtains the set of configured currency pairs.
   * <p>
   * This contains all the currency pairs that have been defined in configuration.
   * Any currency pair instances that have been dynamically created are not included.
   * 
   * @return an immutable set containing all registered currency pairs
   */
  public static Set<CurrencyPair> getAvailablePairs() {
    return CONFIGURED.keySet();
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from two currencies.
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
    if (!matcher.matches()) {
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
   * Checks if this currency pair is an identity pair.
   * <p>
   * The identity pair is one where the base and counter currency are the same..
   * 
   * @return true if this pair is an identity pair
   */
  public boolean isIdentity() {
    return base.equals(counter);
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
   * Finds the currency pair that is a cross between this pair and the other pair.
   * <p>
   * The cross is only returned if the two pairs contains three currencies in total,
   * such as AAA/BBB and BBB/CCC and neither pair is an identity such as AAA/AAA.
   * <ul>
   * <li>Given two pairs AAA/BBB and BBB/CCC the result will be AAA/CCC or CCC/AAA as per the market convention.
   * <li>Given two pairs AAA/BBB and CCC/DDD the result will be empty.
   * <li>Given two pairs AAA/AAA and AAA/BBB the result will be empty.
   * <li>Given two pairs AAA/BBB and AAA/BBB the result will be empty.
   * <li>Given two pairs AAA/AAA and AAA/AAA the result will be empty.
   * </ul>
   * 
   * @param other  the other currency pair
   * @return the cross currency pair, or empty if no cross currency pair can be created
   */
  public Optional<CurrencyPair> cross(CurrencyPair other) {
    ArgChecker.notNull(other, "other");
    if (isIdentity() || other.isIdentity() || this.equals(other) || this.equals(other.inverse())) {
      return Optional.empty();
    }
    // AAA/BBB cross BBB/CCC
    if (counter.equals(other.base)) {
      return Optional.of(of(base, other.counter).toConventional());
    }
    // AAA/BBB cross CCC/BBB
    if (counter.equals(other.counter)) {
      return Optional.of(of(base, other.base).toConventional());
    }
    // BBB/AAA cross BBB/CCC
    if (base.equals(other.base)) {
      return Optional.of(of(counter, other.counter).toConventional());
    }
    // BBB/AAA cross CCC/BBB
    if (base.equals(other.counter)) {
      return Optional.of(of(counter, other.base).toConventional());
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this currency pair is a conventional currency pair.
   * <p>
   * A market convention determines that 'EUR/USD' should be used and not 'USD/EUR'.
   * This knowledge is encoded in configuration for a standard set of pairs.
   * <p>
   * It is possible to create two different currency pairs from any two currencies, and it is guaranteed that
   * exactly one of the pairs will be the market convention pair.
   * <p>
   * If a currency pair is not explicitly listed in the configuration, a priority ordering of currencies
   * is used to choose base currency of the pair that is treated as conventional.
   * <p>
   * If there is no configuration available to determine which pair is the market convention, a pair will
   * be chosen arbitrarily but deterministically. This ensures the same pair will be chosen for any two
   * currencies even if the {@code CurrencyPair} instances are created independently.
   * 
   * @return true if the currency pair follows the market convention, false if it does not
   */
  public boolean isConventional() {
    // If the pair is in the configuration file it is a market convention pair
    if (CONFIGURED.containsKey(this)) {
      return true;
    }
    // Get the priorities of the currencies to determine which should be the base
    Integer basePriority = CURRENCY_ORDERING.getOrDefault(base, Integer.MAX_VALUE);
    Integer counterPriority = CURRENCY_ORDERING.getOrDefault(counter, Integer.MAX_VALUE);

    // If a currency is earlier in the list it has a higher priority
    if (basePriority < counterPriority) {
      return true;
    } else if (basePriority > counterPriority) {
      return false;
    }
    // Neither currency is included in the list defining the ordering.
    // Use lexicographical ordering. It's arbitrary but consistent. This ensures two CurrencyPair instances
    // created independently for the same two currencies will always choose the same conventional pair.
    // The natural ordering of Currency is the same as the natural ordering of the currency code but
    // comparing the Currency instances is more efficient.
    // This is <= 0 so that a pair with two copies of the same currency is conventional
    return base.compareTo(counter) <= 0;
  }

  /**
   * Returns the market convention currency pair for the currencies in the pair.
   * <p>
   * If {@link #isConventional()} is {@code true} this method returns {@code this}, otherwise
   * it returns the {@link #inverse} pair.
   *
   * @return the market convention currency pair for the currencies in the pair
   */
  public CurrencyPair toConventional() {
    return isConventional() ? this : inverse();
  }

  /**
   * Gets the number of digits in the rate.
   * <p>
   * If this rate is a conventional currency pair defined in configuration,
   * then the number of digits in a market FX rate quote is returned.
   * <p>
   * If the currency pair is not defined in configuration the sum of the
   * {@link Currency#getMinorUnitDigits() minor unit digits} from the two currencies is returned.
   * 
   * @return the number of digits in the FX rate
   */
  public int getRateDigits() {
    Integer digits = CONFIGURED.get(this);

    if (digits != null) {
      return digits;
    }
    Integer inverseDigits = CONFIGURED.get(inverse());

    if (inverseDigits != null) {
      return inverseDigits;
    }
    return base.getMinorUnitDigits() + counter.getMinorUnitDigits();
  }

  //-------------------------------------------------------------------------
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

  //-------------------------------------------------------------------------
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

  //-------------------------------------------------------------------------
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
