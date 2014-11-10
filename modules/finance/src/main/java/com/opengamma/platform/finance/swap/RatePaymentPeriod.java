/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.opengamma.basics.currency.Currency;

/**
 * A period over which a fixed or floating rate is paid.
 * <p>
 * A swap leg consists of one or more periods that are the basis of accrual.
 * The payment period is formed from one or more accrual periods
 * <p>
 * This class specifies the data necessary to calculate the value of the period.
 * Any combination of accrual periods is supported in the data model, however
 * there is no guarantee that exotic combinations will price sensibly.
 */
@BeanDefinition
public final class RatePaymentPeriod
    implements PaymentPeriod, ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The date that payment occurs.
   * <p>
   * The date that payment is made for the accrual periods.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate paymentDate;
  /**
   * The accrual periods that combine to form the payment period.
   * <p>
   * If there is more than one accrual period then compounding may apply.
   * All accrual periods must have the same currency.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final ImmutableList<AccrualPeriod> accrualPeriods;
  /**
   * The compounding method to use when there is more than one accrual period, default is 'None'.
   * <p>
   * Compounding is used when combining accrual periods.
   */
  @PropertyDefinition(validate = "notNull")
  private final CompoundingMethod compoundingMethod;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of the payment period from a single accrual period.
   * <p>
   * No compounding will apply.
   * 
   * @param paymentDate  the actual payment date, adjusted for business days
   * @param accrualPeriod  the single accrual period forming the payment period
   * @return the payment period
   */
  public static RatePaymentPeriod of(LocalDate paymentDate, AccrualPeriod accrualPeriod) {
    return RatePaymentPeriod.builder()
        .paymentDate(paymentDate)
        .accrualPeriods(ImmutableList.of(accrualPeriod))
        .build();
  }

  /**
   * Obtains an instance of the payment period with no compounding.
   * 
   * @param paymentDate  the actual payment date, adjusted for business days
   * @param accrualPeriods  the accrual periods forming the payment period
   * @param compoundingMethod  the compounding method
   * @return the payment period
   */
  public static RatePaymentPeriod of(
      LocalDate paymentDate, List<AccrualPeriod> accrualPeriods, CompoundingMethod compoundingMethod) {
    return RatePaymentPeriod.builder()
        .paymentDate(paymentDate)
        .accrualPeriods(accrualPeriods)
        .compoundingMethod(compoundingMethod)
        .build();
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.compoundingMethod(CompoundingMethod.NONE);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets an accrual period by index.
   * <p>
   * This returns a period using a zero-based index.
   * 
   * @param index  the zero-based period index
   * @return the accrual period
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public AccrualPeriod getAccrualPeriod(int index) {
    return accrualPeriods.get(index);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the start date of the period.
   * <p>
   * This is the first accrual date in the period.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date of the period
   */
  @Override
  public LocalDate getStartDate() {
    return getAccrualPeriod(0).getStartDate();
  }

  /**
   * Gets the end date of the period.
   * <p>
   * This is the last accrual date in the period.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date of the period
   */
  @Override
  public LocalDate getEndDate() {
    return getAccrualPeriod(accrualPeriods.size() - 1).getEndDate();
  }

  /**
   * Gets the currency of the payment period.
   * 
   * @return the currency
   */
  @Override
  public Currency getCurrency() {
    return Iterables.getOnlyElement(
        accrualPeriods.stream()
          .map(AccrualPeriod::getCurrency)
          .collect(Collectors.toSet()));
  }

  /**
   * Checks whether compounding applies.
   * <p>
   * Compounding applies if there is more than one accrual period and the
   * compounding method is not 'None'.
   * 
   * @return true if compounding applies
   */
  public boolean isCompounding() {
    return accrualPeriods.size() > 1 && compoundingMethod != CompoundingMethod.NONE;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code RatePaymentPeriod}.
   * @return the meta-bean, not null
   */
  public static RatePaymentPeriod.Meta meta() {
    return RatePaymentPeriod.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(RatePaymentPeriod.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static RatePaymentPeriod.Builder builder() {
    return new RatePaymentPeriod.Builder();
  }

  private RatePaymentPeriod(
      LocalDate paymentDate,
      List<AccrualPeriod> accrualPeriods,
      CompoundingMethod compoundingMethod) {
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    JodaBeanUtils.notEmpty(accrualPeriods, "accrualPeriods");
    JodaBeanUtils.notNull(compoundingMethod, "compoundingMethod");
    this.paymentDate = paymentDate;
    this.accrualPeriods = ImmutableList.copyOf(accrualPeriods);
    this.compoundingMethod = compoundingMethod;
  }

  @Override
  public RatePaymentPeriod.Meta metaBean() {
    return RatePaymentPeriod.Meta.INSTANCE;
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
   * Gets the date that payment occurs.
   * <p>
   * The date that payment is made for the accrual periods.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accrual periods that combine to form the payment period.
   * <p>
   * If there is more than one accrual period then compounding may apply.
   * All accrual periods must have the same currency.
   * @return the value of the property, not empty
   */
  @Override
  public ImmutableList<AccrualPeriod> getAccrualPeriods() {
    return accrualPeriods;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the compounding method to use when there is more than one accrual period, default is 'None'.
   * <p>
   * Compounding is used when combining accrual periods.
   * @return the value of the property, not null
   */
  public CompoundingMethod getCompoundingMethod() {
    return compoundingMethod;
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
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      RatePaymentPeriod other = (RatePaymentPeriod) obj;
      return JodaBeanUtils.equal(getPaymentDate(), other.getPaymentDate()) &&
          JodaBeanUtils.equal(getAccrualPeriods(), other.getAccrualPeriods()) &&
          JodaBeanUtils.equal(getCompoundingMethod(), other.getCompoundingMethod());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getAccrualPeriods());
    hash += hash * 31 + JodaBeanUtils.hashCode(getCompoundingMethod());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("RatePaymentPeriod{");
    buf.append("paymentDate").append('=').append(getPaymentDate()).append(',').append(' ');
    buf.append("accrualPeriods").append('=').append(getAccrualPeriods()).append(',').append(' ');
    buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(getCompoundingMethod()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RatePaymentPeriod}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", RatePaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code accrualPeriods} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<AccrualPeriod>> accrualPeriods = DirectMetaProperty.ofImmutable(
        this, "accrualPeriods", RatePaymentPeriod.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code compoundingMethod} property.
     */
    private final MetaProperty<CompoundingMethod> compoundingMethod = DirectMetaProperty.ofImmutable(
        this, "compoundingMethod", RatePaymentPeriod.class, CompoundingMethod.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "paymentDate",
        "accrualPeriods",
        "compoundingMethod");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return paymentDate;
        case -92208605:  // accrualPeriods
          return accrualPeriods;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public RatePaymentPeriod.Builder builder() {
      return new RatePaymentPeriod.Builder();
    }

    @Override
    public Class<? extends RatePaymentPeriod> beanType() {
      return RatePaymentPeriod.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

    /**
     * The meta-property for the {@code accrualPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<AccrualPeriod>> accrualPeriods() {
      return accrualPeriods;
    }

    /**
     * The meta-property for the {@code compoundingMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CompoundingMethod> compoundingMethod() {
      return compoundingMethod;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return ((RatePaymentPeriod) bean).getPaymentDate();
        case -92208605:  // accrualPeriods
          return ((RatePaymentPeriod) bean).getAccrualPeriods();
        case -1376171496:  // compoundingMethod
          return ((RatePaymentPeriod) bean).getCompoundingMethod();
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
   * The bean-builder for {@code RatePaymentPeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<RatePaymentPeriod> {

    private LocalDate paymentDate;
    private List<AccrualPeriod> accrualPeriods = new ArrayList<AccrualPeriod>();
    private CompoundingMethod compoundingMethod;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(RatePaymentPeriod beanToCopy) {
      this.paymentDate = beanToCopy.getPaymentDate();
      this.accrualPeriods = new ArrayList<AccrualPeriod>(beanToCopy.getAccrualPeriods());
      this.compoundingMethod = beanToCopy.getCompoundingMethod();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return paymentDate;
        case -92208605:  // accrualPeriods
          return accrualPeriods;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case -92208605:  // accrualPeriods
          this.accrualPeriods = (List<AccrualPeriod>) newValue;
          break;
        case -1376171496:  // compoundingMethod
          this.compoundingMethod = (CompoundingMethod) newValue;
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
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public RatePaymentPeriod build() {
      return new RatePaymentPeriod(
          paymentDate,
          accrualPeriods,
          compoundingMethod);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code paymentDate} property in the builder.
     * @param paymentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDate(LocalDate paymentDate) {
      JodaBeanUtils.notNull(paymentDate, "paymentDate");
      this.paymentDate = paymentDate;
      return this;
    }

    /**
     * Sets the {@code accrualPeriods} property in the builder.
     * @param accrualPeriods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder accrualPeriods(List<AccrualPeriod> accrualPeriods) {
      JodaBeanUtils.notEmpty(accrualPeriods, "accrualPeriods");
      this.accrualPeriods = accrualPeriods;
      return this;
    }

    /**
     * Sets the {@code compoundingMethod} property in the builder.
     * @param compoundingMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder compoundingMethod(CompoundingMethod compoundingMethod) {
      JodaBeanUtils.notNull(compoundingMethod, "compoundingMethod");
      this.compoundingMethod = compoundingMethod;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("RatePaymentPeriod.Builder{");
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("accrualPeriods").append('=').append(JodaBeanUtils.toString(accrualPeriods)).append(',').append(' ');
      buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
