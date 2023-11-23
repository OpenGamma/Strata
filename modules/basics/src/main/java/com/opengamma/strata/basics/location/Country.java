/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.location;

import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CharMatcher;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSortedSet;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.io.PropertiesFile;
import com.opengamma.strata.collect.io.ResourceLocator;

/**
 * A country or territory.
 * <p>
 * This class represents a country or territory that it is useful to identify.
 * Any two letter code may be used, however it is intended to use codes based on ISO-3166-1 alpha-2.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class Country
    implements Comparable<Country>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Loads the ISO alpha-2 and alpha-3 country codes into a bidirectional map.
   */
  private static final Supplier<ImmutableBiMap<String, Country>> COUNTRY_CODES = Suppliers.memoize(() ->
      ImmutableBiMap.copyOf(MapStream.of(PropertiesFile.of(
                  ResourceLocator.ofClasspath(Country.class, "country.properties").getCharSource())
              .getProperties().asMap())
          .mapValues(Country::of)
          .toMap()));

  /**
   * A cache of instances.
   */
  private static final ConcurrentMap<String, Country> CACHE = new ConcurrentSkipListMap<>();
  /**
   * The matcher for the code.
   */
  private static final CharMatcher CODE_MATCHER = CharMatcher.inRange('A', 'Z');

  // selected countries of Europe
  /**
   * The region of 'EU' - Europe (special status in ISO-3166).
   */
  public static final Country EU = of("EU");
  /**
   * The country 'AT' - Austria.
   */
  public static final Country AT = of("AT");
  /**
   * The country 'BE' - Belgium.
   */
  public static final Country BE = of("BE");
  /**
   * The country 'CH' - Switzerland.
   */
  public static final Country CH = of("CH");
  /**
   * The currency 'CZ' - Czech Republic.
   */
  public static final Country CZ = of("CZ");
  /**
   * The country 'DE' - Germany.
   */
  public static final Country DE = of("DE");
  /**
   * The country 'DK' - Denmark.
   */
  public static final Country DK = of("DK");
  /**
   * The currency 'ES' - Spain.
   */
  public static final Country ES = of("ES");
  /**
   * The currency 'FI' - Finland.
   */
  public static final Country FI = of("FI");
  /**
   * The currency 'FR' - France.
   */
  public static final Country FR = of("FR");
  /**
   * The country 'GB' - United Kingdom.
   */
  public static final Country GB = of("GB");
  /**
   * The country 'GR' - Greece.
   */
  public static final Country GR = of("GR");
  /**
   * The currency 'HU' = Hungary.
   */
  public static final Country HU = of("HU");
  /**
   * The currency 'IE' - Ireland.
   */
  public static final Country IE = of("IE");
  /**
   * The currency 'IS' - Iceland.
   */
  public static final Country IS = of("IS");
  /**
   * The currency 'IT' - Italy.
   */
  public static final Country IT = of("IT");
  /**
   * The currency 'LU' - Luxembourg.
   */
  public static final Country LU = of("LU");
  /**
   * The currency 'NL' - Netherlands.
   */
  public static final Country NL = of("NL");
  /**
   * The currency 'NO' - Norway.
   */
  public static final Country NO = of("NO");
  /**
   * The currency 'PL' = Poland.
   */
  public static final Country PL = of("PL");
  /**
   * The currency 'PT' - Portugal.
   */
  public static final Country PT = of("PT");
  /**
   * The currency 'SE' - Sweden.
   */
  public static final Country SE = of("SE");
  /**
   * The currency 'SK' - Slovakia.
   */
  public static final Country SK = of("SK");
  /**
   * The country 'TR' - Turkey.
   */
  public static final Country TR = of("TR");

  // selected countries of the Americas
  /**
   * The country 'AR' - Argentina.
   */
  public static final Country AR = of("AR");
  /**
   * The country 'BR' - Brazil.
   */
  public static final Country BR = of("BR");
  /**
   * The country 'CA' - Canada.
   */
  public static final Country CA = of("CA");
  /**
   * The country 'CL' - Chile.
   */
  public static final Country CL = of("CL");
  /**
   * The country 'MX' - Mexico.
   */
  public static final Country MX = of("MX");
  /**
   * The country 'US' - United States.
   */
  public static final Country US = of("US");

  // selected countries of the Rest of the World
  /**
   * The country 'AU' - Australia.
   */
  public static final Country AU = of("AU");
  /**
   * The country 'CN' - China.
   */
  public static final Country CN = of("CN");
  /**
   * The currency 'EG' - Egypt.
   */
  public static final Country EG = of("EG");
  /**
   * The currency 'HK' - Hong Kong.
   */
  public static final Country HK = of("HK");
  /**
   * The country 'ID' - Indonesia.
   */
  public static final Country ID = of("ID");
  /**
   * The country 'IL' - Israel.
   */
  public static final Country IL = of("IL");
  /**
   * The country 'IN' - India.
   */
  public static final Country IN = of("IN");
  /**
   * The country 'JP' - Japan.
   */
  public static final Country JP = of("JP");
  /**
   * The country 'KR' - South Korea.
   */
  public static final Country KR = of("KR");
  /**
   * The country 'MY' - Malaysia.
   */
  public static final Country MY = of("MY");
  /**
   * The country 'NZ' - New Zealand.
   */
  public static final Country NZ = of("NZ");
  /**
   * The currency 'RU' = Russia.
   */
  public static final Country RU = of("RU");
  /**
   * The country 'SA' - Saudi Arabia.
   */
  public static final Country SA = of("SA");
  /**
   * The country 'SG' - Singapore.
   */
  public static final Country SG = of("SG");
  /**
   * The country 'TH' - Thailand.
   */
  public static final Country TH = of("TH");
  /**
   * The country 'ZA' - South Africa.
   */
  public static final Country ZA = of("ZA");

  /**
   * The country code.
   */
  private final String code;

  //-------------------------------------------------------------------------
  /**
   * Obtains the set of available countries.
   * <p>
   * This contains all the countries that have been defined at the point
   * that the method is called.
   *
   * @return an immutable set containing all registered countries
   */
  public static Set<Country> getAvailableCountries() {
    COUNTRY_CODES.get();
    return ImmutableSortedSet.copyOf(CACHE.values());
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified ISO-3166-1 alpha-2
   * two letter country code dynamically creating a country if necessary.
   * <p>
   * A country is uniquely identified by ISO-3166-1 alpha-2 two letter code.
   * This method creates the country if it is not known.
   *
   * @param countryCode  the two letter country code, ASCII and upper case
   * @return the singleton instance
   * @throws IllegalArgumentException if the country code is invalid
   */
  @FromString
  public static Country of(String countryCode) {
    ArgChecker.notNull(countryCode, "countryCode");
    return CACHE.computeIfAbsent(countryCode, c -> addCode(c));
  }

  // add code
  private static Country addCode(String countryCode) {
    ArgChecker.matches(CODE_MATCHER, 2, 2, countryCode, "countryCode", "[A-Z][A-Z]");
    return new Country(countryCode);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a string to obtain a {@code Country}.
   * <p>
   * The parse is identical to {@link #of(String)} except that it will convert
   * letters to upper case first.
   *
   * @param countryCode  the two letter country code, ASCII
   * @return the singleton instance
   * @throws IllegalArgumentException if the country code is invalid
   */
  public static Country parse(String countryCode) {
    ArgChecker.notNull(countryCode, "countryCode");
    return of(countryCode.toUpperCase(Locale.ENGLISH));
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified ISO-3166-1 alpha-3
   * three letter country code dynamically creating a country if necessary.
   * <p>
   * A country is uniquely identified by ISO-3166-1 alpha-3 three letter code.
   * This method creates the country if it is not known.
   *
   * @param countryCode  the three letter country code, ASCII and upper case
   * @return the singleton instance
   * @throws IllegalArgumentException if the country code is invalid
   */
  public static Country of3Char(String countryCode) {
    ArgChecker.matches(CODE_MATCHER, 3, 3, countryCode, "countryCode", "[A-Z][A-Z][A-Z]");
    return Optional.ofNullable(COUNTRY_CODES.get().get(countryCode))
        .orElseThrow(() -> new IllegalArgumentException("Unknown country code: " + countryCode));
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   * 
   * @param code  the two letter country code
   */
  private Country(String code) {
    this.code = code;
  }

  /**
   * Ensure singleton on deserialization.
   * 
   * @return the singleton
   */
  private Object readResolve() {
    return Country.of(code);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the two letter ISO code.
   * 
   * @return the two letter ISO code
   */
  public String getCode() {
    return code;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the ISO-3166-1 alpha-3 three letter country code.
   *
   * @return the three letter country code
   * @throws IllegalArgumentException if the country is invalid
   */
  public String getCode3Char() {
    if (COUNTRY_CODES.get().containsValue(this)) {
      return COUNTRY_CODES.get().inverse().get(this);
    } else {
      throw new IllegalArgumentException("Unknown country: " + this.getCode());
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this country to another.
   * <p>
   * The comparison sorts alphabetically by the two letter country code.
   * 
   * @param other  the other country
   * @return negative if less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(Country other) {
    return code.compareTo(other.code);
  }

  /**
   * Checks if this country equals another country.
   * <p>
   * The comparison checks the two letter country code.
   * 
   * @param obj  the other country, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Country other = (Country) obj;
    return code.equals(other.code);
  }

  /**
   * Returns a suitable hash code for the country.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return code.hashCode();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string representation of the country, which is the two letter code.
   * 
   * @return the two letter country code
   */
  @Override
  @ToString
  public String toString() {
    return code;
  }

}
