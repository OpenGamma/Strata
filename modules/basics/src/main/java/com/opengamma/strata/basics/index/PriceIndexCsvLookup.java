/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.location.Country;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Loads standard Price Index implementations from CSV.
 * <p>
 * See {@link PriceIndices} for the description of each.
 */
final class PriceIndexCsvLookup
    implements NamedLookup<PriceIndex> {

  // http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(PriceIndexCsvLookup.class.getName());
  /**
   * The singleton instance of the lookup.
   */
  public static final PriceIndexCsvLookup INSTANCE = new PriceIndexCsvLookup();

  // CSV column headers
  private static final String NAME_FIELD = "Name";
  private static final String CURRENCY_FIELD = "Currency";
  private static final String COUNTRY_FIELD = "Country";
  private static final String ACTIVE_FIELD = "Active";
  private static final String PUBLICATION_FREQUENCY_FIELD = "Publication Frequency";

  /**
   * The cache by name.
   */
  private static final ImmutableMap<String, PriceIndex> BY_NAME = loadFromCsv();

  /**
   * Restricted constructor.
   */
  private PriceIndexCsvLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<String, PriceIndex> lookupAll() {
    return BY_NAME;
  }

  private static ImmutableMap<String, PriceIndex> loadFromCsv() {
    List<ResourceLocator> resources = ResourceConfig.orderedResources("PriceIndexData.csv");
    Map<String, PriceIndex> map = new HashMap<>();
    for (ResourceLocator resource : resources) {
      try {
        CsvFile csv = CsvFile.of(resource.getCharSource(), true);
        for (CsvRow row : csv.rows()) {
          PriceIndex parsed = parsePriceIndex(row);
          map.put(parsed.getName(), parsed);
          map.putIfAbsent(parsed.getName().toUpperCase(Locale.ENGLISH), parsed);
        }
      } catch (RuntimeException ex) {
        log.log(Level.SEVERE, "Error processing resource as Price Index CSV file: " + resource, ex);
        return ImmutableMap.of();
      }
    }
    return ImmutableMap.copyOf(map);
  }

  private static PriceIndex parsePriceIndex(CsvRow row) {
    String name = row.getField(NAME_FIELD);
    Currency currency = Currency.parse(row.getField(CURRENCY_FIELD));
    Country region = Country.of(row.getField(COUNTRY_FIELD));
    boolean active = Boolean.parseBoolean(row.getField(ACTIVE_FIELD));
    Frequency frequency = Frequency.parse(row.getField(PUBLICATION_FREQUENCY_FIELD));
    // build result
    return ImmutablePriceIndex.builder()
        .name(name)
        .currency(currency)
        .region(region)
        .active(active)
        .publicationFrequency(frequency)
        .build();
  }

}
