/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cashflows;

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

/**
 * Container for the relevant details for pricing a fixed swap leg, with the entries
 * <ul>
 * <li>Start accrual date</li>
 * <li>End accrual date</li>
 * <li>Payment time</li>
 * <li>Payment year fraction</li>
 * <li>Payment amount (non discounted)</li>
 * <li>Discount factor</li>
 * <li>Notional</li>
 * <li>Rate</li>
 * <li>Discounted payment amount</li>
 * <ul>
 * There is an entry for each coupon in a fixed leg.
 */
@BeanDefinition
public class FixedLegCashFlows extends SwapLegCashFlows {

  /**
   * The payment year fraction label.
   */
  public static final String PAYMENT_YEAR_FRACTION = "Payment Year Fraction";
  /**
   * The payment amount label.
   */
  public static final String PAYMENT_AMOUNT = "Payment Amount";
  /**
   * The discount factor label.
   */
  public static final String DISCOUNT_FACTOR = "Discount Factor";
  /**
   * The fixed rate label.
   */
  public static final String FIXED_RATE = "Fixed Rate";
  /**
   * The discounted payment amount
   */
  public static final String DISCOUNTED_PAYMENT_AMOUNT = "Discounted Payment Amount";

  /**
   * An array of discount factors for the payments.
   */
  @PropertyDefinition(validate = "notNull")
  private double[] _discountFactors;

  /**
   * An array of payment year fractions.
   */
  @PropertyDefinition(validate = "notNull")
  private double[] _paymentFractions;
  /**
   * An array of payment amounts.
   */
  @PropertyDefinition(validate = "notNull")
  private  CurrencyAmount[] _paymentAmounts;
  /**
   * An array of fixed rates.
   */
  @PropertyDefinition(validate = "notNull")
  private Double[] _fixedRates;

  /**
   * All arrays must be the same length.
   * @param startAccrualDates The start accrual dates, not null
   * @param endAccrualDates The end accrual dates, not null
   * @param paymentTimes The payment times, not null
   * @param paymentFractions The payment year fractions, not null
   * @param discountFactors The discount factors, not null
   * @param paymentAmounts The payment amounts, not null
   * @param notionals The notionals, not null
   * @param fixedRates The fixed rates, not null
   */
  public FixedLegCashFlows(final LocalDate[] startAccrualDates, final LocalDate[] endAccrualDates,
                             final double[] discountFactors, final double[] paymentTimes, final double[] paymentFractions,
                             final CurrencyAmount[] paymentAmounts, final CurrencyAmount[] notionals, final Double[] fixedRates) {
    super(startAccrualDates, endAccrualDates, paymentTimes, notionals);
    setDiscountFactors(discountFactors);
    setPaymentFractions(paymentFractions);
    setPaymentAmounts(paymentAmounts);
    setFixedRates(fixedRates);
    final int n = startAccrualDates.length;
    ArgumentChecker.isTrue(n == endAccrualDates.length, "Must have same number of start and end accrual dates");
    ArgumentChecker.isTrue(n == discountFactors.length, "Must have same number of start accrual dates and discount factors");
    ArgumentChecker.isTrue(n == paymentTimes.length, "Must have same number of start accrual dates and payment times");
    ArgumentChecker.isTrue(n == paymentFractions.length, "Must have same number of start accrual dates and payment year fractions");
    ArgumentChecker.isTrue(n == paymentAmounts.length, "Must have same number of start accrual dates and payment amounts");
    ArgumentChecker.isTrue(n == notionals.length, "Must have same number of start accrual dates and notionals");
    ArgumentChecker.isTrue(n == fixedRates.length, "Must have same number of start accrual dates and fixed rates");
  }

  /**
   * For the builder.
   */
  private FixedLegCashFlows() {
    super();
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
      final double df = getDiscountFactors()[i];
      cashflows[i] = CurrencyAmount.of(payment.getCurrency(), payment.getAmount() * df);
    }
    return cashflows;
  }

  /**
   * Gets the total number of cash-flows.
   * @return The total number of cash-flows
   */
  @DerivedProperty
  public int getNumberOfCashFlows() {
    return getNotionals().length;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FixedLegCashFlows}.
   * @return the meta-bean, not null
   */
  public static FixedLegCashFlows.Meta meta() {
    return FixedLegCashFlows.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FixedLegCashFlows.Meta.INSTANCE);
  }

  @Override
  public FixedLegCashFlows.Meta metaBean() {
    return FixedLegCashFlows.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of discount factors for the payments.
   * @return the value of the property, not null
   */
  public double[] getDiscountFactors() {
    return _discountFactors;
  }

  /**
   * Sets an array of discount factors for the payments.
   * @param discountFactors  the new value of the property, not null
   */
  public void setDiscountFactors(double[] discountFactors) {
    JodaBeanUtils.notNull(discountFactors, "discountFactors");
    this._discountFactors = discountFactors;
  }

  /**
   * Gets the the {@code discountFactors} property.
   * @return the property, not null
   */
  public final Property<double[]> discountFactors() {
    return metaBean().discountFactors().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of payment year fractions.
   * @return the value of the property, not null
   */
  public double[] getPaymentFractions() {
    return _paymentFractions;
  }

  /**
   * Sets an array of payment year fractions.
   * @param paymentFractions  the new value of the property, not null
   */
  public void setPaymentFractions(double[] paymentFractions) {
    JodaBeanUtils.notNull(paymentFractions, "paymentFractions");
    this._paymentFractions = paymentFractions;
  }

  /**
   * Gets the the {@code paymentFractions} property.
   * @return the property, not null
   */
  public final Property<double[]> paymentFractions() {
    return metaBean().paymentFractions().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an array of payment amounts.
   * @return the value of the property, not null
   */
  public CurrencyAmount[] getPaymentAmounts() {
    return _paymentAmounts;
  }

  /**
   * Sets an array of payment amounts.
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
   * Gets an array of fixed rates.
   * @return the value of the property, not null
   */
  public Double[] getFixedRates() {
    return _fixedRates;
  }

  /**
   * Sets an array of fixed rates.
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
   * Gets the the {@code discountedPaymentAmounts} property.
   * @return the property, not null
   */
  public final Property<CurrencyAmount[]> discountedPaymentAmounts() {
    return metaBean().discountedPaymentAmounts().createProperty(this);
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
  @Override
  public FixedLegCashFlows clone() {
    return JodaBeanUtils.cloneAlways(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FixedLegCashFlows other = (FixedLegCashFlows) obj;
      return JodaBeanUtils.equal(getDiscountFactors(), other.getDiscountFactors()) &&
          JodaBeanUtils.equal(getPaymentFractions(), other.getPaymentFractions()) &&
          JodaBeanUtils.equal(getPaymentAmounts(), other.getPaymentAmounts()) &&
          JodaBeanUtils.equal(getFixedRates(), other.getFixedRates()) &&
          JodaBeanUtils.equal(getDiscountedPaymentAmounts(), other.getDiscountedPaymentAmounts()) &&
          (getNumberOfCashFlows() == other.getNumberOfCashFlows()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getDiscountFactors());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentFractions());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentAmounts());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixedRates());
    hash += hash * 31 + JodaBeanUtils.hashCode(getDiscountedPaymentAmounts());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNumberOfCashFlows());
    return hash ^ super.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("FixedLegCashFlows{");
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
    buf.append("discountFactors").append('=').append(JodaBeanUtils.toString(getDiscountFactors())).append(',').append(' ');
    buf.append("paymentFractions").append('=').append(JodaBeanUtils.toString(getPaymentFractions())).append(',').append(' ');
    buf.append("paymentAmounts").append('=').append(JodaBeanUtils.toString(getPaymentAmounts())).append(',').append(' ');
    buf.append("fixedRates").append('=').append(JodaBeanUtils.toString(getFixedRates())).append(',').append(' ');
    buf.append("discountedPaymentAmounts").append('=').append(JodaBeanUtils.toString(getDiscountedPaymentAmounts())).append(',').append(' ');
    buf.append("numberOfCashFlows").append('=').append(JodaBeanUtils.toString(getNumberOfCashFlows())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FixedLegCashFlows}.
   */
  public static class Meta extends SwapLegCashFlows.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code discountFactors} property.
     */
    private final MetaProperty<double[]> _discountFactors = DirectMetaProperty.ofReadWrite(
        this, "discountFactors", FixedLegCashFlows.class, double[].class);
    /**
     * The meta-property for the {@code paymentFractions} property.
     */
    private final MetaProperty<double[]> _paymentFractions = DirectMetaProperty.ofReadWrite(
        this, "paymentFractions", FixedLegCashFlows.class, double[].class);
    /**
     * The meta-property for the {@code paymentAmounts} property.
     */
    private final MetaProperty<CurrencyAmount[]> _paymentAmounts = DirectMetaProperty.ofReadWrite(
        this, "paymentAmounts", FixedLegCashFlows.class, CurrencyAmount[].class);
    /**
     * The meta-property for the {@code fixedRates} property.
     */
    private final MetaProperty<Double[]> _fixedRates = DirectMetaProperty.ofReadWrite(
        this, "fixedRates", FixedLegCashFlows.class, Double[].class);
    /**
     * The meta-property for the {@code discountedPaymentAmounts} property.
     */
    private final MetaProperty<CurrencyAmount[]> _discountedPaymentAmounts = DirectMetaProperty.ofDerived(
        this, "discountedPaymentAmounts", FixedLegCashFlows.class, CurrencyAmount[].class);
    /**
     * The meta-property for the {@code numberOfCashFlows} property.
     */
    private final MetaProperty<Integer> _numberOfCashFlows = DirectMetaProperty.ofDerived(
        this, "numberOfCashFlows", FixedLegCashFlows.class, Integer.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "discountFactors",
        "paymentFractions",
        "paymentAmounts",
        "fixedRates",
        "discountedPaymentAmounts",
        "numberOfCashFlows");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -91613053:  // discountFactors
          return _discountFactors;
        case 1206997835:  // paymentFractions
          return _paymentFractions;
        case -1875448267:  // paymentAmounts
          return _paymentAmounts;
        case 1695350911:  // fixedRates
          return _fixedRates;
        case 178231285:  // discountedPaymentAmounts
          return _discountedPaymentAmounts;
        case -338982286:  // numberOfCashFlows
          return _numberOfCashFlows;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FixedLegCashFlows> builder() {
      return new DirectBeanBuilder<FixedLegCashFlows>(new FixedLegCashFlows());
    }

    @Override
    public Class<? extends FixedLegCashFlows> beanType() {
      return FixedLegCashFlows.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code discountFactors} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<double[]> discountFactors() {
      return _discountFactors;
    }

    /**
     * The meta-property for the {@code paymentFractions} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<double[]> paymentFractions() {
      return _paymentFractions;
    }

    /**
     * The meta-property for the {@code paymentAmounts} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<CurrencyAmount[]> paymentAmounts() {
      return _paymentAmounts;
    }

    /**
     * The meta-property for the {@code fixedRates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double[]> fixedRates() {
      return _fixedRates;
    }

    /**
     * The meta-property for the {@code discountedPaymentAmounts} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<CurrencyAmount[]> discountedPaymentAmounts() {
      return _discountedPaymentAmounts;
    }

    /**
     * The meta-property for the {@code numberOfCashFlows} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Integer> numberOfCashFlows() {
      return _numberOfCashFlows;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -91613053:  // discountFactors
          return ((FixedLegCashFlows) bean).getDiscountFactors();
        case 1206997835:  // paymentFractions
          return ((FixedLegCashFlows) bean).getPaymentFractions();
        case -1875448267:  // paymentAmounts
          return ((FixedLegCashFlows) bean).getPaymentAmounts();
        case 1695350911:  // fixedRates
          return ((FixedLegCashFlows) bean).getFixedRates();
        case 178231285:  // discountedPaymentAmounts
          return ((FixedLegCashFlows) bean).getDiscountedPaymentAmounts();
        case -338982286:  // numberOfCashFlows
          return ((FixedLegCashFlows) bean).getNumberOfCashFlows();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -91613053:  // discountFactors
          ((FixedLegCashFlows) bean).setDiscountFactors((double[]) newValue);
          return;
        case 1206997835:  // paymentFractions
          ((FixedLegCashFlows) bean).setPaymentFractions((double[]) newValue);
          return;
        case -1875448267:  // paymentAmounts
          ((FixedLegCashFlows) bean).setPaymentAmounts((CurrencyAmount[]) newValue);
          return;
        case 1695350911:  // fixedRates
          ((FixedLegCashFlows) bean).setFixedRates((Double[]) newValue);
          return;
        case 178231285:  // discountedPaymentAmounts
          if (quiet) {
            return;
          }
          throw new UnsupportedOperationException("Property cannot be written: discountedPaymentAmounts");
        case -338982286:  // numberOfCashFlows
          if (quiet) {
            return;
          }
          throw new UnsupportedOperationException("Property cannot be written: numberOfCashFlows");
      }
      super.propertySet(bean, propertyName, newValue, quiet);
    }

    @Override
    protected void validate(Bean bean) {
      JodaBeanUtils.notNull(((FixedLegCashFlows) bean)._discountFactors, "discountFactors");
      JodaBeanUtils.notNull(((FixedLegCashFlows) bean)._paymentFractions, "paymentFractions");
      JodaBeanUtils.notNull(((FixedLegCashFlows) bean)._paymentAmounts, "paymentAmounts");
      JodaBeanUtils.notNull(((FixedLegCashFlows) bean)._fixedRates, "fixedRates");
      super.validate(bean);
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
