/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.snapshot;

import java.util.Map;

import com.opengamma.core.link.SnapshotLink;
import com.opengamma.financial.analytics.isda.credit.CreditCurveData;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataSnapshot;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * A provider function which, given a credit key, will return a valid {@link CreditCurveData} instance.
 */
public class SnapshotCreditCurveDataProviderFn implements CreditCurveDataProviderFn {

  private final SnapshotLink<CreditCurveDataSnapshot> _snapshotLink;

  /**
   * Creates an instance.
   * @param snapshotLink a link to the snapshot to source curve data from
   */
  public SnapshotCreditCurveDataProviderFn(SnapshotLink<CreditCurveDataSnapshot> snapshotLink) {
    _snapshotLink = ArgumentChecker.notNull(snapshotLink, "snapshotLink");
  }

  @Override
  public Result<CreditCurveData> retrieveCreditCurveData(CreditCurveDataKey key) {
    CreditCurveDataSnapshot snapshotResult = _snapshotLink.resolve();
    
    Map<CreditCurveDataKey, CreditCurveData> creditCurveDataMap = snapshotResult.getCreditCurves();
    if (creditCurveDataMap.containsKey(key)) {
      return Result.success(creditCurveDataMap.get(key));
    } else {
      return Result.failure(FailureStatus.MISSING_DATA, 
                            "Failed to load curve data for credit curve key {} in snapshot {}", 
                            key, 
                            snapshotResult.getName());
    }
  }

}
