/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculator to obtain the notional equivalent.
 * <p>
 * This needs the {@link DoubleArray} with present value sensitivity to
 * market quotes obtained during curve calibration to be available.
 */
public class NotionalEquivalentCalculator {

  /**
   * The default instance.
   */
  public static final NotionalEquivalentCalculator DEFAULT = new NotionalEquivalentCalculator();

  //-------------------------------------------------------------------------
  /**
   * Calculates the notional equivalent from the present value market quote sensitivities.
   * <p>
   * The notional equivalent is the notional in each instrument used to calibrate the curves to have the same
   * sensitivity as the one of the portfolio described by the market quote sensitivities.
   * 
   * @param marketQuoteSensitivities  the market quote sensitivities 
   * @param provider  the rates provider, containing sensitivity information
   * @return the notionals
   */
  public CurrencyParameterSensitivities notionalEquivalent(
      CurrencyParameterSensitivities marketQuoteSensitivities,
      RatesProvider provider) {

    List<CurrencyParameterSensitivity> equivalentList = new ArrayList<>();
    for (CurrencyParameterSensitivity s : marketQuoteSensitivities.getSensitivities()) {
      ArgChecker.isTrue(s.getMarketDataName() instanceof CurveName, "curve name");
      CurveName name = (CurveName) s.getMarketDataName();
      Optional<Curve> curveOpt = provider.findData(name);
      ArgChecker.isTrue(curveOpt.isPresent(), "Curve {} in the sensitiivty is not present in the provider", name);
      Curve curve = curveOpt.get();
      Optional<DoubleArray> pvSensiOpt = curve.getMetadata().findInfo(CurveInfoType.PV_SENSITIVITY_TO_MARKET_QUOTE);
      ArgChecker.isTrue(pvSensiOpt.isPresent(), "Present value sensitivity curve info is required");
      DoubleArray pvSensi = pvSensiOpt.get();
      double[] notionalArray = new double[pvSensi.size()];
      for (int i = 0; i < pvSensi.size(); i++) {
        notionalArray[i] = s.getSensitivity().get(i) / pvSensi.get(i);
      }
      DoubleArray notional = DoubleArray.ofUnsafe(notionalArray);
      equivalentList.add(CurrencyParameterSensitivity.of(name, s.getParameterMetadata(), s.getCurrency(), notional));
    }
    return CurrencyParameterSensitivities.of(equivalentList);
  }

}
