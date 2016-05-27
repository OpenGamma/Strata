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
import com.opengamma.strata.basics.index.Index;
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
    Map<CurveName, Set<GroupAndReference>> curveGroups = new HashMap<>();
    CsvFile csv = CsvFile.of(groupsResource.getCharSource(), true);
    for (CsvRow row : csv.rows()) {
      String curveGroupStr = row.getField(GROUPS_NAME);
      String curveTypeStr = row.getField(GROUPS_CURVE_TYPE);
      String referenceStr = row.getField(GROUPS_REFERENCE);
      String curveNameStr = row.getField(GROUPS_CURVE_NAME);

      GroupAndReference gar = createCurveId(CurveGroupName.of(curveGroupStr), curveTypeStr, referenceStr);
      CurveName curveName = CurveName.of(curveNameStr);
      Set<GroupAndReference> curveUses = curveGroups.computeIfAbsent(curveName, k -> new HashSet<>());
      curveUses.add(gar);
    }
    return buildCurveGroups(curveGroups);
  }

  // parses the identifier
  private static GroupAndReference createCurveId(
      CurveGroupName curveGroup,
      String curveTypeStr,
      String referenceStr) {

    // discount and forward curves are supported
    if (FORWARD.equalsIgnoreCase(curveTypeStr.toLowerCase(Locale.ENGLISH))) {
      Index index = LoaderUtils.findIndex(referenceStr);
      return new GroupAndReference(curveGroup, index);

    } else if (DISCOUNT.equalsIgnoreCase(curveTypeStr.toLowerCase(Locale.ENGLISH))) {
      Currency ccy = Currency.of(referenceStr);
      return new GroupAndReference(curveGroup, ccy);

    } else {
      throw new IllegalArgumentException(Messages.format("Unsupported curve type: {}", curveTypeStr));
    }
  }

  /**
   * Builds a list of curve group definitions from the map of curves and their IDs.
   * <p>
   * The curve IDs specify which curve groups each curve belongs to and how it is used in the group, for example
   * as a discount curve for a particular currency or as a forward curve for an index.
   *
   * @param garMap  the map of group-reference pairs
   * @return a map of curve group name to curve group definition built from the curves
   */
  private static ImmutableList<CurveGroupDefinition> buildCurveGroups(
      Map<CurveName, Set<GroupAndReference>> garMap) {

    Multimap<CurveGroupName, CurveGroupEntry> groups = HashMultimap.create();

    for (Map.Entry<CurveName, Set<GroupAndReference>> entry : garMap.entrySet()) {
      CurveName curveName = entry.getKey();
      Set<GroupAndReference> curveIds = entry.getValue();
      Map<CurveGroupName, List<GroupAndReference>> idsByGroup =
          curveIds.stream().collect(groupingBy(p -> p.groupName));

      for (Map.Entry<CurveGroupName, List<GroupAndReference>> groupEntry : idsByGroup.entrySet()) {
        CurveGroupName groupName = groupEntry.getKey();
        List<GroupAndReference> gars = groupEntry.getValue();
        groups.put(groupName, curveGroupEntry(curveName, gars));
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
   * @param gars  the group-reference pairs
   * @return a curve group entry built from the data in the IDs
   */
  private static CurveGroupEntry curveGroupEntry(CurveName curveName, List<GroupAndReference> gars) {
    Set<Currency> currencies = new HashSet<>();
    Set<Index> indices = new HashSet<>();

    for (GroupAndReference gar : gars) {
      if (gar.currency != null) {
        currencies.add(gar.currency);
      } else {
        indices.add(gar.index);
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

  //-------------------------------------------------------------------------
  // data holder
  private static final class GroupAndReference {
    private final CurveGroupName groupName;
    private final Currency currency;
    private final Index index;

    private GroupAndReference(CurveGroupName groupName, Currency currency) {
      this.groupName = groupName;
      this.currency = currency;
      this.index = null;
    }

    private GroupAndReference(CurveGroupName groupName, Index index) {
      this.groupName = groupName;
      this.currency = null;
      this.index = index;
    }
  }

}
