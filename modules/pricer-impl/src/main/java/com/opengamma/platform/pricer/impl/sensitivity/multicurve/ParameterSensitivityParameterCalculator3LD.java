/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.sensitivity.multicurve;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.ForwardSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.SimplyCompoundedForwardSensitivity;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.basics.index.IborIndex;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.Legacy;
import com.opengamma.util.money.Currency;
import com.opengamma.util.tuple.DoublesPair;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * 
 */
public class ParameterSensitivityParameterCalculator3LD {
  
  static public MultipleCurrencyParameterSensitivity pointToParameterSensitivity(MulticurveSensitivity3LD sensitivity, 
      PricingEnvironment environment, LocalDate valuationDate) {
    MultipleCurrencyParameterSensitivity result = new MultipleCurrencyParameterSensitivity();
    // YieldAndDiscount
    List<ZeroRateSensitivityLD> listZR = sensitivity.getZeroRateSensitivities();
    int nbZr = listZR.size();
    if(nbZr != 0) {
      int loopzr = 0;
      while(loopzr < nbZr) {
        com.opengamma.basics.currency.Currency ccyDiscount = listZR.get(loopzr).getCurrencyDiscount();
        com.opengamma.basics.currency.Currency ccySensitivity = listZR.get(loopzr).getCurrencySensitivity();
        List<DoublesPair> list = new ArrayList<>();
        while((loopzr < nbZr) &&
            (listZR.get(loopzr).getCurrencyDiscount() == ccyDiscount) &&
            (listZR.get(loopzr).getCurrencySensitivity() == ccySensitivity) ) {
          double time = environment.relativeTime(listZR.get(loopzr).getDate());
          list.add(DoublesPair.of(time, listZR.get(loopzr).getValue()));
          loopzr++;
        }
        String currentName = environment.rawData(MulticurveProviderInterface.class).getName(
            Currency.of(ccyDiscount.toString()));
        Pair<String, Currency> pair = Pairs.of(currentName, Currency.of(ccySensitivity.toString()));
        double[] sensiParam = environment.rawData(MulticurveProviderInterface.class).parameterSensitivity(currentName,
            list);
        result = result.plus(pair, new DoubleMatrix1D(sensiParam));
      }
    }
    // Forward
    List<ForwardRateSensitivityLD> listFR = sensitivity.getForwardRateSensitivities();
    int nbFr = listFR.size();
    if(nbFr != 0) {
      int loopfr = 0;
      while(loopfr < nbFr) {
        IborIndex currentIndex = (IborIndex) listFR.get(loopfr).getIndex();
        com.opengamma.basics.currency.Currency currentCcy = listFR.get(loopfr).getCurrency();
        List<ForwardSensitivity> list = new ArrayList<>();
        while((loopfr < nbFr) &&
            (listFR.get(loopfr).getIndex() == currentIndex) &&
            (listFR.get(loopfr).getCurrency() == currentCcy) ) {
          LocalDate fixingDate = listFR.get(loopfr).getFixingDate();
          LocalDate fixingStartDate = listFR.get(loopfr).getFixingPeriodStartDate();
          if(fixingStartDate == null) {
            fixingStartDate = currentIndex.calculateEffectiveFromFixing(fixingDate);
          }
          LocalDate fixingEndDate = listFR.get(loopfr).getFixingPeriodEndDate();
          if(fixingEndDate == null) {
            fixingEndDate = currentIndex.calculateMaturityFromEffective(fixingStartDate);
          }
          double fixingAccrualFactor = currentIndex.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
          double fixingStartTime = environment.relativeTime(fixingStartDate);
          double fixingEndTime = environment.relativeTime(fixingEndDate);
          ForwardSensitivity s = new SimplyCompoundedForwardSensitivity(fixingStartTime, fixingEndTime, 
              fixingAccrualFactor, listFR.get(loopfr).getValue());
          list.add(s);
          loopfr++;
        }
        String currentName = environment.rawData(MulticurveProviderInterface.class).getName(
            Legacy.iborIndex(currentIndex));
        Pair<String, Currency> pair = Pairs.of(currentName, Currency.of(currentCcy.toString()));
        double[] sensiParam = environment.rawData(MulticurveProviderInterface.class).parameterForwardSensitivity(
            currentName, list);
        result = result.plus(pair, new DoubleMatrix1D(sensiParam));
      }
    }
    return result;
  }

}
