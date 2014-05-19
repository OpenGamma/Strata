/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cashflows;

/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.DerivedProperty;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.threeten.bp.LocalDate;

import com.google.common.collect.Lists;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.time.Tenor;

/**
 * Container for the relevant details for pricing a floating swap leg, with the entries
 * <ul>
 * <li>accrualStartDates</li>
 * <li>accrualEndDates</li>
 * <li>accrualYearFractions</li>
 * <li>fixingStart</li>
 * <li>fixingEnd</li>
 * <li>fixingYearFractions</li>
 * <li>forwardRates</li>
 * <li>fixedRates</li>
 * <li>paymentDates</li>
 * <li>paymentTimes</li>
 * <li>paymentDiscountFactors</li>
 * <li>paymentAmounts</li>
 * <li>projectedAmounts</li>
 * <li>notionals</li>
 * <li>spreads</li>
 * <li>gearings</li>
 * <li>indexTenors</li>
 * </ul>
 */
@BeanDefinition
public class FloatingLegCashFlows implements ImmutableBean, SwapLegCashFlows {

  //TODO replace these static strings with an annotation and reference linked to the corresponding variable - PLAT-6507
  /**
   * The accrual fraction label.
   */
  public static final String ACCRUAL_YEAR_FRACTION = "Accrual Year Fraction";
  /**
   * The start fixing date label.
   */
  public static final String START_FIXING_DATES = "Start Fixing Date";
  /**
   * The end fixing date label.
   */
  public static final String END_FIXING_DATES = "End Fixing Date";
  /**
   * The fixing fraction label.
   */
  public static final String FIXING_FRACTIONS = "Fixing Year Fraction";
  /**
   * The forward rate. Used when the fixing is in the future.
   */
  public static final String FORWARD_RATE = "Forward Rate";
  /**
   * The fixed rate. Used when the fixing is known.
   */
  public static final String FIXED_RATE = "Fixed Rate";
  /**
   * The payment date.
   */
  public static final String PAYMENT_DATE = "Payment Date";
  /**
   * The payment amount.
   */
  public static final String PAYMENT_AMOUNT = "Payment Amount";
  /**
   * The spread.
   */
  public static final String SPREAD = "Spread";
  /**
   * The gearing.
   */
  public static final String GEARING = "Gearing";
  /**
   * The payment discount factor. Used when the fixing is known
   */
  public static final String PAYMENT_DISCOUNT_FACTOR = "Payment Discount Factor";
  /**
   * The projected amount.
   */
  public static final String PROJECTED_AMOUNT = "Projected Amount";
  /**
   * The index tenor.
   */
  public static final String INDEX_TERM = "Index Tenor";
  /**
   * The discounted payment amount
   */
  public static final String DISCOUNTED_PAYMENT_AMOUNT = "Discounted Payment Amount";
  /**
   * The discounted projected amount
   */
  public static final String DISCOUNTED_PROJECTED_PAYMENT = "Discounted Projected Payment";
  /**
   * The start accrual dates label.
   */
  public static final String START_ACCRUAL_DATES = "Start Accrual Date";
  /**
   * The end accrual dates label.
   */
  public static final String END_ACCRUAL_DATES = "End Accrual Date";
  /**
   * The notional label.
   */
  public static final String NOTIONAL = "Notional";
  /**
   * The payment time label.
   */
  public static final String PAYMENT_TIME = "Payment Time";
  /**
   * An array of accrual start dates.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<LocalDate> _accrualStart;
  /**
   * An array of accrual end dates.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<LocalDate> _accrualEnd;
  /**
   * An array of accrual year fractions.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<Double> _accrualYearFractions;
  /**
   * An array of fixing start dates.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<LocalDate> _fixingStart;
  /**
   * An array of fixing end dates.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<LocalDate> _fixingEnd;
  /**
   * An array of fixing year fractions. May contain null values if there have been fixings as of the valuation date.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<Double> _fixingYearFractions;
  /**
   * An array of forward rates.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<Double> _forwardRates;
  /**
   * An array of fixed rates. May contain null values if there have been no fixings as of the valuation date.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<Double> _fixedRates;
  /**
   * An array of payment dates.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<LocalDate> _paymentDates;
  /**
   * An array of payment times.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<Double> _paymentTimes;
  /**
   * An array of payment discount factors.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<Double> _paymentDiscountFactors;
  /**
   * An array of payment amounts. May contain nulls if there have been no fixings as of the valuation date.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<CurrencyAmount> _paymentAmounts;
  /**
   * An array of projected amounts. May contain nulls if there has been a fixing as of the valuation date.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<CurrencyAmount> _projectedAmounts;
  /**
   * An array of notionals.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<CurrencyAmount> _notionals;
  /**
   * An array of spreads.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<Double> _spreads;
  /**
   * An array of gearings.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<Double> _gearings;
  /**
   * An array of index tenors. May contain nulls if there has been a fixing as of the valuation date.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<Tenor> _indexTenors;

  /**
   * @param startAccrualDates The start accrual dates, not null
   * @param endAccrualDates The end accrual dates, not null
   * @param accrualYearFractions The accrual year fractions, not null
   * @param fixingStart The fixing start dates, not null
   * @param fixingEnd The fixing end dates, not null
   * @param fixingYearFractions The fixing year fractions, not null
   * @param forwardRates The forward rates, not null
   * @param fixedRates The fixed rates, not null
   * @param paymentDates The payment dates, not null
   * @param paymentTimes The payment times, not null
   * @param paymentDiscountFactors The payment discount factors, not null
   * @param paymentAmounts The payment amounts, not null
   * @param projectedAmounts The projected amounts, not null
   * @param notionals The notionals, not null
   * @param spreads The spreads, not null
   * @param gearings The gearings, not null
   * @param indexTenors The index tenors, not null
   */
  @ImmutableConstructor
  public FloatingLegCashFlows(List<LocalDate> startAccrualDates,
                              List<LocalDate> endAccrualDates,
                              List<Double> accrualYearFractions,
                              List<LocalDate> fixingStart,
                              List<LocalDate> fixingEnd,
                              List<Double> fixingYearFractions,
                              List<Double> forwardRates,
                              List<Double> fixedRates,
                              List<LocalDate> paymentDates,
                              List<Double> paymentTimes,
                              List<Double> paymentDiscountFactors,
                              List<CurrencyAmount> paymentAmounts,
                              List<CurrencyAmount> projectedAmounts,
                              List<CurrencyAmount> notionals,
                              List<Double> spreads,
                              List<Double> gearings,
                              List<Tenor> indexTenors) {

    ArgumentChecker.notNull(startAccrualDates, "startAccrualDates");
    ArgumentChecker.notNull(endAccrualDates, "endAccrualDates");
    ArgumentChecker.notNull(accrualYearFractions, "accrualYearFractions");
    ArgumentChecker.notNull(fixingStart, "fixingStart");
    ArgumentChecker.notNull(fixingEnd, "fixingEnd");
    ArgumentChecker.notNull(fixingYearFractions, "fixingYearFractions");
    ArgumentChecker.notNull(forwardRates, "forwardRates");
    ArgumentChecker.notNull(fixedRates, "fixedRates");
    ArgumentChecker.notNull(paymentDates, "paymentDates");
    ArgumentChecker.notNull(paymentTimes, "paymentTimes");
    ArgumentChecker.notNull(paymentDiscountFactors, "paymentDiscountFactors");
    ArgumentChecker.notNull(paymentAmounts, "paymentAmounts");
    ArgumentChecker.notNull(projectedAmounts, "projectedAmounts");
    ArgumentChecker.notNull(notionals, "notionals");
    ArgumentChecker.notNull(spreads, "spreads");
    ArgumentChecker.notNull(gearings, "gearings");
    ArgumentChecker.notNull(indexTenors, "indexTenors");

    _accrualStart = Collections.unmodifiableList(Lists.newArrayList(startAccrualDates));
    _accrualEnd = Collections.unmodifiableList(Lists.newArrayList(endAccrualDates));
    _notionals = Collections.unmodifiableList(Lists.newArrayList(notionals));
    _paymentTimes = Collections.unmodifiableList(Lists.newArrayList(paymentTimes));
    _accrualYearFractions = Collections.unmodifiableList(Lists.newArrayList(accrualYearFractions));
    _fixingStart = Collections.unmodifiableList(Lists.newArrayList(fixingStart));
    _fixingEnd = Collections.unmodifiableList(Lists.newArrayList(fixingEnd));
    _fixingYearFractions = Collections.unmodifiableList(Lists.newArrayList(fixingYearFractions));
    _forwardRates = Collections.unmodifiableList(Lists.newArrayList(forwardRates));
    _paymentDates = Collections.unmodifiableList(Lists.newArrayList(paymentDates));
    _fixedRates = Collections.unmodifiableList(Lists.newArrayList(fixedRates));
    _paymentDiscountFactors = Collections.unmodifiableList(Lists.newArrayList(paymentDiscountFactors));
    _paymentAmounts = Collections.unmodifiableList(Lists.newArrayList(paymentAmounts));
    _projectedAmounts = Collections.unmodifiableList(Lists.newArrayList(projectedAmounts));
    _spreads = Collections.unmodifiableList(Lists.newArrayList(spreads));
    _gearings = Collections.unmodifiableList(Lists.newArrayList(gearings));
    _indexTenors = Collections.unmodifiableList(Lists.newArrayList(indexTenors));

    int n = notionals.size();
    ArgumentChecker.isTrue(n == startAccrualDates.size(), "number of accrual start dates must equal number of notionals");
    ArgumentChecker.isTrue(n == endAccrualDates.size(), "number of accrual end dates must equal number of notionals");
    ArgumentChecker.isTrue(n == accrualYearFractions.size(), "number of accrual year fractions must equal number of notionals");
    ArgumentChecker.isTrue(n == fixingStart.size(), "number of fixing start dates must equal number of notionals");
    ArgumentChecker.isTrue(n == fixingEnd.size(), "number of fixing end dates must equal number of notionals");
    ArgumentChecker.isTrue(n == fixingYearFractions.size(), "number of fixing year fractions must equal number of notionals");
    ArgumentChecker.isTrue(n == forwardRates.size(), "number of forward rates must equal number of notionals");
    ArgumentChecker.isTrue(n == fixedRates.size(), "number of fixed rates must equal number of notionals");
    ArgumentChecker.isTrue(n == paymentDates.size(), "number of payment dates must equal number of notionals");
    ArgumentChecker.isTrue(n == paymentDiscountFactors.size(), "number of payment discount factors must equal number of notionals");
    ArgumentChecker.isTrue(n == paymentAmounts.size(), "number of payment amounts must equal number of notionals");
    ArgumentChecker.isTrue(n == projectedAmounts.size(), "number of projected amounts must equal number of notionals");
    ArgumentChecker.isTrue(n == spreads.size(), "number of spreads must equal number of notionals");
    ArgumentChecker.isTrue(n == gearings.size(), "number of gearings must equal number of notionals");
    ArgumentChecker.isTrue(n == indexTenors.size(), "number of index tenors must equal number of notionals");
  }

  /**
   * Gets the number of fixed cash-flows.
   * @return The number of fixed cash-flows
   */
  @DerivedProperty
  public int getNumberOfFixedCashFlows() {
    return getFixedRates().size();
  }

  /**
   * Gets the number of floating cash-flows.
   * @return The number of floating cash-flows
   */
  @DerivedProperty
  public int getNumberOfFloatingCashFlows() {
    return getForwardRates().size();
  }

  /**
   * Gets the number of cash-flows.
   * @return the number of cash-flows
   */
  @DerivedProperty
  public int getNumberOfCashFlows() {
    return getAccrualStart().size();
  }

  /**
   * Gets the discounted payment amounts.
   * @return the discounted cashflows
   */
  @DerivedProperty
  public List<CurrencyAmount> getDiscountedPaymentAmounts() {
    List<CurrencyAmount> cashflows = new ArrayList<>();
    for (int i = 0; i < getNumberOfCashFlows(); i++) {
      CurrencyAmount payment = getPaymentAmounts().get(i);
      if (payment == null) {
        cashflows.add(null);
        continue;
      }
      double df = getPaymentDiscountFactors().get(i);
      cashflows.add(CurrencyAmount.of(payment.getCurrency(), payment.getAmount() * df));
    }
    return cashflows;
  }

  /**
   * Gets the discounted projected payment amounts.
   * @return the discounted cashflows
   */
  @DerivedProperty
  public List<CurrencyAmount> getDiscountedProjectedAmounts() {
    List<CurrencyAmount> cashflows = new ArrayList<>();
    for (int i = 0; i < getNumberOfCashFlows(); i++) {
      CurrencyAmount payment = getProjectedAmounts().get(i);
      if (payment == null) {
        cashflows.add(null);
        continue;
      }
      double df = getPaymentDiscountFactors().get(i);
      cashflows.add(CurrencyAmount.of(payment.getCurrency(), payment.getAmount() * df));
    }
    return cashflows;
  }
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FloatingLegCashFlows}.
   * @return the meta-bean, not null
   */
  public static FloatingLegCashFlows.Meta meta() {
    return FloatingLegCashFlows.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FloatingLegCashFlows.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FloatingLegCashFlows.Builder builder() {
    return new FloatingLegCashFlows.Builder();
  }

  @Override
  public FloatingLegCashFlows.Meta metaBean() {
    return FloatingLegCashFlows.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of accrual start dates.
   * @return the value of the property, not null
   */
  public List<LocalDate> getAccrualStart() {
    return _accrualStart;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of accrual end dates.
   * @return the value of the property, not null
   */
  public List<LocalDate> getAccrualEnd() {
    return _accrualEnd;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of accrual year fractions.
   * @return the value of the property, not null
   */
  public List<Double> getAccrualYearFractions() {
    return _accrualYearFractions;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of fixing start dates.
   * @return the value of the property, not null
   */
  public List<LocalDate> getFixingStart() {
    return _fixingStart;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of fixing end dates.
   * @return the value of the property, not null
   */
  public List<LocalDate> getFixingEnd() {
    return _fixingEnd;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of fixing year fractions. May contain null values if there have been fixings as of the valuation date.
   * @return the value of the property, not null
   */
  public List<Double> getFixingYearFractions() {
    return _fixingYearFractions;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of forward rates.
   * @return the value of the property, not null
   */
  public List<Double> getForwardRates() {
    return _forwardRates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of fixed rates. May contain null values if there have been no fixings as of the valuation date.
   * @return the value of the property, not null
   */
  public List<Double> getFixedRates() {
    return _fixedRates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of payment dates.
   * @return the value of the property, not null
   */
  public List<LocalDate> getPaymentDates() {
    return _paymentDates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of payment times.
   * @return the value of the property, not null
   */
  public List<Double> getPaymentTimes() {
    return _paymentTimes;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of payment discount factors.
   * @return the value of the property, not null
   */
  public List<Double> getPaymentDiscountFactors() {
    return _paymentDiscountFactors;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of payment amounts. May contain nulls if there have been no fixings as of the valuation date.
   * @return the value of the property, not null
   */
  public List<CurrencyAmount> getPaymentAmounts() {
    return _paymentAmounts;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of projected amounts. May contain nulls if there has been a fixing as of the valuation date.
   * @return the value of the property, not null
   */
  public List<CurrencyAmount> getProjectedAmounts() {
    return _projectedAmounts;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of notionals.
   * @return the value of the property, not null
   */
  public List<CurrencyAmount> getNotionals() {
    return _notionals;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of spreads.
   * @return the value of the property, not null
   */
  public List<Double> getSpreads() {
    return _spreads;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of gearings.
   * @return the value of the property, not null
   */
  public List<Double> getGearings() {
    return _gearings;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of index tenors. May contain nulls if there has been a fixing as of the valuation date.
   * @return the value of the property, not null
   */
  public List<Tenor> getIndexTenors() {
    return _indexTenors;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public FloatingLegCashFlows clone() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FloatingLegCashFlows other = (FloatingLegCashFlows) obj;
      return JodaBeanUtils.equal(getAccrualStart(), other.getAccrualStart()) &&
          JodaBeanUtils.equal(getAccrualEnd(), other.getAccrualEnd()) &&
          JodaBeanUtils.equal(getAccrualYearFractions(), other.getAccrualYearFractions()) &&
          JodaBeanUtils.equal(getFixingStart(), other.getFixingStart()) &&
          JodaBeanUtils.equal(getFixingEnd(), other.getFixingEnd()) &&
          JodaBeanUtils.equal(getFixingYearFractions(), other.getFixingYearFractions()) &&
          JodaBeanUtils.equal(getForwardRates(), other.getForwardRates()) &&
          JodaBeanUtils.equal(getFixedRates(), other.getFixedRates()) &&
          JodaBeanUtils.equal(getPaymentDates(), other.getPaymentDates()) &&
          JodaBeanUtils.equal(getPaymentTimes(), other.getPaymentTimes()) &&
          JodaBeanUtils.equal(getPaymentDiscountFactors(), other.getPaymentDiscountFactors()) &&
          JodaBeanUtils.equal(getPaymentAmounts(), other.getPaymentAmounts()) &&
          JodaBeanUtils.equal(getProjectedAmounts(), other.getProjectedAmounts()) &&
          JodaBeanUtils.equal(getNotionals(), other.getNotionals()) &&
          JodaBeanUtils.equal(getSpreads(), other.getSpreads()) &&
          JodaBeanUtils.equal(getGearings(), other.getGearings()) &&
          JodaBeanUtils.equal(getIndexTenors(), other.getIndexTenors()) &&
          (getNumberOfFixedCashFlows() == other.getNumberOfFixedCashFlows()) &&
          (getNumberOfFloatingCashFlows() == other.getNumberOfFloatingCashFlows()) &&
          (getNumberOfCashFlows() == other.getNumberOfCashFlows()) &&
          JodaBeanUtils.equal(getDiscountedPaymentAmounts(), other.getDiscountedPaymentAmounts()) &&
          JodaBeanUtils.equal(getDiscountedProjectedAmounts(), other.getDiscountedProjectedAmounts());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getAccrualStart());
    hash += hash * 31 + JodaBeanUtils.hashCode(getAccrualEnd());
    hash += hash * 31 + JodaBeanUtils.hashCode(getAccrualYearFractions());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixingStart());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixingEnd());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixingYearFractions());
    hash += hash * 31 + JodaBeanUtils.hashCode(getForwardRates());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixedRates());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentDates());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentTimes());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentDiscountFactors());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentAmounts());
    hash += hash * 31 + JodaBeanUtils.hashCode(getProjectedAmounts());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNotionals());
    hash += hash * 31 + JodaBeanUtils.hashCode(getSpreads());
    hash += hash * 31 + JodaBeanUtils.hashCode(getGearings());
    hash += hash * 31 + JodaBeanUtils.hashCode(getIndexTenors());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNumberOfFixedCashFlows());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNumberOfFloatingCashFlows());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNumberOfCashFlows());
    hash += hash * 31 + JodaBeanUtils.hashCode(getDiscountedPaymentAmounts());
    hash += hash * 31 + JodaBeanUtils.hashCode(getDiscountedProjectedAmounts());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(736);
    buf.append("FloatingLegCashFlows{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("accrualStart").append('=').append(JodaBeanUtils.toString(getAccrualStart())).append(',').append(' ');
    buf.append("accrualEnd").append('=').append(JodaBeanUtils.toString(getAccrualEnd())).append(',').append(' ');
    buf.append("accrualYearFractions").append('=').append(JodaBeanUtils.toString(getAccrualYearFractions())).append(',').append(' ');
    buf.append("fixingStart").append('=').append(JodaBeanUtils.toString(getFixingStart())).append(',').append(' ');
    buf.append("fixingEnd").append('=').append(JodaBeanUtils.toString(getFixingEnd())).append(',').append(' ');
    buf.append("fixingYearFractions").append('=').append(JodaBeanUtils.toString(getFixingYearFractions())).append(',').append(' ');
    buf.append("forwardRates").append('=').append(JodaBeanUtils.toString(getForwardRates())).append(',').append(' ');
    buf.append("fixedRates").append('=').append(JodaBeanUtils.toString(getFixedRates())).append(',').append(' ');
    buf.append("paymentDates").append('=').append(JodaBeanUtils.toString(getPaymentDates())).append(',').append(' ');
    buf.append("paymentTimes").append('=').append(JodaBeanUtils.toString(getPaymentTimes())).append(',').append(' ');
    buf.append("paymentDiscountFactors").append('=').append(JodaBeanUtils.toString(getPaymentDiscountFactors())).append(',').append(' ');
    buf.append("paymentAmounts").append('=').append(JodaBeanUtils.toString(getPaymentAmounts())).append(',').append(' ');
    buf.append("projectedAmounts").append('=').append(JodaBeanUtils.toString(getProjectedAmounts())).append(',').append(' ');
    buf.append("notionals").append('=').append(JodaBeanUtils.toString(getNotionals())).append(',').append(' ');
    buf.append("spreads").append('=').append(JodaBeanUtils.toString(getSpreads())).append(',').append(' ');
    buf.append("gearings").append('=').append(JodaBeanUtils.toString(getGearings())).append(',').append(' ');
    buf.append("indexTenors").append('=').append(JodaBeanUtils.toString(getIndexTenors())).append(',').append(' ');
    buf.append("numberOfFixedCashFlows").append('=').append(JodaBeanUtils.toString(getNumberOfFixedCashFlows())).append(',').append(' ');
    buf.append("numberOfFloatingCashFlows").append('=').append(JodaBeanUtils.toString(getNumberOfFloatingCashFlows())).append(',').append(' ');
    buf.append("numberOfCashFlows").append('=').append(JodaBeanUtils.toString(getNumberOfCashFlows())).append(',').append(' ');
    buf.append("discountedPaymentAmounts").append('=').append(JodaBeanUtils.toString(getDiscountedPaymentAmounts())).append(',').append(' ');
    buf.append("discountedProjectedAmounts").append('=').append(JodaBeanUtils.toString(getDiscountedProjectedAmounts())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FloatingLegCashFlows}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code accrualStart} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<LocalDate>> _accrualStart = DirectMetaProperty.ofImmutable(
        this, "accrualStart", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code accrualEnd} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<LocalDate>> _accrualEnd = DirectMetaProperty.ofImmutable(
        this, "accrualEnd", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code accrualYearFractions} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Double>> _accrualYearFractions = DirectMetaProperty.ofImmutable(
        this, "accrualYearFractions", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code fixingStart} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<LocalDate>> _fixingStart = DirectMetaProperty.ofImmutable(
        this, "fixingStart", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code fixingEnd} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<LocalDate>> _fixingEnd = DirectMetaProperty.ofImmutable(
        this, "fixingEnd", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code fixingYearFractions} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Double>> _fixingYearFractions = DirectMetaProperty.ofImmutable(
        this, "fixingYearFractions", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code forwardRates} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Double>> _forwardRates = DirectMetaProperty.ofImmutable(
        this, "forwardRates", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code fixedRates} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Double>> _fixedRates = DirectMetaProperty.ofImmutable(
        this, "fixedRates", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code paymentDates} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<LocalDate>> _paymentDates = DirectMetaProperty.ofImmutable(
        this, "paymentDates", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code paymentTimes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Double>> _paymentTimes = DirectMetaProperty.ofImmutable(
        this, "paymentTimes", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code paymentDiscountFactors} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Double>> _paymentDiscountFactors = DirectMetaProperty.ofImmutable(
        this, "paymentDiscountFactors", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code paymentAmounts} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<CurrencyAmount>> _paymentAmounts = DirectMetaProperty.ofImmutable(
        this, "paymentAmounts", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code projectedAmounts} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<CurrencyAmount>> _projectedAmounts = DirectMetaProperty.ofImmutable(
        this, "projectedAmounts", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code notionals} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<CurrencyAmount>> _notionals = DirectMetaProperty.ofImmutable(
        this, "notionals", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code spreads} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Double>> _spreads = DirectMetaProperty.ofImmutable(
        this, "spreads", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code gearings} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Double>> _gearings = DirectMetaProperty.ofImmutable(
        this, "gearings", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code indexTenors} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Tenor>> _indexTenors = DirectMetaProperty.ofImmutable(
        this, "indexTenors", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code numberOfFixedCashFlows} property.
     */
    private final MetaProperty<Integer> _numberOfFixedCashFlows = DirectMetaProperty.ofDerived(
        this, "numberOfFixedCashFlows", FloatingLegCashFlows.class, Integer.TYPE);
    /**
     * The meta-property for the {@code numberOfFloatingCashFlows} property.
     */
    private final MetaProperty<Integer> _numberOfFloatingCashFlows = DirectMetaProperty.ofDerived(
        this, "numberOfFloatingCashFlows", FloatingLegCashFlows.class, Integer.TYPE);
    /**
     * The meta-property for the {@code numberOfCashFlows} property.
     */
    private final MetaProperty<Integer> _numberOfCashFlows = DirectMetaProperty.ofDerived(
        this, "numberOfCashFlows", FloatingLegCashFlows.class, Integer.TYPE);
    /**
     * The meta-property for the {@code discountedPaymentAmounts} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<CurrencyAmount>> _discountedPaymentAmounts = DirectMetaProperty.ofDerived(
        this, "discountedPaymentAmounts", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-property for the {@code discountedProjectedAmounts} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<CurrencyAmount>> _discountedProjectedAmounts = DirectMetaProperty.ofDerived(
        this, "discountedProjectedAmounts", FloatingLegCashFlows.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "accrualStart",
        "accrualEnd",
        "accrualYearFractions",
        "fixingStart",
        "fixingEnd",
        "fixingYearFractions",
        "forwardRates",
        "fixedRates",
        "paymentDates",
        "paymentTimes",
        "paymentDiscountFactors",
        "paymentAmounts",
        "projectedAmounts",
        "notionals",
        "spreads",
        "gearings",
        "indexTenors",
        "numberOfFixedCashFlows",
        "numberOfFloatingCashFlows",
        "numberOfCashFlows",
        "discountedPaymentAmounts",
        "discountedProjectedAmounts");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1071260659:  // accrualStart
          return _accrualStart;
        case 1846909100:  // accrualEnd
          return _accrualEnd;
        case 1516259717:  // accrualYearFractions
          return _accrualYearFractions;
        case 270958773:  // fixingStart
          return _fixingStart;
        case 871775726:  // fixingEnd
          return _fixingEnd;
        case 309118023:  // fixingYearFractions
          return _fixingYearFractions;
        case -291258418:  // forwardRates
          return _forwardRates;
        case 1695350911:  // fixedRates
          return _fixedRates;
        case -522438625:  // paymentDates
          return _paymentDates;
        case -507430688:  // paymentTimes
          return _paymentTimes;
        case -650014307:  // paymentDiscountFactors
          return _paymentDiscountFactors;
        case -1875448267:  // paymentAmounts
          return _paymentAmounts;
        case -176306557:  // projectedAmounts
          return _projectedAmounts;
        case 1910080819:  // notionals
          return _notionals;
        case -1996407456:  // spreads
          return _spreads;
        case 1449942752:  // gearings
          return _gearings;
        case 1358155045:  // indexTenors
          return _indexTenors;
        case -857546850:  // numberOfFixedCashFlows
          return _numberOfFixedCashFlows;
        case -582457076:  // numberOfFloatingCashFlows
          return _numberOfFloatingCashFlows;
        case -338982286:  // numberOfCashFlows
          return _numberOfCashFlows;
        case 178231285:  // discountedPaymentAmounts
          return _discountedPaymentAmounts;
        case 2019754051:  // discountedProjectedAmounts
          return _discountedProjectedAmounts;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FloatingLegCashFlows.Builder builder() {
      return new FloatingLegCashFlows.Builder();
    }

    @Override
    public Class<? extends FloatingLegCashFlows> beanType() {
      return FloatingLegCashFlows.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code accrualStart} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<LocalDate>> accrualStart() {
      return _accrualStart;
    }

    /**
     * The meta-property for the {@code accrualEnd} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<LocalDate>> accrualEnd() {
      return _accrualEnd;
    }

    /**
     * The meta-property for the {@code accrualYearFractions} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<Double>> accrualYearFractions() {
      return _accrualYearFractions;
    }

    /**
     * The meta-property for the {@code fixingStart} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<LocalDate>> fixingStart() {
      return _fixingStart;
    }

    /**
     * The meta-property for the {@code fixingEnd} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<LocalDate>> fixingEnd() {
      return _fixingEnd;
    }

    /**
     * The meta-property for the {@code fixingYearFractions} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<Double>> fixingYearFractions() {
      return _fixingYearFractions;
    }

    /**
     * The meta-property for the {@code forwardRates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<Double>> forwardRates() {
      return _forwardRates;
    }

    /**
     * The meta-property for the {@code fixedRates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<Double>> fixedRates() {
      return _fixedRates;
    }

    /**
     * The meta-property for the {@code paymentDates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<LocalDate>> paymentDates() {
      return _paymentDates;
    }

    /**
     * The meta-property for the {@code paymentTimes} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<Double>> paymentTimes() {
      return _paymentTimes;
    }

    /**
     * The meta-property for the {@code paymentDiscountFactors} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<Double>> paymentDiscountFactors() {
      return _paymentDiscountFactors;
    }

    /**
     * The meta-property for the {@code paymentAmounts} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<CurrencyAmount>> paymentAmounts() {
      return _paymentAmounts;
    }

    /**
     * The meta-property for the {@code projectedAmounts} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<CurrencyAmount>> projectedAmounts() {
      return _projectedAmounts;
    }

    /**
     * The meta-property for the {@code notionals} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<CurrencyAmount>> notionals() {
      return _notionals;
    }

    /**
     * The meta-property for the {@code spreads} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<Double>> spreads() {
      return _spreads;
    }

    /**
     * The meta-property for the {@code gearings} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<Double>> gearings() {
      return _gearings;
    }

    /**
     * The meta-property for the {@code indexTenors} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<Tenor>> indexTenors() {
      return _indexTenors;
    }

    /**
     * The meta-property for the {@code numberOfFixedCashFlows} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Integer> numberOfFixedCashFlows() {
      return _numberOfFixedCashFlows;
    }

    /**
     * The meta-property for the {@code numberOfFloatingCashFlows} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Integer> numberOfFloatingCashFlows() {
      return _numberOfFloatingCashFlows;
    }

    /**
     * The meta-property for the {@code numberOfCashFlows} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Integer> numberOfCashFlows() {
      return _numberOfCashFlows;
    }

    /**
     * The meta-property for the {@code discountedPaymentAmounts} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<CurrencyAmount>> discountedPaymentAmounts() {
      return _discountedPaymentAmounts;
    }

    /**
     * The meta-property for the {@code discountedProjectedAmounts} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<CurrencyAmount>> discountedProjectedAmounts() {
      return _discountedProjectedAmounts;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1071260659:  // accrualStart
          return ((FloatingLegCashFlows) bean).getAccrualStart();
        case 1846909100:  // accrualEnd
          return ((FloatingLegCashFlows) bean).getAccrualEnd();
        case 1516259717:  // accrualYearFractions
          return ((FloatingLegCashFlows) bean).getAccrualYearFractions();
        case 270958773:  // fixingStart
          return ((FloatingLegCashFlows) bean).getFixingStart();
        case 871775726:  // fixingEnd
          return ((FloatingLegCashFlows) bean).getFixingEnd();
        case 309118023:  // fixingYearFractions
          return ((FloatingLegCashFlows) bean).getFixingYearFractions();
        case -291258418:  // forwardRates
          return ((FloatingLegCashFlows) bean).getForwardRates();
        case 1695350911:  // fixedRates
          return ((FloatingLegCashFlows) bean).getFixedRates();
        case -522438625:  // paymentDates
          return ((FloatingLegCashFlows) bean).getPaymentDates();
        case -507430688:  // paymentTimes
          return ((FloatingLegCashFlows) bean).getPaymentTimes();
        case -650014307:  // paymentDiscountFactors
          return ((FloatingLegCashFlows) bean).getPaymentDiscountFactors();
        case -1875448267:  // paymentAmounts
          return ((FloatingLegCashFlows) bean).getPaymentAmounts();
        case -176306557:  // projectedAmounts
          return ((FloatingLegCashFlows) bean).getProjectedAmounts();
        case 1910080819:  // notionals
          return ((FloatingLegCashFlows) bean).getNotionals();
        case -1996407456:  // spreads
          return ((FloatingLegCashFlows) bean).getSpreads();
        case 1449942752:  // gearings
          return ((FloatingLegCashFlows) bean).getGearings();
        case 1358155045:  // indexTenors
          return ((FloatingLegCashFlows) bean).getIndexTenors();
        case -857546850:  // numberOfFixedCashFlows
          return ((FloatingLegCashFlows) bean).getNumberOfFixedCashFlows();
        case -582457076:  // numberOfFloatingCashFlows
          return ((FloatingLegCashFlows) bean).getNumberOfFloatingCashFlows();
        case -338982286:  // numberOfCashFlows
          return ((FloatingLegCashFlows) bean).getNumberOfCashFlows();
        case 178231285:  // discountedPaymentAmounts
          return ((FloatingLegCashFlows) bean).getDiscountedPaymentAmounts();
        case 2019754051:  // discountedProjectedAmounts
          return ((FloatingLegCashFlows) bean).getDiscountedProjectedAmounts();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code FloatingLegCashFlows}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<FloatingLegCashFlows> {

    private List<LocalDate> _accrualStart = new ArrayList<LocalDate>();
    private List<LocalDate> _accrualEnd = new ArrayList<LocalDate>();
    private List<Double> _accrualYearFractions = new ArrayList<Double>();
    private List<LocalDate> _fixingStart = new ArrayList<LocalDate>();
    private List<LocalDate> _fixingEnd = new ArrayList<LocalDate>();
    private List<Double> _fixingYearFractions = new ArrayList<Double>();
    private List<Double> _forwardRates = new ArrayList<Double>();
    private List<Double> _fixedRates = new ArrayList<Double>();
    private List<LocalDate> _paymentDates = new ArrayList<LocalDate>();
    private List<Double> _paymentTimes = new ArrayList<Double>();
    private List<Double> _paymentDiscountFactors = new ArrayList<Double>();
    private List<CurrencyAmount> _paymentAmounts = new ArrayList<CurrencyAmount>();
    private List<CurrencyAmount> _projectedAmounts = new ArrayList<CurrencyAmount>();
    private List<CurrencyAmount> _notionals = new ArrayList<CurrencyAmount>();
    private List<Double> _spreads = new ArrayList<Double>();
    private List<Double> _gearings = new ArrayList<Double>();
    private List<Tenor> _indexTenors = new ArrayList<Tenor>();

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(FloatingLegCashFlows beanToCopy) {
      this._accrualStart = new ArrayList<LocalDate>(beanToCopy.getAccrualStart());
      this._accrualEnd = new ArrayList<LocalDate>(beanToCopy.getAccrualEnd());
      this._accrualYearFractions = new ArrayList<Double>(beanToCopy.getAccrualYearFractions());
      this._fixingStart = new ArrayList<LocalDate>(beanToCopy.getFixingStart());
      this._fixingEnd = new ArrayList<LocalDate>(beanToCopy.getFixingEnd());
      this._fixingYearFractions = new ArrayList<Double>(beanToCopy.getFixingYearFractions());
      this._forwardRates = new ArrayList<Double>(beanToCopy.getForwardRates());
      this._fixedRates = new ArrayList<Double>(beanToCopy.getFixedRates());
      this._paymentDates = new ArrayList<LocalDate>(beanToCopy.getPaymentDates());
      this._paymentTimes = new ArrayList<Double>(beanToCopy.getPaymentTimes());
      this._paymentDiscountFactors = new ArrayList<Double>(beanToCopy.getPaymentDiscountFactors());
      this._paymentAmounts = new ArrayList<CurrencyAmount>(beanToCopy.getPaymentAmounts());
      this._projectedAmounts = new ArrayList<CurrencyAmount>(beanToCopy.getProjectedAmounts());
      this._notionals = new ArrayList<CurrencyAmount>(beanToCopy.getNotionals());
      this._spreads = new ArrayList<Double>(beanToCopy.getSpreads());
      this._gearings = new ArrayList<Double>(beanToCopy.getGearings());
      this._indexTenors = new ArrayList<Tenor>(beanToCopy.getIndexTenors());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1071260659:  // accrualStart
          return _accrualStart;
        case 1846909100:  // accrualEnd
          return _accrualEnd;
        case 1516259717:  // accrualYearFractions
          return _accrualYearFractions;
        case 270958773:  // fixingStart
          return _fixingStart;
        case 871775726:  // fixingEnd
          return _fixingEnd;
        case 309118023:  // fixingYearFractions
          return _fixingYearFractions;
        case -291258418:  // forwardRates
          return _forwardRates;
        case 1695350911:  // fixedRates
          return _fixedRates;
        case -522438625:  // paymentDates
          return _paymentDates;
        case -507430688:  // paymentTimes
          return _paymentTimes;
        case -650014307:  // paymentDiscountFactors
          return _paymentDiscountFactors;
        case -1875448267:  // paymentAmounts
          return _paymentAmounts;
        case -176306557:  // projectedAmounts
          return _projectedAmounts;
        case 1910080819:  // notionals
          return _notionals;
        case -1996407456:  // spreads
          return _spreads;
        case 1449942752:  // gearings
          return _gearings;
        case 1358155045:  // indexTenors
          return _indexTenors;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1071260659:  // accrualStart
          this._accrualStart = (List<LocalDate>) newValue;
          break;
        case 1846909100:  // accrualEnd
          this._accrualEnd = (List<LocalDate>) newValue;
          break;
        case 1516259717:  // accrualYearFractions
          this._accrualYearFractions = (List<Double>) newValue;
          break;
        case 270958773:  // fixingStart
          this._fixingStart = (List<LocalDate>) newValue;
          break;
        case 871775726:  // fixingEnd
          this._fixingEnd = (List<LocalDate>) newValue;
          break;
        case 309118023:  // fixingYearFractions
          this._fixingYearFractions = (List<Double>) newValue;
          break;
        case -291258418:  // forwardRates
          this._forwardRates = (List<Double>) newValue;
          break;
        case 1695350911:  // fixedRates
          this._fixedRates = (List<Double>) newValue;
          break;
        case -522438625:  // paymentDates
          this._paymentDates = (List<LocalDate>) newValue;
          break;
        case -507430688:  // paymentTimes
          this._paymentTimes = (List<Double>) newValue;
          break;
        case -650014307:  // paymentDiscountFactors
          this._paymentDiscountFactors = (List<Double>) newValue;
          break;
        case -1875448267:  // paymentAmounts
          this._paymentAmounts = (List<CurrencyAmount>) newValue;
          break;
        case -176306557:  // projectedAmounts
          this._projectedAmounts = (List<CurrencyAmount>) newValue;
          break;
        case 1910080819:  // notionals
          this._notionals = (List<CurrencyAmount>) newValue;
          break;
        case -1996407456:  // spreads
          this._spreads = (List<Double>) newValue;
          break;
        case 1449942752:  // gearings
          this._gearings = (List<Double>) newValue;
          break;
        case 1358155045:  // indexTenors
          this._indexTenors = (List<Tenor>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public FloatingLegCashFlows build() {
      return new FloatingLegCashFlows(
          _accrualStart,
          _accrualEnd,
          _accrualYearFractions,
          _fixingStart,
          _fixingEnd,
          _fixingYearFractions,
          _forwardRates,
          _fixedRates,
          _paymentDates,
          _paymentTimes,
          _paymentDiscountFactors,
          _paymentAmounts,
          _projectedAmounts,
          _notionals,
          _spreads,
          _gearings,
          _indexTenors);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code accrualStart} property in the builder.
     * @param accrualStart  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualStart(List<LocalDate> accrualStart) {
      JodaBeanUtils.notNull(accrualStart, "accrualStart");
      this._accrualStart = accrualStart;
      return this;
    }

    /**
     * Sets the {@code accrualEnd} property in the builder.
     * @param accrualEnd  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualEnd(List<LocalDate> accrualEnd) {
      JodaBeanUtils.notNull(accrualEnd, "accrualEnd");
      this._accrualEnd = accrualEnd;
      return this;
    }

    /**
     * Sets the {@code accrualYearFractions} property in the builder.
     * @param accrualYearFractions  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualYearFractions(List<Double> accrualYearFractions) {
      JodaBeanUtils.notNull(accrualYearFractions, "accrualYearFractions");
      this._accrualYearFractions = accrualYearFractions;
      return this;
    }

    /**
     * Sets the {@code fixingStart} property in the builder.
     * @param fixingStart  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingStart(List<LocalDate> fixingStart) {
      JodaBeanUtils.notNull(fixingStart, "fixingStart");
      this._fixingStart = fixingStart;
      return this;
    }

    /**
     * Sets the {@code fixingEnd} property in the builder.
     * @param fixingEnd  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingEnd(List<LocalDate> fixingEnd) {
      JodaBeanUtils.notNull(fixingEnd, "fixingEnd");
      this._fixingEnd = fixingEnd;
      return this;
    }

    /**
     * Sets the {@code fixingYearFractions} property in the builder.
     * @param fixingYearFractions  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingYearFractions(List<Double> fixingYearFractions) {
      JodaBeanUtils.notNull(fixingYearFractions, "fixingYearFractions");
      this._fixingYearFractions = fixingYearFractions;
      return this;
    }

    /**
     * Sets the {@code forwardRates} property in the builder.
     * @param forwardRates  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder forwardRates(List<Double> forwardRates) {
      JodaBeanUtils.notNull(forwardRates, "forwardRates");
      this._forwardRates = forwardRates;
      return this;
    }

    /**
     * Sets the {@code fixedRates} property in the builder.
     * @param fixedRates  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixedRates(List<Double> fixedRates) {
      JodaBeanUtils.notNull(fixedRates, "fixedRates");
      this._fixedRates = fixedRates;
      return this;
    }

    /**
     * Sets the {@code paymentDates} property in the builder.
     * @param paymentDates  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDates(List<LocalDate> paymentDates) {
      JodaBeanUtils.notNull(paymentDates, "paymentDates");
      this._paymentDates = paymentDates;
      return this;
    }

    /**
     * Sets the {@code paymentTimes} property in the builder.
     * @param paymentTimes  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentTimes(List<Double> paymentTimes) {
      JodaBeanUtils.notNull(paymentTimes, "paymentTimes");
      this._paymentTimes = paymentTimes;
      return this;
    }

    /**
     * Sets the {@code paymentDiscountFactors} property in the builder.
     * @param paymentDiscountFactors  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDiscountFactors(List<Double> paymentDiscountFactors) {
      JodaBeanUtils.notNull(paymentDiscountFactors, "paymentDiscountFactors");
      this._paymentDiscountFactors = paymentDiscountFactors;
      return this;
    }

    /**
     * Sets the {@code paymentAmounts} property in the builder.
     * @param paymentAmounts  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentAmounts(List<CurrencyAmount> paymentAmounts) {
      JodaBeanUtils.notNull(paymentAmounts, "paymentAmounts");
      this._paymentAmounts = paymentAmounts;
      return this;
    }

    /**
     * Sets the {@code projectedAmounts} property in the builder.
     * @param projectedAmounts  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder projectedAmounts(List<CurrencyAmount> projectedAmounts) {
      JodaBeanUtils.notNull(projectedAmounts, "projectedAmounts");
      this._projectedAmounts = projectedAmounts;
      return this;
    }

    /**
     * Sets the {@code notionals} property in the builder.
     * @param notionals  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notionals(List<CurrencyAmount> notionals) {
      JodaBeanUtils.notNull(notionals, "notionals");
      this._notionals = notionals;
      return this;
    }

    /**
     * Sets the {@code spreads} property in the builder.
     * @param spreads  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spreads(List<Double> spreads) {
      JodaBeanUtils.notNull(spreads, "spreads");
      this._spreads = spreads;
      return this;
    }

    /**
     * Sets the {@code gearings} property in the builder.
     * @param gearings  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder gearings(List<Double> gearings) {
      JodaBeanUtils.notNull(gearings, "gearings");
      this._gearings = gearings;
      return this;
    }

    /**
     * Sets the {@code indexTenors} property in the builder.
     * @param indexTenors  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder indexTenors(List<Tenor> indexTenors) {
      JodaBeanUtils.notNull(indexTenors, "indexTenors");
      this._indexTenors = indexTenors;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(576);
      buf.append("FloatingLegCashFlows.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("accrualStart").append('=').append(JodaBeanUtils.toString(_accrualStart)).append(',').append(' ');
      buf.append("accrualEnd").append('=').append(JodaBeanUtils.toString(_accrualEnd)).append(',').append(' ');
      buf.append("accrualYearFractions").append('=').append(JodaBeanUtils.toString(_accrualYearFractions)).append(',').append(' ');
      buf.append("fixingStart").append('=').append(JodaBeanUtils.toString(_fixingStart)).append(',').append(' ');
      buf.append("fixingEnd").append('=').append(JodaBeanUtils.toString(_fixingEnd)).append(',').append(' ');
      buf.append("fixingYearFractions").append('=').append(JodaBeanUtils.toString(_fixingYearFractions)).append(',').append(' ');
      buf.append("forwardRates").append('=').append(JodaBeanUtils.toString(_forwardRates)).append(',').append(' ');
      buf.append("fixedRates").append('=').append(JodaBeanUtils.toString(_fixedRates)).append(',').append(' ');
      buf.append("paymentDates").append('=').append(JodaBeanUtils.toString(_paymentDates)).append(',').append(' ');
      buf.append("paymentTimes").append('=').append(JodaBeanUtils.toString(_paymentTimes)).append(',').append(' ');
      buf.append("paymentDiscountFactors").append('=').append(JodaBeanUtils.toString(_paymentDiscountFactors)).append(',').append(' ');
      buf.append("paymentAmounts").append('=').append(JodaBeanUtils.toString(_paymentAmounts)).append(',').append(' ');
      buf.append("projectedAmounts").append('=').append(JodaBeanUtils.toString(_projectedAmounts)).append(',').append(' ');
      buf.append("notionals").append('=').append(JodaBeanUtils.toString(_notionals)).append(',').append(' ');
      buf.append("spreads").append('=').append(JodaBeanUtils.toString(_spreads)).append(',').append(' ');
      buf.append("gearings").append('=').append(JodaBeanUtils.toString(_gearings)).append(',').append(' ');
      buf.append("indexTenors").append('=').append(JodaBeanUtils.toString(_indexTenors)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

