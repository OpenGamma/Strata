/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.util.stream.Collectors.groupingBy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.NodalCurveDefinition;

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
  private static final ImmutableList<String> HEADERS = ImmutableList.of(
      GROUPS_NAME, GROUPS_CURVE_TYPE, GROUPS_REFERENCE, GROUPS_CURVE_NAME);

  /** Name used in the reference column of the CSV file for discount curves. */
  private static final String DISCOUNT = "discount";

  /** Name used in the reference column of the CSV file for forward curves. */
  private static final String FORWARD = "forward";

  //-------------------------------------------------------------------------
  /**
   * Loads the curve groups definition CSV file.
   * <p>
   * The list of {@link NodalCurveDefinition} will be empty in the resulting definition.
   *
   * @param groupsResource  the curve groups CSV resource
   * @return the list of definitions
   * @deprecated Use better named {@link #loadCurveGroupDefinitions(ResourceLocator)}
   */
  @Deprecated
  public static List<CurveGroupDefinition> loadCurveGroups(ResourceLocator groupsResource) {
    return loadCurveGroupDefinitions(groupsResource);
  }

  /**
   * Loads the curve groups definition CSV file.
   * <p>
   * The list of {@link NodalCurveDefinition} will be empty in the resulting definition.
   *
   * @param groupsResource  the curve groups CSV resource
   * @return the list of definitions
   */
  public static List<CurveGroupDefinition> loadCurveGroupDefinitions(ResourceLocator groupsResource) {
    return parseCurveGroupDefinitions(groupsResource.getCharSource());
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the curve groups definition CSV file.
   * <p>
   * The list of {@link NodalCurveDefinition} will be empty in the resulting definition.
   *
   * @param groupsCharSource  the curve groups CSV character source
   * @return the list of definitions
   */
  public static List<CurveGroupDefinition> parseCurveGroupDefinitions(CharSource groupsCharSource) {
    Map<CurveName, Set<GroupAndReference>> curveGroups = new LinkedHashMap<>();
    CsvFile csv = CsvFile.of(groupsCharSource, true);
    for (CsvRow row : csv.rows()) {
      String curveGroupStr = row.getField(GROUPS_NAME);
      String curveTypeStr = row.getField(GROUPS_CURVE_TYPE);
      String referenceStr = row.getField(GROUPS_REFERENCE);
      String curveNameStr = row.getField(GROUPS_CURVE_NAME);

      GroupAndReference gar = createKey(CurveGroupName.of(curveGroupStr), curveTypeStr, referenceStr);
      CurveName curveName = CurveName.of(curveNameStr);
      curveGroups.computeIfAbsent(curveName, k -> new LinkedHashSet<>()).add(gar);
    }
    return buildCurveGroups(curveGroups);
  }

  //-------------------------------------------------------------------------
  // parses the identifier
  private static GroupAndReference createKey(
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
   * Builds a list of curve group definitions from the map of curves and their keys.
   * <p>
   * The keys specify which curve groups each curve belongs to and how it is used in the group, for example
   * as a discount curve for a particular currency or as a forward curve for an index.
   *
   * @param garMap  the map of name to keys
   * @return a map of curve group name to curve group definition built from the curves
   */
  private static ImmutableList<CurveGroupDefinition> buildCurveGroups(
      Map<CurveName, Set<GroupAndReference>> garMap) {

    Multimap<CurveGroupName, CurveGroupEntry> groups = LinkedHashMultimap.create();

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
   * Creates a curve group entry for a curve from a list of keys from the same curve group.
   *
   * @param curveName  the name of the curve
   * @param gars  the group-reference pairs
   * @return a curve group entry built from the data in the IDs
   */
  private static CurveGroupEntry curveGroupEntry(CurveName curveName, List<GroupAndReference> gars) {
    Set<Currency> currencies = new LinkedHashSet<>();
    Set<Index> indices = new LinkedHashSet<>();

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

  //-------------------------------------------------------------------------
  /**
   * Writes the curve groups definition in a CSV format to a file.
   * 
   * @param file  the destination for the CSV, such as a file
   * @param groups  the curve groups
   */
  public static void writeCurveGroupDefinition(File file, CurveGroupDefinition... groups) {
    try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
      writeCurveGroupDefinition(writer, groups);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Writes the curve groups definition in a CSV format to an appendable.
   * 
   * @param underlying  the underlying appendable destination
   * @param groups  the curve groups
   */
  public static void writeCurveGroupDefinition(Appendable underlying, CurveGroupDefinition... groups) {
    CsvOutput csv = new CsvOutput(underlying);
    csv.writeLine(HEADERS);
    for (CurveGroupDefinition group : groups) {
      writeCurveGroupDefinition(csv, group);
    }
  }

  // write a single group definition to CSV
  private static void writeCurveGroupDefinition(CsvOutput csv, CurveGroupDefinition group) {
    String groupName = group.getName().getName();
    for (CurveGroupEntry entry : group.getEntries()) {
      for (Currency currency : entry.getDiscountCurrencies()) {
        csv.writeLine(ImmutableList.of(groupName, DISCOUNT, currency.toString(), entry.getCurveName().getName()));
      }
      for (Index index : entry.getIndices()) {
        csv.writeLine(ImmutableList.of(groupName, FORWARD, index.toString(), entry.getCurveName().getName()));
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Writes the curve group in a CSV format to a file.
   * 
   * @param file  the file
   * @param groups  the curve groups
   */
  public static void writeCurveGroup(File file, CurveGroup... groups) {
    try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
      writeCurveGroup(writer, groups);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Writes the curve group in a CSV format to an appendable.
   * 
   * @param underlying  the underlying appendable destination
   * @param groups  the curve groups
   */
  public static void writeCurveGroup(Appendable underlying, CurveGroup... groups) {
    CsvOutput csv = new CsvOutput(underlying);
    csv.writeLine(HEADERS);
    for (CurveGroup group : groups) {
      writeCurveGroup(csv, group);
    }
  }

  // write a single group to CSV
  private static void writeCurveGroup(CsvOutput csv, CurveGroup group) {
    String groupName = group.getName().getName();
    Map<Currency, Curve> discountingCurves = group.getDiscountCurves();
    for (Entry<Currency, Curve> entry : discountingCurves.entrySet()) {
      List<String> line = new ArrayList<>(4);
      line.add(groupName);
      line.add(DISCOUNT);
      line.add(entry.getKey().toString());
      line.add(entry.getValue().getName().getName());
      csv.writeLine(line);
    }
    Map<Index, Curve> forwardCurves = group.getForwardCurves();
    for (Entry<Index, Curve> entry : forwardCurves.entrySet()) {
      List<String> line = new ArrayList<>(4);
      line.add(groupName);
      line.add(FORWARD);
      line.add(entry.getKey().toString());
      line.add(entry.getValue().getName().getName());
      csv.writeLine(line);
    }
  }

  //-------------------------------------------------------------------------
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
