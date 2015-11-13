/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
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

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * A credit default swap (CDS), including single-name and index swaps.
 * <p>
 * A CDS is a financial instrument where the protection seller agrees to compensate
 * the protection buyer if a specified specified company or Sovereign entity experiences
 * a credit event, indicating it is or may be unable to service its debts.
 * The protection seller is typically paid a fee and/or premium, expressed as an annualized
 * percentage of the notional in basis points, regularly over the life of the transaction or
 * otherwise as agreed by the parties.
 * <p>
 * For example, a company engaged in another financial instrument with a counterparty may
 * wish to protect itself against the risk of the counterparty defaulting.
 */
@BeanDefinition
public final class Cds
    implements CdsProduct, ImmutableBean, Serializable {

  /**
   * Whether the CDS is buy or sell.
   * <p>
   * A value of 'Buy' implies that the fee leg payments are being paid, and protection is being bought.
   * A value of 'Sell' implies that the fee leg payments are being received, and protection is being sold.
   */
  @PropertyDefinition(validate = "notNull")
  private final BuySell buySellProtection;
  /**
   * The first date of the term of the trade.
   * <p>
   * This day may be subject to adjustment in accordance with a business day convention.
   * This is typically the previous CDS date (quarter on 20th) before the trade date, adjusted.
   * <p>
   * ISDA 2003 Term: Effective Date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The scheduled date on which the credit protection will lapse.
   * <p>
   * This day may be subject to adjustment in accordance with a business day convention.
   * This is typically an unadjusted CDS date.
   * <p>
   * ISDA 2003 Term: Scheduled Termination Date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The business day adjustment to apply to the start and end dates.
   * <p>
   * ISDA 2003 Terms: Business Day and Business Day Convention.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The reference against which protection applies.
   * <p>
   * For a single-name CDS, this contains information on the entity/issue
   * For a CDS index, this contains information about the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ReferenceInformation referenceInformation;
  /**
   * The fee leg.
   * <p>
   * This contains all the terms relevant to defining the fixed amounts/payments
   * per the applicable ISDA definitions.
   */
  @PropertyDefinition(validate = "notNull")
  private final FeeLeg feeLeg;
  /**
   * Whether the accrued premium is paid in the event of a default.
   */
  @PropertyDefinition(validate = "notNull")
  private final boolean payAccruedOnDefault;

  //-------------------------------------------------------------------------
  /**
   * Expands this CDS.
   * <p>
   * Expanding a CDS causes the dates to be adjusted according to the relevant
   * holiday calendar. Other one-off calculations may also be performed.
   * 
   * @return the equivalent expanded CDS
   * @throws RuntimeException if unable to expand due to an invalid definition
   */
  @Override
  public ExpandedCds expand() {
    Period paymentInterval = getFeeLeg().getPeriodicPayments().getPaymentFrequency().getPeriod();
    StubConvention stubConvention = getFeeLeg().getPeriodicPayments().getStubConvention();
    DayCount accrualDayCount = getFeeLeg().getPeriodicPayments().getDayCount();
    double upfrontFeeAmount = getFeeLeg().getUpfrontFee().getAmount();
    LocalDate upfrontFeePaymentDate = getFeeLeg().getUpfrontFee().getDate();
    double coupon = getFeeLeg().getPeriodicPayments().getCoupon();
    double notional = getFeeLeg().getPeriodicPayments().getNotional().getAmount();
    Currency currency = getFeeLeg().getPeriodicPayments().getNotional().getCurrency();

    return ExpandedCds
        .builder()
        .buySellProtection(buySellProtection)
        .currency(currency)
        .notional(notional)
        .coupon(coupon)
        .startDate(startDate)
        .endDate(endDate)
        .businessDayAdjustment(businessDayAdjustment)
        .payAccruedOnDefault(payAccruedOnDefault)
        .paymentInterval(paymentInterval)
        .stubConvention(stubConvention)
        .accrualDayCount(accrualDayCount)
        .accrualDayCount(accrualDayCount)
        .upfrontFeeAmount(upfrontFeeAmount)
        .upfrontFeePaymentDate(upfrontFeePaymentDate)
        .build();
  }

  // TODO add validation that notional currency matches the fee currency

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Cds}.
   * @return the meta-bean, not null
   */
  public static Cds.Meta meta() {
    return Cds.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Cds.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Cds.Builder builder() {
    return new Cds.Builder();
  }

  private Cds(
      BuySell buySellProtection,
      LocalDate startDate,
      LocalDate endDate,
      BusinessDayAdjustment businessDayAdjustment,
      ReferenceInformation referenceInformation,
      FeeLeg feeLeg,
      boolean payAccruedOnDefault) {
    JodaBeanUtils.notNull(buySellProtection, "buySellProtection");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    JodaBeanUtils.notNull(referenceInformation, "referenceInformation");
    JodaBeanUtils.notNull(feeLeg, "feeLeg");
    JodaBeanUtils.notNull(payAccruedOnDefault, "payAccruedOnDefault");
    this.buySellProtection = buySellProtection;
    this.startDate = startDate;
    this.endDate = endDate;
    this.businessDayAdjustment = businessDayAdjustment;
    this.referenceInformation = referenceInformation;
    this.feeLeg = feeLeg;
    this.payAccruedOnDefault = payAccruedOnDefault;
  }

  @Override
  public Cds.Meta metaBean() {
    return Cds.Meta.INSTANCE;
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
   * Gets whether the CDS is buy or sell.
   * <p>
   * A value of 'Buy' implies that the fee leg payments are being paid, and protection is being bought.
   * A value of 'Sell' implies that the fee leg payments are being received, and protection is being sold.
   * @return the value of the property, not null
   */
  public BuySell getBuySellProtection() {
    return buySellProtection;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first date of the term of the trade.
   * <p>
   * This day may be subject to adjustment in accordance with a business day convention.
   * This is typically the previous CDS date (quarter on 20th) before the trade date, adjusted.
   * <p>
   * ISDA 2003 Term: Effective Date.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the scheduled date on which the credit protection will lapse.
   * <p>
   * This day may be subject to adjustment in accordance with a business day convention.
   * This is typically an unadjusted CDS date.
   * <p>
   * ISDA 2003 Term: Scheduled Termination Date.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to the start and end dates.
   * <p>
   * ISDA 2003 Terms: Business Day and Business Day Convention.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference against which protection applies.
   * <p>
   * For a single-name CDS, this contains information on the entity/issue
   * For a CDS index, this contains information about the index.
   * @return the value of the property, not null
   */
  public ReferenceInformation getReferenceInformation() {
    return referenceInformation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fee leg.
   * <p>
   * This contains all the terms relevant to defining the fixed amounts/payments
   * per the applicable ISDA definitions.
   * @return the value of the property, not null
   */
  public FeeLeg getFeeLeg() {
    return feeLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the accrued premium is paid in the event of a default.
   * @return the value of the property, not null
   */
  public boolean isPayAccruedOnDefault() {
    return payAccruedOnDefault;
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
      Cds other = (Cds) obj;
      return JodaBeanUtils.equal(buySellProtection, other.buySellProtection) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(referenceInformation, other.referenceInformation) &&
          JodaBeanUtils.equal(feeLeg, other.feeLeg) &&
          (payAccruedOnDefault == other.payAccruedOnDefault);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(buySellProtection);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceInformation);
    hash = hash * 31 + JodaBeanUtils.hashCode(feeLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(payAccruedOnDefault);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("Cds{");
    buf.append("buySellProtection").append('=').append(buySellProtection).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(businessDayAdjustment).append(',').append(' ');
    buf.append("referenceInformation").append('=').append(referenceInformation).append(',').append(' ');
    buf.append("feeLeg").append('=').append(feeLeg).append(',').append(' ');
    buf.append("payAccruedOnDefault").append('=').append(JodaBeanUtils.toString(payAccruedOnDefault));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Cds}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code buySellProtection} property.
     */
    private final MetaProperty<BuySell> buySellProtection = DirectMetaProperty.ofImmutable(
        this, "buySellProtection", Cds.class, BuySell.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", Cds.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", Cds.class, LocalDate.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", Cds.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code referenceInformation} property.
     */
    private final MetaProperty<ReferenceInformation> referenceInformation = DirectMetaProperty.ofImmutable(
        this, "referenceInformation", Cds.class, ReferenceInformation.class);
    /**
     * The meta-property for the {@code feeLeg} property.
     */
    private final MetaProperty<FeeLeg> feeLeg = DirectMetaProperty.ofImmutable(
        this, "feeLeg", Cds.class, FeeLeg.class);
    /**
     * The meta-property for the {@code payAccruedOnDefault} property.
     */
    private final MetaProperty<Boolean> payAccruedOnDefault = DirectMetaProperty.ofImmutable(
        this, "payAccruedOnDefault", Cds.class, Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "buySellProtection",
        "startDate",
        "endDate",
        "businessDayAdjustment",
        "referenceInformation",
        "feeLeg",
        "payAccruedOnDefault");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -405622799:  // buySellProtection
          return buySellProtection;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case -2117930783:  // referenceInformation
          return referenceInformation;
        case -1278433112:  // feeLeg
          return feeLeg;
        case -43782841:  // payAccruedOnDefault
          return payAccruedOnDefault;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Cds.Builder builder() {
      return new Cds.Builder();
    }

    @Override
    public Class<? extends Cds> beanType() {
      return Cds.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code buySellProtection} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BuySell> buySellProtection() {
      return buySellProtection;
    }

    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code referenceInformation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ReferenceInformation> referenceInformation() {
      return referenceInformation;
    }

    /**
     * The meta-property for the {@code feeLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FeeLeg> feeLeg() {
      return feeLeg;
    }

    /**
     * The meta-property for the {@code payAccruedOnDefault} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> payAccruedOnDefault() {
      return payAccruedOnDefault;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -405622799:  // buySellProtection
          return ((Cds) bean).getBuySellProtection();
        case -2129778896:  // startDate
          return ((Cds) bean).getStartDate();
        case -1607727319:  // endDate
          return ((Cds) bean).getEndDate();
        case -1065319863:  // businessDayAdjustment
          return ((Cds) bean).getBusinessDayAdjustment();
        case -2117930783:  // referenceInformation
          return ((Cds) bean).getReferenceInformation();
        case -1278433112:  // feeLeg
          return ((Cds) bean).getFeeLeg();
        case -43782841:  // payAccruedOnDefault
          return ((Cds) bean).isPayAccruedOnDefault();
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
   * The bean-builder for {@code Cds}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Cds> {

    private BuySell buySellProtection;
    private LocalDate startDate;
    private LocalDate endDate;
    private BusinessDayAdjustment businessDayAdjustment;
    private ReferenceInformation referenceInformation;
    private FeeLeg feeLeg;
    private boolean payAccruedOnDefault;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Cds beanToCopy) {
      this.buySellProtection = beanToCopy.getBuySellProtection();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
      this.referenceInformation = beanToCopy.getReferenceInformation();
      this.feeLeg = beanToCopy.getFeeLeg();
      this.payAccruedOnDefault = beanToCopy.isPayAccruedOnDefault();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -405622799:  // buySellProtection
          return buySellProtection;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case -2117930783:  // referenceInformation
          return referenceInformation;
        case -1278433112:  // feeLeg
          return feeLeg;
        case -43782841:  // payAccruedOnDefault
          return payAccruedOnDefault;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -405622799:  // buySellProtection
          this.buySellProtection = (BuySell) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case -2117930783:  // referenceInformation
          this.referenceInformation = (ReferenceInformation) newValue;
          break;
        case -1278433112:  // feeLeg
          this.feeLeg = (FeeLeg) newValue;
          break;
        case -43782841:  // payAccruedOnDefault
          this.payAccruedOnDefault = (Boolean) newValue;
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
    public Cds build() {
      return new Cds(
          buySellProtection,
          startDate,
          endDate,
          businessDayAdjustment,
          referenceInformation,
          feeLeg,
          payAccruedOnDefault);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the CDS is buy or sell.
     * <p>
     * A value of 'Buy' implies that the fee leg payments are being paid, and protection is being bought.
     * A value of 'Sell' implies that the fee leg payments are being received, and protection is being sold.
     * @param buySellProtection  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySellProtection(BuySell buySellProtection) {
      JodaBeanUtils.notNull(buySellProtection, "buySellProtection");
      this.buySellProtection = buySellProtection;
      return this;
    }

    /**
     * Sets the first date of the term of the trade.
     * <p>
     * This day may be subject to adjustment in accordance with a business day convention.
     * This is typically the previous CDS date (quarter on 20th) before the trade date, adjusted.
     * <p>
     * ISDA 2003 Term: Effective Date.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the scheduled date on which the credit protection will lapse.
     * <p>
     * This day may be subject to adjustment in accordance with a business day convention.
     * This is typically an unadjusted CDS date.
     * <p>
     * ISDA 2003 Term: Scheduled Termination Date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the start and end dates.
     * <p>
     * ISDA 2003 Terms: Business Day and Business Day Convention.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the reference against which protection applies.
     * <p>
     * For a single-name CDS, this contains information on the entity/issue
     * For a CDS index, this contains information about the index.
     * @param referenceInformation  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceInformation(ReferenceInformation referenceInformation) {
      JodaBeanUtils.notNull(referenceInformation, "referenceInformation");
      this.referenceInformation = referenceInformation;
      return this;
    }

    /**
     * Sets the fee leg.
     * <p>
     * This contains all the terms relevant to defining the fixed amounts/payments
     * per the applicable ISDA definitions.
     * @param feeLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder feeLeg(FeeLeg feeLeg) {
      JodaBeanUtils.notNull(feeLeg, "feeLeg");
      this.feeLeg = feeLeg;
      return this;
    }

    /**
     * Sets whether the accrued premium is paid in the event of a default.
     * @param payAccruedOnDefault  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payAccruedOnDefault(boolean payAccruedOnDefault) {
      JodaBeanUtils.notNull(payAccruedOnDefault, "payAccruedOnDefault");
      this.payAccruedOnDefault = payAccruedOnDefault;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("Cds.Builder{");
      buf.append("buySellProtection").append('=').append(JodaBeanUtils.toString(buySellProtection)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("referenceInformation").append('=').append(JodaBeanUtils.toString(referenceInformation)).append(',').append(' ');
      buf.append("feeLeg").append('=').append(JodaBeanUtils.toString(feeLeg)).append(',').append(' ');
      buf.append("payAccruedOnDefault").append('=').append(JodaBeanUtils.toString(payAccruedOnDefault));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
