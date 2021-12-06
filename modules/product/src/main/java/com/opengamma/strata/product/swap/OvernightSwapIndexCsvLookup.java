/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.named.NamedLookup;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapConvention;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;

/**
 * Loads standard Overnight Swap Index implementations from CSV.
 * <p>
 * See {@link OvernightSwapIndices} for the description of each.
 */
final class OvernightSwapIndexCsvLookup
    implements NamedLookup<OvernightSwapIndex> {

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(OvernightSwapIndexCsvLookup.class.getName());
  /**
   * The singleton instance of the lookup.
   */
  public static final OvernightSwapIndexCsvLookup INSTANCE = new OvernightSwapIndexCsvLookup();

  private static final String NAME_FIELD = "Name";
  private static final String ACTIVE_FIELD = "Active";
  private static final String CONVENTION_FIELD = "Convention";
  private static final String TENOR_FIELD = "Tenor";
  private static final String FIXING_TIME_FIELD = "FixingTime";
  private static final String FIXING_ZONE_FIELD = "FixingZone";

  /**
   * The time formatter.
   */
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH[:mm]", Locale.ENGLISH);
  /**
   * The cache by name.
   */
  private static final ImmutableMap<String, OvernightSwapIndex> BY_NAME = loadFromCsv();

  /**
   * Restricted constructor.
   */
  private OvernightSwapIndexCsvLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<String, OvernightSwapIndex> lookupAll() {
    return BY_NAME;
  }

  private static ImmutableMap<String, OvernightSwapIndex> loadFromCsv() {
    List<ResourceLocator> resources = ResourceConfig.orderedResources("OvernightSwapIndexData.csv");
    Map<String, OvernightSwapIndex> map = new HashMap<>();
    for (ResourceLocator resource : resources) {
      try {
        CsvFile csv = CsvFile.of(resource.getCharSource(), true);
        for (CsvRow row : csv.rows()) {
          OvernightSwapIndex parsed = parseSwapIndex(row);
          map.put(parsed.getName(), parsed);
          map.putIfAbsent(parsed.getName().toUpperCase(Locale.ENGLISH), parsed);
        }
      } catch (RuntimeException ex) {
        log.log(Level.SEVERE, "Error processing resource as Swap Index CSV file: " + resource, ex);
        return ImmutableMap.of();
      }
    }
    return ImmutableMap.copyOf(map);
  }

  private static OvernightSwapIndex parseSwapIndex(CsvRow row) {
    String name = row.getField(NAME_FIELD);
    boolean active = Boolean.parseBoolean(row.getField(ACTIVE_FIELD));
    FixedOvernightSwapConvention convention = FixedOvernightSwapConvention.of(row.getField(CONVENTION_FIELD));
    Tenor tenor = Tenor.parse(row.getField(TENOR_FIELD));
    LocalTime time = LocalTime.parse(row.getField(FIXING_TIME_FIELD), TIME_FORMAT);
    ZoneId zoneId = ZoneId.of(row.getField(FIXING_ZONE_FIELD));
    // build result
    return ImmutableOvernightSwapIndex.builder()
        .name(name)
        .active(active)
        .fixingTime(time)
        .fixingZone(zoneId)
        .template(FixedOvernightSwapTemplate.of(tenor, convention))
        .build();
  }

}
