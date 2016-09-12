/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import com.opengamma.strata.basics.currency.Currency;

/**
 * Builder used to create point sensitivities.
 * <p>
 * The sensitivity to a single point on a curve is known as <i>point sensitivity</i>.
 * This builder allows the individual sensitivities to be built into a combined result.
 * <p>
 * Implementations may be mutable, however the methods are intended to be used in an immutable style.
 * Once a method is called, code should refer and use only the result, not the original instance.
 */
public interface PointSensitivityBuilder {

  /**
   * Returns a builder representing no sensitivity.
   * <p>
   * This would be used if the rate was fixed, or if the rate was obtained from a historic
   * time-series rather than a forward curve.
   * 
   * @return the empty builder
   */
  public static PointSensitivityBuilder none() {
    return NoPointSensitivity.INSTANCE;
  }

  /**
   * Returns a builder with the specified sensitivities.
   * 
   * @param sensitivities  the list of sensitivities, which is copied
   * @return the builder
   */
  public static PointSensitivityBuilder of(PointSensitivity... sensitivities) {
    switch (sensitivities.length) {
      case 0:
        return PointSensitivityBuilder.none();
      case 1:
        PointSensitivity sens = sensitivities[0];
        if (sens instanceof PointSensitivityBuilder) {
          return (PointSensitivityBuilder) sens;
        }
        return new MutablePointSensitivities(sens);
      default:
        return new MutablePointSensitivities(Arrays.asList(sensitivities));
    }
  }

  /**
   * Returns a builder with the specified sensitivities.
   * 
   * @param sensitivities  the list of sensitivities, which is copied
   * @return the builder
   */
  public static PointSensitivityBuilder of(List<? extends PointSensitivity> sensitivities) {
    switch (sensitivities.size()) {
      case 0:
        return PointSensitivityBuilder.none();
      case 1:
        PointSensitivity sens = sensitivities.get(0);
        if (sens instanceof PointSensitivityBuilder) {
          return (PointSensitivityBuilder) sens;
        }
        return new MutablePointSensitivities(sens);
      default:
        return new MutablePointSensitivities(sensitivities);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified currency applied to the sensitivities in this builder.
   * <p>
   * The result will consists of the same points, but with the sensitivity currency assigned.
   * <p>
   * Builders may be mutable.
   * Once this method is called, this instance must not be used.
   * Instead, the result of the method must be used.
   *
   * @param currency  the currency to be applied to the sensitivities
   * @return the resulting builder, replacing this builder
   */
  public abstract PointSensitivityBuilder withCurrency(Currency currency);

  /**
   * Multiplies the sensitivities in this builder by the specified factor.
   * <p>
   * The result will consist of the same points, but with each sensitivity multiplied.
   * <p>
   * Builders may be mutable.
   * Once this method is called, this instance must not be used.
   * Instead, the result of the method must be used.
   * 
   * @param factor  the multiplicative factor
   * @return the resulting builder, replacing this builder
   */
  public default PointSensitivityBuilder multipliedBy(double factor) {
    return mapSensitivity(s -> s * factor);
  }

  /**
   * Returns an instance with the specified operation applied to the sensitivities in this builder.
   * <p>
   * The result will consist of the same points, but with the operator applied to each sensitivity.
   * <p>
   * This is used to apply a mathematical operation to the sensitivities.
   * For example, the operator could multiply the sensitivities by a constant, or take the inverse.
   * <pre>
   *   inverse = base.mapSensitivities(value -> 1 / value);
   * </pre>
   * <p>
   * Builders may be mutable.
   * Once this method is called, this instance must not be used.
   * Instead, the result of the method must be used.
   *
   * @param operator  the operator to be applied to the sensitivities
   * @return the resulting builder, replacing this builder
   */
  public abstract PointSensitivityBuilder mapSensitivity(DoubleUnaryOperator operator);

  /**
   * Normalizes the point sensitivities by sorting and merging.
   * <p>
   * The sensitivities in the builder are sorted and then merged.
   * Any two entries that represent the same curve query are merged.
   * For example, if there are two point sensitivities that were created based on the same curve,
   * currency and fixing date, then the entries are combined, summing the sensitivity value.
   * <p>
   * Builders may be mutable.
   * Once this method is called, this instance must not be used.
   * Instead, the result of the method must be used.
   * 
   * @return the resulting builder, replacing this builder
   */
  public abstract PointSensitivityBuilder normalize();

  //-------------------------------------------------------------------------
  /**
   * Combines this sensitivity with another instance.
   * <p>
   * This returns an instance with a combined list of point sensitivities.
   * <p>
   * Builders may be mutable.
   * Once this method is called, this instance must not be used.
   * Instead, the result of the method must be used.
   * 
   * @param other  the other sensitivity builder
   * @return the combined builder, replacing this builder and the specified builder
   */
  public default PointSensitivityBuilder combinedWith(PointSensitivityBuilder other) {
    if (other instanceof MutablePointSensitivities) {
      MutablePointSensitivities otherCombination = (MutablePointSensitivities) other;
      return buildInto(otherCombination);
    }
    MutablePointSensitivities combination = new MutablePointSensitivities();
    return other.buildInto(this.buildInto(combination));
  }

  /**
   * Builds the point sensitivity, adding to the specified mutable instance.
   * 
   * @param combination  the combination object to add to
   * @return the specified mutable point sensitivities instance is returned, for method chaining
   */
  public abstract MutablePointSensitivities buildInto(MutablePointSensitivities combination);

  /**
   * Builds the resulting point sensitivity.
   * <p>
   * This returns a {@link PointSensitivities} instance.
   * <p>
   * Builders may be mutable.
   * Once this method is called, this instance must not be used.
   *
   * @return the built combined sensitivity
   */
  public default PointSensitivities build() {
    return buildInto(new MutablePointSensitivities()).toImmutable();
  }

  /**
   * Clones the point sensitivity builder.
   * <p>
   * This returns a {@link PointSensitivityBuilder} instance that is independent
   * from the original. Immutable implementations may return themselves.
   * <p>
   * Builders may be mutable. Using this method allows a copy of the original
   * to be obtained, so both the original and the clone can be used.
   *
   * @return the built combined sensitivity
   */
  public abstract PointSensitivityBuilder cloned();

}
