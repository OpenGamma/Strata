/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.function.credit;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDAInstrumentTypes;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConvention;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConventions;
import com.opengamma.strata.finance.credit.type.StandardCdsConvention;
import com.opengamma.strata.finance.credit.type.StandardCdsConventions;

import java.time.Period;

public class Curves {

  public static double recoveryRate() {
    return .40;
  }

  static ImmutableList<String> raytheon20141020Ir = ImmutableList.of(
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

  // ParSpreadQuote
  static ImmutableList<String> raytheon20141020Cr = ImmutableList.of(
      "6M,0.0028",
      "1Y,0.0028",
      "2Y,0.0028",
      "3Y,0.0028",
      "4Y,0.0028",
      "5Y,0.0028",
      "7Y,0.0028",
      "10Y,0.0028"
  );

  public static int numOfYieldCurvePoints() {
    return raytheon20141020Ir.size();
  }

  public static int numOfCreditCurvePoints() {
    return raytheon20141020Cr.size();
  }

  public static CurveYieldPlaceholder discountCurve() {
    return discountCurvePar(0D);
  }

  public static CurveYieldPlaceholder discountCurvePar(double shift) {
    return discountCurveParWithData(
        raytheon20141020Ir
            .stream()
            .mapToDouble(s -> Double.valueOf(s.split(",")[2]) + shift)
            .toArray()
    );
  }

  public static CurveYieldPlaceholder discountCurveParBucket(int index, double shift) {
    double[] rates = raytheon20141020Ir
        .stream()
        .mapToDouble(s -> Double.valueOf(s.split(",")[2]))
        .toArray();
    rates[index] = rates[index] + shift;
    return discountCurveParWithData(rates);
  }

  private static CurveYieldPlaceholder discountCurveParWithData(double[] rates) {

    Period[] yieldCurvePoints = raytheon20141020Ir
        .stream()
        .map(s -> Tenor.parse(s.split(",")[0]).getPeriod())
        .toArray(Period[]::new);
    ISDAInstrumentTypes[] yieldCurveInstruments = raytheon20141020Ir
        .stream()
        .map(s -> (s.split(",")[1].equals("M") ? ISDAInstrumentTypes.MoneyMarket : ISDAInstrumentTypes.Swap))
        .toArray(ISDAInstrumentTypes[]::new);

    IsdaYieldCurveConvention curveConvention = IsdaYieldCurveConventions.northAmericanUsd;
    return CurveYieldPlaceholder.of(yieldCurvePoints, yieldCurveInstruments, rates, curveConvention);
  }

  public static CurveCreditPlaceholder creditCurve() {
    return creditCurvePar(0D);
  }

  public static CurveCreditPlaceholder creditCurvePar(double shift) {
    return creditCurveParWithData(
        raytheon20141020Cr
            .stream()
            .mapToDouble(s -> Double.valueOf(s.split(",")[1]) + shift)
            .toArray()
    );
  }

  public static CurveCreditPlaceholder creditCurveParBucket(int index, double shift) {
    double[] rates = raytheon20141020Cr
        .stream()
        .mapToDouble(s -> Double.valueOf(s.split(",")[1]))
        .toArray();
    rates[index] = rates[index] + shift;
    return creditCurveParWithData(rates);
  }

  private static CurveCreditPlaceholder creditCurveParWithData(double rates[]) {

    Period[] creditCurvePoints = raytheon20141020Cr
        .stream()
        .map(s -> Tenor.parse(s.split(",")[0]).getPeriod())
        .toArray(Period[]::new);


    StandardCdsConvention cdsConvention = StandardCdsConventions.northAmericanUsd();

    return CurveCreditPlaceholder.of(creditCurvePoints, rates, cdsConvention);
  }

}
