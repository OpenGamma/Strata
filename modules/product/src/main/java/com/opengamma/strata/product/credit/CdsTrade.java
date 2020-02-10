/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

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
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.ProductTrade;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.ResolvableTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.SummarizerUtils;

/**
 * A trade in a single-name credit default swap (CDS).
 * <p>
 * An Over-The-Counter (OTC) trade in a {@link Cds}.
 * <p>
 * A CDS is a financial instrument where the protection seller agrees to compensate
 * the protection buyer when the reference entity suffers a default.
 * The protection seller is paid premium regularly from the protection buyer until
 * the expiry of the CDS contract or the reference entity defaults before the expiry.
 */
@BeanDefinition
public final class CdsTrade
    implements ProductTrade, ResolvableTrade<ResolvedCdsTrade>, ImmutableBean, Serializable {

  /**
   * The additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final TradeInfo info;
  /**
   * The CDS product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Cds product;
  /**
   * The upfront fee of the product.
   * <p>
   * This specifies a single amount payable by the buyer to the seller.
   * Thus the sign must be compatible with the product Pay/Receive flag.
   * <p>
   * Some CDSs, especially legacy products, are traded at par and the upfront fee is not paid.
   */
  @PropertyDefinition(get = "optional")
  private final AdjustablePayment upfrontFee;

  //-------------------------------------------------------------------------
  @Override
  public CdsTrade withInfo(PortfolioItemInfo info) {
    return new CdsTrade(TradeInfo.from(info), product, upfrontFee);
  }

  //-------------------------------------------------------------------------
  @Override
  public PortfolioItemSummary summarize() {
    // 2Y Buy USD 1mm ENTITY / 1.5% : 21Jan18-21Jan20
    PeriodicSchedule paymentSchedule = product.getPaymentSchedule();
    StringBuilder buf = new StringBuilder(96);
    buf.append(SummarizerUtils.datePeriod(paymentSchedule.getStartDate(), paymentSchedule.getEndDate()));
    buf.append(' ');
    buf.append(product.getBuySell());
    buf.append(' ');
    buf.append(SummarizerUtils.amount(product.getCurrency(), product.getNotional()));
    buf.append(' ');
    buf.append(product.getLegalEntityId().getValue());
    buf.append(" / ");
    buf.append(SummarizerUtils.percent(product.getFixedRate()));
    buf.append(" : ");
    buf.append(SummarizerUtils.dateRange(paymentSchedule.getStartDate(), paymentSchedule.getEndDate()));
    return SummarizerUtils.summary(this, ProductType.CDS, buf.toString(), product.getCurrency());
  }

  @Override
  public ResolvedCdsTrade resolve(ReferenceData refData) {
    return ResolvedCdsTrade.builder()
        .info(info)
        .product(product.resolve(refData))
        .upfrontFee(upfrontFee != null ? upfrontFee.resolve(refData) : null)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code CdsTrade}.
   * @return the meta-bean, not null
   */
  public static CdsTrade.Meta meta() {
    return CdsTrade.Meta.INSTANCE;
  }

  static {
    MetaBean.register(CdsTrade.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CdsTrade.Builder builder() {
    return new CdsTrade.Builder();
  }

  private CdsTrade(
      TradeInfo info,
      Cds product,
      AdjustablePayment upfrontFee) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(product, "product");
    this.info = info;
    this.product = product;
    this.upfrontFee = upfrontFee;
  }

  @Override
  public CdsTrade.Meta metaBean() {
    return CdsTrade.Meta.INSTANCE;
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
   * Gets the CDS product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   * @return the value of the property, not null
   */
  @Override
  public Cds getProduct() {
    return product;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the upfront fee of the product.
   * <p>
   * This specifies a single amount payable by the buyer to the seller.
   * Thus the sign must be compatible with the product Pay/Receive flag.
   * <p>
   * Some CDSs, especially legacy products, are traded at par and the upfront fee is not paid.
   * @return the optional value of the property, not null
   */
  public Optional<AdjustablePayment> getUpfrontFee() {
    return Optional.ofNullable(upfrontFee);
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
      CdsTrade other = (CdsTrade) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(product, other.product) &&
          JodaBeanUtils.equal(upfrontFee, other.upfrontFee);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(product);
    hash = hash * 31 + JodaBeanUtils.hashCode(upfrontFee);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("CdsTrade{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("product").append('=').append(product).append(',').append(' ');
    buf.append("upfrontFee").append('=').append(JodaBeanUtils.toString(upfrontFee));
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
     * The meta-property for the {@code info} property.
     */
    private final MetaProperty<TradeInfo> info = DirectMetaProperty.ofImmutable(
        this, "info", CdsTrade.class, TradeInfo.class);
    /**
     * The meta-property for the {@code product} property.
     */
    private final MetaProperty<Cds> product = DirectMetaProperty.ofImmutable(
        this, "product", CdsTrade.class, Cds.class);
    /**
     * The meta-property for the {@code upfrontFee} property.
     */
    private final MetaProperty<AdjustablePayment> upfrontFee = DirectMetaProperty.ofImmutable(
        this, "upfrontFee", CdsTrade.class, AdjustablePayment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "product",
        "upfrontFee");

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
        case 963468344:  // upfrontFee
          return upfrontFee;
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
    public MetaProperty<Cds> product() {
      return product;
    }

    /**
     * The meta-property for the {@code upfrontFee} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustablePayment> upfrontFee() {
      return upfrontFee;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((CdsTrade) bean).getInfo();
        case -309474065:  // product
          return ((CdsTrade) bean).getProduct();
        case 963468344:  // upfrontFee
          return ((CdsTrade) bean).upfrontFee;
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

    private TradeInfo info;
    private Cds product;
    private AdjustablePayment upfrontFee;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CdsTrade beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.product = beanToCopy.getProduct();
      this.upfrontFee = beanToCopy.upfrontFee;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case -309474065:  // product
          return product;
        case 963468344:  // upfrontFee
          return upfrontFee;
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
          this.product = (Cds) newValue;
          break;
        case 963468344:  // upfrontFee
          this.upfrontFee = (AdjustablePayment) newValue;
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
    public CdsTrade build() {
      return new CdsTrade(
          info,
          product,
          upfrontFee);
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
     * Sets the CDS product that was agreed when the trade occurred.
     * <p>
     * The product captures the contracted financial details of the trade.
     * @param product  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder product(Cds product) {
      JodaBeanUtils.notNull(product, "product");
      this.product = product;
      return this;
    }

    /**
     * Sets the upfront fee of the product.
     * <p>
     * This specifies a single amount payable by the buyer to the seller.
     * Thus the sign must be compatible with the product Pay/Receive flag.
     * <p>
     * Some CDSs, especially legacy products, are traded at par and the upfront fee is not paid.
     * @param upfrontFee  the new value
     * @return this, for chaining, not null
     */
    public Builder upfrontFee(AdjustablePayment upfrontFee) {
      this.upfrontFee = upfrontFee;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("CdsTrade.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
      buf.append("upfrontFee").append('=').append(JodaBeanUtils.toString(upfrontFee));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
