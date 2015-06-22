/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.marketdata.credit.markit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConvention;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConventions;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveUnderlyingType;
import com.opengamma.strata.market.id.IsdaYieldCurveParRatesId;

import java.io.Reader;
import java.time.Period;
import java.util.Map;

public class ISDAYieldCurveDataParser {

  public static Map<IsdaYieldCurveParRatesId, IsdaYieldCurveParRates> parse(Reader source) {
    Map<IsdaYieldCurveParRatesId, IsdaYieldCurveParRates> result = Maps.newHashMap();

    Currency currency = Currency.USD;
    IsdaYieldCurveConvention curveConvention = IsdaYieldCurveConventions.ISDA_USD;
    ImmutableList<String> usd20141020Ir = ImmutableList.of(
        "1M,M,0.001535",
        "2M,M,0.001954",
        "3M,M,0.002281",
        "6M,M,0.003217",
        "1Y,M,0.005444",
        "2Y,S,0.005905",
        "3Y,S,0.009555",
        "4Y,S,0.012775",
        "5Y,S,0.015395",
        "6Y,S,0.017445",
        "7Y,S,0.019205",
        "8Y,S,0.020660",
        "9Y,S,0.021885",
        "10Y,S,0.022940",
        "12Y,S,0.024615",
        "15Y,S,0.026300",
        "20Y,S,0.027950",
        "25Y,S,0.028715",
        "30Y,S,0.029160"
    );
    double[] rates = usd20141020Ir
        .stream()
        .mapToDouble(s -> Double.valueOf(s.split(",")[2]))
        .toArray();
    Period[] yieldCurvePoints = usd20141020Ir
        .stream()
        .map(s -> Tenor.parse(s.split(",")[0]).getPeriod())
        .toArray(Period[]::new);
    IsdaYieldCurveUnderlyingType[] yieldCurveInstruments = usd20141020Ir
        .stream()
        .map(s -> s.split(",")[1])
        .map(ISDAYieldCurveDataParser::mapUnderlyingType)
        .toArray(IsdaYieldCurveUnderlyingType[]::new);

    String name = curveConvention.getName();

    result.put(IsdaYieldCurveParRatesId.of(currency),
        IsdaYieldCurveParRates.of(
            name,
            yieldCurvePoints,
            yieldCurveInstruments,
            rates,
            curveConvention
        ));

    return result;
  }

  private static IsdaYieldCurveUnderlyingType mapUnderlyingType(String type) {
    switch (type) {
      case "M":
        return IsdaYieldCurveUnderlyingType.IsdaMoneyMarket;
      case "S":
        return IsdaYieldCurveUnderlyingType.IsdaSwap;
      default:
        throw new IllegalStateException("Unknown underlying type, only M or S allowed: " + type);
    }
  }


}
