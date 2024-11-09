/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.ProductTrade;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.ResolvableTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.SummarizerUtils;

/**
 * A trade in an overnight rate in arrears cap/floor.
 * <p>
 * An Over-The-Counter (OTC) trade in a {@link OvernightInArrearsCapFloor}.
 * <p>
 * An overnight rate in arrears cap/floor is a financial instrument that provides a set of call/put options on
 * successive compounded overnight index rates.
 */
@BeanDefinition
public final class OvernightInArrearsCapFloorTrade
    implements ProductTrade, ResolvableTrade<ResolvedOvernightInArrearsCapFloorTrade>, ImmutableBean, Serializable {

  /**
   * The additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final TradeInfo info;
  /**
   * The cap/floor product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final OvernightInArrearsCapFloor product;
  /**
   * The optional premium of the product.
   * <p>
   * For most cap/floor products, a premium is paid upfront. This typically occurs instead
   * of periodic payments based on fixed or rate index rates over the lifetime of the product.
   * <p>
   * The premium sign must be compatible with the product Pay/Receive flag.
   */
  @PropertyDefinition(get = "optional")
  private final AdjustablePayment premium;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.info = TradeInfo.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightInArrearsCapFloorTrade withInfo(PortfolioItemInfo info) {
    return new OvernightInArrearsCapFloorTrade(TradeInfo.from(info), product, premium);
  }

  //-------------------------------------------------------------------------
  @Override
  public PortfolioItemSummary summarize() {
    // 5Y USD 2mm Rec Compounded USD-SOFR Cap 1% / Pay Premium : 21Jan17-21Jan22
    StringBuilder buf = new StringBuilder(96);
    OvernightInArrearsCapFloorLeg mainLeg = product.getCapFloorLeg();
    buf.append(
        SummarizerUtils.datePeriod(
        mainLeg.getStartDate().getUnadjusted(),
        mainLeg.getEndDate().getUnadjusted()));
    buf.append(' ');
    buf.append(SummarizerUtils.amount(mainLeg.getCurrency(), mainLeg.getNotional().getInitialValue()));
    buf.append(' ');
    if (mainLeg.getPayReceive().isReceive()) {
      buf.append("Rec ");
      summarizeMainLeg(mainLeg, buf);
      buf.append(getPremium().isPresent() ?
          " / Pay Premium" :
          (product.getPayLeg().isPresent() ? " /  Pay Periodic" : ""));
    } else {
      buf.append(getPremium().isPresent() ?
          "Rec Premium / Pay " :
          (product.getPayLeg().isPresent() ? "Rec Periodic / Pay " : ""));
      summarizeMainLeg(mainLeg, buf);
    }
    buf.append(" : ");
    buf.append(SummarizerUtils.dateRange(mainLeg.getStartDate().getUnadjusted(), mainLeg.getEndDate().getUnadjusted()));
    return SummarizerUtils.summary(
        this,
        ProductType.OVERNIGHT_IN_ARREARS_CAP_FLOOR,
        buf.toString(),
        mainLeg.getCurrency());
  }

  // summarize the main leg
  private void summarizeMainLeg(OvernightInArrearsCapFloorLeg mainLeg, StringBuilder buf) {
    buf.append(mainLeg.getCalculation().getAccrualMethod());
    buf.append(' ');
    buf.append(mainLeg.getIndex());
    buf.append(' ');
    if (mainLeg.getCapSchedule().isPresent()) {
      buf.append("Cap ");
      buf.append(SummarizerUtils.percent(mainLeg.getCapSchedule().get().getInitialValue()));
    }
    if (mainLeg.getFloorSchedule().isPresent()) {
      buf.append("Floor ");
      buf.append(SummarizerUtils.percent(mainLeg.getFloorSchedule().get().getInitialValue()));
    }
  }

  @Override
  public ResolvedOvernightInArrearsCapFloorTrade resolve(ReferenceData refData) {
    return ResolvedOvernightInArrearsCapFloorTrade.builder()
        .info(info)
        .product(product.resolve(refData))
        .premium(premium != null ? premium.resolve(refData) : null)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code OvernightInArrearsCapFloorTrade}.
   * @return the meta-bean, not null
   */
  public static OvernightInArrearsCapFloorTrade.Meta meta() {
    return OvernightInArrearsCapFloorTrade.Meta.INSTANCE;
  }

  static {
    MetaBean.register(OvernightInArrearsCapFloorTrade.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightInArrearsCapFloorTrade.Builder builder() {
    return new OvernightInArrearsCapFloorTrade.Builder();
  }

  private OvernightInArrearsCapFloorTrade(
      TradeInfo info,
      OvernightInArrearsCapFloor product,
      AdjustablePayment premium) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(product, "product");
    this.info = info;
    this.product = product;
    this.premium = premium;
  }

  @Override
  public OvernightInArrearsCapFloorTrade.Meta metaBean() {
    return OvernightInArrearsCapFloorTrade.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   * @return the value of the property, not null
   */
  @Override
  public TradeInfo getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the cap/floor product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   * @return the value of the property, not null
   */
  @Override
  public OvernightInArrearsCapFloor getProduct() {
    return product;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional premium of the product.
   * <p>
   * For most cap/floor products, a premium is paid upfront. This typically occurs instead
   * of periodic payments based on fixed or rate index rates over the lifetime of the product.
   * <p>
   * The premium sign must be compatible with the product Pay/Receive flag.
   * @return the optional value of the property, not null
   */
  public Optional<AdjustablePayment> getPremium() {
    return Optional.ofNullable(premium);
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
      OvernightInArrearsCapFloorTrade other = (OvernightInArrearsCapFloorTrade) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(product, other.product) &&
          JodaBeanUtils.equal(premium, other.premium);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(product);
    hash = hash * 31 + JodaBeanUtils.hashCode(premium);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("OvernightInArrearsCapFloorTrade{");
    buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
    buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
    buf.append("premium").append('=').append(JodaBeanUtils.toString(premium));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightInArrearsCapFloorTrade}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code info} property.
     */
    private final MetaProperty<TradeInfo> info = DirectMetaProperty.ofImmutable(
        this, "info", OvernightInArrearsCapFloorTrade.class, TradeInfo.class);
    /**
     * The meta-property for the {@code product} property.
     */
    private final MetaProperty<OvernightInArrearsCapFloor> product = DirectMetaProperty.ofImmutable(
        this, "product", OvernightInArrearsCapFloorTrade.class, OvernightInArrearsCapFloor.class);
    /**
     * The meta-property for the {@code premium} property.
     */
    private final MetaProperty<AdjustablePayment> premium = DirectMetaProperty.ofImmutable(
        this, "premium", OvernightInArrearsCapFloorTrade.class, AdjustablePayment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "product",
        "premium");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case -309474065:  // product
          return product;
        case -318452137:  // premium
          return premium;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public OvernightInArrearsCapFloorTrade.Builder builder() {
      return new OvernightInArrearsCapFloorTrade.Builder();
    }

    @Override
    public Class<? extends OvernightInArrearsCapFloorTrade> beanType() {
      return OvernightInArrearsCapFloorTrade.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code info} property.
     * @return the meta-property, not null
     */
    public MetaProperty<TradeInfo> info() {
      return info;
    }

    /**
     * The meta-property for the {@code product} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightInArrearsCapFloor> product() {
      return product;
    }

    /**
     * The meta-property for the {@code premium} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustablePayment> premium() {
      return premium;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((OvernightInArrearsCapFloorTrade) bean).getInfo();
        case -309474065:  // product
          return ((OvernightInArrearsCapFloorTrade) bean).getProduct();
        case -318452137:  // premium
          return ((OvernightInArrearsCapFloorTrade) bean).premium;
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
   * The bean-builder for {@code OvernightInArrearsCapFloorTrade}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightInArrearsCapFloorTrade> {

    private TradeInfo info;
    private OvernightInArrearsCapFloor product;
    private AdjustablePayment premium;

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
    private Builder(OvernightInArrearsCapFloorTrade beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.product = beanToCopy.getProduct();
      this.premium = beanToCopy.premium;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case -309474065:  // product
          return product;
        case -318452137:  // premium
          return premium;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          this.info = (TradeInfo) newValue;
          break;
        case -309474065:  // product
          this.product = (OvernightInArrearsCapFloor) newValue;
          break;
        case -318452137:  // premium
          this.premium = (AdjustablePayment) newValue;
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
    public OvernightInArrearsCapFloorTrade build() {
      return new OvernightInArrearsCapFloorTrade(
          info,
          product,
          premium);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the additional trade information, defaulted to an empty instance.
     * <p>
     * This allows additional information to be attached to the trade.
     * @param info  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder info(TradeInfo info) {
      JodaBeanUtils.notNull(info, "info");
      this.info = info;
      return this;
    }

    /**
     * Sets the cap/floor product that was agreed when the trade occurred.
     * <p>
     * The product captures the contracted financial details of the trade.
     * @param product  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder product(OvernightInArrearsCapFloor product) {
      JodaBeanUtils.notNull(product, "product");
      this.product = product;
      return this;
    }

    /**
     * Sets the optional premium of the product.
     * <p>
     * For most cap/floor products, a premium is paid upfront. This typically occurs instead
     * of periodic payments based on fixed or rate index rates over the lifetime of the product.
     * <p>
     * The premium sign must be compatible with the product Pay/Receive flag.
     * @param premium  the new value
     * @return this, for chaining, not null
     */
    public Builder premium(AdjustablePayment premium) {
      this.premium = premium;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("OvernightInArrearsCapFloorTrade.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
      buf.append("premium").append('=').append(JodaBeanUtils.toString(premium));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
