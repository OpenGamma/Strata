/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.swaption;

import javax.annotation.Nonnull;

import com.opengamma.core.link.ConfigLink;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.sabr.SabrConfigSelector;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Provides a set of SABR parameters based on the
 * security passed. This implementation looks up a
 * named {@link SabrConfigSelector} object and then
 * uses that to get the SABR data.
 */
@Nonnull
public class DefaultSABRParametersProviderFn implements SABRParametersProviderFn {

  /**
   * The name of the configuration which contains the SABR
   * configuration. Note that a String is held as we want to
   * defer the lookup so that version correction and change
   * management can be handled in the same way as the rest
   * of the engine.
   */
  private final String _configurationName;

  /**
   * Create the provider, storing the configuration name.
   *
   * @param configurationName  the name of a {@link SabrConfigSelector}
   * config object, not null
   */
  public DefaultSABRParametersProviderFn(String configurationName) {
    _configurationName = ArgumentChecker.notNull(configurationName, "_configurationName");
  }

  @Override
  public Result<SabrParametersConfiguration> getSabrParameters(Environment env, FinancialSecurity security) {
    ConfigLink<SabrConfigSelector> configLink = ConfigLink.resolvable(_configurationName, SabrConfigSelector.class);
    SabrConfigSelector selector = configLink.resolve();
    return selector.getSabrConfig(security);
  }
}
