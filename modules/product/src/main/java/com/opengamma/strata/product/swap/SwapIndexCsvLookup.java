/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.named.NamedLookup;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

/**
 * Loads standard Swap Index implementations from CSV.
 * <p>
 * See {@link SwapIndices} for the description of each.
 */
final class SwapIndexCsvLookup
    implements NamedLookup<SwapIndex> {

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(SwapIndexCsvLookup.class.getName());
  /**
   * The singleton instance of the lookup.
   */
  public static final SwapIndexCsvLookup INSTANCE = new SwapIndexCsvLookup();

  private static final String NAME_FIELD = "Name";
  private static final String CONVENTION_FIELD = "Convention";
  private static final String TENOR_FIELD = "Tenor";

  /**
   * The cache by name.
   */
  private static final ImmutableMap<String, SwapIndex> BY_NAME = loadFromCsv();

  /**
   * Restricted constructor.
   */
  private SwapIndexCsvLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<String, SwapIndex> lookupAll() {
    return BY_NAME;
  }

  private static ImmutableMap<String, SwapIndex> loadFromCsv() {
    List<ResourceLocator> resources = ResourceConfig.orderedResources("SwapIndexData.csv");
    Map<String, SwapIndex> map = new HashMap<>();
    for (ResourceLocator resource : resources) {
      try {
        CsvFile csv = CsvFile.of(resource.getCharSource(), true);
        for (int i = 0; i < csv.rowCount(); i++) {
          SwapIndex parsed = parseSwapIndex(csv, i);
          map.put(parsed.getName(), parsed);
        }
      } catch (RuntimeException ex) {
        log.log(Level.SEVERE, "Error processing resource as Swap Index CSV file: " + resource, ex);
        return ImmutableMap.of();
      }
    }
    return ImmutableMap.copyOf(map);
  }

  private static SwapIndex parseSwapIndex(CsvFile csv, int row) {
    String name = csv.field(row, NAME_FIELD);
    FixedIborSwapConvention convention = FixedIborSwapConvention.of(csv.field(row, CONVENTION_FIELD));
    Tenor tenor = Tenor.parse(csv.field(row, TENOR_FIELD));
    // build result
    return ImmutableSwapIndex.of(name, FixedIborSwapTemplate.of(tenor, convention));
  }

}
