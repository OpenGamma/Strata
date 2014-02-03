/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.success;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

public class ConfigDbMarketExposureSelectorFn implements MarketExposureSelectorFn {

  /** The exposure config, not null */
  private final ExposureFunctions _exposures;
  private final ConfigSource _configSource;
  private final SecuritySource _securitySource;

  public ConfigDbMarketExposureSelectorFn(ExposureFunctions exposureConfig,
                                          ConfigSource configSource,
                                          SecuritySource securitySource) {
    _exposures = ArgumentChecker.notNull(exposureConfig, "exposureConfig");
    _securitySource = ArgumentChecker.notNull(securitySource, "securitySource");
    _configSource = ArgumentChecker.notNull(configSource, "configSource");
  }

  @Override
  public Result<MarketExposureSelector> getMarketExposureSelector() {
    return success(new MarketExposureSelector(_exposures, _securitySource, _configSource));
  }
}
