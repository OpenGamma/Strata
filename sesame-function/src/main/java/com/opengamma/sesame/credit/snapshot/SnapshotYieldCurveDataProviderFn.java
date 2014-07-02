/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.snapshot;

import java.util.Map;

import com.opengamma.core.link.SnapshotLink;
import com.opengamma.financial.analytics.isda.credit.YieldCurveData;
import com.opengamma.financial.analytics.isda.credit.YieldCurveDataSnapshot;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * A {@link YieldCurveDataProviderFn} which sources {@link YieldCurveData} instances from a 
 * {@link YieldCurveDataSnapshot}.
 */
public class SnapshotYieldCurveDataProviderFn implements YieldCurveDataProviderFn {
  
  private final SnapshotLink<YieldCurveDataSnapshot> _snapshotLink;

  /**
   * Creates an instance which sources data from the given snapshot link.
   * @param snapshotLink the snapshot instance to use
   */
  public SnapshotYieldCurveDataProviderFn(SnapshotLink<YieldCurveDataSnapshot> snapshotLink) {
    _snapshotLink = ArgumentChecker.notNull(snapshotLink, "snapshotLink");
  }

  @Override
  public Result<YieldCurveData> retrieveYieldCurveData(Currency currency) {
    YieldCurveDataSnapshot snapshotResult = _snapshotLink.resolve();
    
    Map<Currency, YieldCurveData> creditCurveDataMap = snapshotResult.getYieldCurves();
    if (creditCurveDataMap.containsKey(currency)) {
      return Result.success(creditCurveDataMap.get(currency));
    } else {
      return Result.failure(FailureStatus.MISSING_DATA, 
                            "Failed to load curve data for credit curve key {} in snapshot {}", 
                            currency, 
                            snapshotResult.getName());
    }
  }

}
