/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata.credit.markit;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.engine.marketdata.MarketEnvironmentBuilder;
import com.opengamma.strata.finance.credit.IndexReferenceInformation;
import com.opengamma.strata.finance.credit.type.CdsConvention;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.id.IsdaIndexCreditCurveParRatesId;
import com.opengamma.strata.market.id.IsdaIndexRecoveryRateId;
import com.opengamma.strata.market.value.CdsRecoveryRate;

/**
 * Parser to load daily index curve information provided by Markit.
 * <p>
 * The columns are defined as {@code
 * Date,Name,Series,Version,Term,
 * RED Code,Index ID,Maturity,On The Run,Composite Price,
 * Composite Spread,Model Price,Model Spread,Depth,Heat}.
 * <p>
 * Also reads static data from a csv file
 * <p>
 * RedCode,From Date,Convention,Recovery Rate,Index Factor
 */
public class MarkitIndexCreditCurveDataParser {

  // Markit date format with the month in full caps. e.g. 11-JUL-14
  private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
      .parseCaseInsensitive().appendPattern("dd-MMM-yy").toFormatter(Locale.ENGLISH);

  enum Columns {

    Series("Series"),
    Version("Version"),
    Term("Term"),
    RedCode("RED Code"),
    Maturity("Maturity"),
    CompositeSpread("Composite Spread"),
    ModelSpread("Model Spread");

    private final String columnName;

    Columns(String columnName) {
      this.columnName = columnName;
    }

    public String getColumnName() {
      return columnName;
    }
  }

  /**
   * Parses the specified sources.
   * 
   * @param builder  the market data builder that the resulting curve and recovery rate items should be loaded into
   * @param curveSource  the source of curve data to parse
   * @param staticDataSource  the source of static data to parse
   */
  public static void parse(
      MarketEnvironmentBuilder builder,
      CharSource curveSource,
      CharSource staticDataSource) {

    Map<IsdaIndexCreditCurveParRatesId, List<Point>> curveData = Maps.newHashMap();
    Map<MarkitRedCode, StaticData> staticDataMap = parseStaticData(staticDataSource);

    CsvFile csv = CsvFile.of(curveSource, true);
    for (int i = 0; i < csv.rowCount(); i++) {

      String seriesText = csv.field(i, Columns.Series.getColumnName());
      String versionText = csv.field(i, Columns.Version.getColumnName());
      String termText = csv.field(i, Columns.Term.getColumnName());
      String redCodeText = csv.field(i, Columns.RedCode.getColumnName());
      String maturityText = csv.field(i, Columns.Maturity.getColumnName());
      String compositeSpreadText = csv.field(i, Columns.CompositeSpread.getColumnName());
      String modelSpreadText = csv.field(i, Columns.ModelSpread.getColumnName());

      StandardId indexId = MarkitRedCode.id(redCodeText);
      int indexSeries = Integer.parseInt(seriesText);
      int indexAnnexVersion = Integer.parseInt(versionText);

      IsdaIndexCreditCurveParRatesId id = IsdaIndexCreditCurveParRatesId.of(
          IndexReferenceInformation.of(
              indexId,
              indexSeries,
              indexAnnexVersion));

      Tenor term = Tenor.parse(termText);
      LocalDate maturity = LocalDate.parse(maturityText, DATE_FORMAT);

      double spread;
      if (compositeSpreadText.isEmpty()) {
        if (modelSpreadText.isEmpty()) {
          // there is no rate for this row, continue
          continue;
        }
        // fall back to the model rate is the composite is missing
        spread = parseRate(modelSpreadText);
      } else {
        // prefer the composite rate if it is present
        spread = parseRate(compositeSpreadText);
      }

      List<Point> points = curveData.get(id);
      if (points == null) {
        points = Lists.newArrayList();
        curveData.put(id, points);
      }
      points.add(new Point(term, maturity, spread));
    }

    for (IsdaIndexCreditCurveParRatesId curveId : curveData.keySet()) {
      MarkitRedCode redCode = MarkitRedCode.from(curveId.getReferenceInformation().getIndexId());
      StaticData staticData = staticDataMap.get(redCode);
      ArgChecker.notNull(staticData, "Did not find a static data record for " + redCode);
      CdsConvention convention = staticData.getConvention();
      double recoveryRate = staticData.getRecoveryRate();
      double indexFactor = staticData.getIndexFactor();
      // TODO add fromDate handling

      String creditCurveName = curveId.toString();

      List<Point> points = curveData.get(curveId);

      Period[] periods = points.stream().map(s -> s.getTenor().getPeriod()).toArray(Period[]::new);
      LocalDate[] endDates = points.stream().map(s -> s.getDate()).toArray(LocalDate[]::new);
      double[] rates = points.stream().mapToDouble(s -> s.getRate()).toArray();

      IsdaCreditCurveParRates parRates = IsdaCreditCurveParRates.of(
          CurveName.of(creditCurveName),
          periods,
          endDates,
          rates,
          convention,
          indexFactor);

      builder.addValue(curveId, parRates);

      IsdaIndexRecoveryRateId recoveryRateId = IsdaIndexRecoveryRateId.of(curveId.getReferenceInformation());
      CdsRecoveryRate cdsRecoveryRate = CdsRecoveryRate.of(recoveryRate);

      builder.addValue(recoveryRateId, cdsRecoveryRate);
    }
  }

  // parses the static data file
  private static Map<MarkitRedCode, StaticData> parseStaticData(CharSource source) {
    CsvFile csv = CsvFile.of(source, true);

    Map<MarkitRedCode, StaticData> result = Maps.newHashMap();
    for (int i = 0; i < csv.rowCount(); i++) {
      String redCodeText = csv.field(i, "RedCode");
      String fromDateText = csv.field(i, "From Date");
      String conventionText = csv.field(i, "Convention");
      String recoveryRateText = csv.field(i, "Recovery Rate");
      String indexFactorText = csv.field(i, "Index Factor");

      MarkitRedCode redCode = MarkitRedCode.of(redCodeText);
      LocalDate fromDate = LocalDate.parse(fromDateText, DATE_FORMAT);
      CdsConvention convention = CdsConvention.of(conventionText);
      double recoveryRate = parseRate(recoveryRateText);
      double indexFactor = Double.parseDouble(indexFactorText);

      result.put(redCode, new StaticData(fromDate, convention, recoveryRate, indexFactor));
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Stores the parsed static data.
   */
  private static class StaticData {

    private LocalDate fromDate;
    private CdsConvention convention;
    private double recoveryRate;
    private double indexFactor;

    private StaticData(LocalDate fromDate, CdsConvention convention, double recoveryRate, double indexFactor) {
      this.fromDate = fromDate;
      this.convention = convention;
      this.recoveryRate = recoveryRate;
      this.indexFactor = indexFactor;
    }

    @SuppressWarnings("unused")
    public LocalDate getFromDate() {
      return fromDate;
    }

    public CdsConvention getConvention() {
      return convention;
    }

    public double getRecoveryRate() {
      return recoveryRate;
    }

    public double getIndexFactor() {
      return indexFactor;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Stores the parsed data points.
   */
  private static class Point {
    private final Tenor tenor;

    private final LocalDate date;

    private final double rate;

    private Point(Tenor tenor, LocalDate date, double rate) {
      this.tenor = tenor;
      this.date = date;
      this.rate = rate;
    }

    public Tenor getTenor() {
      return tenor;
    }

    public LocalDate getDate() {
      return date;
    }

    public double getRate() {
      return rate;
    }
  }

  // Converts from a string percentage rate with a percent sign to a double rate
  // e.g. 0.12% => 0.0012d
  private static double parseRate(String input) {
    return Double.parseDouble(input.replace("%", "")) / 100d;
  }

}
