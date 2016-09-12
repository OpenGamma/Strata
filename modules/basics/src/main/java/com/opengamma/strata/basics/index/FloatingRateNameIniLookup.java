/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
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
   * The cache by name.
   */
  private static final ImmutableMap<String, FloatingRateName> BY_NAME = loadIndices();

  /**
   * Restricted constructor.
   */
  private FloatingRateNameIniLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<String, FloatingRateName> lookupAll() {
    return BY_NAME;
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the available indices.
   * 
   * @return the map of known indices
   */
  static ImmutableMap<String, FloatingRateName> loadIndices() {
    try {
      IniFile ini = ResourceConfig.combinedIniFile(FLOATING_RATE_NAME_INI);
      return parseIndices(ini);

    } catch (RuntimeException ex) {
      // logging used because this is loaded in a static variable
      log.severe(Throwables.getStackTraceAsString(ex));
      // return an empty instance to avoid ExceptionInInitializerError
      return ImmutableMap.of();
    }
  }

  // parse the config file FloatingRateName.ini
  private static ImmutableMap<String, FloatingRateName> parseIndices(IniFile ini) {
    ImmutableMap.Builder<String, FloatingRateName> builder = ImmutableMap.builder();
    parseSection(ini.section("ibor"), "-", FloatingRateType.IBOR, builder);
    parseSection(ini.section("overnightCompounded"), "", FloatingRateType.OVERNIGHT_COMPOUNDED, builder);
    parseSection(ini.section("overnightAveraged"), "", FloatingRateType.OVERNIGHT_AVERAGED, builder);
    parseSection(ini.section("price"), "", FloatingRateType.PRICE, builder);
    return builder.build();
  }

  // parse a single section
  private static void parseSection(
      PropertySet section,
      String indexNameSuffix,
      FloatingRateType type,
      ImmutableMap.Builder<String, FloatingRateName> builder) {

    // find our names from the RHS of the key/value pairs
    for (String key : section.keys()) {
      builder.put(key, ImmutableFloatingRateName.of(key, section.value(key) + indexNameSuffix, type));
    }
  }

}
