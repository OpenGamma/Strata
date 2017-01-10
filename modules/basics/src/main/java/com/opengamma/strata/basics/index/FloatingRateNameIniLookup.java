/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Loads standard floating rate names from INI.
 */
final class FloatingRateNameIniLookup
    implements NamedLookup<FloatingRateName> {

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(FloatingRateNameIniLookup.class.getName());
  /**
   * The singleton instance of the lookup.
   */
  public static final FloatingRateNameIniLookup INSTANCE = new FloatingRateNameIniLookup();

  /**
   * INI file for floating rate names.
   */
  private static final String FLOATING_RATE_NAME_INI = "FloatingRateNameData.ini";

  /**
   * Restricted constructor.
   */
  private FloatingRateNameIniLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<String, FloatingRateName> lookupAll() {
    return Loader.INSTANCE.names;
  }

  // finds a default
  FloatingRateName defaultIborIndex(Currency currency) {
    FloatingRateName frname = Loader.INSTANCE.iborDefaults.get(currency);
    if (frname == null) {
      throw new IllegalArgumentException("No default Ibor index for currency " + currency);
    }
    return frname;
  }

  // finds a default
  FloatingRateName defaultOvernightIndex(Currency currency) {
    FloatingRateName frname = Loader.INSTANCE.overnightDefaults.get(currency);
    if (frname == null) {
      throw new IllegalArgumentException("No default Overnight index for currency " + currency);
    }
    return frname;
  }

  //-------------------------------------------------------------------------
  static class Loader {
    /** Instance. */
    private static final Loader INSTANCE = new Loader();

    /** The cache by name. */
    private final ImmutableMap<String, FloatingRateName> names;
    /** The Ibor defaults by currency. */
    private final ImmutableMap<Currency, FloatingRateName> iborDefaults;
    /** The Overnight defaults by currency. */
    private final ImmutableMap<Currency, FloatingRateName> overnightDefaults;

    //-------------------------------------------------------------------------
    Loader() {
      ImmutableMap<String, FloatingRateName> names = ImmutableMap.of();
      ImmutableMap<Currency, FloatingRateName> iborDefaults = ImmutableMap.of();
      ImmutableMap<Currency, FloatingRateName> overnightDefaults = ImmutableMap.of();
      try {
        IniFile ini = ResourceConfig.combinedIniFile(FLOATING_RATE_NAME_INI);
        names = parseIndices(ini);
        iborDefaults = parseIborDefaults(ini, names);
        overnightDefaults = parseOvernightDefaults(ini, names);

      } catch (RuntimeException ex) {
        // logging used because this is loaded in a static variable
        log.severe(Throwables.getStackTraceAsString(ex));
        // return an empty instance to avoid ExceptionInInitializerError
      }
      this.names = names;
      this.iborDefaults = iborDefaults;
      this.overnightDefaults = overnightDefaults;
    }

    // parse the config file FloatingRateName.ini
    private static ImmutableMap<String, FloatingRateName> parseIndices(IniFile ini) {
      HashMap<String, FloatingRateName> map = new HashMap<>();
      parseSection(ini.section("ibor"), "-", FloatingRateType.IBOR, map);
      parseSection(ini.section("overnightCompounded"), "", FloatingRateType.OVERNIGHT_COMPOUNDED, map);
      parseSection(ini.section("overnightAveraged"), "", FloatingRateType.OVERNIGHT_AVERAGED, map);
      parseSection(ini.section("price"), "", FloatingRateType.PRICE, map);
      return ImmutableMap.copyOf(map);
    }

    // parse a single section
    private static void parseSection(
        PropertySet section,
        String indexNameSuffix,
        FloatingRateType type,
        HashMap<String, FloatingRateName> mutableMap) {

      // find our names from the RHS of the key/value pairs
      for (String key : section.keys()) {
        ImmutableFloatingRateName name = ImmutableFloatingRateName.of(key, section.value(key) + indexNameSuffix, type);
        mutableMap.put(key, name);
        mutableMap.putIfAbsent(key.toUpperCase(Locale.ENGLISH), name);
      }
    }

    //-------------------------------------------------------------------------
    // load currency defaults
    private static ImmutableMap<Currency, FloatingRateName> parseIborDefaults(
        IniFile ini,
        ImmutableMap<String, FloatingRateName> names) {

      ImmutableMap.Builder<Currency, FloatingRateName> map = ImmutableMap.builder();
      PropertySet section = ini.section("currencyDefaultIbor");
      for (String key : section.keys()) {
        FloatingRateName frname = names.get(section.value(key));
        if (frname == null) {
          throw new IllegalArgumentException("Invalid default Ibor index for currency " + key);
        }
        map.put(Currency.of(key), frname);
      }
      return map.build();
    }

    //-------------------------------------------------------------------------
    // load currency defaults
    private static ImmutableMap<Currency, FloatingRateName> parseOvernightDefaults(
        IniFile ini,
        ImmutableMap<String, FloatingRateName> names) {

      ImmutableMap.Builder<Currency, FloatingRateName> map = ImmutableMap.builder();
      PropertySet section = ini.section("currencyDefaultOvernight");
      for (String key : section.keys()) {
        FloatingRateName frname = names.get(section.value(key));
        if (frname == null) {
          throw new IllegalArgumentException("Invalid default Overnight index for currency " + key);
        }
        map.put(Currency.of(key), frname);
      }
      return map.build();
    }
  }
}
