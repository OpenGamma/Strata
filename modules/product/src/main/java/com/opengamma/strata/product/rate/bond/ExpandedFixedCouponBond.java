/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.bond;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.id.StandardId;

/**
 * An expanded fixed coupon bond.
 * <p>
 * A fixed coupon bond is a financial instrument that represents a stream of fixed payments. 
 * The payments consist two types: periodic coupon payments and nominal payment.
 * The periodic payments are made {@code n} times a year with a fixed coupon rate at individual coupon dates.   
 * The nominal payment is the unique payment at the final coupon date.
 * <p>
 * The list of {@link FixedCouponBondPaymentPeriod} represents the periodic coupon payments,  
 * whereas the nominal payment is defined by {@link Payment}.
 * <p>
 * The accrual factor between two dates is computed {@code dayCount}. 
 * The legal entity of this fixed coupon bond is identified by {@link StandardId}.
 * The enum, {@link YieldConvention}, specifies the yield computation convention.
 */
@BeanDefinition
public final class ExpandedFixedCouponBond
    implements FixedCouponBondProduct, ImmutableBean, Serializable {

  /**
   * The nominal payment of the product.
   * <p>
   * The payment date of the nominal payment agrees with the final coupon payment date of the periodic payments.
   */
  @PropertyDefinition(validate = "notNull")
  private final Payment nominalPayment;
  /**
   * The periodic payments of the product.
   * <p>
   * Each payment period represents part of the life-time of the product.
   * The start date and end date of the leg are determined from the first and last period.
   * As such, the periods should be sorted.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<FixedCouponBondPaymentPeriod> periodicPayments;
  /**
   * The day count convention applicable. 
   * <p>
   * The conversion from dates to a numerical value is made based on this day count. 
   * For the fixed bond, the day count convention is used to compute accrued interest.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * Yield convention.
   * <p>
   * The convention defines how to convert from yield to price and inversely.  
   */
  @PropertyDefinition(validate = "notNull")
  private final YieldConvention yieldConvention;
  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the legal entity which issues the fixed coupon bond product. 
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;
  /**
   * The number of days between valuation date and settlement date. 
   * <p>
   * This is used to compute clean price. 
   * The clean price is the relative price to be paid at the standard settlement date in exchange for the bond.
   * <p>
   * It is usually one business day for US treasuries and UK Gilts and three days for Euroland government bonds. 
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment settlementDateOffset;

  //-------------------------------------------------------------------------
  /**
   * Gets the start date of the product.
   * <p>
   * This is the first coupon period date of the bond, often known as the effective date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date
   */
  public LocalDate getStartDate() {
    return periodicPayments.get(0).getStartDate();
  }

  /**
   * Gets the end date of the product.
   * <p>
   * This is the last coupon period date of the bond, often known as the maturity date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date
   */
  public LocalDate getEndDate() {
    return periodicPayments.get(periodicPayments.size() - 1).getEndDate();
  }

  /**
   * Gets the currency of the product.
   * <p>
   * All payments in the bond will have this currency.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return nominalPayment.getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public ExpandedFixedCouponBond expand() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExpandedFixedCouponBond}.
   * @return the meta-bean, not null
   */
  public static ExpandedFixedCouponBond.Meta meta() {
    return ExpandedFixedCouponBond.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExpandedFixedCouponBond.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ExpandedFixedCouponBond.Builder builder() {
    return new ExpandedFixedCouponBond.Builder();
  }

  private ExpandedFixedCouponBond(
      Payment nominalPayment,
      List<FixedCouponBondPaymentPeriod> periodicPayments,
      DayCount dayCount,
      YieldConvention yieldConvention,
      StandardId legalEntityId,
      DaysAdjustment settlementDateOffset) {
    JodaBeanUtils.notNull(nominalPayment, "nominalPayment");
    JodaBeanUtils.notNull(periodicPayments, "periodicPayments");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    this.nominalPayment = nominalPayment;
    this.periodicPayments = ImmutableList.copyOf(periodicPayments);
    this.dayCount = dayCount;
    this.yieldConvention = yieldConvention;
    this.legalEntityId = legalEntityId;
    this.settlementDateOffset = settlementDateOffset;
  }

  @Override
  public ExpandedFixedCouponBond.Meta metaBean() {
    return ExpandedFixedCouponBond.Meta.INSTANCE;
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
   * Gets the nominal payment of the product.
   * <p>
   * The payment date of the nominal payment agrees with the final coupon payment date of the periodic payments.
   * @return the value of the property, not null
   */
  public Payment getNominalPayment() {
    return nominalPayment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the periodic payments of the product.
   * <p>
   * Each payment period represents part of the life-time of the product.
   * The start date and end date of the leg are determined from the first and last period.
   * As such, the periods should be sorted.
   * @return the value of the property, not null
   */
  public ImmutableList<FixedCouponBondPaymentPeriod> getPeriodicPayments() {
    return periodicPayments;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention applicable.
   * <p>
   * The conversion from dates to a numerical value is made based on this day count.
   * For the fixed bond, the day count convention is used to compute accrued interest.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets yield convention.
   * <p>
   * The convention defines how to convert from yield to price and inversely.
   * @return the value of the property, not null
   */
  public YieldConvention getYieldConvention() {
    return yieldConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the legal entity which issues the fixed coupon bond product.
   * @return the value of the property, not null
   */
  public StandardId getLegalEntityId() {
    return legalEntityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days between valuation date and settlement date.
   * <p>
   * This is used to compute clean price.
   * The clean price is the relative price to be paid at the standard settlement date in exchange for the bond.
   * <p>
   * It is usually one business day for US treasuries and UK Gilts and three days for Euroland government bonds.
   * @return the value of the property, not null
   */
  public DaysAdjustment getSettlementDateOffset() {
    return settlementDateOffset;
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
      ExpandedFixedCouponBond other = (ExpandedFixedCouponBond) obj;
      return JodaBeanUtils.equal(nominalPayment, other.nominalPayment) &&
          JodaBeanUtils.equal(periodicPayments, other.periodicPayments) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(yieldConvention, other.yieldConvention) &&
          JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(settlementDateOffset, other.settlementDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(nominalPayment);
    hash = hash * 31 + JodaBeanUtils.hashCode(periodicPayments);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(yieldConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDateOffset);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("ExpandedFixedCouponBond{");
    buf.append("nominalPayment").append('=').append(nominalPayment).append(',').append(' ');
    buf.append("periodicPayments").append('=').append(periodicPayments).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("yieldConvention").append('=').append(yieldConvention).append(',').append(' ');
    buf.append("legalEntityId").append('=').append(legalEntityId).append(',').append(' ');
    buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExpandedFixedCouponBond}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code nominalPayment} property.
     */
    private final MetaProperty<Payment> nominalPayment = DirectMetaProperty.ofImmutable(
        this, "nominalPayment", ExpandedFixedCouponBond.class, Payment.class);
    /**
     * The meta-property for the {@code periodicPayments} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<FixedCouponBondPaymentPeriod>> periodicPayments = DirectMetaProperty.ofImmutable(
        this, "periodicPayments", ExpandedFixedCouponBond.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ExpandedFixedCouponBond.class, DayCount.class);
    /**
     * The meta-property for the {@code yieldConvention} property.
     */
    private final MetaProperty<YieldConvention> yieldConvention = DirectMetaProperty.ofImmutable(
        this, "yieldConvention", ExpandedFixedCouponBond.class, YieldConvention.class);
    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", ExpandedFixedCouponBond.class, StandardId.class);
    /**
     * The meta-property for the {@code settlementDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> settlementDateOffset = DirectMetaProperty.ofImmutable(
        this, "settlementDateOffset", ExpandedFixedCouponBond.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "nominalPayment",
        "periodicPayments",
        "dayCount",
        "yieldConvention",
        "legalEntityId",
        "settlementDateOffset");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -44199542:  // nominalPayment
          return nominalPayment;
        case -367345944:  // periodicPayments
          return periodicPayments;
        case 1905311443:  // dayCount
          return dayCount;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ExpandedFixedCouponBond.Builder builder() {
      return new ExpandedFixedCouponBond.Builder();
    }

    @Override
    public Class<? extends ExpandedFixedCouponBond> beanType() {
      return ExpandedFixedCouponBond.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code nominalPayment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Payment> nominalPayment() {
      return nominalPayment;
    }

    /**
     * The meta-property for the {@code periodicPayments} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<FixedCouponBondPaymentPeriod>> periodicPayments() {
      return periodicPayments;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code yieldConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YieldConvention> yieldConvention() {
      return yieldConvention;
    }

    /**
     * The meta-property for the {@code legalEntityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> legalEntityId() {
      return legalEntityId;
    }

    /**
     * The meta-property for the {@code settlementDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> settlementDateOffset() {
      return settlementDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -44199542:  // nominalPayment
          return ((ExpandedFixedCouponBond) bean).getNominalPayment();
        case -367345944:  // periodicPayments
          return ((ExpandedFixedCouponBond) bean).getPeriodicPayments();
        case 1905311443:  // dayCount
          return ((ExpandedFixedCouponBond) bean).getDayCount();
        case -1895216418:  // yieldConvention
          return ((ExpandedFixedCouponBond) bean).getYieldConvention();
        case 866287159:  // legalEntityId
          return ((ExpandedFixedCouponBond) bean).getLegalEntityId();
        case 135924714:  // settlementDateOffset
          return ((ExpandedFixedCouponBond) bean).getSettlementDateOffset();
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
   * The bean-builder for {@code ExpandedFixedCouponBond}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ExpandedFixedCouponBond> {

    private Payment nominalPayment;
    private List<FixedCouponBondPaymentPeriod> periodicPayments = ImmutableList.of();
    private DayCount dayCount;
    private YieldConvention yieldConvention;
    private StandardId legalEntityId;
    private DaysAdjustment settlementDateOffset;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ExpandedFixedCouponBond beanToCopy) {
      this.nominalPayment = beanToCopy.getNominalPayment();
      this.periodicPayments = beanToCopy.getPeriodicPayments();
      this.dayCount = beanToCopy.getDayCount();
      this.yieldConvention = beanToCopy.getYieldConvention();
      this.legalEntityId = beanToCopy.getLegalEntityId();
      this.settlementDateOffset = beanToCopy.getSettlementDateOffset();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -44199542:  // nominalPayment
          return nominalPayment;
        case -367345944:  // periodicPayments
          return periodicPayments;
        case 1905311443:  // dayCount
          return dayCount;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -44199542:  // nominalPayment
          this.nominalPayment = (Payment) newValue;
          break;
        case -367345944:  // periodicPayments
          this.periodicPayments = (List<FixedCouponBondPaymentPeriod>) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -1895216418:  // yieldConvention
          this.yieldConvention = (YieldConvention) newValue;
          break;
        case 866287159:  // legalEntityId
          this.legalEntityId = (StandardId) newValue;
          break;
        case 135924714:  // settlementDateOffset
          this.settlementDateOffset = (DaysAdjustment) newValue;
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
    public ExpandedFixedCouponBond build() {
      return new ExpandedFixedCouponBond(
          nominalPayment,
          periodicPayments,
          dayCount,
          yieldConvention,
          legalEntityId,
          settlementDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the nominal payment of the product.
     * <p>
     * The payment date of the nominal payment agrees with the final coupon payment date of the periodic payments.
     * @param nominalPayment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder nominalPayment(Payment nominalPayment) {
      JodaBeanUtils.notNull(nominalPayment, "nominalPayment");
      this.nominalPayment = nominalPayment;
      return this;
    }

    /**
     * Sets the periodic payments of the product.
     * <p>
     * Each payment period represents part of the life-time of the product.
     * The start date and end date of the leg are determined from the first and last period.
     * As such, the periods should be sorted.
     * @param periodicPayments  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder periodicPayments(List<FixedCouponBondPaymentPeriod> periodicPayments) {
      JodaBeanUtils.notNull(periodicPayments, "periodicPayments");
      this.periodicPayments = periodicPayments;
      return this;
    }

    /**
     * Sets the {@code periodicPayments} property in the builder
     * from an array of objects.
     * @param periodicPayments  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder periodicPayments(FixedCouponBondPaymentPeriod... periodicPayments) {
      return periodicPayments(ImmutableList.copyOf(periodicPayments));
    }

    /**
     * Sets the day count convention applicable.
     * <p>
     * The conversion from dates to a numerical value is made based on this day count.
     * For the fixed bond, the day count convention is used to compute accrued interest.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets yield convention.
     * <p>
     * The convention defines how to convert from yield to price and inversely.
     * @param yieldConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder yieldConvention(YieldConvention yieldConvention) {
      JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
      this.yieldConvention = yieldConvention;
      return this;
    }

    /**
     * Sets the legal entity identifier.
     * <p>
     * This identifier is used for the legal entity which issues the fixed coupon bond product.
     * @param legalEntityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder legalEntityId(StandardId legalEntityId) {
      JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
      this.legalEntityId = legalEntityId;
      return this;
    }

    /**
     * Sets the number of days between valuation date and settlement date.
     * <p>
     * This is used to compute clean price.
     * The clean price is the relative price to be paid at the standard settlement date in exchange for the bond.
     * <p>
     * It is usually one business day for US treasuries and UK Gilts and three days for Euroland government bonds.
     * @param settlementDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder settlementDateOffset(DaysAdjustment settlementDateOffset) {
      JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
      this.settlementDateOffset = settlementDateOffset;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("ExpandedFixedCouponBond.Builder{");
      buf.append("nominalPayment").append('=').append(JodaBeanUtils.toString(nominalPayment)).append(',').append(' ');
      buf.append("periodicPayments").append('=').append(JodaBeanUtils.toString(periodicPayments)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("yieldConvention").append('=').append(JodaBeanUtils.toString(yieldConvention)).append(',').append(' ');
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
