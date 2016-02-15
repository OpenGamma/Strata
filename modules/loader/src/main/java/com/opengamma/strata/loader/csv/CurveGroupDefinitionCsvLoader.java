/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.util.stream.Collectors.groupingBy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.id.CurveId;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.IborIndexCurveId;
import com.opengamma.strata.market.id.IndexCurveId;
import com.opengamma.strata.market.id.OvernightIndexCurveId;
import com.opengamma.strata.market.id.PriceIndexCurveId;

/**
 * Loads a set of curve group definitions into memory by reading from CSV resources.
 * <p>
 * The CSV file has the following header row:<br />
 * {@code Group Name, Curve Type, Reference, Curve Name}.
 *
 * <ul>
 *   <li>The 'Group Name' column is the name of the group of curves.
 *   <li>The 'Curve Type' column is the type of the curve, "forward" or "discount".
 *   <li>The 'Reference' column is the reference the curve is used for, such as "USD" or "USD-LIBOR-3M".
 *   <li>The 'Curve Name' column is the name of the curve.
 * </ul>
 *
 * @see CurveGroupDefinition
 */
public final class CurveGroupDefinitionCsvLoader {

  // Column headers
  private static final String GROUPS_NAME = "Group Name";
  private static final String GROUPS_CURVE_TYPE = "Curve Type";
  private static final String GROUPS_REFERENCE = "Reference";
  private static final String GROUPS_CURVE_NAME = "Curve Name";

  /** Name used in the reference column of the CSV file for discount curves. */
  private static final String DISCOUNT = "discount";

  /** Name used in the reference column of the CSV file for forward curves. */
  private static final String FORWARD = "forward";

  /**
   * Loads the curve groups definition CSV file.
   *
   * @param groupsResource  the curve groups CSV resource
   * @return the set of IDs specifying how each curve is used, keyed by the name of the curve
   */
  public static List<CurveGroupDefinition> loadCurveGroups(ResourceLocator groupsResource) {
    Map<CurveName, Set<CurveId>> curveGroups = new HashMap<>();
    CsvFile csv = CsvFile.of(groupsResource.getCharSource(), true);
    for (CsvRow row : csv.rows()) {
      String curveGroupStr = row.getField(GROUPS_NAME);
      String curveTypeStr = row.getField(GROUPS_CURVE_TYPE);
      String referenceStr = row.getField(GROUPS_REFERENCE);
      String curveNameStr = row.getField(GROUPS_CURVE_NAME);

      CurveId curveId = createCurveId(CurveGroupName.of(curveGroupStr), curveTypeStr, referenceStr);
      CurveName curveName = CurveName.of(curveNameStr);
      Set<CurveId> curveUses = curveGroups.computeIfAbsent(curveName, k -> new HashSet<>());
      curveUses.add(curveId);
    }
    return buildCurveGroups(curveGroups);
  }

  // parses the identifier
  private static CurveId createCurveId(CurveGroupName curveGroup, String curveTypeStr, String referenceStr) {
    // discount and forward curves are supported
    if (FORWARD.equalsIgnoreCase(curveTypeStr.toLowerCase(Locale.ENGLISH))) {
      Index index = LoaderUtils.findIndex(referenceStr);
      return createCurveId(index, curveGroup);

    } else if (DISCOUNT.equalsIgnoreCase(curveTypeStr.toLowerCase(Locale.ENGLISH))) {
      Currency ccy = Currency.of(referenceStr);
      return DiscountCurveId.of(ccy, curveGroup);

    } else {
      throw new IllegalArgumentException(Messages.format("Unsupported curve type: {}", curveTypeStr));
    }
  }

  // creates a forward curve id
  private static CurveId createCurveId(Index index, CurveGroupName curveGroup) {
    if (index instanceof IborIndex) {
      return IborIndexCurveId.of((IborIndex) index, curveGroup);
    } else if (index instanceof OvernightIndex) {
      return OvernightIndexCurveId.of((OvernightIndex) index, curveGroup);
    } else if (index instanceof PriceIndex) {
      return PriceIndexCurveId.of((PriceIndex) index, curveGroup);
    }
    throw new IllegalArgumentException("Unexpected index type " + index.getClass().getName());
  }

  /**
   * Builds a list of curve group definitions from the map of curves and their IDs.
   * <p>
   * The curve IDs specify which curve groups each curve belongs to and how it is used in the group, for example
   * as a discount curve for a particular currency or as a forward curve for an index.
   *
   * @param curves  curve IDs keyed by the name of the curve
   * @return a map of curve group name to curve group definition built from the curves
   */
  private static ImmutableList<CurveGroupDefinition> buildCurveGroups(Map<CurveName, Set<CurveId>> curves) {
    Multimap<CurveGroupName, CurveGroupEntry> groups = HashMultimap.create();

    for (Map.Entry<CurveName, Set<CurveId>> entry : curves.entrySet()) {
      CurveName curveName = entry.getKey();
      Set<CurveId> curveIds = entry.getValue();
      Map<CurveGroupName, List<CurveId>> idsByGroup = curveIds.stream().collect(groupingBy(CurveId::getCurveGroupName));

      for (Map.Entry<CurveGroupName, List<CurveId>> groupEntry : idsByGroup.entrySet()) {
        CurveGroupName groupName = groupEntry.getKey();
        List<CurveId> groupIds = groupEntry.getValue();
        groups.put(groupName, curveGroupEntry(curveName, groupIds));
      }
    }
    return MapStream.of(groups.asMap())
        .map((name, entry) -> CurveGroupDefinition.of(name, entry, ImmutableList.of()))
        .collect(toImmutableList());
  }

  /**
   * Creates a curve group entry for a curve from a list of the curve's IDs from the same curve group.
   *
   * @param curveName  the name of the curve
   * @param ids  curve IDs from the same curve group
   * @return a curve group entry built from the data in the IDs
   */
  private static CurveGroupEntry curveGroupEntry(CurveName curveName, List<CurveId> ids) {
    Set<Currency> currencies = new HashSet<>();
    Set<Index> indices = new HashSet<>();

    for (CurveId id : ids) {
      if (id instanceof DiscountCurveId) {
        currencies.add(((DiscountCurveId) id).getCurrency());
      } else if (id instanceof IndexCurveId) {
        indices.add(((IndexCurveId) id).getIndex());
      } else {
        throw new IllegalArgumentException("Unexpected curve ID type " + id.getClass().getName());
      }
    }
    return CurveGroupEntry.builder()
        .curveName(curveName)
        .discountCurrencies(currencies)
        .indices(indices)
        .build();
  }

  // This class only has static methods
  private CurveGroupDefinitionCsvLoader() {
  }
}
