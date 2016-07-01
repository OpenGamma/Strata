package com.opengamma.strata.pricer.credit.cds;

import java.time.LocalDate;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;

public interface RecoveryRates {

  public abstract StandardId getLegalEntityId();

  public abstract double recoveryRate(LocalDate date);

  public abstract RecoveryRateSensitivity recoveryRatePointSensitivity(LocalDate date);

  public abstract CurrencyParameterSensitivities parameterSensitivities(RecoveryRateSensitivity pointSensitivity);
}
