/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.finance.ProductTrade;
import com.opengamma.strata.finance.TradeInfo;
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

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A trade in a credit default swap.
 * <p>
 * An Over-The-Counter (OTC) trade in a {@link Cds}.
 * <p>
 */
@BeanDefinition
public final class CdsTrade
    implements ProductTrade<Cds>, ImmutableBean, Serializable {

  /**
   * The additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   */
  @PropertyDefinition(overrideGet = true)
  private final TradeInfo tradeInfo;
  /**
   * The swap product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Cds product;

  //-------------------------------------------------------------------------
  @SuppressWarnings({"rawtypes", "unchecked"})
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.tradeInfo = TradeInfo.EMPTY;
  }

  public static CdsTrade of(
      TradeInfo tradeInfo,
      Cds product
  ) {
    return builder()
        .tradeInfo(tradeInfo)
        .product(product)
        .build();
  }

  /**
   * tradeDate The trade date
   */
  public LocalDate modelTradeDate() {
    return tradeInfo.getTradeDate().get();
  }

  /**
   * stepinDate (aka Protection Effective date or assignment date).
   * Date when party assumes ownership.
   * This is usually T+1. This is when protection (and risk) starts in terms of the model.
   * Note, this is sometimes just called the Effective Date, however this can cause
   * confusion with the legal effective date which is T-60 or T-90.
   */
  public LocalDate modelStepInDate() { // put this in the product, it comes from the convention
    return product
        .getGeneralTerms()
        .getDateAdjustments()
        .adjust(
            modelTradeDate()
                .plusDays(1)
        );
  }

  /**
   * valueDate The valuation date. The date that values are PVed to.
   * Is is normally today + 3 business days.  Aka cash-settle date.
   */
  public LocalDate modelValueDate() { // put this in the product, it comes from the convention
    return tradeInfo.getSettlementDate().get();
  }

  /**
   * accStartDate This is when the CDS nominally starts in terms of premium payments.  i.e. the number of days in the first
   * period (and thus the amount of the first premium payment) is counted from this date.
   */
  public LocalDate modelAccStartDate() {
    return product.getFeeLeg().getPeriodicPayments().getPeriodicSchedule().getStartDate();
  }

  /**
   * endDate (aka maturity date) This is when the contract expires and protection ends - any default after this date does not
   *  trigger a payment. (the protection ends at end of day)
   */
  public LocalDate modelEndDate() {
    return product.getFeeLeg().getPeriodicPayments().getPeriodicSchedule().getEndDate();
  }

  /**
   * payAccOnDefault Is the accrued premium paid in the event of a default
   */
  public boolean modelPayAccOnDefault() {
    return false;
  }

  /**
   * paymentInterval The nominal step between premium payments (e.g. 3 months, 6 months).
   */
  public Period modelPaymentInterval() {
    return product.getFeeLeg().getPeriodicPayments().getPeriodicSchedule().getFrequency().getPeriod();
  }

  /**
   * stubType stubType Options are FRONTSHORT, FRONTLONG, BACKSHORT, BACKLONG or NONE
   *  - <b>Note</b> in this code NONE is not allowed
   */
  public StubConvention modelStubConvention() {
    return product.getFeeLeg().getPeriodicPayments().getPeriodicSchedule().getStubConvention().get();
  }

  /**
   * If protectStart = true, then protections starts at the beginning of the day, otherwise it is at the end.
   */
  public boolean modelProtectStart() {
    return true;
  }

  /**
   * businessdayAdjustmentConvention How are adjustments for non-business days made
   */
  public BusinessDayConvention modelBusinessdayAdjustmentConvention() {
    return getProduct().getGeneralTerms().getDateAdjustments().getConvention();
  }

  /**
   * calendar HolidayCalendar defining what is a non-business day
   */
  public HolidayCalendar modelCalendar() {
    return getProduct().getGeneralTerms().getDateAdjustments().getCalendar();
  }

  /**
   * accrualDayCount Day count used for accrual
   */
  public DayCount modelAccrualDayCount() {
    return getProduct().getFeeLeg().getPeriodicPayments().getDayCountFraction();
  }


  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF

  /**
   * The meta-bean for {@code CdsTrade}.
   *
   * @return the meta-bean, not null
   */
  public static CdsTrade.Meta meta() {
    return CdsTrade.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CdsTrade.Meta.INSTANCE);
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
  public static CdsTrade.Builder builder() {
    return new CdsTrade.Builder();
  }

  private CdsTrade(
      TradeInfo tradeInfo,
      Cds product) {
    JodaBeanUtils.notNull(product, "product");
    this.tradeInfo = tradeInfo;
    this.product = product;
  }

  @Override
  public CdsTrade.Meta metaBean() {
    return CdsTrade.Meta.INSTANCE;
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
   * Gets the additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   *
   * @return the value of the property
   */
  @Override
  public TradeInfo getTradeInfo() {
    return tradeInfo;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets the swap product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   *
   * @return the value of the property, not null
   */
  @Override
  public Cds getProduct() {
    return product;
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
      CdsTrade other = (CdsTrade) obj;
      return JodaBeanUtils.equal(getTradeInfo(), other.getTradeInfo()) &&
          JodaBeanUtils.equal(getProduct(), other.getProduct());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getTradeInfo());
    hash = hash * 31 + JodaBeanUtils.hashCode(getProduct());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("CdsTrade{");
    buf.append("tradeInfo").append('=').append(getTradeInfo()).append(',').append(' ');
    buf.append("product").append('=').append(JodaBeanUtils.toString(getProduct()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------

  /**
   * The meta-bean for {@code CdsTrade}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code tradeInfo} property.
     */
    private final MetaProperty<TradeInfo> tradeInfo = DirectMetaProperty.ofImmutable(
        this, "tradeInfo", CdsTrade.class, TradeInfo.class);
    /**
     * The meta-property for the {@code product} property.
     */
    private final MetaProperty<Cds> product = DirectMetaProperty.ofImmutable(
        this, "product", CdsTrade.class, Cds.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "tradeInfo",
        "product");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          return tradeInfo;
        case -309474065:  // product
          return product;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CdsTrade.Builder builder() {
      return new CdsTrade.Builder();
    }

    @Override
    public Class<? extends CdsTrade> beanType() {
      return CdsTrade.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------

    /**
     * The meta-property for the {@code tradeInfo} property.
     *
     * @return the meta-property, not null
     */
    public MetaProperty<TradeInfo> tradeInfo() {
      return tradeInfo;
    }

    /**
     * The meta-property for the {@code product} property.
     *
     * @return the meta-property, not null
     */
    public MetaProperty<Cds> product() {
      return product;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          return ((CdsTrade) bean).getTradeInfo();
        case -309474065:  // product
          return ((CdsTrade) bean).getProduct();
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
   * The bean-builder for {@code CdsTrade}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CdsTrade> {

    private TradeInfo tradeInfo;
    private Cds product;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     *
     * @param beanToCopy the bean to copy from, not null
     */
    private Builder(CdsTrade beanToCopy) {
      this.tradeInfo = beanToCopy.getTradeInfo();
      this.product = beanToCopy.getProduct();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          return tradeInfo;
        case -309474065:  // product
          return product;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          this.tradeInfo = (TradeInfo) newValue;
          break;
        case -309474065:  // product
          this.product = (Cds) newValue;
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
    public CdsTrade build() {
      return new CdsTrade(
          tradeInfo,
          product);
    }

    //-----------------------------------------------------------------------

    /**
     * Sets the {@code tradeInfo} property in the builder.
     *
     * @param tradeInfo the new value
     * @return this, for chaining, not null
     */
    public Builder tradeInfo(TradeInfo tradeInfo) {
      this.tradeInfo = tradeInfo;
      return this;
    }

    /**
     * Sets the {@code product} property in the builder.
     *
     * @param product the new value, not null
     * @return this, for chaining, not null
     */
    public Builder product(Cds product) {
      JodaBeanUtils.notNull(product, "product");
      this.product = product;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("CdsTrade.Builder{");
      buf.append("tradeInfo").append('=').append(JodaBeanUtils.toString(tradeInfo)).append(',').append(' ');
      buf.append("product").append('=').append(JodaBeanUtils.toString(product));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
