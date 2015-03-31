/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.builders;

import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.marketdata.curve.CurveGroup;
import com.opengamma.strata.marketdata.id.CurveGroupId;
import com.opengamma.strata.marketdata.id.DiscountingCurveId;
import com.opengamma.strata.pricer.impl.Legacy;

/**
 * Market data builder that builds a {@link YieldCurve} representing the discounting curve for a currency.
 * <p>
 * The curve is not actually built in this class, it is extracted from an existing {@link CurveGroup}
 * The curve group must be available in the {@code BaseMarketData} passed to the {@link #build} method.
 * <p>
 * This builder assumes all discounting curves are of type {@link YieldCurve}, although the signature of
 * {@code CurveGroup} allows them to be {@link YieldAndDiscountCurve}. This assumption is safe. In future
 * it is possible that it will change, but there are many other changes needed before other curve types
 * can be used.
 */
public class DiscountingCurveMarketDataBuilder extends SingleValueMarketDataBuilder<YieldCurve, DiscountingCurveId> {

  /**
   * Default constructor.
   */
  public DiscountingCurveMarketDataBuilder() {
    super(DiscountingCurveId.class);
  }

  @Override
  public MarketDataRequirements requirements(DiscountingCurveId id) {
    CurveGroupId curveGroupId = CurveGroupId.of(id.getCurveGroupName());
    return MarketDataRequirements.builder()
        // TODO Get rid of ImmutableSet.of when Joda Beans #104 is fixed
        .singleValueRequirements(ImmutableSet.of(curveGroupId))
        .build();
  }

  @Override
  protected Result<YieldCurve> buildSingleValue(DiscountingCurveId requirement, BaseMarketData builtData) {
    String curveGroupName = requirement.getCurveGroupName();
    CurveGroupId curveGroupId = CurveGroupId.of(curveGroupName);

    if (!builtData.containsValue(curveGroupId)) {
      return Result.failure(FailureReason.MISSING_DATA, "No curve group found with name {}", curveGroupId.getName());
    }
    CurveGroup curveGroup = builtData.getValue(curveGroupId);
    Result<YieldAndDiscountCurve> result = getCurve(curveGroup, curveGroupName, requirement.getCurrency());
    return result.flatMap(curve -> castCurve(curve, requirement));
  }

  /**
   * Returns a discounting curve from the curve group, or a failure if there isn't one for the currency.
   *
   * @param curveGroup  a curve group
   * @param curveGroupName  the name of the curve group
   * @param currency  the curve currency
   * @return a discounting curve from the curve group, or a failure if there isn't one for the currency
   */
  private static Result<YieldAndDiscountCurve> getCurve(
      CurveGroup curveGroup,
      String curveGroupName,
      Currency currency) {

    return Result.ofNullable(
        curveGroup.getMulticurveProvider().getCurve(Legacy.currency(currency)),
        FailureReason.MISSING_DATA,
        "No discounting curve available for currency {} in curve group {}",
        currency,
        curveGroupName);
  }

  /**
   * Returns a success result containing the curve if it is a {@link YieldCurve}, else returns a failure.
   *
   * @param curve  a curve
   * @param id  the curve ID
   * @return a success result containing the curve if it is a {@link YieldCurve}, else a failure
   */
  private Result<YieldCurve> castCurve(YieldAndDiscountCurve curve, DiscountingCurveId id) {
    return (curve instanceof YieldCurve) ?
        Result.success(((YieldCurve) curve)) :
        Result.failure(
            FailureReason.OTHER,
            "Curve with ID {} should be a YieldCurve but type is {}",
            id,
            curve.getClass().getName());
  }
}
