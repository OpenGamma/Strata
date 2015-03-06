/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.sensitivity.multicurve;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.analytics.financial.provider.description.interestrate.ParameterProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.ForwardSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.SimplyCompoundedForwardSensitivity;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.util.money.Currency;
import com.opengamma.util.tuple.DoublesPair;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * 
 */
public class ParameterSensitivityParameterCalculator3 {
  
  static public MultipleCurrencyParameterSensitivity pointToParameterSensitivity(final MulticurveSensitivity3 sensitivity, 
      final ParameterProviderInterface parameterMulticurves) {
    MultipleCurrencyParameterSensitivity result = new MultipleCurrencyParameterSensitivity();
    // YieldAndDiscount
    List<ZeroRateSensitivity> listZR = sensitivity.getZeroRateSensitivities();
    int nbZr = listZR.size();
    if(nbZr != 0) {
      int loopzr = 0;
      while(loopzr < nbZr) {
        String currentName = listZR.get(loopzr).getCurveName();
        com.opengamma.basics.currency.Currency currentCcy = listZR.get(loopzr).getCurrency();
        Pair<String, Currency> pair = Pairs.of(currentName, Currency.of(currentCcy.toString()));
        List<DoublesPair> list = new ArrayList<>();
        while((loopzr < nbZr) &&
            (listZR.get(loopzr).getCurveName() == currentName) &&
            (listZR.get(loopzr).getCurrency() == currentCcy) ) {
          list.add(DoublesPair.of(listZR.get(loopzr).getTime(), listZR.get(loopzr).getValue()));
          loopzr++;
        }
        double[] sensiParam = parameterMulticurves.parameterSensitivity(currentName, list);
        result = result.plus(pair, new DoubleMatrix1D(sensiParam));
      }
    }
//    for(ZeroRateSensitivity zr: sensitivity.getZeroRateSensitivities()) {
//      String curveName = zr.getCurveName();
//      Pair<String, Currency> pair = Pairs.of(curveName, Currency.of(zr.getCurrency().toString()));
//      List<DoublesPair> list = new ArrayList<>();
//      list.add(DoublesPair.of(zr.getTime(), zr.getValue()));
//      double[] sensiParam = parameterMulticurves.parameterSensitivity(curveName, list);
//      result = result.plus(pair, new DoubleMatrix1D(sensiParam));
//    }
    // Forward
    List<ForwardRateSensitivity> listFR = sensitivity.getForwardRateSensitivities();
    int nbFr = listFR.size();
    if(nbFr != 0) {
      int loopfr = 0;
      while(loopfr < nbFr) {
        String currentName = listFR.get(loopfr).getCurveName();
        com.opengamma.basics.currency.Currency currentCcy = listFR.get(loopfr).getCurrency();
        Pair<String, Currency> pair = Pairs.of(currentName, Currency.of(currentCcy.toString()));
        List<ForwardSensitivity> list = new ArrayList<>();
        while((loopfr < nbFr) &&
            (listFR.get(loopfr).getCurveName() == currentName) &&
            (listFR.get(loopfr).getCurrency() == currentCcy) ) {
          ForwardSensitivity s = new SimplyCompoundedForwardSensitivity(listFR.get(loopfr).getStartTime(), 
              listFR.get(loopfr).getEndTime(), listFR.get(loopfr).getAccrualFactor(), listFR.get(loopfr).getValue());
          list.add(s);
          loopfr++;
        }
        double[] sensiParam = parameterMulticurves.parameterForwardSensitivity(currentName, list);
        result = result.plus(pair, new DoubleMatrix1D(sensiParam));
      }
    }
//    for(ForwardRateSensitivity fr: sensitivity.getForwardRateSensitivities()) {
//      String curveName = fr.getCurveName();
//      Pair<String, Currency> pair = Pairs.of(curveName, Currency.of(fr.getCurrency().toString()));
//      List<ForwardSensitivity> list = new ArrayList<>();
//      ForwardSensitivity s = new SimplyCompoundedForwardSensitivity(fr.getStartTime(), fr.getEndTime(), 
//          fr.getAccrualFactor(), fr.getValue());
//      list.add(s);
//      double[] sensiParam = parameterMulticurves.parameterForwardSensitivity(curveName, list);
//      result = result.plus(pair, new DoubleMatrix1D(sensiParam));
//    }
    return result;
  }

}
