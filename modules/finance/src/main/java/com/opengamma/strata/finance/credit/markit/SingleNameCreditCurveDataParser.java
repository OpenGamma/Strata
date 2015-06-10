/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.finance.credit.markit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.finance.credit.RestructuringClause;
import com.opengamma.strata.finance.credit.SeniorityLevel;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Parser to load daily credit curve information provided by Markit
 * <p>
 * Date Ticker ShortName RedCode Tier
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
public class SingleNameCreditCurveDataParser {

  // Index used to access the specified columns of string data in the file
  private static final int s_redcode = 3;
  private static final int s_tier = 4;
  private static final int s_currency = 5;
  private static final int s_docclause = 6;
  private static final int s_firstspreadcolumn = 8;

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

  public static Map<SingleNameDataKey, List<Pair<Tenor, Double>>> parse(InputStream source) {
    Map<SingleNameDataKey, List<Pair<Tenor, Double>>> result = Maps.newHashMap();
    Scanner scanner = new Scanner(source);

    while (scanner.hasNextLine()) {

      String line = scanner.nextLine();
      String[] columns = line.split(",");

      StandardId redCode = RedCode.id(columns[s_redcode]);
      SeniorityLevel seniorityLevel = SeniorityLevel.valueOf(columns[s_tier]);
      Currency currency = Currency.parse(columns[s_currency]);
      RestructuringClause restructuringClause = RestructuringClause.valueOf(columns[s_docclause]);

      SingleNameDataKey key = SingleNameDataKey
          .builder()
          .currency(currency)
          .restructuringClause(restructuringClause)
          .seniorityLevel(seniorityLevel)
          .entityId(redCode)
          .build();
      List<Pair<Tenor, Double>> value = Lists.newArrayList();

      for (int i = 0; i < s_tenors.size(); i++) {
        Tenor tenor = s_tenors.get(i);
        Double rate = parseSpreadRate(columns[s_firstspreadcolumn + i]);
        Pair<Tenor, Double> curvePoint = Pair.of(tenor, rate);
        value.add(curvePoint);
      }

      result.put(key, value);

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
  private static Double parseSpreadRate(String input) {
    return Double.parseDouble(input.replace("%", "")) / 100D;
  }

}
