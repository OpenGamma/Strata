/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;

/**
 * Simple class to read in a csv file with ISDA inputs load them into a test harness
 *
 * Could extend in a more generic way but left simple for now.
 */
public class IsdaModelDatasetsSheetReader extends IsdaModelDatasets {

  private static final String SHEET_LOCATION = "isda_comparison_sheets/";
  private final List<ISDA_Results> results = new ArrayList<>(100); // ~100 rows nominally
  private CsvFile csvFile;
  private String[] headers;

  // header fields we expect in the file (lowercased when loaded)
  private static final String TODAY_HEADER = "today";
  @SuppressWarnings("unused")
  private static final String CURVE_INSTRUMENT_START_DATE = "curve instrument start date";
  private static final String START_DATE_HEADER = "start date";
  private static final String END_DATE_HEADER = "end date";
  private static final String SPREAD_HEADER = "spread";
  @SuppressWarnings("unused")
  private static final String CLEAN_PRICE_HEADER = "clean price";
  @SuppressWarnings("unused")
  private static final String CLEAN_PRICE_NOACC_HEADER = "clean price (no acc on default)";
  @SuppressWarnings("unused")
  private static final String DIRTY_PRICE_NOACC_HEADER = "dirty price (no acc on default)";
  private static final String PREMIUM_LEG_HEADER = "premium leg";
  private static final String PROTECTION_LEG_HEADER = "protection leg";
  private static final String DEFAULT_ACC_HEADER = "default acc";
  private static final String ACC_PREMIUM_HEADER = "accrued premium";
  private static final String ACC_DAYS_HEADER = "accrued days";

  private static final DateTimeFormatter DATE_TIME_PARSER = DateTimeFormatter.ofPattern("dd-MMM-uu", Locale.ENGLISH);

  // component parts of the resultant ISDA_Results instances
  private LocalDate[] _parSpreadDates; // assume in ascending order
  private ZonedDateTime[] _curveTenors;

  /**
   * Load specified sheet.
   *
   * @param sheetName the sheet name
   *  @param recoveryRate the recovery rate 
   * @return at set of ISDA results
   */
  public static ISDA_Results[] loadSheet(final String sheetName, final double recoveryRate) {
    return new IsdaModelDatasetsSheetReader(sheetName, recoveryRate).getResults();
  }

  /**
   * Load specified sheet.
   *
   * @param sheetName the sheet name
   * @param recoveryRate the recovery rate 
   */
  public IsdaModelDatasetsSheetReader(final String sheetName, final double recoveryRate) {
    ArgChecker.notEmpty(sheetName, "filename");

    // Set up CSV reader
    String sheetFilePath = SHEET_LOCATION + sheetName;
    URL resource = IsdaModelDatasetsSheetReader.class.getClassLoader().getResource(sheetFilePath);
    if (resource == null) {
      throw new IllegalArgumentException(sheetFilePath + ": does not exist");
    }
    csvFile = CsvFile.of(Resources.asCharSource(resource, StandardCharsets.UTF_8), true);

    // Set columns
    headers = readHeaderRow();

    for (CsvRow row : csvFile.rows()) {
      ISDA_Results temp = getResult(parseRow(row));
      temp.recoveryRate = recoveryRate;
      results.add(temp);
    }
  }

  private ISDA_Results getResult(Map<String, String> fields) {
    final ISDA_Results result = new ISDA_Results();

    result.today = getLocalDate(TODAY_HEADER, fields);
    result.startDate = getLocalDate(START_DATE_HEADER, fields);
    result.endDate = getLocalDate(END_DATE_HEADER, fields);
    result.protectionLeg = getDouble(PROTECTION_LEG_HEADER, fields);
    result.premiumLeg = getDouble(PREMIUM_LEG_HEADER, fields);
    result.defaultAcc = getDouble(DEFAULT_ACC_HEADER, fields);
    result.accruedPremium = getDouble(ACC_PREMIUM_HEADER, fields);
    result.accruedDays = new Double(getDouble(ACC_DAYS_HEADER, fields)).intValue();
    result.fracSpread = getDouble(SPREAD_HEADER, fields) / 10000;

    result.creditCurve = getCreditCurve(fields, result.today);

    return result;

  }

  private double getDouble(final String field, final Map<String, String> fields) {
    if (fields.containsKey(field)) {
      return Double.valueOf(fields.get(field));
    }
    throw new IllegalArgumentException(field + " not present in sheet row, got " + fields);
  }

  private LocalDate getLocalDate(final String field, final Map<String, String> fields) {
    if (fields.containsKey(field)) {
      return LocalDate.parse(fields.get(field), DATE_TIME_PARSER);
    }
    throw new IllegalArgumentException(field + " not present in sheet row, got " + fields);
  }

  private IsdaCompliantDateCreditCurve getCreditCurve(final Map<String, String> fields, final LocalDate today) {
    // load the curve dates from the inputs
    final int nCurvePoints = _parSpreadDates.length;
    final double[] negLogP = new double[nCurvePoints];
    for (int i = 0; i < nCurvePoints; i++) {
      negLogP[i] = getDouble(_parSpreadDates[i].format(DATE_TIME_PARSER), fields);
    }

    final double[] t = new double[nCurvePoints];
    final double[] r = new double[nCurvePoints];
    for (int j = 0; j < nCurvePoints; j++) {
      t[j] = ACT365.yearFraction(today, _parSpreadDates[j]);
      r[j] = negLogP[j] / t[j];
    }
    return new IsdaCompliantDateCreditCurve(today, _parSpreadDates, r, ACT365);
  }

  public ISDA_Results[] getResults() {
    return results.toArray(new ISDA_Results[results.size()]);
  }

  private String[] readHeaderRow() {
    // Read in the header row
    ImmutableList<String> rawRow = csvFile.headers();
    final List<LocalDate> parSpreadDates = new ArrayList<>();

    // Normalise read-in headers (to lower case) and set as columns
    String[] columns = new String[rawRow.size()];
    for (int i = 0; i < rawRow.size(); i++) {
      columns[i] = rawRow.get(i).trim();

      // if a date add to list of spread dates
      try {
        final LocalDate date = LocalDate.parse(columns[i], DATE_TIME_PARSER);
        parSpreadDates.add(date);
        continue;
      } catch (Exception ex) {
        columns[i] = columns[i].toLowerCase(Locale.ENGLISH); // lowercase non dates
      }
    }
    _parSpreadDates = parSpreadDates.toArray(new LocalDate[parSpreadDates.size()]);
    _curveTenors = new ZonedDateTime[_parSpreadDates.length];
    for (int j = 0; j < _parSpreadDates.length; j++) {
      _curveTenors[j] = ZonedDateTime.of(_parSpreadDates[j], LOCAL_TIME, TIME_ZONE);
    }
    ArgChecker.notEmpty(_parSpreadDates, "par spread dates");
    ArgChecker.notEmpty(_curveTenors, "curve tenors");
    return columns;
  }

  // map row onto expected columns
  private Map<String, String> parseRow(CsvRow rawRow) {
    Map<String, String> result = new HashMap<>();
    for (int i = 0; i < headers.length; i++) {
      if (i >= rawRow.fieldCount()) {
        break;
      }
      if (rawRow.field(i) != null && rawRow.field(i).trim().length() > 0) {
        result.put(headers[i], rawRow.field(i));
      }
    }
    return result;
  }

}
