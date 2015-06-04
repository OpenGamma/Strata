/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.util.Map.Entry;
import java.util.logging.Logger;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.collect.io.ResourceLocator;

/**
 * Internal loader of currency and currency pair data.
 * <p>
 * This loads configuration files for {@link Currency} and {@link CurrencyPair}.
 */
final class CurrencyDataLoader {

  /**
   * INI file for currency data.
   */
  private static final String CURRENCY_INI = "com/opengamma/strata/basics/currency/Currency.ini";
  /**
   * INI file for currency pair data.
   */
  private static final String PAIR_INI = "com/opengamma/strata/basics/currency/CurrencyPair.ini";

  // restricted constructor
  private CurrencyDataLoader() {
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the available currencies.
   * 
   * @param loadHistoric  whether to load the historic or active currencies
   * @return the map of known currencies
   */
  static ImmutableMap<String, Currency> loadCurrencies(boolean loadHistoric) {
    try {
      IniFile ini = IniFile.ofChained(
          ResourceLocator.streamOfClasspathResources(CURRENCY_INI).map(ResourceLocator::getCharSource));
      return parseCurrencies(ini, loadHistoric);

    } catch (RuntimeException ex) {
      // logging used because this is loaded in a static variable
      Logger logger = Logger.getLogger(CurrencyDataLoader.class.getName());
      logger.severe(Throwables.getStackTraceAsString(ex));
      // return an empty instance to avoid ExceptionInInitializerError
      return ImmutableMap.of();
    }
  }

  // parse currency info
  private static ImmutableMap<String, Currency> parseCurrencies(IniFile ini, boolean loadHistoric) {
    ImmutableMap.Builder<String, Currency> builder = ImmutableMap.builder();
    for (Entry<String, PropertySet> entry : ini.asMap().entrySet()) {
      String currencyCode = entry.getKey();
      if (Currency.REGEX_FORMAT.matcher(currencyCode).matches()) {
        PropertySet properties = entry.getValue();
        boolean isHistoric =
            (properties.keys().contains("historic") && Boolean.parseBoolean(properties.getValue("historic")));
        if (isHistoric == loadHistoric) {
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
   * @return the map of known currency pairs, where the value is the number of digits in the rate
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
      if (CurrencyPair.REGEX_FORMAT.matcher(pairStr).matches()) {
        CurrencyPair pair = CurrencyPair.parse(pairStr);
        PropertySet properties = entry.getValue();
        Integer rateDigits = Integer.parseInt(properties.getValue("rateDigits"));
        builder.put(pair, rateDigits);
      }
    }
    return builder.build();
  }

}
