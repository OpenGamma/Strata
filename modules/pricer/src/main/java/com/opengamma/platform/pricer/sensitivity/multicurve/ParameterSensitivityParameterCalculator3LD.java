/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity.multicurve;


/**
 * 
 */
public class ParameterSensitivityParameterCalculator3LD {
  //  using 2.x
  //
  //  static public MultipleCurrencyParameterSensitivity pointToParameterSensitivity(MulticurveSensitivity3LD sensitivity, 
  //      PricingEnvironment environment, LocalDate valuationDate) {
  //    MultipleCurrencyParameterSensitivity result = new MultipleCurrencyParameterSensitivity();
  //    // YieldAndDiscount
  //    List<ZeroRateSensitivityLD> listZR = sensitivity.getZeroRateSensitivities();
  //    int nbZr = listZR.size();
  //    if(nbZr != 0) {
  //      int loopzr = 0;
  //      while(loopzr < nbZr) {
  //        com.opengamma.basics.currency.Currency ccyDiscount = listZR.get(loopzr).getCurrencyDiscount();
  //        com.opengamma.basics.currency.Currency ccySensitivity = listZR.get(loopzr).getCurrencySensitivity();
  //        List<DoublesPair> list = new ArrayList<>();
  //        while((loopzr < nbZr) &&
  //            (listZR.get(loopzr).getCurrencyDiscount() == ccyDiscount) &&
  //            (listZR.get(loopzr).getCurrencySensitivity() == ccySensitivity) ) {
  //          double time = environment.relativeTime(listZR.get(loopzr).getDate());
  //          list.add(DoublesPair.of(time, listZR.get(loopzr).getValue()));
  //          loopzr++;
  //        }
  //        String currentName = environment.rawData(MulticurveProviderInterface.class).getName(
  //            Currency.of(ccyDiscount.toString()));
  //        Pair<String, Currency> pair = Pairs.of(currentName, Currency.of(ccySensitivity.toString()));
  //        double[] sensiParam = environment.rawData(MulticurveProviderInterface.class).parameterSensitivity(currentName,
  //            list);
  //        result = result.plus(pair, new DoubleMatrix1D(sensiParam));
  //      }
  //    }
  //    // Forward
  //    List<ForwardRateSensitivityLD> listFR = sensitivity.getForwardRateSensitivities();
  //    int nbFr = listFR.size();
  //    if(nbFr != 0) {
  //      int loopfr = 0;
  //      while(loopfr < nbFr) {
  //        IborIndex currentIndex = (IborIndex) listFR.get(loopfr).getIndex();
  //        com.opengamma.basics.currency.Currency currentCcy = listFR.get(loopfr).getCurrency();
  //        List<ForwardSensitivity> list = new ArrayList<>();
  //        while((loopfr < nbFr) &&
  //            (listFR.get(loopfr).getIndex() == currentIndex) &&
  //            (listFR.get(loopfr).getCurrency() == currentCcy) ) {
  //          LocalDate fixingDate = listFR.get(loopfr).getFixingDate();
  //          LocalDate fixingStartDate = listFR.get(loopfr).getFixingPeriodStartDate();
  //          if(fixingStartDate == null) {
  //            fixingStartDate = currentIndex.calculateEffectiveFromFixing(fixingDate);
  //          }
  //          LocalDate fixingEndDate = listFR.get(loopfr).getFixingPeriodEndDate();
  //          if(fixingEndDate == null) {
  //            fixingEndDate = currentIndex.calculateMaturityFromEffective(fixingStartDate);
  //          }
  //          double fixingAccrualFactor = currentIndex.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
  //          double fixingStartTime = environment.relativeTime(fixingStartDate);
  //          double fixingEndTime = environment.relativeTime(fixingEndDate);
  //          ForwardSensitivity s = new SimplyCompoundedForwardSensitivity(fixingStartTime, fixingEndTime, 
  //              fixingAccrualFactor, listFR.get(loopfr).getValue());
  //          list.add(s);
  //          loopfr++;
  //        }
  //        String currentName = environment.rawData(MulticurveProviderInterface.class).getName(
  //            Legacy.iborIndex(currentIndex));
  //        Pair<String, Currency> pair = Pairs.of(currentName, Currency.of(currentCcy.toString()));
  //        double[] sensiParam = environment.rawData(MulticurveProviderInterface.class).parameterForwardSensitivity(
  //            currentName, list);
  //        result = result.plus(pair, new DoubleMatrix1D(sensiParam));
  //      }
  //    }
  //    return result;
  //  }

}
