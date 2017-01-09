/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.SeasonalityDefinition;

/**
 * Loads a set of seasonality definitions into memory by reading from CSV resources.
 * <p>
 * The CSV file has the following header row:<br />
 * {@code Curve Name,Shift Type,Jan-Feb,Feb-Mar,Mar-Apr,Apr-May,May-Jun,Jun-Jul,Jul-Aug,Aug-Sep,Sep-Oct,Oct-Nov,Nov-Dec,Dec-Jan}.
 *
 * <ul>
 *   <li>The 'Curve Name' column is the name of the curve.
 *   <li>The 'Shift Type' column is the type of the shift, "Scaled" (multiplicative) or "Absolute" (additive).
 *   <li>The 'Jan-Feb' and similar columns are the seasonality values month-on-month.
 * </ul>
 */
public final class SeasonalityDefinitionCsvLoader {

  // Column headers
  private static final String CURVE_NAME = "Curve Name";
  private static final String SHIFT_TYPE = "Shift Type";
  private static final ImmutableList<String> MONTH_PAIRS = ImmutableList.of(
      "Jan-Feb",
      "Feb-Mar",
      "Mar-Apr",
      "Apr-May",
      "May-Jun",
      "Jun-Jul",
      "Jul-Aug",
      "Aug-Sep",
      "Sep-Oct",
      "Oct-Nov",
      "Nov-Dec",
      "Dec-Jan");

  //-------------------------------------------------------------------------
  /**
   * Loads the seasonality definition CSV file.
   *
   * @param resource  the seasonality CSV resource
   * @return the map of seasonality definitions
   */
  public static Map<CurveName, SeasonalityDefinition> loadSeasonalityDefinitions(ResourceLocator resource) {
    return parseSeasonalityDefinitions(resource.getCharSource());
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the seasonality definition CSV file.
   *
   * @param charSource  the seasonality CSV character source
   * @return the map of seasonality definitions
   */
  public static Map<CurveName, SeasonalityDefinition> parseSeasonalityDefinitions(CharSource charSource) {
    ImmutableMap.Builder<CurveName, SeasonalityDefinition> builder = ImmutableMap.builder();
    CsvFile csv = CsvFile.of(charSource, true);
    for (CsvRow row : csv.rows()) {
      String curveNameStr = row.getField(CURVE_NAME);
      String shiftTypeStr = row.getField(SHIFT_TYPE);
      DoubleArray values = DoubleArray.of(12, i -> Double.parseDouble(row.getField(MONTH_PAIRS.get(i))));

      CurveName curveName = CurveName.of(curveNameStr);
      ShiftType shiftType = ShiftType.valueOf(shiftTypeStr.toUpperCase(Locale.ENGLISH));
      builder.put(curveName, SeasonalityDefinition.of(values, shiftType));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // this class only has static methods
  private SeasonalityDefinitionCsvLoader() {
  }

}
