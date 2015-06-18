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
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.finance.credit.type.StandardCdsConvention;
import com.opengamma.strata.finance.credit.type.StandardCdsConventions;

import java.time.Period;

public class Curves {

  public static double recoveryRate() {
    return .40;
  }


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


  public static int numOfCreditCurvePoints() {
    return raytheon20141020Cr.size();
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
