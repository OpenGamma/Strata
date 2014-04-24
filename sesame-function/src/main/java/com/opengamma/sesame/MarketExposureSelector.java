/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunction;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctionFactory;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.util.ArgumentChecker;

/**
 * Responsible for taking the set of available exposure functions and determining the
 * set of curve configurations that are applicable for a particular security.
 *
 * Note that this functionality should really be provided by the ExposureFunctions config
 * object i.e. obeying tell, don't ask.
 */
public class MarketExposureSelector {

  private final SecuritySource _securitySource;
  private final ConfigSource _configSource;

  private final List<ExposureFunction> _exposureFunctions;
  private final Map<ExternalId, String> _idsToNames;

  public MarketExposureSelector(ExposureFunctions exposure, SecuritySource securitySource, ConfigSource configSource) {
    _securitySource = securitySource;
    _configSource = configSource;
    ArgumentChecker.notNull(exposure, "exposure");

    _exposureFunctions = extractFunctions(exposure.getExposureFunctions());
    _idsToNames = exposure.getIdsToNames();
  }

  private List<ExposureFunction> extractFunctions(List<String> exposureFunctions) {

    return Lists.transform(exposureFunctions, new Function<String, ExposureFunction>() {
      @Override
      public ExposureFunction apply(String name) {
        return ExposureFunctionFactory.getExposureFunction(_securitySource, name);
      }
    });
  }

  /**
   * Returns the set of curves that are required for the supplied security.
   *
   * @param security the security to find curves for, not null
   * @return the set of names of the required curves, not null
   */
  public Set<CurveConstructionConfiguration> determineCurveConfigurationsForSecurity(FinancialSecurity security) {

    for (ExposureFunction exposureFunction : _exposureFunctions) {

      List<ExternalId> ids = security.accept(exposureFunction);

      if (ids != null && !ids.isEmpty()) {

        Set<CurveConstructionConfiguration> curveConfigs = new HashSet<>();

        for (final ExternalId id : ids) {

          final String name = _idsToNames.get(id);
          if (name != null) {
            curveConfigs.add(resolve(name));
          } else {
            break;
          }
        }

        return curveConfigs;
      }
    }

    return ImmutableSet.of();
  }

  private CurveConstructionConfiguration resolve(String name) {
    return ConfigLink.resolvable(name, CurveConstructionConfiguration.class).resolve();
  }

}
