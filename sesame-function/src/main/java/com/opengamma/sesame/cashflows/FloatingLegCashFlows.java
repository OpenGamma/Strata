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
import java.util.Map;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.DerivedProperty;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.threeten.bp.LocalDate;

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
public class FloatingLegCashFlows extends SwapLegCashFlows {


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
   * An array of accrual year fractions.
   */
  @PropertyDefinition(validate = "notNull")
  private  double[] _accrualYearFractions;
  /**
   * An array of fixing start dates.
   */
  @PropertyDefinition(validate = "notNull")
  private  LocalDate[] _fixingStart;
  /**
   * An array of fixing end dates.
   */
  @PropertyDefinition(validate = "notNull")
  private  LocalDate[] _fixingEnd;
  /**
   * An array of fixing year fractions. May contain null values if there have been fixings as of the valuation date.
   */
  @PropertyDefinition(validate = "notNull")
  private Double[] _fixingYearFractions;
  /**
   * An array of forward rates.
   */
  @PropertyDefinition(validate = "notNull")
  private Double[] _forwardRates;
  /**
   * An array of fixed rates. May contain null values if there have been no fixings as of the valuation date.
   */
  @PropertyDefinition(validate = "notNull")
  private Double[] _fixedRates;
  /**
   * An array of payment dates.
   */
  @PropertyDefinition(validate = "notNull")
  private LocalDate[] _paymentDates;
  /**
   * An array of payment amounts. May contain nulls if there have been no fixings as of the valuation date.
   */
  @PropertyDefinition(validate = "notNull")
  private CurrencyAmount[] _paymentAmounts;
  /**
   * An array of spreads.
   */
  @PropertyDefinition(validate = "notNull")
  private double[] _spreads;
  /**
   * An array of gearings.
   */
  @PropertyDefinition(validate = "notNull")
  private double[] _gearings;
  /**
   * An array of payment discount factors.
   */
  @PropertyDefinition(validate = "notNull")
  private double[] _paymentDiscountFactors;
  /**
   * An array of projected amounts. May contain nulls if there has been a fixing as of the valuation date.
   */
  @PropertyDefinition(validate = "notNull")
  private CurrencyAmount[] _projectedAmounts;
  /**
   * An array of index tenors. May contain nulls if there has been a fixing as of the valuation date.
   */
  @PropertyDefinition(validate = "notNull")
  private Tenor[] _indexTenors;

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
  public FloatingLegCashFlows(final LocalDate[] startAccrualDates, final LocalDate[] endAccrualDates, final double[] accrualYearFractions,
                                final LocalDate[] fixingStart, final LocalDate[] fixingEnd, final Double[] fixingYearFractions, final Double[] forwardRates,
                                final Double[] fixedRates, final LocalDate[] paymentDates, final double[] paymentTimes, final double[] paymentDiscountFactors,
                                final CurrencyAmount[] paymentAmounts, final CurrencyAmount[] projectedAmounts, final CurrencyAmount[] notionals, final double[] spreads,
                                final double[] gearings, final Tenor[] indexTenors) {
    super(startAccrualDates, endAccrualDates, paymentTimes, notionals);
    setAccrualYearFractions(accrualYearFractions);
    setFixingStart(fixingStart);
    setFixingEnd(fixingEnd);
    setFixingYearFractions(fixingYearFractions);
    setForwardRates(forwardRates);
    setFixedRates(fixedRates);
    setPaymentDates(paymentDates);
    setPaymentDiscountFactors(paymentDiscountFactors);
    setPaymentAmounts(paymentAmounts);
    setProjectedAmounts(projectedAmounts);
    setSpreads(spreads);
    setGearings(gearings);
    setIndexTenors(indexTenors);

    final int n = notionals.length;
    ArgumentChecker.isTrue(n == startAccrualDates.length, "number of accrual start dates must equal number of notionals");
    ArgumentChecker.isTrue(n == endAccrualDates.length, "number of accrual end dates must equal number of notionals");
    ArgumentChecker.isTrue(n == accrualYearFractions.length, "number of accrual year fractions must equal number of notionals");
    ArgumentChecker.isTrue(n == fixingStart.length, "number of fixing start dates must equal number of notionals");
    ArgumentChecker.isTrue(n == fixingEnd.length, "number of fixing end dates must equal number of notionals");
    ArgumentChecker.isTrue(n == fixingYearFractions.length, "number of fixing year fractions must equal number of notionals");
    ArgumentChecker.isTrue(n == forwardRates.length, "number of forward rates must equal number of notionals");
    ArgumentChecker.isTrue(n == fixedRates.length, "number of fixed rates must equal number of notionals");
    ArgumentChecker.isTrue(n == paymentDates.length, "number of payment dates must equal number of notionals");
    ArgumentChecker.isTrue(n == paymentDiscountFactors.length, "number of payment discount factors must equal number of notionals");
    ArgumentChecker.isTrue(n == paymentAmounts.length, "number of payment amounts must equal number of notionals");
    ArgumentChecker.isTrue(n == projectedAmounts.length, "number of projected amounts must equal number of notionals");
    ArgumentChecker.isTrue(n == spreads.length, "number of spreads must equal number of notionals");
    ArgumentChecker.isTrue(n == gearings.length, "number of gearings must equal number of notionals");
    ArgumentChecker.isTrue(n == indexTenors.length, "number of index tenors must equal number of notionals");
  }

  /**
   * For the builder.
   */
  private FloatingLegCashFlows() {
    super();
  }

  /**
   * Gets the number of fixed cash-flows.
   * @return The number of fixed cash-flows
   */
  @DerivedProperty
  public int getNumberOfFixedCashFlows() {
    return getFixedRates().length;
  }

  /**
   * Gets the number of floating cash-flows.
   * @return The number of floating cash-flows
   */
  @DerivedProperty
  public int getNumberOfFloatingCashFlows() {
    return getForwardRates().length;
  }

  /**
   * Gets the number of cash-flows.
   * @return the number of cash-flows
   */
  @DerivedProperty
  public int getNumberOfCashFlows() {
    return getAccrualStart().length;
  }

  /**
   * Gets the discounted payment amounts.
   * @return the discounted cashflows
   */
  @DerivedProperty
  public CurrencyAmount[] getDiscountedPaymentAmounts() {
    final CurrencyAmount[] cashflows = new CurrencyAmount[getNumberOfCashFlows()];
    for (int i = 0; i < getNumberOfCashFlows(); i++) {
      final CurrencyAmount payment = getPaymentAmounts()[i];
      if (payment == null) {
        continue;
      }
      final double df = getPaymentDiscountFactors()[i];
      cashflows[i] = CurrencyAmount.of(payment.getCurrency(), payment.getAmount() * df);
    }
    return cashflows;
  }

  /**
   * Gets the discounted projected payment amounts.
   * @return the discounted cashflows
   */
  @DerivedProperty
  public CurrencyAmount[] getDiscountedProjectedAmounts() {
    final CurrencyAmount[] cashflows = new CurrencyAmount[getNumberOfCashFlows()];
    for (int i = 0; i < getNumberOfCashFlows(); i++) {
      final CurrencyAmount payment = getProjectedAmounts()[i];
      if (payment == null) {
        continue;
      }
      final double df = getPaymentDiscountFactors()[i];
      cashflows[i] = CurrencyAmount.of(payment.getCurrency(), payment.getAmount() * df);
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

  @Override
  public FloatingLegCashFlows.Meta metaBean() {
    return FloatingLegCashFlows.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of accrual year fractions.
   * @return the value of the property, not null
   */
  public double[] getAccrualYearFractions() {
    return _accrualYearFractions;
  }

  /**
   * Sets an array of accrual year fractions.
   * @param accrualYearFractions  the new value of the property, not null
   */
  public void setAccrualYearFractions(double[] accrualYearFractions) {
    JodaBeanUtils.notNull(accrualYearFractions, "accrualYearFractions");
    this._accrualYearFractions = accrualYearFractions;
  }

  /**
   * Gets the the {@code accrualYearFractions} property.
   * @return the property, not null
   */
  public final Property<double[]> accrualYearFractions() {
    return metaBean().accrualYearFractions().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of fixing start dates.
   * @return the value of the property, not null
   */
  public LocalDate[] getFixingStart() {
    return _fixingStart;
  }

  /**
   * Sets an array of fixing start dates.
   * @param fixingStart  the new value of the property, not null
   */
  public void setFixingStart(LocalDate[] fixingStart) {
    JodaBeanUtils.notNull(fixingStart, "fixingStart");
    this._fixingStart = fixingStart;
  }

  /**
   * Gets the the {@code fixingStart} property.
   * @return the property, not null
   */
  public final Property<LocalDate[]> fixingStart() {
    return metaBean().fixingStart().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of fixing end dates.
   * @return the value of the property, not null
   */
  public LocalDate[] getFixingEnd() {
    return _fixingEnd;
  }

  /**
   * Sets an array of fixing end dates.
   * @param fixingEnd  the new value of the property, not null
   */
  public void setFixingEnd(LocalDate[] fixingEnd) {
    JodaBeanUtils.notNull(fixingEnd, "fixingEnd");
    this._fixingEnd = fixingEnd;
  }

  /**
   * Gets the the {@code fixingEnd} property.
   * @return the property, not null
   */
  public final Property<LocalDate[]> fixingEnd() {
    return metaBean().fixingEnd().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of fixing year fractions. May contain null values if there have been fixings as of the valuation date.
   * @return the value of the property, not null
   */
  public Double[] getFixingYearFractions() {
    return _fixingYearFractions;
  }

  /**
   * Sets an array of fixing year fractions. May contain null values if there have been fixings as of the valuation date.
   * @param fixingYearFractions  the new value of the property, not null
   */
  public void setFixingYearFractions(Double[] fixingYearFractions) {
    JodaBeanUtils.notNull(fixingYearFractions, "fixingYearFractions");
    this._fixingYearFractions = fixingYearFractions;
  }

  /**
   * Gets the the {@code fixingYearFractions} property.
   * @return the property, not null
   */
  public final Property<Double[]> fixingYearFractions() {
    return metaBean().fixingYearFractions().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of forward rates.
   * @return the value of the property, not null
   */
  public Double[] getForwardRates() {
    return _forwardRates;
  }

  /**
   * Sets an array of forward rates.
   * @param forwardRates  the new value of the property, not null
   */
  public void setForwardRates(Double[] forwardRates) {
    JodaBeanUtils.notNull(forwardRates, "forwardRates");
    this._forwardRates = forwardRates;
  }

  /**
   * Gets the the {@code forwardRates} property.
   * @return the property, not null
   */
  public final Property<Double[]> forwardRates() {
    return metaBean().forwardRates().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of fixed rates. May contain null values if there have been no fixings as of the valuation date.
   * @return the value of the property, not null
   */
  public Double[] getFixedRates() {
    return _fixedRates;
  }

  /**
   * Sets an array of fixed rates. May contain null values if there have been no fixings as of the valuation date.
   * @param fixedRates  the new value of the property, not null
   */
  public void setFixedRates(Double[] fixedRates) {
    JodaBeanUtils.notNull(fixedRates, "fixedRates");
    this._fixedRates = fixedRates;
  }

  /**
   * Gets the the {@code fixedRates} property.
   * @return the property, not null
   */
  public final Property<Double[]> fixedRates() {
    return metaBean().fixedRates().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of payment dates.
   * @return the value of the property, not null
   */
  public LocalDate[] getPaymentDates() {
    return _paymentDates;
  }

  /**
   * Sets an array of payment dates.
   * @param paymentDates  the new value of the property, not null
   */
  public void setPaymentDates(LocalDate[] paymentDates) {
    JodaBeanUtils.notNull(paymentDates, "paymentDates");
    this._paymentDates = paymentDates;
  }

  /**
   * Gets the the {@code paymentDates} property.
   * @return the property, not null
   */
  public final Property<LocalDate[]> paymentDates() {
    return metaBean().paymentDates().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of payment amounts. May contain nulls if there have been no fixings as of the valuation date.
   * @return the value of the property, not null
   */
  public CurrencyAmount[] getPaymentAmounts() {
    return _paymentAmounts;
  }

  /**
   * Sets an array of payment amounts. May contain nulls if there have been no fixings as of the valuation date.
   * @param paymentAmounts  the new value of the property, not null
   */
  public void setPaymentAmounts(CurrencyAmount[] paymentAmounts) {
    JodaBeanUtils.notNull(paymentAmounts, "paymentAmounts");
    this._paymentAmounts = paymentAmounts;
  }

  /**
   * Gets the the {@code paymentAmounts} property.
   * @return the property, not null
   */
  public final Property<CurrencyAmount[]> paymentAmounts() {
    return metaBean().paymentAmounts().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of spreads.
   * @return the value of the property, not null
   */
  public double[] getSpreads() {
    return _spreads;
  }

  /**
   * Sets an array of spreads.
   * @param spreads  the new value of the property, not null
   */
  public void setSpreads(double[] spreads) {
    JodaBeanUtils.notNull(spreads, "spreads");
    this._spreads = spreads;
  }

  /**
   * Gets the the {@code spreads} property.
   * @return the property, not null
   */
  public final Property<double[]> spreads() {
    return metaBean().spreads().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of gearings.
   * @return the value of the property, not null
   */
  public double[] getGearings() {
    return _gearings;
  }

  /**
   * Sets an array of gearings.
   * @param gearings  the new value of the property, not null
   */
  public void setGearings(double[] gearings) {
    JodaBeanUtils.notNull(gearings, "gearings");
    this._gearings = gearings;
  }

  /**
   * Gets the the {@code gearings} property.
   * @return the property, not null
   */
  public final Property<double[]> gearings() {
    return metaBean().gearings().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of payment discount factors.
   * @return the value of the property, not null
   */
  public double[] getPaymentDiscountFactors() {
    return _paymentDiscountFactors;
  }

  /**
   * Sets an array of payment discount factors.
   * @param paymentDiscountFactors  the new value of the property, not null
   */
  public void setPaymentDiscountFactors(double[] paymentDiscountFactors) {
    JodaBeanUtils.notNull(paymentDiscountFactors, "paymentDiscountFactors");
    this._paymentDiscountFactors = paymentDiscountFactors;
  }

  /**
   * Gets the the {@code paymentDiscountFactors} property.
   * @return the property, not null
   */
  public final Property<double[]> paymentDiscountFactors() {
    return metaBean().paymentDiscountFactors().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of projected amounts. May contain nulls if there has been a fixing as of the valuation date.
   * @return the value of the property, not null
   */
  public CurrencyAmount[] getProjectedAmounts() {
    return _projectedAmounts;
  }

  /**
   * Sets an array of projected amounts. May contain nulls if there has been a fixing as of the valuation date.
   * @param projectedAmounts  the new value of the property, not null
   */
  public void setProjectedAmounts(CurrencyAmount[] projectedAmounts) {
    JodaBeanUtils.notNull(projectedAmounts, "projectedAmounts");
    this._projectedAmounts = projectedAmounts;
  }

  /**
   * Gets the the {@code projectedAmounts} property.
   * @return the property, not null
   */
  public final Property<CurrencyAmount[]> projectedAmounts() {
    return metaBean().projectedAmounts().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of index tenors. May contain nulls if there has been a fixing as of the valuation date.
   * @return the value of the property, not null
   */
  public Tenor[] getIndexTenors() {
    return _indexTenors;
  }

  /**
   * Sets an array of index tenors. May contain nulls if there has been a fixing as of the valuation date.
   * @param indexTenors  the new value of the property, not null
   */
  public void setIndexTenors(Tenor[] indexTenors) {
    JodaBeanUtils.notNull(indexTenors, "indexTenors");
    this._indexTenors = indexTenors;
  }

  /**
   * Gets the the {@code indexTenors} property.
   * @return the property, not null
   */
  public final Property<Tenor[]> indexTenors() {
    return metaBean().indexTenors().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the the {@code numberOfFixedCashFlows} property.
   * @return the property, not null
   */
  public final Property<Integer> numberOfFixedCashFlows() {
    return metaBean().numberOfFixedCashFlows().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the the {@code numberOfFloatingCashFlows} property.
   * @return the property, not null
   */
  public final Property<Integer> numberOfFloatingCashFlows() {
    return metaBean().numberOfFloatingCashFlows().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the the {@code numberOfCashFlows} property.
   * @return the property, not null
   */
  public final Property<Integer> numberOfCashFlows() {
    return metaBean().numberOfCashFlows().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the the {@code discountedPaymentAmounts} property.
   * @return the property, not null
   */
  public final Property<CurrencyAmount[]> discountedPaymentAmounts() {
    return metaBean().discountedPaymentAmounts().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the the {@code discountedProjectedAmounts} property.
   * @return the property, not null
   */
  public final Property<CurrencyAmount[]> discountedProjectedAmounts() {
    return metaBean().discountedProjectedAmounts().createProperty(this);
  }

  //-----------------------------------------------------------------------
  @Override
  public FloatingLegCashFlows clone() {
    return JodaBeanUtils.cloneAlways(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FloatingLegCashFlows other = (FloatingLegCashFlows) obj;
      return JodaBeanUtils.equal(getAccrualYearFractions(), other.getAccrualYearFractions()) &&
          JodaBeanUtils.equal(getFixingStart(), other.getFixingStart()) &&
          JodaBeanUtils.equal(getFixingEnd(), other.getFixingEnd()) &&
          JodaBeanUtils.equal(getFixingYearFractions(), other.getFixingYearFractions()) &&
          JodaBeanUtils.equal(getForwardRates(), other.getForwardRates()) &&
          JodaBeanUtils.equal(getFixedRates(), other.getFixedRates()) &&
          JodaBeanUtils.equal(getPaymentDates(), other.getPaymentDates()) &&
          JodaBeanUtils.equal(getPaymentAmounts(), other.getPaymentAmounts()) &&
          JodaBeanUtils.equal(getSpreads(), other.getSpreads()) &&
          JodaBeanUtils.equal(getGearings(), other.getGearings()) &&
          JodaBeanUtils.equal(getPaymentDiscountFactors(), other.getPaymentDiscountFactors()) &&
          JodaBeanUtils.equal(getProjectedAmounts(), other.getProjectedAmounts()) &&
          JodaBeanUtils.equal(getIndexTenors(), other.getIndexTenors()) &&
          (getNumberOfFixedCashFlows() == other.getNumberOfFixedCashFlows()) &&
          (getNumberOfFloatingCashFlows() == other.getNumberOfFloatingCashFlows()) &&
          (getNumberOfCashFlows() == other.getNumberOfCashFlows()) &&
          JodaBeanUtils.equal(getDiscountedPaymentAmounts(), other.getDiscountedPaymentAmounts()) &&
          JodaBeanUtils.equal(getDiscountedProjectedAmounts(), other.getDiscountedProjectedAmounts()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getAccrualYearFractions());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixingStart());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixingEnd());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixingYearFractions());
    hash += hash * 31 + JodaBeanUtils.hashCode(getForwardRates());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixedRates());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentDates());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentAmounts());
    hash += hash * 31 + JodaBeanUtils.hashCode(getSpreads());
    hash += hash * 31 + JodaBeanUtils.hashCode(getGearings());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentDiscountFactors());
    hash += hash * 31 + JodaBeanUtils.hashCode(getProjectedAmounts());
    hash += hash * 31 + JodaBeanUtils.hashCode(getIndexTenors());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNumberOfFixedCashFlows());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNumberOfFloatingCashFlows());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNumberOfCashFlows());
    hash += hash * 31 + JodaBeanUtils.hashCode(getDiscountedPaymentAmounts());
    hash += hash * 31 + JodaBeanUtils.hashCode(getDiscountedProjectedAmounts());
    return hash ^ super.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(608);
    buf.append("FloatingLegCashFlows{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  @Override
  protected void toString(StringBuilder buf) {
    super.toString(buf);
    buf.append("accrualYearFractions").append('=').append(JodaBeanUtils.toString(getAccrualYearFractions())).append(',').append(' ');
    buf.append("fixingStart").append('=').append(JodaBeanUtils.toString(getFixingStart())).append(',').append(' ');
    buf.append("fixingEnd").append('=').append(JodaBeanUtils.toString(getFixingEnd())).append(',').append(' ');
    buf.append("fixingYearFractions").append('=').append(JodaBeanUtils.toString(getFixingYearFractions())).append(',').append(' ');
    buf.append("forwardRates").append('=').append(JodaBeanUtils.toString(getForwardRates())).append(',').append(' ');
    buf.append("fixedRates").append('=').append(JodaBeanUtils.toString(getFixedRates())).append(',').append(' ');
    buf.append("paymentDates").append('=').append(JodaBeanUtils.toString(getPaymentDates())).append(',').append(' ');
    buf.append("paymentAmounts").append('=').append(JodaBeanUtils.toString(getPaymentAmounts())).append(',').append(' ');
    buf.append("spreads").append('=').append(JodaBeanUtils.toString(getSpreads())).append(',').append(' ');
    buf.append("gearings").append('=').append(JodaBeanUtils.toString(getGearings())).append(',').append(' ');
    buf.append("paymentDiscountFactors").append('=').append(JodaBeanUtils.toString(getPaymentDiscountFactors())).append(',').append(' ');
    buf.append("projectedAmounts").append('=').append(JodaBeanUtils.toString(getProjectedAmounts())).append(',').append(' ');
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
  public static class Meta extends SwapLegCashFlows.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code accrualYearFractions} property.
     */
    private final MetaProperty<double[]> _accrualYearFractions = DirectMetaProperty.ofReadWrite(
        this, "accrualYearFractions", FloatingLegCashFlows.class, double[].class);
    /**
     * The meta-property for the {@code fixingStart} property.
     */
    private final MetaProperty<LocalDate[]> _fixingStart = DirectMetaProperty.ofReadWrite(
        this, "fixingStart", FloatingLegCashFlows.class, LocalDate[].class);
    /**
     * The meta-property for the {@code fixingEnd} property.
     */
    private final MetaProperty<LocalDate[]> _fixingEnd = DirectMetaProperty.ofReadWrite(
        this, "fixingEnd", FloatingLegCashFlows.class, LocalDate[].class);
    /**
     * The meta-property for the {@code fixingYearFractions} property.
     */
    private final MetaProperty<Double[]> _fixingYearFractions = DirectMetaProperty.ofReadWrite(
        this, "fixingYearFractions", FloatingLegCashFlows.class, Double[].class);
    /**
     * The meta-property for the {@code forwardRates} property.
     */
    private final MetaProperty<Double[]> _forwardRates = DirectMetaProperty.ofReadWrite(
        this, "forwardRates", FloatingLegCashFlows.class, Double[].class);
    /**
     * The meta-property for the {@code fixedRates} property.
     */
    private final MetaProperty<Double[]> _fixedRates = DirectMetaProperty.ofReadWrite(
        this, "fixedRates", FloatingLegCashFlows.class, Double[].class);
    /**
     * The meta-property for the {@code paymentDates} property.
     */
    private final MetaProperty<LocalDate[]> _paymentDates = DirectMetaProperty.ofReadWrite(
        this, "paymentDates", FloatingLegCashFlows.class, LocalDate[].class);
    /**
     * The meta-property for the {@code paymentAmounts} property.
     */
    private final MetaProperty<CurrencyAmount[]> _paymentAmounts = DirectMetaProperty.ofReadWrite(
        this, "paymentAmounts", FloatingLegCashFlows.class, CurrencyAmount[].class);
    /**
     * The meta-property for the {@code spreads} property.
     */
    private final MetaProperty<double[]> _spreads = DirectMetaProperty.ofReadWrite(
        this, "spreads", FloatingLegCashFlows.class, double[].class);
    /**
     * The meta-property for the {@code gearings} property.
     */
    private final MetaProperty<double[]> _gearings = DirectMetaProperty.ofReadWrite(
        this, "gearings", FloatingLegCashFlows.class, double[].class);
    /**
     * The meta-property for the {@code paymentDiscountFactors} property.
     */
    private final MetaProperty<double[]> _paymentDiscountFactors = DirectMetaProperty.ofReadWrite(
        this, "paymentDiscountFactors", FloatingLegCashFlows.class, double[].class);
    /**
     * The meta-property for the {@code projectedAmounts} property.
     */
    private final MetaProperty<CurrencyAmount[]> _projectedAmounts = DirectMetaProperty.ofReadWrite(
        this, "projectedAmounts", FloatingLegCashFlows.class, CurrencyAmount[].class);
    /**
     * The meta-property for the {@code indexTenors} property.
     */
    private final MetaProperty<Tenor[]> _indexTenors = DirectMetaProperty.ofReadWrite(
        this, "indexTenors", FloatingLegCashFlows.class, Tenor[].class);
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
    private final MetaProperty<CurrencyAmount[]> _discountedPaymentAmounts = DirectMetaProperty.ofDerived(
        this, "discountedPaymentAmounts", FloatingLegCashFlows.class, CurrencyAmount[].class);
    /**
     * The meta-property for the {@code discountedProjectedAmounts} property.
     */
    private final MetaProperty<CurrencyAmount[]> _discountedProjectedAmounts = DirectMetaProperty.ofDerived(
        this, "discountedProjectedAmounts", FloatingLegCashFlows.class, CurrencyAmount[].class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "accrualYearFractions",
        "fixingStart",
        "fixingEnd",
        "fixingYearFractions",
        "forwardRates",
        "fixedRates",
        "paymentDates",
        "paymentAmounts",
        "spreads",
        "gearings",
        "paymentDiscountFactors",
        "projectedAmounts",
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
        case -1875448267:  // paymentAmounts
          return _paymentAmounts;
        case -1996407456:  // spreads
          return _spreads;
        case 1449942752:  // gearings
          return _gearings;
        case -650014307:  // paymentDiscountFactors
          return _paymentDiscountFactors;
        case -176306557:  // projectedAmounts
          return _projectedAmounts;
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
    public BeanBuilder<? extends FloatingLegCashFlows> builder() {
      return new DirectBeanBuilder<FloatingLegCashFlows>(new FloatingLegCashFlows());
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
     * The meta-property for the {@code accrualYearFractions} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<double[]> accrualYearFractions() {
      return _accrualYearFractions;
    }

    /**
     * The meta-property for the {@code fixingStart} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate[]> fixingStart() {
      return _fixingStart;
    }

    /**
     * The meta-property for the {@code fixingEnd} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate[]> fixingEnd() {
      return _fixingEnd;
    }

    /**
     * The meta-property for the {@code fixingYearFractions} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double[]> fixingYearFractions() {
      return _fixingYearFractions;
    }

    /**
     * The meta-property for the {@code forwardRates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double[]> forwardRates() {
      return _forwardRates;
    }

    /**
     * The meta-property for the {@code fixedRates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double[]> fixedRates() {
      return _fixedRates;
    }

    /**
     * The meta-property for the {@code paymentDates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate[]> paymentDates() {
      return _paymentDates;
    }

    /**
     * The meta-property for the {@code paymentAmounts} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<CurrencyAmount[]> paymentAmounts() {
      return _paymentAmounts;
    }

    /**
     * The meta-property for the {@code spreads} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<double[]> spreads() {
      return _spreads;
    }

    /**
     * The meta-property for the {@code gearings} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<double[]> gearings() {
      return _gearings;
    }

    /**
     * The meta-property for the {@code paymentDiscountFactors} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<double[]> paymentDiscountFactors() {
      return _paymentDiscountFactors;
    }

    /**
     * The meta-property for the {@code projectedAmounts} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<CurrencyAmount[]> projectedAmounts() {
      return _projectedAmounts;
    }

    /**
     * The meta-property for the {@code indexTenors} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Tenor[]> indexTenors() {
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
    public final MetaProperty<CurrencyAmount[]> discountedPaymentAmounts() {
      return _discountedPaymentAmounts;
    }

    /**
     * The meta-property for the {@code discountedProjectedAmounts} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<CurrencyAmount[]> discountedProjectedAmounts() {
      return _discountedProjectedAmounts;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
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
        case -1875448267:  // paymentAmounts
          return ((FloatingLegCashFlows) bean).getPaymentAmounts();
        case -1996407456:  // spreads
          return ((FloatingLegCashFlows) bean).getSpreads();
        case 1449942752:  // gearings
          return ((FloatingLegCashFlows) bean).getGearings();
        case -650014307:  // paymentDiscountFactors
          return ((FloatingLegCashFlows) bean).getPaymentDiscountFactors();
        case -176306557:  // projectedAmounts
          return ((FloatingLegCashFlows) bean).getProjectedAmounts();
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
      switch (propertyName.hashCode()) {
        case 1516259717:  // accrualYearFractions
          ((FloatingLegCashFlows) bean).setAccrualYearFractions((double[]) newValue);
          return;
        case 270958773:  // fixingStart
          ((FloatingLegCashFlows) bean).setFixingStart((LocalDate[]) newValue);
          return;
        case 871775726:  // fixingEnd
          ((FloatingLegCashFlows) bean).setFixingEnd((LocalDate[]) newValue);
          return;
        case 309118023:  // fixingYearFractions
          ((FloatingLegCashFlows) bean).setFixingYearFractions((Double[]) newValue);
          return;
        case -291258418:  // forwardRates
          ((FloatingLegCashFlows) bean).setForwardRates((Double[]) newValue);
          return;
        case 1695350911:  // fixedRates
          ((FloatingLegCashFlows) bean).setFixedRates((Double[]) newValue);
          return;
        case -522438625:  // paymentDates
          ((FloatingLegCashFlows) bean).setPaymentDates((LocalDate[]) newValue);
          return;
        case -1875448267:  // paymentAmounts
          ((FloatingLegCashFlows) bean).setPaymentAmounts((CurrencyAmount[]) newValue);
          return;
        case -1996407456:  // spreads
          ((FloatingLegCashFlows) bean).setSpreads((double[]) newValue);
          return;
        case 1449942752:  // gearings
          ((FloatingLegCashFlows) bean).setGearings((double[]) newValue);
          return;
        case -650014307:  // paymentDiscountFactors
          ((FloatingLegCashFlows) bean).setPaymentDiscountFactors((double[]) newValue);
          return;
        case -176306557:  // projectedAmounts
          ((FloatingLegCashFlows) bean).setProjectedAmounts((CurrencyAmount[]) newValue);
          return;
        case 1358155045:  // indexTenors
          ((FloatingLegCashFlows) bean).setIndexTenors((Tenor[]) newValue);
          return;
        case -857546850:  // numberOfFixedCashFlows
          if (quiet) {
            return;
          }
          throw new UnsupportedOperationException("Property cannot be written: numberOfFixedCashFlows");
        case -582457076:  // numberOfFloatingCashFlows
          if (quiet) {
            return;
          }
          throw new UnsupportedOperationException("Property cannot be written: numberOfFloatingCashFlows");
        case -338982286:  // numberOfCashFlows
          if (quiet) {
            return;
          }
          throw new UnsupportedOperationException("Property cannot be written: numberOfCashFlows");
        case 178231285:  // discountedPaymentAmounts
          if (quiet) {
            return;
          }
          throw new UnsupportedOperationException("Property cannot be written: discountedPaymentAmounts");
        case 2019754051:  // discountedProjectedAmounts
          if (quiet) {
            return;
          }
          throw new UnsupportedOperationException("Property cannot be written: discountedProjectedAmounts");
      }
      super.propertySet(bean, propertyName, newValue, quiet);
    }

    @Override
    protected void validate(Bean bean) {
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._accrualYearFractions, "accrualYearFractions");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._fixingStart, "fixingStart");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._fixingEnd, "fixingEnd");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._fixingYearFractions, "fixingYearFractions");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._forwardRates, "forwardRates");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._fixedRates, "fixedRates");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._paymentDates, "paymentDates");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._paymentAmounts, "paymentAmounts");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._spreads, "spreads");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._gearings, "gearings");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._paymentDiscountFactors, "paymentDiscountFactors");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._projectedAmounts, "projectedAmounts");
      JodaBeanUtils.notNull(((FloatingLegCashFlows) bean)._indexTenors, "indexTenors");
      super.validate(bean);
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

