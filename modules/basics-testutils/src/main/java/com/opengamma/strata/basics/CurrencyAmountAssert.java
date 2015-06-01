/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * An assert helper that provides useful AssertJ assertion
 * methods for {@link CurrencyAmount} instances.
 * <p>
 * These allow {code CurrencyAmount}s to be inspected in tests in the
 * same fluent style as other basic classes.
 * <p>
 * So the following:
 * <pre>
 *   CurrencyAmount result = someMethodCall();
 *   assertEquals(result.getCurrency(), USD);
 *   assertEquals(result.getAmount(), 123.45, 1e-6);
 * </pre>
 * can be replaced with:
 * <pre>
 *   CurrencyAmount result = someMethodCall();
 *   assertThat(result)
 *     .hasCurrency(USD)
 *     .hasAmount(123.45, within(1e-6));
 * </pre>
 * or:
 * <pre>
 *   CurrencyAmount result = someMethodCall();
 *   CurrencyAmount expected = CurrencyAmount.of(USD, 123.45);
 *   assertThat(result).isEqualTo(expected, within(1e-6));
 * </pre>
 * <p>
 * In order to be able to use a statically imported assertThat()
 * method for both {@code CurrencyAmount} and other types, statically
 * import {@link BasicProjectAssertions#assertThat(CurrencyAmount)}
 * rather than this class.
 */
public class CurrencyAmountAssert extends AbstractAssert<CurrencyAmountAssert, CurrencyAmount> {

  /**
   * Private constructor, use {@link #assertThat(CurrencyAmount)} to
   * construct an instance.
   *
   * @param actual the amount to create an {@code Assert} for
   */
  private CurrencyAmountAssert(CurrencyAmount actual) {
    super(actual, CurrencyAmountAssert.class);
  }

  /**
   * Create an {@code Assert} instance for the supplied {@code CurrencyAmount}.
   *
   * @param amount  the amount to create an {@code Assert} for
   * @return an {@code Assert} instance
   */
  public static CurrencyAmountAssert assertThat(CurrencyAmount amount) {
    return new CurrencyAmountAssert(amount);
  }

  /**
   * Assert that the {@code CurrencyAmount} is of the expected currency.
   *
   * @param ccy  the expected currency
   * @return this if the currency matches the expectation, else
   *   throw an {@code AssertionError}
   */
  public CurrencyAmountAssert hasCurrency(Currency ccy) {
    isNotNull();
    if (!actual.getCurrency().equals(ccy)) {
      failWithMessage("Expected CurrencyAmount with currency: <%s> but was: <%s>",
          ccy, actual.getCurrency());
    }
    return this;
  }

  /**
   * Assert that the {@code CurrencyAmount} is for the expected amount.
   *
   * @param expectedAmount  the expected amount
   * @return this if the amount matches the expectation, else
   *   throw an {@code AssertionError}
   */
  public CurrencyAmountAssert hasAmount(double expectedAmount) {
    isNotNull();
    Assertions.assertThat(actual.getAmount()).isEqualTo(expectedAmount);
    return this;
  }

  /**
   * Assert that the {@code CurrencyAmount} is within range
   * of an expected amount.
   *
   * @param expectedAmount  the expected amount
   * @param tolerance  the tolerance to use
   * @return this if the amount matches the expectation, else
   *   throw an {@code AssertionError}
   */
  public CurrencyAmountAssert hasAmount(double expectedAmount, Offset<Double> tolerance) {
    isNotNull();
    Assertions.assertThat(actual.getAmount()).isEqualTo(expectedAmount, tolerance);
    return this;
  }

  /**
   * Assert that the {@code CurrencyAmount} has the same currency as
   * the supplied {@code CurrencyAmount} and that the amount is within
   * range of the supplied {@code CurrencyAmount}'s amount.
   *
   * @param expected  the expected {@code CurrencyAmount}
   * @param tolerance  the tolerance to use
   * @return this if the amount matches the expectation, else
   *   throw an {@code AssertionError}
   */
  public CurrencyAmountAssert isEqualTo(CurrencyAmount expected, Offset<Double> tolerance) {
    isNotNull();
    hasCurrency(expected.getCurrency());
    hasAmount(expected.getAmount(), tolerance);
    return this;
  }

}
