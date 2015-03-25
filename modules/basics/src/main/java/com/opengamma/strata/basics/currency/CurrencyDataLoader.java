/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.collect.io.ResourceLocator;

/**
 * A unit of currency.
 * <p>
 * This class represents a unit of currency such as the British Pound, Euro or US Dollar.
 * The currency is represented by a three letter code, intended to be ISO-4217.
 * <p>
 * Currencies must be defined in configuration before they can be used.
 * The currencies defined as constants have been defined in the standard configuration.
 * <p>
 * This class is immutable and thread-safe.
 */
final class CurrencyDataLoader {

  /**
   * INI file for currency data.
   */
  private static final String CURRENCY_INI = "com/opengamma/basics/currency/Currency.ini";
  /**
   * INI file for currency pair data.
   */
  private static final String PAIR_INI = "com/opengamma/basics/currency/CurrencyPair.ini";
  /**
   * The valid regex for an ISO code.
   */
  private static final Pattern CODE = Pattern.compile("[A-Z]{3}");
  /**
   * The valid regex for an currency pair.
   */
  private static final Pattern PAIR = Pattern.compile("[A-Z]{3}[/][A-Z]{3}");

  // restricted constructor
  private CurrencyDataLoader() {
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the available currencies.
   * 
   * @return the map of known currencies
   */
  static ImmutableMap<String, Currency> loadCurrencies(boolean historic) {
    try {
      IniFile ini = IniFile.ofChained(
          ResourceLocator.streamOfClasspathResources(CURRENCY_INI).map(ResourceLocator::getCharSource));
      return parseCurrencies(ini, historic);

    } catch (RuntimeException ex) {
      // logging used because this is loaded in a static variable
      Logger logger = Logger.getLogger(CurrencyDataLoader.class.getName());
      logger.severe(Throwables.getStackTraceAsString(ex));
      // return an empty instance to avoid ExceptionInInitializerError
      return ImmutableMap.of();
    }
  }

  // parse currency info
  private static ImmutableMap<String, Currency> parseCurrencies(IniFile ini, boolean historic) {
    ImmutableMap.Builder<String, Currency> builder = ImmutableMap.builder();
    for (Entry<String, PropertySet> entry : ini.asMap().entrySet()) {
      String currencyCode = entry.getKey();
      if (CODE.matcher(currencyCode).matches()) {
        PropertySet properties = entry.getValue();
        boolean isHistoric = false;
        if (properties.keys().contains("historic")) {
          isHistoric = Boolean.parseBoolean(properties.getValue("historic"));
        }
        if (isHistoric == historic) {
          Integer minorUnits = Integer.parseInt(properties.getValue("minorUnitDigits"));
          String triangulationCurrency = properties.getValue("triangulationCurrency");
          builder.put(currencyCode, new Currency(currencyCode, minorUnits, triangulationCurrency));
        }
      }
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the available currency pairs.
   * 
   * @return the map of known currency pairs
   */
  static ImmutableMap<CurrencyPair, Integer> loadPairs() {
    try {
      IniFile ini = IniFile.ofChained(
          ResourceLocator.streamOfClasspathResources(PAIR_INI).map(ResourceLocator::getCharSource));
      return parsePairs(ini);

    } catch (RuntimeException ex) {
      // logging used because this is loaded in a static variable
      Logger logger = Logger.getLogger(CurrencyDataLoader.class.getName());
      logger.severe(Throwables.getStackTraceAsString(ex));
      // return an empty instance to avoid ExceptionInInitializerError
      return ImmutableMap.of();
    }
  }

  // parse pair info
  private static ImmutableMap<CurrencyPair, Integer> parsePairs(IniFile ini) {
    ImmutableMap.Builder<CurrencyPair, Integer> builder = ImmutableMap.builder();
    for (Entry<String, PropertySet> entry : ini.asMap().entrySet()) {
      String pairStr = entry.getKey();
      if (PAIR.matcher(pairStr).matches()) {
        CurrencyPair pair = CurrencyPair.parse(pairStr);
        PropertySet properties = entry.getValue();
        Integer rateDigits = Integer.parseInt(properties.getValue("rateDigits"));
        builder.put(pair, rateDigits);
      }
    }
    return builder.build();
  }

}
