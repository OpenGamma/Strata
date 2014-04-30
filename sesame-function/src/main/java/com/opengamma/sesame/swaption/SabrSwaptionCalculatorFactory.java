/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.swaption;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.swaption.derivative.SwaptionPhysicalFixedIbor;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.SABRSwaptionProviderDiscount;
import com.opengamma.financial.analytics.conversion.FixedIncomeConverterDataProvider;
import com.opengamma.financial.analytics.conversion.SwaptionSecurityConverter;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.option.SwaptionSecurity;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.sabr.SabrParametersConfiguration;
import com.opengamma.sesame.sabr.SabrParametersProviderFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Factory class for creating a calculator for a swaption using SABR data.
 */
public class SabrSwaptionCalculatorFactory implements SwaptionCalculatorFactory {

  /**
   * Converter for transforming a swaption into its derivative form.
   */
  private final FixedIncomeConverterDataProvider _definitionToDerivativeConverter;

  /**
   * Function used to generate a combined multicurve bundle suitable
   * for use with a particular security.
   */
  private final DiscountingMulticurveCombinerFn _discountingMulticurveCombinerFn;

  /**
   * Converter for transforming a swaption into its InstrumentDefinition form.
   */
  private final SwaptionSecurityConverter _swaptionSecurityConverter;

  /**
   * Function used to retrieve time series data.
   */
  // todo - the method for fixings should be moving from this function as per JIRA - SSM-215
  private final HistoricalTimeSeriesFn _htsFn;

  /**
   * Function used to retrieve SABR parameter data.
   */
  private final SabrParametersProviderFn _sabrParametersProviderFn;

  /**
   * Creates the factory.
   *
   * @param definitionToDerivativeConverter converter for transforming a swaption
   * into its derivative form, not null
   * @param discountingMulticurveCombinerFn function for creating multicurve
   * bundles, not null
   * @param swaptionSecurityConverter converter for transforming a swaption into
   * its InstrumentDefinition form, not null
   * @param htsFn function used to retrieve time series data, not null
   * @param sabrParametersProviderFn function used to retrieve SABR parameter
   * data, not null
   */
  public SabrSwaptionCalculatorFactory(FixedIncomeConverterDataProvider definitionToDerivativeConverter,
                                       DiscountingMulticurveCombinerFn discountingMulticurveCombinerFn,
                                       SwaptionSecurityConverter swaptionSecurityConverter,
                                       HistoricalTimeSeriesFn htsFn,
                                       SabrParametersProviderFn sabrParametersProviderFn) {
    _definitionToDerivativeConverter =
        ArgumentChecker.notNull(definitionToDerivativeConverter, "definitionToDerivativeConverter");
    _discountingMulticurveCombinerFn =
        ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
    _swaptionSecurityConverter = ArgumentChecker.notNull(swaptionSecurityConverter, "swaptionSecurityConverter");
    _htsFn = ArgumentChecker.notNull(htsFn, "htsFn");
    _sabrParametersProviderFn =
        ArgumentChecker.notNull(sabrParametersProviderFn, "sabrParametersProviderFn");

  }

  @Override
  public Result<SwaptionCalculator> createCalculator(Environment env,
                                                     SwaptionSecurity security) {

    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult =
        _discountingMulticurveCombinerFn.createMergedMulticurveBundle(env, security, Result.success(new FXMatrix()));

    Result<HistoricalTimeSeriesBundle> fixingsResult = _htsFn.getFixingsForSecurity(env, security);

    Result<SabrParametersConfiguration> sabrResult = _sabrParametersProviderFn.getSabrParameters(env, security);

    if (Result.allSuccessful(bundleResult, fixingsResult, sabrResult)) {

      MulticurveProviderDiscount multicurveBundle = bundleResult.getValue().getFirst();
      CurveBuildingBlockBundle blockBundle = bundleResult.getValue().getSecond();
      SwaptionPhysicalFixedIbor swaption =
          createInstrumentDerivative(security, env.getValuationTime(), fixingsResult.getValue());
      SabrParametersConfiguration sabrConfig = sabrResult.getValue();

      SwaptionCalculator calculator =
          new SabrSwaptionCalculator(swaption, buildSabrBundle(multicurveBundle, sabrConfig), blockBundle, sabrConfig.getSabrParameters());

      return Result.success(calculator);
    } else {
      return Result.failure(bundleResult, fixingsResult, sabrResult);
    }
  }


  private SABRSwaptionProviderDiscount buildSabrBundle(MulticurveProviderDiscount multicurveBundle,
                                                       SabrParametersConfiguration sabrConfig) {

    return new SABRSwaptionProviderDiscount(multicurveBundle, sabrConfig.getSabrParameters(), sabrConfig.getSwapConvention());
  }


  private SwaptionPhysicalFixedIbor createInstrumentDerivative(SwaptionSecurity security,
                                                          ZonedDateTime valuationTime,
                                                          HistoricalTimeSeriesBundle fixings) {
    InstrumentDefinition<?> definition = security.accept(_swaptionSecurityConverter);

    // Cast is necessary due to IMPLIED_VOL_CALCULATOR requiring it on method calls
    // todo - this is soooooo brittle - SwaptionCashFixedIbor is equally probable - PLAT-6364 should address
    return (SwaptionPhysicalFixedIbor) _definitionToDerivativeConverter.convert(security, definition, valuationTime, fixings);
  }
}
