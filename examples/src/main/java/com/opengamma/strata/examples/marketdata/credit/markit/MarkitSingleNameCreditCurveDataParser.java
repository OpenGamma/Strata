/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata.credit.markit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.engine.marketdata.BaseMarketDataBuilder;
import com.opengamma.strata.examples.marketdata.CsvFile;
import com.opengamma.strata.finance.credit.RestructuringClause;
import com.opengamma.strata.finance.credit.SeniorityLevel;
import com.opengamma.strata.finance.credit.SingleNameReferenceInformation;
import com.opengamma.strata.finance.credit.type.CdsConvention;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.id.IsdaSingleNameCreditCurveParRatesId;
import com.opengamma.strata.market.id.IsdaSingleNameRecoveryRateId;
import com.opengamma.strata.market.value.CdsRecoveryRate;

/**
 * Parser to load daily credit curve information provided by Markit.
 * <p>
 * The columns are defined as {@code
 * Date Ticker ShortName MarkitRedCode Tier
 * Ccy DocClause Contributor Spread6m Spread1y
 * Spread2y Spread3y Spread4y Spread5y Spread7y
 * Spread10y Spread15y Spread20y Spread30y Recovery
 * Rating6m Rating1y Rating2y Rating3y Rating4y
 * Rating5y Rating7y Rating10y Rating15y Rating20y
 * Rating30y CompositeCurveRating CompositeDepth5y Sector Region
 * Country AvRating ImpliedRating CompositeLevel6m CompositeLevel1y
 * CompositeLevel2y CompositeLevel3y CompositeLevel4y CompositeLevel5y CompositeLevel7y
 * CompositeLevel10y CompositeLevel15y CompositeLevel20y CompositeLevel30y CompositeLevelRecovery}.
 * <p>
 * Also reads static data from a csv.
 * <p>
 * RedCode,Convention
 */
public class MarkitSingleNameCreditCurveDataParser {

  // Markit date format with the month in full caps. e.g. 11-JUL-14
  private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
      .parseCaseInsensitive().appendPattern("dd-MMM-yy").toFormatter(Locale.ENGLISH);

  // Index used to access the specified columns of string data in the file
  private static final int DATE = 0;
  private static final int RED_CODE = 3;
  private static final int TIER = 4;
  private static final int CURRENCY = 5;
  private static final int DOCS_CLAUSE = 6;
  private static final int FIRST_SPREAD_COLUMN = 8;
  private static final int RECOVERY = 19;

  private static final List<Tenor> TENORS = ImmutableList.of(
      Tenor.TENOR_6M,
      Tenor.TENOR_1Y,
      Tenor.TENOR_2Y,
      Tenor.TENOR_3Y,
      Tenor.TENOR_4Y,
      Tenor.TENOR_5Y,
      Tenor.TENOR_7Y,
      Tenor.TENOR_10Y,
      Tenor.TENOR_15Y,
      Tenor.TENOR_20Y,
      Tenor.TENOR_30Y);

  /**
   * Parses the specified sources.
   *
   * @param builder  the market data builder that the resulting curve and recovery rate items should be loaded into
   * @param curveSource  the source of curve data to parse
   * @param staticDataSource  the source of static data to parse
   */
  public static void parse(
      BaseMarketDataBuilder builder,
      CharSource curveSource,
      CharSource staticDataSource) {

    Map<MarkitRedCode, CdsConvention> conventions = parseStaticData(staticDataSource);
    try (Scanner scanner = new Scanner(curveSource.openStream())) {
      while (scanner.hasNextLine()) {

        String line = scanner.nextLine();
        // skip over header rows
        if (line.startsWith("V5 CDS Composites by Convention") ||
            line.trim().isEmpty() ||
            line.startsWith("\"Date\",")) {
          continue;
        }
        String[] columns = line.split(",");
        for (int i = 0; i < columns.length; i++) {
          // get rid of quotes and trim the string
          columns[i] = columns[i].replaceFirst("^\"", "").replaceFirst("\"$", "").trim();
        }

        LocalDate valuationDate = LocalDate.parse(columns[DATE], DATE_FORMAT);

        MarkitRedCode redCode = MarkitRedCode.of(columns[RED_CODE]);
        SeniorityLevel seniorityLevel = MarkitSeniorityLevel.valueOf(columns[TIER]).translate();
        Currency currency = Currency.parse(columns[CURRENCY]);
        RestructuringClause restructuringClause = MarkitRestructuringClause.valueOf(columns[DOCS_CLAUSE]).translate();

        double recoveryRate = parseRate(columns[RECOVERY]);

        SingleNameReferenceInformation referenceInformation = SingleNameReferenceInformation.of(
            redCode.toStandardId(),
            seniorityLevel,
            currency,
            restructuringClause);

        IsdaSingleNameCreditCurveParRatesId curveId = IsdaSingleNameCreditCurveParRatesId.of(referenceInformation);

        List<Period> periodsList = Lists.newArrayList();
        List<Double> ratesList = Lists.newArrayList();
        for (int i = 0; i < TENORS.size(); i++) {
          String rateString = columns[FIRST_SPREAD_COLUMN + i];
          if (rateString.isEmpty()) {
            // no data at this point
            continue;
          }
          periodsList.add(TENORS.get(i).getPeriod());
          ratesList.add(parseRate(rateString));
        }

        String creditCurveName = curveId.toString();

        CdsConvention cdsConvention = conventions.get(redCode);

        Period[] periods = periodsList.stream().toArray(Period[]::new);
        LocalDate[] endDates = Lists
            .newArrayList(periods)
            .stream()
            .map(p -> cdsConvention.getUnadjustedMaturityDateFromValuationDate(valuationDate, p))
            .toArray(LocalDate[]::new);

        double[] rates = ratesList.stream().mapToDouble(s -> s).toArray();
        double unitScalingFactor = 1d; // for single name, we don't do any scaling (no index factor)

        IsdaCreditCurveParRates parRates = IsdaCreditCurveParRates.of(
            creditCurveName,
            periods,
            endDates,
            rates,
            cdsConvention,
            unitScalingFactor);

        builder.addValue(curveId, parRates);

        IsdaSingleNameRecoveryRateId recoveryRateId = IsdaSingleNameRecoveryRateId.of(referenceInformation);
        CdsRecoveryRate cdsRecoveryRate = CdsRecoveryRate.of(recoveryRate);

        builder.addValue(recoveryRateId, cdsRecoveryRate);

      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // parses the static data file of RED code to convention
  private static Map<MarkitRedCode, CdsConvention> parseStaticData(CharSource source) {
    CsvFile csv = CsvFile.of(source, true);
    Map<MarkitRedCode, CdsConvention> result = Maps.newHashMap();
    for (int i = 0; i < csv.lineCount(); i++) {
      String redCodeText = csv.field(i, "RedCode");
      String conventionText = csv.field(i, "Convention");
      result.put(MarkitRedCode.of(redCodeText), CdsConvention.of(conventionText));
    }
    return result;
  }

  // Converts from a string percentage rate with a percent sign to a double rate
  // e.g. 0.12% => 0.0012d
  private static double parseRate(String input) {
    return Double.parseDouble(input.replace("%", "")) / 100d;
  }

}
