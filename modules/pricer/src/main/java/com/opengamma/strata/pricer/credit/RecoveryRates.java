/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.time.LocalDate;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;

/**
 * Recovery rates.
 * <p>
 * This represents the recovery rates of a legal entity.
 */
public interface RecoveryRates
    extends MarketDataView, ParameterizedData {

  /**
   * Obtains an instance from a curve.
   * <p>
   * If the curve is {@code ConstantCurve}, {@code ConstantRecoveryRates} is always instantiated.
   * If the curve is {@code NodalCurve}, {@code NodalRecoveryRates} is always instantiated.
   * 
   * @param legalEntityId  the legal entity identifier
   * @param valuationDate  the valuation date for which the curve is valid
   * @param curve  the underlying curve
   * @return the instance
   */
  public static RecoveryRates of(StandardId legalEntityId, LocalDate valuationDate, Curve curve) {
    if (curve.getMetadata().getYValueType().equals(ValueType.RECOVERY_RATE)) {
      if (curve instanceof ConstantCurve) {
        ConstantCurve constantCurve = (ConstantCurve) curve;
        return ConstantRecoveryRates.of(legalEntityId, valuationDate, constantCurve.getYValue());
      }
      if (curve instanceof NodalCurve) {
        NodalCurve nodalCurve = (NodalCurve) curve;
        return NodalRecoveryRates.of(legalEntityId, valuationDate, nodalCurve);
      }
    }
    throw new IllegalArgumentException("Unknown curve type");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the valuation date. 
   * 
   * @return the valuation date
   */
  @Override
  public abstract LocalDate getValuationDate();

  /**
   * Gets the standard identifier of a legal entity.
   * 
   * @return the legal entity ID
   */
  public abstract StandardId getLegalEntityId();

  /**
   * Gets the recovery rate for the specified date. 
   * 
   * @param date  the date
   * @return the recovery rate
   */
  public abstract double recoveryRate(LocalDate date);

  @Override
  public abstract RecoveryRates withParameter(int parameterIndex, double newValue);

  @Override
  public abstract RecoveryRates withPerturbation(ParameterPerturbation perturbation);

}
