package com.opengamma.strata.function.credit;

import com.google.common.collect.Lists;
import com.opengamma.strata.finance.credit.type.StandardCdsConvention;

import java.time.LocalDate;
import java.time.Period;

public class CurveCreditPlaceholder {

  private final Period[] _creditCurvePoints;
  private final double[] _fractionalParSpreads;
  private final StandardCdsConvention _cdsConvention;

  private CurveCreditPlaceholder(Period[] creditCurvePoints, double[] fractionalParSpreads, StandardCdsConvention cdsConvention) {
    _creditCurvePoints = creditCurvePoints;
    _fractionalParSpreads = fractionalParSpreads;
    _cdsConvention = cdsConvention;
  }

  public LocalDate[] getCreditCurvePoints(LocalDate valueDate) {
    return Lists.newArrayList(_creditCurvePoints).stream().map(x -> x.addTo(valueDate)).toArray(LocalDate[]::new);
  }

  public Period[] getCreditCurvePoints() {
    return _creditCurvePoints;
  }

  public double[] getFractionalParSpreads() {
    return _fractionalParSpreads;
  }

  public StandardCdsConvention getCdsConvention() {
    return _cdsConvention;
  }

  public static CurveCreditPlaceholder of(Period[] creditCurvePoints, double[] fractionalParSpreads, StandardCdsConvention cdsConvention) {
    return new CurveCreditPlaceholder(creditCurvePoints, fractionalParSpreads, cdsConvention);
  }

}
