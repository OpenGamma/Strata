/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import org.threeten.bp.Instant;

import com.opengamma.id.VersionCorrection;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.util.ArgumentChecker;

public class FixedInstantVersionCorrectionProvider implements VersionCorrectionProvider {

  private final Instant _versionAsOf;

  public FixedInstantVersionCorrectionProvider(Instant versionAsOf) {
    _versionAsOf = ArgumentChecker.notNull(versionAsOf, "versionAsOf");
  }

  public FixedInstantVersionCorrectionProvider() {
    this(Instant.now());
  }

  @Override
  public VersionCorrection getPortfolioVersionCorrection() {
    // todo - this needs to be integrated with the new engine caching, atm this will not respond to portfolio updates
    return VersionCorrection.ofVersionAsOf(_versionAsOf);
  }

  @Override
  public VersionCorrection getConfigVersionCorrection() {
    // todo - this needs to be integrated with the new engine caching, atm this will not respond to config updates
    return VersionCorrection.ofVersionAsOf(_versionAsOf);
  }
}
