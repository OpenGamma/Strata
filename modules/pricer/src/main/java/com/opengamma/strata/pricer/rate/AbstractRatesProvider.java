/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.FxForwardSensitivity;
import com.opengamma.strata.market.sensitivity.FxIndexSensitivity;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.FxForwardRates;
import com.opengamma.strata.market.value.FxIndexRates;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.market.value.OvernightIndexRates;
import com.opengamma.strata.market.value.PriceIndexValues;

/**
 * An abstract rates provider implementation.
 * <p>
 * This class exists to provide common functionality between rate provider implementations.
 */
public abstract class AbstractRatesProvider
    implements RatesProvider {

  //-------------------------------------------------------------------------
  @Override
  public CurveCurrencyParameterSensitivities curveParameterSensitivity(PointSensitivities sensitivities) {
    CurveCurrencyParameterSensitivities sens = CurveCurrencyParameterSensitivities.empty();
    for (PointSensitivity point : sensitivities.getSensitivities()) {
      if (point instanceof ZeroRateSensitivity) {
        ZeroRateSensitivity pt = (ZeroRateSensitivity) point;
        DiscountFactors factors = discountFactors(pt.getCurveCurrency());
        sens = sens.combinedWith(factors.curveParameterSensitivity(pt));

      } else if (point instanceof IborRateSensitivity) {
        IborRateSensitivity pt = (IborRateSensitivity) point;
        IborIndexRates rates = iborIndexRates(pt.getIndex());
        sens = sens.combinedWith(rates.curveParameterSensitivity(pt));

      } else if (point instanceof OvernightRateSensitivity) {
        OvernightRateSensitivity pt = (OvernightRateSensitivity) point;
        OvernightIndexRates rates = overnightIndexRates(pt.getIndex());
        sens = sens.combinedWith(rates.curveParameterSensitivity(pt));

      } else if (point instanceof FxIndexSensitivity) {
        FxIndexSensitivity pt = (FxIndexSensitivity) point;
        FxIndexRates rates = fxIndexRates(pt.getIndex());
        sens = sens.combinedWith(rates.curveParameterSensitivity(pt));

      } else if (point instanceof InflationRateSensitivity) {
        InflationRateSensitivity pt = (InflationRateSensitivity) point;
        PriceIndexValues rates = priceIndexValues(pt.getIndex());
        sens = sens.combinedWith(rates.curveParameterSensitivity(pt));

      } else if (point instanceof FxForwardSensitivity) {
        FxForwardSensitivity pt = (FxForwardSensitivity) point;
        FxForwardRates rates = fxForwardRates(pt.getCurrencyPair());
        sens = sens.combinedWith(rates.curveParameterSensitivity(pt));
      }
    }
    return sens;
  }

  @Override
  public MultiCurrencyAmount currencyExposure(PointSensitivities sensitivities) {
    MultiCurrencyAmount ce = MultiCurrencyAmount.empty();
    for (PointSensitivity point : sensitivities.getSensitivities()) {
      if (point instanceof FxIndexSensitivity) {
        FxIndexSensitivity pt = (FxIndexSensitivity) point;
        FxIndexRates rates = fxIndexRates(pt.getIndex());
        ce = ce.plus(rates.currencyExposure(pt));
      }
      if (point instanceof FxForwardSensitivity) {
        FxForwardSensitivity pt = (FxForwardSensitivity) point;
        pt = (FxForwardSensitivity) pt.convertedTo(pt.getReferenceCurrency(), this);
        FxForwardRates rates = fxForwardRates(pt.getCurrencyPair());
        ce = ce.plus(rates.currencyExposure(pt));
      }
    }
    return ce;
  }

}
