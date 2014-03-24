/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.marketdata;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;

/**
 * Applies a shock to a market data value if its ID matches a rule.
 */
public abstract class MarketDataShock {

  /** For matching the market data ID. */
  private final MarketDataMatcher _matcher;

  /**
   * @param matcher for matching the market data ID
   */
  protected MarketDataShock(MarketDataMatcher matcher) {
    _matcher = ArgumentChecker.notNull(matcher, "matcher");
  }

  /**
   * Applies a shock to the value if the ID matches the rule, otherwise returns the value.
   *
   * @param id the ID of the market data
   * @param value the market data value
   * @return the shocked market data value if the ID matches the rule, otherwise the value itself
   */
  public final double apply(ExternalIdBundle id, double value) {
    ArgumentChecker.notNull(id, "id");

    if (_matcher.matches(id)) {
      return shock(value);
    } else {
      return value;
    }
  }

  /**
   * Applies the shock to the value. Invoked if the ID matches the rule.
   *
   * @param value the market data value
   * @return the shocked value
   */
  protected abstract double shock(double value);

  /**
   * Creates a shock that adds an absolute amount to the market data values.
   * The shock is only applied if the ID of the market data matches the matcher.
   *
   * @param shiftAmount the amount to add to the market data values
   * @param matcher decides if a value should be shocked
   * @return a shock to add an absolute amount to any matching data
   */
  public static MarketDataShock absoluteShift(double shiftAmount, MarketDataMatcher matcher) {
    return new AbsoluteShift(shiftAmount, matcher);
  }

  /**
   * Creates a shock that adds a relative amount to the market data values.
   * The shock is only applied if the ID of the market data matches the matcher.
   * A shift of 0.1 (+10%) scales the point value by 1.1, a shift of -0.2 (-20%) scales the point value by 0.8.
   *
   * @param shiftAmount the amount to add to the market data values
   * @param matcher decides if a value should be shocked
   * @return a shock to add a relative amount to any matching data
   */
  public static MarketDataShock relativeShift(double shiftAmount, MarketDataMatcher matcher) {
    return new RelativeShift(shiftAmount, matcher);
  }

  public static MarketDataShock replace(double value, MarketDataMatcher matcher) {
    return new Replace(value, matcher);
  }

  private static final class AbsoluteShift extends MarketDataShock {

    private final double _shift;

    private AbsoluteShift(double shift, MarketDataMatcher matcher) {
      super(matcher);
      _shift = shift;
    }

    @Override
    public double shock(double value) {
      return value + _shift;
    }
  }

  private static final class RelativeShift extends MarketDataShock {

    private final double _shift;

    private RelativeShift(double shift, MarketDataMatcher matcher) {
      super(matcher);
      // shift is specified as e.g. +10% = 0.1 which means scale by 1.1 or -20% = -0.2 scale by 0.8
      _shift = 1 + shift;
    }

    @Override
    public double shock(double value) {
      return value * _shift;
    }
  }

  private static final class Replace extends MarketDataShock {

    private final double _replacementValue;

    private Replace(double replacementValue, MarketDataMatcher matcher) {
      super(matcher);
      _replacementValue = replacementValue;
    }

    @Override
    public double shock(double value) {
      return _replacementValue;
    }
  }
}
