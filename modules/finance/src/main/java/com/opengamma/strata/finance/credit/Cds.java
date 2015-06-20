/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.finance.credit.reference.ReferenceInformation;
import com.opengamma.strata.finance.credit.fee.FeeLeg;
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

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A credit default swap (single name and index).
 * <p>
 * In a credit default swap one party (the protection seller) agrees to compensate another party
 * (the protection buyer) if a specified company or Sovereign (the reference entity)
 * experiences a credit event, indicating it is or may be unable to service its debts.
 * The protection seller is typically paid a fee and/or premium, expressed as an annualized
 * percent of the notional in basis points, regularly over the life of the transaction or
 * otherwise as agreed by the parties.
 */
@BeanDefinition
public final class Cds
    implements CdsProduct, ImmutableBean, Serializable {

  /**
   * The first day of the term of the trade. This day may be subject to adjustment in accordance
   * with a business day convention. ISDA 2003 Term: Effective Date
   * <p>
   * This is typically the previous cds (qtr on 20th) date before trade date, adjusted.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;

  /**
   * The scheduled date on which the credit protection will lapse. This day may be subject to
   * adjustment in accordance with a business day convention. ISDA 2003 Term: Scheduled Termination Date.
   * <p>
   * This is typically an unadjusted cds date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;

  /**
   * Indicator of whether we are buying or selling protection
   * Buy means we are paying the fee leg payments and buying the protection
   * Sell means we are receiving the fee leg payments and selling the protection
   */
  @PropertyDefinition(validate = "notNull")
  private final BuySell buySellProtection;

  /**
   * ISDA 2003 Terms: Business Day and Business Day Convention
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayConvention businessDayConvention;

  /**
   * Holiday Calendar to use in business day adjustments
   */
  @PropertyDefinition(validate = "notNull")
  private final HolidayCalendar holidayCalendar;

  /**
   * Contains information on reference entity/issue for single name or
   * index information for index trades
   */
  @PropertyDefinition(validate = "notNull")
  private final ReferenceInformation referenceInformation;

  /**
   * This element contains all the terms relevant to defining the fixed amounts/payments per
   * the applicable ISDA definitions.
   */
  @PropertyDefinition(validate = "notNull")
  private final FeeLeg feeLeg;

  /**
   * Value that the ISDA Standard model needs but that does not occur in the FpML
   */
  @PropertyDefinition(validate = "notNull")
  private final boolean payAccOnDefault;

  @Override
  public ExpandedCds expand() {
    Period paymentInterval = getFeeLeg().getPeriodicPayments().getPaymentFrequency().getPeriod();
    StubConvention stubConvention = getFeeLeg().getPeriodicPayments().getStubConvention();
    DayCount accrualDayCount = getFeeLeg().getPeriodicPayments().getDayCount();
    double upfrontFeeAmount = getFeeLeg().getUpfrontFee().getFixedAmount().getAmount();
    LocalDate upfrontFeePaymentDate = getFeeLeg().getUpfrontFee().getPaymentDate();
    double coupon = getFeeLeg().getPeriodicPayments().getCoupon();
    double notional = getFeeLeg().getPeriodicPayments().getNotional().getAmount();
    Currency currency = getFeeLeg().getPeriodicPayments().getNotional().getCurrency();

    return ExpandedCds
        .builder()
        .accStartDate(startDate)
        .endDate(endDate)
        .payAccOnDefault(payAccOnDefault)
        .paymentInterval(paymentInterval)
        .stubConvention(stubConvention)
        .businessdayAdjustmentConvention(businessDayConvention)
        .calendar(holidayCalendar)
        .accrualDayCount(accrualDayCount)
        .buySellProtection(buySellProtection)
        .accrualDayCount(accrualDayCount)
        .upfrontFeeAmount(upfrontFeeAmount)
        .upfrontFeePaymentDate(upfrontFeePaymentDate)
        .coupon(coupon)
        .notional(notional)
        .currency(currency)
        .build();

  }


  // TODO add validation that notional currency match the fee currency

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF

  /**
   * The meta-bean for {@code Cds}.
   *
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
   *
   * @return the builder, not null
   */
  public static Cds.Builder builder() {
    return new Cds.Builder();
  }

  private Cds(
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySellProtection,
      BusinessDayConvention businessDayConvention,
      HolidayCalendar holidayCalendar,
      ReferenceInformation referenceInformation,
      FeeLeg feeLeg,
      boolean payAccOnDefault) {
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(buySellProtection, "buySellProtection");
    JodaBeanUtils.notNull(businessDayConvention, "businessDayConvention");
    JodaBeanUtils.notNull(holidayCalendar, "holidayCalendar");
    JodaBeanUtils.notNull(referenceInformation, "referenceInformation");
    JodaBeanUtils.notNull(feeLeg, "feeLeg");
    JodaBeanUtils.notNull(payAccOnDefault, "payAccOnDefault");
    this.startDate = startDate;
    this.endDate = endDate;
    this.buySellProtection = buySellProtection;
    this.businessDayConvention = businessDayConvention;
    this.holidayCalendar = holidayCalendar;
    this.referenceInformation = referenceInformation;
    this.feeLeg = feeLeg;
    this.payAccOnDefault = payAccOnDefault;
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
   * Gets the first day of the term of the trade. This day may be subject to adjustment in accordance
   * with a business day convention. ISDA 2003 Term: Effective Date
   * <p>
   * This is typically the previous cds (qtr on 20th) date before trade date, adjusted.
   *
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets the scheduled date on which the credit protection will lapse. This day may be subject to
   * adjustment in accordance with a business day convention. ISDA 2003 Term: Scheduled Termination Date.
   * <p>
   * This is typically an unadjusted cds date.
   *
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets indicator of whether we are buying or selling protection
   * Buy means we are paying the fee leg payments and buying the protection
   * Sell means we are receiving the fee leg payments and selling the protection
   *
   * @return the value of the property, not null
   */
  public BuySell getBuySellProtection() {
    return buySellProtection;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets iSDA 2003 Terms: Business Day and Business Day Convention
   *
   * @return the value of the property, not null
   */
  public BusinessDayConvention getBusinessDayConvention() {
    return businessDayConvention;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets holiday Calendar to use in business day adjustments
   *
   * @return the value of the property, not null
   */
  public HolidayCalendar getHolidayCalendar() {
    return holidayCalendar;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets contains information on reference entity/issue for single name or
   * index information for index trades
   *
   * @return the value of the property, not null
   */
  public ReferenceInformation getReferenceInformation() {
    return referenceInformation;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets this element contains all the terms relevant to defining the fixed amounts/payments per
   * the applicable ISDA definitions.
   *
   * @return the value of the property, not null
   */
  public FeeLeg getFeeLeg() {
    return feeLeg;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets value that the ISDA Standard model needs but that does not occur in the FpML
   *
   * @return the value of the property, not null
   */
  public boolean isPayAccOnDefault() {
    return payAccOnDefault;
  }

  //-----------------------------------------------------------------------

  /**
   * Returns a builder that allows this bean to be mutated.
   *
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
      return JodaBeanUtils.equal(getStartDate(), other.getStartDate()) &&
          JodaBeanUtils.equal(getEndDate(), other.getEndDate()) &&
          JodaBeanUtils.equal(getBuySellProtection(), other.getBuySellProtection()) &&
          JodaBeanUtils.equal(getBusinessDayConvention(), other.getBusinessDayConvention()) &&
          JodaBeanUtils.equal(getHolidayCalendar(), other.getHolidayCalendar()) &&
          JodaBeanUtils.equal(getReferenceInformation(), other.getReferenceInformation()) &&
          JodaBeanUtils.equal(getFeeLeg(), other.getFeeLeg()) &&
          (isPayAccOnDefault() == other.isPayAccOnDefault());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getStartDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getEndDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getBuySellProtection());
    hash = hash * 31 + JodaBeanUtils.hashCode(getBusinessDayConvention());
    hash = hash * 31 + JodaBeanUtils.hashCode(getHolidayCalendar());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReferenceInformation());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFeeLeg());
    hash = hash * 31 + JodaBeanUtils.hashCode(isPayAccOnDefault());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("Cds{");
    buf.append("startDate").append('=').append(getStartDate()).append(',').append(' ');
    buf.append("endDate").append('=').append(getEndDate()).append(',').append(' ');
    buf.append("buySellProtection").append('=').append(getBuySellProtection()).append(',').append(' ');
    buf.append("businessDayConvention").append('=').append(getBusinessDayConvention()).append(',').append(' ');
    buf.append("holidayCalendar").append('=').append(getHolidayCalendar()).append(',').append(' ');
    buf.append("referenceInformation").append('=').append(getReferenceInformation()).append(',').append(' ');
    buf.append("feeLeg").append('=').append(getFeeLeg()).append(',').append(' ');
    buf.append("payAccOnDefault").append('=').append(JodaBeanUtils.toString(isPayAccOnDefault()));
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
     * The meta-property for the {@code buySellProtection} property.
     */
    private final MetaProperty<BuySell> buySellProtection = DirectMetaProperty.ofImmutable(
        this, "buySellProtection", Cds.class, BuySell.class);
    /**
     * The meta-property for the {@code businessDayConvention} property.
     */
    private final MetaProperty<BusinessDayConvention> businessDayConvention = DirectMetaProperty.ofImmutable(
        this, "businessDayConvention", Cds.class, BusinessDayConvention.class);
    /**
     * The meta-property for the {@code holidayCalendar} property.
     */
    private final MetaProperty<HolidayCalendar> holidayCalendar = DirectMetaProperty.ofImmutable(
        this, "holidayCalendar", Cds.class, HolidayCalendar.class);
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
     * The meta-property for the {@code payAccOnDefault} property.
     */
    private final MetaProperty<Boolean> payAccOnDefault = DirectMetaProperty.ofImmutable(
        this, "payAccOnDefault", Cds.class, Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "buySellProtection",
        "businessDayConvention",
        "holidayCalendar",
        "referenceInformation",
        "feeLeg",
        "payAccOnDefault");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -405622799:  // buySellProtection
          return buySellProtection;
        case -1002835891:  // businessDayConvention
          return businessDayConvention;
        case -30625866:  // holidayCalendar
          return holidayCalendar;
        case -2117930783:  // referenceInformation
          return referenceInformation;
        case -1278433112:  // feeLeg
          return feeLeg;
        case -988493655:  // payAccOnDefault
          return payAccOnDefault;
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
     * The meta-property for the {@code startDate} property.
     *
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     *
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code buySellProtection} property.
     *
     * @return the meta-property, not null
     */
    public MetaProperty<BuySell> buySellProtection() {
      return buySellProtection;
    }

    /**
     * The meta-property for the {@code businessDayConvention} property.
     *
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayConvention> businessDayConvention() {
      return businessDayConvention;
    }

    /**
     * The meta-property for the {@code holidayCalendar} property.
     *
     * @return the meta-property, not null
     */
    public MetaProperty<HolidayCalendar> holidayCalendar() {
      return holidayCalendar;
    }

    /**
     * The meta-property for the {@code referenceInformation} property.
     *
     * @return the meta-property, not null
     */
    public MetaProperty<ReferenceInformation> referenceInformation() {
      return referenceInformation;
    }

    /**
     * The meta-property for the {@code feeLeg} property.
     *
     * @return the meta-property, not null
     */
    public MetaProperty<FeeLeg> feeLeg() {
      return feeLeg;
    }

    /**
     * The meta-property for the {@code payAccOnDefault} property.
     *
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> payAccOnDefault() {
      return payAccOnDefault;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((Cds) bean).getStartDate();
        case -1607727319:  // endDate
          return ((Cds) bean).getEndDate();
        case -405622799:  // buySellProtection
          return ((Cds) bean).getBuySellProtection();
        case -1002835891:  // businessDayConvention
          return ((Cds) bean).getBusinessDayConvention();
        case -30625866:  // holidayCalendar
          return ((Cds) bean).getHolidayCalendar();
        case -2117930783:  // referenceInformation
          return ((Cds) bean).getReferenceInformation();
        case -1278433112:  // feeLeg
          return ((Cds) bean).getFeeLeg();
        case -988493655:  // payAccOnDefault
          return ((Cds) bean).isPayAccOnDefault();
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

    private LocalDate startDate;
    private LocalDate endDate;
    private BuySell buySellProtection;
    private BusinessDayConvention businessDayConvention;
    private HolidayCalendar holidayCalendar;
    private ReferenceInformation referenceInformation;
    private FeeLeg feeLeg;
    private boolean payAccOnDefault;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     *
     * @param beanToCopy the bean to copy from, not null
     */
    private Builder(Cds beanToCopy) {
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.buySellProtection = beanToCopy.getBuySellProtection();
      this.businessDayConvention = beanToCopy.getBusinessDayConvention();
      this.holidayCalendar = beanToCopy.getHolidayCalendar();
      this.referenceInformation = beanToCopy.getReferenceInformation();
      this.feeLeg = beanToCopy.getFeeLeg();
      this.payAccOnDefault = beanToCopy.isPayAccOnDefault();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -405622799:  // buySellProtection
          return buySellProtection;
        case -1002835891:  // businessDayConvention
          return businessDayConvention;
        case -30625866:  // holidayCalendar
          return holidayCalendar;
        case -2117930783:  // referenceInformation
          return referenceInformation;
        case -1278433112:  // feeLeg
          return feeLeg;
        case -988493655:  // payAccOnDefault
          return payAccOnDefault;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -405622799:  // buySellProtection
          this.buySellProtection = (BuySell) newValue;
          break;
        case -1002835891:  // businessDayConvention
          this.businessDayConvention = (BusinessDayConvention) newValue;
          break;
        case -30625866:  // holidayCalendar
          this.holidayCalendar = (HolidayCalendar) newValue;
          break;
        case -2117930783:  // referenceInformation
          this.referenceInformation = (ReferenceInformation) newValue;
          break;
        case -1278433112:  // feeLeg
          this.feeLeg = (FeeLeg) newValue;
          break;
        case -988493655:  // payAccOnDefault
          this.payAccOnDefault = (Boolean) newValue;
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
          startDate,
          endDate,
          buySellProtection,
          businessDayConvention,
          holidayCalendar,
          referenceInformation,
          feeLeg,
          payAccOnDefault);
    }

    //-----------------------------------------------------------------------

    /**
     * Sets the {@code startDate} property in the builder.
     *
     * @param startDate the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the {@code endDate} property in the builder.
     *
     * @param endDate the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the {@code buySellProtection} property in the builder.
     *
     * @param buySellProtection the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySellProtection(BuySell buySellProtection) {
      JodaBeanUtils.notNull(buySellProtection, "buySellProtection");
      this.buySellProtection = buySellProtection;
      return this;
    }

    /**
     * Sets the {@code businessDayConvention} property in the builder.
     *
     * @param businessDayConvention the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayConvention(BusinessDayConvention businessDayConvention) {
      JodaBeanUtils.notNull(businessDayConvention, "businessDayConvention");
      this.businessDayConvention = businessDayConvention;
      return this;
    }

    /**
     * Sets the {@code holidayCalendar} property in the builder.
     *
     * @param holidayCalendar the new value, not null
     * @return this, for chaining, not null
     */
    public Builder holidayCalendar(HolidayCalendar holidayCalendar) {
      JodaBeanUtils.notNull(holidayCalendar, "holidayCalendar");
      this.holidayCalendar = holidayCalendar;
      return this;
    }

    /**
     * Sets the {@code referenceInformation} property in the builder.
     *
     * @param referenceInformation the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceInformation(ReferenceInformation referenceInformation) {
      JodaBeanUtils.notNull(referenceInformation, "referenceInformation");
      this.referenceInformation = referenceInformation;
      return this;
    }

    /**
     * Sets the {@code feeLeg} property in the builder.
     *
     * @param feeLeg the new value, not null
     * @return this, for chaining, not null
     */
    public Builder feeLeg(FeeLeg feeLeg) {
      JodaBeanUtils.notNull(feeLeg, "feeLeg");
      this.feeLeg = feeLeg;
      return this;
    }

    /**
     * Sets the {@code payAccOnDefault} property in the builder.
     *
     * @param payAccOnDefault the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payAccOnDefault(boolean payAccOnDefault) {
      JodaBeanUtils.notNull(payAccOnDefault, "payAccOnDefault");
      this.payAccOnDefault = payAccOnDefault;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("Cds.Builder{");
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("buySellProtection").append('=').append(JodaBeanUtils.toString(buySellProtection)).append(',').append(' ');
      buf.append("businessDayConvention").append('=').append(JodaBeanUtils.toString(businessDayConvention)).append(',').append(' ');
      buf.append("holidayCalendar").append('=').append(JodaBeanUtils.toString(holidayCalendar)).append(',').append(' ');
      buf.append("referenceInformation").append('=').append(JodaBeanUtils.toString(referenceInformation)).append(',').append(' ');
      buf.append("feeLeg").append('=').append(JodaBeanUtils.toString(feeLeg)).append(',').append(' ');
      buf.append("payAccOnDefault").append('=').append(JodaBeanUtils.toString(payAccOnDefault));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
