/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.FxIndexSensitivity;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.DiscountFactors;
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
    PointSensitivities sensiFxDecomposed = resolveFxRateSensitivities(sensitivities);
    CurveCurrencyParameterSensitivities sens = CurveCurrencyParameterSensitivities.empty();
    for (PointSensitivity point : sensiFxDecomposed.getSensitivities()) {
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

      } else if (point instanceof InflationRateSensitivity) {
        InflationRateSensitivity pt = (InflationRateSensitivity) point;
        PriceIndexValues rates = priceIndexValues(pt.getIndex());
        sens = sens.combinedWith(rates.curveParameterSensitivity(pt));
      }
    }
    return sens;
  }

  // resolve any FX Index sensitivity into zero-rate sensitivities
  private PointSensitivities resolveFxRateSensitivities(PointSensitivities sensitivities) {
    if (!sensitivities.getSensitivities().stream().anyMatch(s -> s instanceof FxIndexSensitivity)) {
      return sensitivities;
    }
    MutablePointSensitivities mutable = new MutablePointSensitivities();
    for (PointSensitivity point : sensitivities.getSensitivities()) {
      if (point instanceof FxIndexSensitivity) {
        mutable.combinedWith(fxIndexForwardRateSensitivity((FxIndexSensitivity) point));
      } else {
        mutable.add(point);
      }
    }
    return mutable.build();
  }

  // resolve single FX Index sensitivity into zero-rate sensitivity
  private PointSensitivityBuilder fxIndexForwardRateSensitivity(FxIndexSensitivity fxRateSensitivity) {
    // use the specified base currency to determine the desired currency pair
    // then derive sensitivity from discount factors based off desired currency pair, not that of the index
    FxIndex index = fxRateSensitivity.getIndex();
    Currency refBaseCurrency = fxRateSensitivity.getReferenceCurrency();
    Currency refCounterCurrency = fxRateSensitivity.getReferenceCounterCurrency();
    Currency sensitivityCurrency = fxRateSensitivity.getCurrency();
    LocalDate maturityDate = index.calculateMaturityFromFixing(fxRateSensitivity.getFixingDate());

    DiscountFactors discountFactorsBase = discountFactors(refBaseCurrency);
    DiscountFactors discountFactorsCounter = discountFactors(refCounterCurrency);
    double dfCcyBaseAtMaturity = discountFactorsBase.discountFactor(maturityDate);
    double dfCcyCounterAtMaturityInv = 1.0 / discountFactorsCounter.discountFactor(maturityDate);

    PointSensitivityBuilder dfCcyBaseAtMaturitySensitivity =
        discountFactorsBase.zeroRatePointSensitivity(maturityDate, sensitivityCurrency);
    dfCcyBaseAtMaturitySensitivity = dfCcyBaseAtMaturitySensitivity.multipliedBy(
        fxRate(refBaseCurrency, refCounterCurrency) * dfCcyCounterAtMaturityInv * fxRateSensitivity.getSensitivity());

    PointSensitivityBuilder dfCcyCounterAtMaturitySensitivity =
        discountFactorsCounter.zeroRatePointSensitivity(maturityDate, sensitivityCurrency);
    dfCcyCounterAtMaturitySensitivity = dfCcyCounterAtMaturitySensitivity.multipliedBy(
        -fxRate(refBaseCurrency, refCounterCurrency) * dfCcyBaseAtMaturity * dfCcyCounterAtMaturityInv *
            dfCcyCounterAtMaturityInv * fxRateSensitivity.getSensitivity());

    return dfCcyBaseAtMaturitySensitivity.combinedWith(dfCcyCounterAtMaturitySensitivity);
  }

}
