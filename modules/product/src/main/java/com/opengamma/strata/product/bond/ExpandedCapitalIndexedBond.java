/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
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
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;

/**
 * A capital indexed bond.
 * <p>
 * A capital indexed bond is a financial instrument that represents a stream of inflation-adjusted payments. 
 * The payments consist two types: periodic coupon payments and nominal payment.
 * All of the payments are adjusted for inflation. 
 * <p>
 * The periodic coupon payments are defined in {@code periodicPayments}, 
 * whereas {@code nominalPayment} separately represents the nominal payments. 
 * <p>
 * The legal entity of this bond is identified by {@code legalEntityId}.
 * The enum, {@code yieldConvention}, specifies the yield computation convention.
 * The accrued interest must be computed with {@code dayCount}. 
 */
@BeanDefinition
public final class ExpandedCapitalIndexedBond
    implements CapitalIndexedBondProduct, ImmutableBean, Serializable {

  /**
   * The nominal payment of the product.
   * <p>
   * The payment date of the nominal payment agrees with the final coupon payment date of the periodic payments.
   */
  @PropertyDefinition(validate = "notNull")
  private final CapitalIndexedBondPaymentPeriod nominalPayment;
  /**
   * The periodic payments of the product.
   * <p>
   * Each payment period represents part of the life-time of the product.
   * The start date and end date of the leg are determined from the first and last period.
   * As such, the periods should be sorted.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<CapitalIndexedBondPaymentPeriod> periodicPayments;
  /**
   * The day count convention applicable. 
   * <p>
   * The conversion from dates to a numerical value is made based on this day count. 
   * For the inflation-indexed bond, the day count convention is used to compute accrued interest.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the legal entity which issues the bond product. 
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;
  /**
   * Yield convention.
   * <p>
   * The convention defines how to convert from yield to price and inversely.  
   */
  @PropertyDefinition(validate = "notNull")
  private final YieldConvention yieldConvention;
  /**
   * The number of days between valuation date and settlement date. 
   * <p>
   * This is used to compute clean price. 
   * The clean price is the relative price to be paid at the standard settlement date in exchange for the bond.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment settlementDateOffset;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    Currency currencyNominal = nominalPayment.getCurrency();
    Set<Currency> currencies =
        periodicPayments.stream().map(CapitalIndexedBondPaymentPeriod::getCurrency).collect(Collectors.toSet());
    currencies.add(currencyNominal);
    ArgChecker.isTrue(currencies.size() == 1, "Product must have a single currency, found: " + currencies);
  }

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
  public ExpandedCapitalIndexedBond expand() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExpandedCapitalIndexedBond}.
   * @return the meta-bean, not null
   */
  public static ExpandedCapitalIndexedBond.Meta meta() {
    return ExpandedCapitalIndexedBond.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExpandedCapitalIndexedBond.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ExpandedCapitalIndexedBond.Builder builder() {
    return new ExpandedCapitalIndexedBond.Builder();
  }

  private ExpandedCapitalIndexedBond(
      CapitalIndexedBondPaymentPeriod nominalPayment,
      List<CapitalIndexedBondPaymentPeriod> periodicPayments,
      DayCount dayCount,
      StandardId legalEntityId,
      YieldConvention yieldConvention,
      DaysAdjustment settlementDateOffset) {
    JodaBeanUtils.notNull(nominalPayment, "nominalPayment");
    JodaBeanUtils.notNull(periodicPayments, "periodicPayments");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    this.nominalPayment = nominalPayment;
    this.periodicPayments = ImmutableList.copyOf(periodicPayments);
    this.dayCount = dayCount;
    this.legalEntityId = legalEntityId;
    this.yieldConvention = yieldConvention;
    this.settlementDateOffset = settlementDateOffset;
    validate();
  }

  @Override
  public ExpandedCapitalIndexedBond.Meta metaBean() {
    return ExpandedCapitalIndexedBond.Meta.INSTANCE;
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
  public CapitalIndexedBondPaymentPeriod getNominalPayment() {
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
  public ImmutableList<CapitalIndexedBondPaymentPeriod> getPeriodicPayments() {
    return periodicPayments;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention applicable.
   * <p>
   * The conversion from dates to a numerical value is made based on this day count.
   * For the inflation-indexed bond, the day count convention is used to compute accrued interest.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the legal entity which issues the bond product.
   * @return the value of the property, not null
   */
  public StandardId getLegalEntityId() {
    return legalEntityId;
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
   * Gets the number of days between valuation date and settlement date.
   * <p>
   * This is used to compute clean price.
   * The clean price is the relative price to be paid at the standard settlement date in exchange for the bond.
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
      ExpandedCapitalIndexedBond other = (ExpandedCapitalIndexedBond) obj;
      return JodaBeanUtils.equal(nominalPayment, other.nominalPayment) &&
          JodaBeanUtils.equal(periodicPayments, other.periodicPayments) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(yieldConvention, other.yieldConvention) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(yieldConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDateOffset);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("ExpandedCapitalIndexedBond{");
    buf.append("nominalPayment").append('=').append(nominalPayment).append(',').append(' ');
    buf.append("periodicPayments").append('=').append(periodicPayments).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("legalEntityId").append('=').append(legalEntityId).append(',').append(' ');
    buf.append("yieldConvention").append('=').append(yieldConvention).append(',').append(' ');
    buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExpandedCapitalIndexedBond}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code nominalPayment} property.
     */
    private final MetaProperty<CapitalIndexedBondPaymentPeriod> nominalPayment = DirectMetaProperty.ofImmutable(
        this, "nominalPayment", ExpandedCapitalIndexedBond.class, CapitalIndexedBondPaymentPeriod.class);
    /**
     * The meta-property for the {@code periodicPayments} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<CapitalIndexedBondPaymentPeriod>> periodicPayments = DirectMetaProperty.ofImmutable(
        this, "periodicPayments", ExpandedCapitalIndexedBond.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ExpandedCapitalIndexedBond.class, DayCount.class);
    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", ExpandedCapitalIndexedBond.class, StandardId.class);
    /**
     * The meta-property for the {@code yieldConvention} property.
     */
    private final MetaProperty<YieldConvention> yieldConvention = DirectMetaProperty.ofImmutable(
        this, "yieldConvention", ExpandedCapitalIndexedBond.class, YieldConvention.class);
    /**
     * The meta-property for the {@code settlementDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> settlementDateOffset = DirectMetaProperty.ofImmutable(
        this, "settlementDateOffset", ExpandedCapitalIndexedBond.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "nominalPayment",
        "periodicPayments",
        "dayCount",
        "legalEntityId",
        "yieldConvention",
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
        case 866287159:  // legalEntityId
          return legalEntityId;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ExpandedCapitalIndexedBond.Builder builder() {
      return new ExpandedCapitalIndexedBond.Builder();
    }

    @Override
    public Class<? extends ExpandedCapitalIndexedBond> beanType() {
      return ExpandedCapitalIndexedBond.class;
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
    public MetaProperty<CapitalIndexedBondPaymentPeriod> nominalPayment() {
      return nominalPayment;
    }

    /**
     * The meta-property for the {@code periodicPayments} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<CapitalIndexedBondPaymentPeriod>> periodicPayments() {
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
     * The meta-property for the {@code legalEntityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> legalEntityId() {
      return legalEntityId;
    }

    /**
     * The meta-property for the {@code yieldConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YieldConvention> yieldConvention() {
      return yieldConvention;
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
          return ((ExpandedCapitalIndexedBond) bean).getNominalPayment();
        case -367345944:  // periodicPayments
          return ((ExpandedCapitalIndexedBond) bean).getPeriodicPayments();
        case 1905311443:  // dayCount
          return ((ExpandedCapitalIndexedBond) bean).getDayCount();
        case 866287159:  // legalEntityId
          return ((ExpandedCapitalIndexedBond) bean).getLegalEntityId();
        case -1895216418:  // yieldConvention
          return ((ExpandedCapitalIndexedBond) bean).getYieldConvention();
        case 135924714:  // settlementDateOffset
          return ((ExpandedCapitalIndexedBond) bean).getSettlementDateOffset();
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
   * The bean-builder for {@code ExpandedCapitalIndexedBond}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ExpandedCapitalIndexedBond> {

    private CapitalIndexedBondPaymentPeriod nominalPayment;
    private List<CapitalIndexedBondPaymentPeriod> periodicPayments = ImmutableList.of();
    private DayCount dayCount;
    private StandardId legalEntityId;
    private YieldConvention yieldConvention;
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
    private Builder(ExpandedCapitalIndexedBond beanToCopy) {
      this.nominalPayment = beanToCopy.getNominalPayment();
      this.periodicPayments = beanToCopy.getPeriodicPayments();
      this.dayCount = beanToCopy.getDayCount();
      this.legalEntityId = beanToCopy.getLegalEntityId();
      this.yieldConvention = beanToCopy.getYieldConvention();
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
        case 866287159:  // legalEntityId
          return legalEntityId;
        case -1895216418:  // yieldConvention
          return yieldConvention;
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
          this.nominalPayment = (CapitalIndexedBondPaymentPeriod) newValue;
          break;
        case -367345944:  // periodicPayments
          this.periodicPayments = (List<CapitalIndexedBondPaymentPeriod>) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 866287159:  // legalEntityId
          this.legalEntityId = (StandardId) newValue;
          break;
        case -1895216418:  // yieldConvention
          this.yieldConvention = (YieldConvention) newValue;
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
    public ExpandedCapitalIndexedBond build() {
      return new ExpandedCapitalIndexedBond(
          nominalPayment,
          periodicPayments,
          dayCount,
          legalEntityId,
          yieldConvention,
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
    public Builder nominalPayment(CapitalIndexedBondPaymentPeriod nominalPayment) {
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
    public Builder periodicPayments(List<CapitalIndexedBondPaymentPeriod> periodicPayments) {
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
    public Builder periodicPayments(CapitalIndexedBondPaymentPeriod... periodicPayments) {
      return periodicPayments(ImmutableList.copyOf(periodicPayments));
    }

    /**
     * Sets the day count convention applicable.
     * <p>
     * The conversion from dates to a numerical value is made based on this day count.
     * For the inflation-indexed bond, the day count convention is used to compute accrued interest.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the legal entity identifier.
     * <p>
     * This identifier is used for the legal entity which issues the bond product.
     * @param legalEntityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder legalEntityId(StandardId legalEntityId) {
      JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
      this.legalEntityId = legalEntityId;
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
     * Sets the number of days between valuation date and settlement date.
     * <p>
     * This is used to compute clean price.
     * The clean price is the relative price to be paid at the standard settlement date in exchange for the bond.
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
      buf.append("ExpandedCapitalIndexedBond.Builder{");
      buf.append("nominalPayment").append('=').append(JodaBeanUtils.toString(nominalPayment)).append(',').append(' ');
      buf.append("periodicPayments").append('=').append(JodaBeanUtils.toString(periodicPayments)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("yieldConvention").append('=').append(JodaBeanUtils.toString(yieldConvention)).append(',').append(' ');
      buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
