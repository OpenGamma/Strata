/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.marketdata.credit.markit;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.opengamma.analytics.util.ArrayUtils;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.finance.credit.RestructuringClause;
import com.opengamma.strata.finance.credit.SeniorityLevel;
import com.opengamma.strata.finance.credit.reference.SingleNameReferenceInformation;
import com.opengamma.strata.finance.credit.type.CdsConvention;
import com.opengamma.strata.finance.credit.type.CdsConventions;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.id.IsdaSingleNameCreditCurveParRatesId;

import java.io.InputStream;
import java.io.Reader;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Parser to load daily credit curve information provided by Markit
 * <p>
 * Date Ticker ShortName MarkitRedCode Tier
 * Ccy DocClause Contributor Spread6m Spread1y
 * Spread2y Spread3y Spread4y Spread5y Spread7y
 * Spread10y Spread15y Spread20y Spread30y Recovery
 * Rating6m Rating1y Rating2y Rating3y Rating4y
 * Rating5y Rating7y Rating10y Rating15y Rating20y
 * Rating30y CompositeCurveRating CompositeDepth5y Sector Region
 * Country AvRating ImpliedRating CompositeLevel6m CompositeLevel1y
 * CompositeLevel2y CompositeLevel3y CompositeLevel4y CompositeLevel5y CompositeLevel7y
 * CompositeLevel10y CompositeLevel15y CompositeLevel20y CompositeLevel30y CompositeLevelRecovery
 */
public class MarkitSingleNameCreditCurveDataParser {

  // Index used to access the specified columns of string data in the file
  private static final int s_redcode = 3;
  private static final int s_tier = 4;
  private static final int s_currency = 5;
  private static final int s_docclause = 6;
  private static final int s_firstspreadcolumn = 8;
  private static final int s_recovery = 19;

  private static final List<Tenor> s_tenors = ImmutableList.of(
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
      Tenor.TENOR_30Y
  );

  public static Map<IsdaSingleNameCreditCurveParRatesId, IsdaCreditCurveParRates> parse(Reader source) {
    Map<IsdaSingleNameCreditCurveParRatesId, IsdaCreditCurveParRates> result = Maps.newHashMap();
    Scanner scanner = new Scanner(source);

    while (scanner.hasNextLine()) {

      String line = scanner.nextLine();
      // skip over header rows
      if (line.startsWith("V5 CDS Composites by Convention") ||
          line.trim().isEmpty() ||
          line.startsWith("\"Date\",")) {
        continue;
      }
      String[] columns = line.split(",");
      for (int i=0; i<columns.length; i++) {
        // get rid of quotes and trim the string
        columns[i] = columns[i].replaceFirst("^\"","").replaceFirst("\"$", "").trim();
      }

      // TODO validate date
      MarkitRedCode redCode = MarkitRedCode.of(columns[s_redcode]);
      SeniorityLevel seniorityLevel = MarkitSeniorityLevel.valueOf(columns[s_tier]).translate();
      Currency currency = Currency.parse(columns[s_currency]);
      RestructuringClause restructuringClause = MarkitRestructuringClause.valueOf(columns[s_docclause]).translate();
      double recoveryRate = parseRate(columns[s_recovery]);

      IsdaSingleNameCreditCurveParRatesId id = IsdaSingleNameCreditCurveParRatesId.of(
          SingleNameReferenceInformation.of(
              redCode.toStandardId(),
              seniorityLevel,
              currency,
              restructuringClause
          )
      );

      List<Period> periodsList = Lists.newArrayList();
      List<Double> ratesList = Lists.newArrayList();
      for (int i = 0; i < s_tenors.size(); i++) {
        String rateString = columns[s_firstspreadcolumn + i];
        if (rateString.isEmpty()) {
          // no data at this point
          continue;
        }
        periodsList.add(s_tenors.get(i).getPeriod());
        ratesList.add(parseRate(rateString));
      }

      Period[] periods = periodsList.stream().toArray(Period[]::new);
      double[] rates = ratesList.stream().mapToDouble(s -> s).toArray();

      String creditCurveName = id.toString();

      // TODO hardcoded
      CdsConvention cdsConvention = CdsConventions.NORTH_AMERICAN_USD;

      IsdaCreditCurveParRates parRates = IsdaCreditCurveParRates.of(
          creditCurveName,
          periods,
          rates,
          cdsConvention,
          recoveryRate
      );

      result.put(id, parRates);

    }

    return result;
  }

  /**
   * converting from a string percentage rate with a percent sign to.
   * a proper double rate
   * e.g. 0.12% => 0.0012D
   *
   * @return double representation of the interest data
   */
  private static Double parseRate(String input) {
    return Double.parseDouble(input.replace("%", "")) / 100D;
  }

}
