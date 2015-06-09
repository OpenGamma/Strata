package com.opengamma.strata.function.credit;

import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDAInstrumentTypes;
import com.opengamma.strata.finance.credit.type.IsdaYieldCurveConvention;

import java.time.Period;

public class CurveYieldPlaceholder {

  private final Period[] _yieldCurvePoints;
  private final ISDAInstrumentTypes[] _yieldCurveInstruments;
  private final double[] _parRates;
  private final IsdaYieldCurveConvention _curveConvention;

  private CurveYieldPlaceholder(
      Period[] yieldCurvePoints,
      ISDAInstrumentTypes[] yieldCurveInstruments,
      double[] parRates,
      IsdaYieldCurveConvention curveConvention
  ) {
    _yieldCurvePoints = yieldCurvePoints;
    _yieldCurveInstruments = yieldCurveInstruments;
    _parRates = parRates;
    _curveConvention = curveConvention;
  }

  public Period[] getYieldCurvePoints() {
    return _yieldCurvePoints;
  }

  public ISDAInstrumentTypes[] getYieldCurveInstruments() {
    return _yieldCurveInstruments;
  }

  public double[] getParRates() {
    return _parRates;
  }

  public IsdaYieldCurveConvention getCurveConvention() {
    return _curveConvention;
  }

  public String name() {return "interest rates";}

  public static CurveYieldPlaceholder of(
      Period[] yieldCurvePoints,
      ISDAInstrumentTypes[] yieldCurveInstruments,
      double[] parRates,
      IsdaYieldCurveConvention curveConvention
  ) {
    return new CurveYieldPlaceholder(
        yieldCurvePoints,
        yieldCurveInstruments,
        parRates,
        curveConvention
    );
  }
}
