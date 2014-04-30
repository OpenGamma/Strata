/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.sabr;

import com.opengamma.analytics.financial.instrument.index.GeneratorSwapFixedIbor;
import com.opengamma.analytics.financial.model.option.definition.SABRInterestRateParameters;
import com.opengamma.util.ArgumentChecker;

/**
 * Holds the complete configuration required for using SABR parameters for
 * volatility data.
 */
public class SabrParametersConfiguration {

  /**
   * The sabr data, not null.
   */
  private final SABRInterestRateParameters _sabrParameters;

  /**
   * The swap convention to be used, not null.
   */
  private final GeneratorSwapFixedIbor _swapConvention;

  /**
   * Constructor for the configuration.
   *
   * @param sabrParameters the sabr data, not null
   * @param swapConvention the swap convention to be used, not null
   */
  public SabrParametersConfiguration(SABRInterestRateParameters sabrParameters, GeneratorSwapFixedIbor swapConvention) {
    _sabrParameters = ArgumentChecker.notNull(sabrParameters, "sabrParameters");
    _swapConvention = ArgumentChecker.notNull(swapConvention, "swapConvention");
  }

  /**
   * Get the sabr data.
   *
   * @return the sabr data, not null
   */
  public SABRInterestRateParameters getSabrParameters() {
    return _sabrParameters;
  }

  /**
   * Get the swap convention to be used.
   *
   * @return the swap convention, not null
   */
  public GeneratorSwapFixedIbor getSwapConvention() {
    return _swapConvention;
  }
}
