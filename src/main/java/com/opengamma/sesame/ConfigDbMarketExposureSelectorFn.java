/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.success;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

public class ConfigDbMarketExposureSelectorFn implements MarketExposureSelectorFn {

  /** The exposure config name, not null */
  private final String _exposureConfigName;

  private final ConfigSource _configSource;
  private final SecuritySource _securitySource;

  public ConfigDbMarketExposureSelectorFn(String exposureConfigName,
                                          ConfigSource configSource,
                                          SecuritySource securitySource) {
    _securitySource = securitySource;
    _exposureConfigName = ArgumentChecker.notNull(exposureConfigName, "exposure config names");
    _configSource = ArgumentChecker.notNull(configSource, "config source");
  }

  @Override
  public Result<MarketExposureSelector> getMarketExposureSelector() {

    // todo - we should not be using latest
    ExposureFunctions exposures = _configSource.getLatestByName(ExposureFunctions.class, _exposureConfigName);
    if (exposures != null) {
      return success(new MarketExposureSelector(exposures, _securitySource, _configSource));
    } else {
      return failure(MISSING_DATA, "Could not get instrument exposure configuration called: {}", _exposureConfigName);
    }
  }
}
