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
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.examples.marketdata.CsvFile;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConvention;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveUnderlyingType;
import com.opengamma.strata.market.id.IsdaYieldCurveParRatesId;

import java.time.Period;
import java.util.List;
import java.util.Map;

/**
 * Parser to load daily yield curve information provided by Markit
 * <p>
 * Valuation Date,Tenor,Instrument Type,Rate,Curve Convention
 */
public class ISDAYieldCurveDataParser {

  private static final String s_tenor = "Tenor";
  private static final String s_instrument = "Instrument Type";
  private static final String s_rate = "Rate";
  private static final String s_convention = "Curve Convention";

  public static Map<IsdaYieldCurveParRatesId, IsdaYieldCurveParRates> parse(CharSource source) {
    Map<IsdaYieldCurveConvention, List<Point>> curveData = Maps.newHashMap();

    CsvFile csv = CsvFile.of(source, true);

    for (int i = 0; i < csv.lineCount(); i++) {

      String tenorText = csv.field(i, s_tenor);
      String instrumentText = csv.field(i, s_instrument);
      String rateText = csv.field(i, s_rate);
      String conventionText = csv.field(i, s_convention);

      Point point = Point.of(
          Tenor.parse(tenorText),
          mapUnderlyingType(instrumentText),
          Double.parseDouble(rateText)
      );

      IsdaYieldCurveConvention convention = IsdaYieldCurveConvention.of(conventionText);

      List<Point> points = curveData.get(convention);
      if (points == null) {
        points = Lists.newArrayList();
        curveData.put(convention, points);
      }

      points.add(point);

    }

    Map<IsdaYieldCurveParRatesId, IsdaYieldCurveParRates> result = Maps.newHashMap();

    for (IsdaYieldCurveConvention convention : curveData.keySet()) {
      List<Point> points = curveData.get(convention);

      result.put(IsdaYieldCurveParRatesId.of(convention.getCurrency()),
          IsdaYieldCurveParRates.of(
              convention.getName(),
              points.stream().map(s -> s.getTenor().getPeriod()).toArray(Period[]::new),
              points.stream().map(s -> s.getInstrumentType()).toArray(IsdaYieldCurveUnderlyingType[]::new),
              points.stream().mapToDouble(s -> s.getRate()).toArray(),
              convention
          ));

    }

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

  private static class Point {
    private final Tenor tenor;

    private final IsdaYieldCurveUnderlyingType instrumentType;

    private final double rate;

    private Point(Tenor tenor, IsdaYieldCurveUnderlyingType instrumentType, double rate) {
      this.tenor = tenor;
      this.instrumentType = instrumentType;
      this.rate = rate;
    }

    public Tenor getTenor() {
      return tenor;
    }

    public IsdaYieldCurveUnderlyingType getInstrumentType() {
      return instrumentType;
    }

    public double getRate() {
      return rate;
    }

    public static Point of(Tenor tenor, IsdaYieldCurveUnderlyingType instrumentType, double rate) {
      return new Point(tenor, instrumentType, rate);
    }
  }


}
