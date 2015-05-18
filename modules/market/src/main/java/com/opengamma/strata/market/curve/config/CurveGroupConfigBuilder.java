/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.config;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A mutable builder for creating instances of {@link CurveGroupConfig}.
 */
@SuppressWarnings("unchecked")
public final class CurveGroupConfigBuilder {

  /** The entries in the curve group. */
  private final List<CurveGroupEntry> entries = new ArrayList<>();

  /** The name of the curve group. */
  private String name;

  /**
   * Sets the name of the curve group.
   *
   * @param name  the name of the curve group, not empty
   * @return this builder
   */
  public CurveGroupConfigBuilder name(String name) {
    ArgChecker.notEmpty(name, "name");
    this.name = name;
    return this;
  }

  /**
   * Adds an entry to the curve group containing the configuration for a curve and a key identifying
   * how the curve is used.
   * <p>
   * A curve can be used for multiple purposes and therefore the curve itself contains no information about how
   * it is used.
   * <p>
   * In the simple case a curve is only used for a single purpose. For example, if a curve is used for discounting
   * it will have a key of type {@code DiscountingCurveKey}.
   *
   * @param curveConfig  configuration of a curve
   * @param key  market data key identifying how the curve is used in the group
   * @return this builder
   */
  public CurveGroupConfigBuilder addCurve(CurveConfig curveConfig, MarketDataKey<YieldCurve> key) {
    ArgChecker.notNull(curveConfig, "curveConfig");
    ArgChecker.notNull(key, "key");

    CurveGroupEntry entry =
        CurveGroupEntry.builder()
            .curveConfig(curveConfig)
            .curveKeys(ImmutableSet.<MarketDataKey<YieldCurve>>builder().add(key).build())
            .build();
    entries.add(entry);
    return this;
  }

  /**
   * Adds an entry to the curve group containing the configuration for a curve and the keys identifying
   * how the curve is used.
   * <p>
   * A curve can be used for multiple purposes and therefore the curve itself contains no information about how
   * it is used.
   * <p>
   * In the simple case a curve is only used for a single purpose. For example, if a curve is used for discounting
   * it will have one key of type {@code DiscountingCurveKey}.
   * <p>
   * A single curve can also be used as both a discounting curve and a forward curve.
   * In that case its key set would contain a {@code DiscountingCurveKey} and a {@code RateIndexCurveKey}.
   * <p>
   * Every curve must be associated with at least once key.
   *
   * @param curveConfig  configuration of a curve
   * @param key  market data key identifying how the curve is used in the group
   * @param otherKeys  additional market data keys identifying how the curve is used in the group
   * @return this builder
   */
  public CurveGroupConfigBuilder addCurve(
      CurveConfig curveConfig,
      MarketDataKey<YieldCurve> key,
      MarketDataKey<YieldCurve>... otherKeys) {

    ArgChecker.notNull(curveConfig, "curveConfig");
    ArgChecker.notNull(key, "key");

    CurveGroupEntry entry =
        CurveGroupEntry.builder()
            .curveConfig(curveConfig)
            .curveKeys(ImmutableSet.<MarketDataKey<YieldCurve>>builder().add(key).add(otherKeys).build())
            .build();
    entries.add(entry);
    return this;
  }

  /**
   * Returns configuration for a curve group built from the data in this object.
   *
   * @return configuration for a curve group built from the data in this object
   */
  public CurveGroupConfig build() {
    return new CurveGroupConfig(name, entries);
  }
}
