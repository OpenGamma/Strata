package com.opengamma.service;

import com.opengamma.id.VersionCorrection;

public interface VersionCorrectionProvider {
  VersionCorrection getPortfolioVersionCorrection();
  VersionCorrection getConfigVersionCorrection();
}
