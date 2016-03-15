/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.ResolvableTrade;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.rate.RateObservation;
import com.opengamma.strata.product.swap.KnownAmountPaymentPeriod;
import com.opengamma.strata.product.swap.PaymentPeriod;

/**
 * A trade representing a capital indexed bond.
 * <p>
 * A trade in an underlying {@link CapitalIndexedBond}.
 */
@BeanDefinition
public final class CapitalIndexedBondTrade
    implements SecurityTrade<CapitalIndexedBond>, ResolvableTrade<ResolvedCapitalIndexedBondTrade>, ImmutableBean, Serializable {

  /**
   * The additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   */
  @PropertyDefinition(overrideGet = true)
  private final TradeInfo tradeInfo;
  /**
   * The link to the capital indexed bond that was traded.
   * <p>
   * This property returns a link to the security via a {@link StandardId}.
   * See {@link #getSecurity()} and {@link SecurityLink} for more details.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityLink<CapitalIndexedBond> securityLink;
  /**
   * The quantity, indicating the number of bond contracts in the trade.
   * <p>
   * This will be positive if buying and negative if selling.
   */
  @PropertyDefinition
  private final long quantity;
  /**
   * The clean price.
   * <p>
   * The price to be paid/received under this trade. 
   * This clean price is nominal price or real price depending on the yield convention of the product. 
   */
  @PropertyDefinition
  private final double cleanPrice;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.tradeInfo = TradeInfo.EMPTY;
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(getTradeInfo().getSettlementDate().isPresent(), "trade settlement date must be specified");
  }

  //-------------------------------------------------------------------------
  @Override
  public SecurityTrade<CapitalIndexedBond> resolveLinks(LinkResolver resolver) {
    return resolver.resolveLinksIn(this, securityLink, resolved -> toBuilder().securityLink(resolved).build());
  }

  @Override
  public ResolvedCapitalIndexedBondTrade resolve(ReferenceData refData) {
    CapitalIndexedBond product = getProduct();
    ResolvedCapitalIndexedBond resolvedProduct = product.resolve(refData);
    LocalDate settlementDate = tradeInfo.getSettlementDate().get();
    double accruedInterest = resolvedProduct.accruedInterest(settlementDate) / product.getNotional();
    if (settlementDate.isBefore(resolvedProduct.getStartDate())) {
      throw new IllegalArgumentException("Settlement date must not be before bond starts");
    }
    PaymentPeriod settlement;
    if (product.getYieldConvention().equals(CapitalIndexedBondYieldConvention.INDEX_LINKED_FLOAT)) {
      settlement = KnownAmountPaymentPeriod.of(
          Payment.of(product.getCurrency(),
              -product.getNotional() * quantity * (cleanPrice + accruedInterest), settlementDate),
          SchedulePeriod.of(
              resolvedProduct.getStartDate(),
              settlementDate,
              product.getPeriodicSchedule().getStartDate(),
              settlementDate));
    } else {
      RateObservation rateObservation =
          product.getRateCalculation().createRateObservation(settlementDate, product.getStartIndexValue());
      settlement = CapitalIndexedBondPaymentPeriod.builder()
          .startDate(resolvedProduct.getStartDate())
          .unadjustedStartDate(product.getPeriodicSchedule().getStartDate())
          .endDate(settlementDate)
          .rateObservation(rateObservation)
          .currency(product.getCurrency())
          .notional(-product.getNotional() * quantity * (cleanPrice + accruedInterest))
          .realCoupon(1d)
          .build();
    }

    return ResolvedCapitalIndexedBondTrade.builder()
        .tradeInfo(tradeInfo)
        .product(resolvedProduct)
        .securityStandardId(getSecurity().getStandardId())
        .quantity(quantity)
        .settlement(settlement)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CapitalIndexedBondTrade}.
   * @return the meta-bean, not null
   */
  public static CapitalIndexedBondTrade.Meta meta() {
    return CapitalIndexedBondTrade.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CapitalIndexedBondTrade.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CapitalIndexedBondTrade.Builder builder() {
    return new CapitalIndexedBondTrade.Builder();
  }

  private CapitalIndexedBondTrade(
      TradeInfo tradeInfo,
      SecurityLink<CapitalIndexedBond> securityLink,
      long quantity,
      double cleanPrice) {
    JodaBeanUtils.notNull(securityLink, "securityLink");
    this.tradeInfo = tradeInfo;
    this.securityLink = securityLink;
    this.quantity = quantity;
    this.cleanPrice = cleanPrice;
    validate();
  }

  @Override
  public CapitalIndexedBondTrade.Meta metaBean() {
    return CapitalIndexedBondTrade.Meta.INSTANCE;
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
   * @return the value of the property
   */
  @Override
  public TradeInfo getTradeInfo() {
    return tradeInfo;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the link to the capital indexed bond that was traded.
   * <p>
   * This property returns a link to the security via a {@link StandardId}.
   * See {@link #getSecurity()} and {@link SecurityLink} for more details.
   * @return the value of the property, not null
   */
  @Override
  public SecurityLink<CapitalIndexedBond> getSecurityLink() {
    return securityLink;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the quantity, indicating the number of bond contracts in the trade.
   * <p>
   * This will be positive if buying and negative if selling.
   * @return the value of the property
   */
  public long getQuantity() {
    return quantity;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the clean price.
   * <p>
   * The price to be paid/received under this trade.
   * This clean price is nominal price or real price depending on the yield convention of the product.
   * @return the value of the property
   */
  public double getCleanPrice() {
    return cleanPrice;
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
      CapitalIndexedBondTrade other = (CapitalIndexedBondTrade) obj;
      return JodaBeanUtils.equal(tradeInfo, other.tradeInfo) &&
          JodaBeanUtils.equal(securityLink, other.securityLink) &&
          (quantity == other.quantity) &&
          JodaBeanUtils.equal(cleanPrice, other.cleanPrice);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(tradeInfo);
    hash = hash * 31 + JodaBeanUtils.hashCode(securityLink);
    hash = hash * 31 + JodaBeanUtils.hashCode(quantity);
    hash = hash * 31 + JodaBeanUtils.hashCode(cleanPrice);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("CapitalIndexedBondTrade{");
    buf.append("tradeInfo").append('=').append(tradeInfo).append(',').append(' ');
    buf.append("securityLink").append('=').append(securityLink).append(',').append(' ');
    buf.append("quantity").append('=').append(quantity).append(',').append(' ');
    buf.append("cleanPrice").append('=').append(JodaBeanUtils.toString(cleanPrice));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CapitalIndexedBondTrade}.
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
        this, "tradeInfo", CapitalIndexedBondTrade.class, TradeInfo.class);
    /**
     * The meta-property for the {@code securityLink} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<SecurityLink<CapitalIndexedBond>> securityLink = DirectMetaProperty.ofImmutable(
        this, "securityLink", CapitalIndexedBondTrade.class, (Class) SecurityLink.class);
    /**
     * The meta-property for the {@code quantity} property.
     */
    private final MetaProperty<Long> quantity = DirectMetaProperty.ofImmutable(
        this, "quantity", CapitalIndexedBondTrade.class, Long.TYPE);
    /**
     * The meta-property for the {@code cleanPrice} property.
     */
    private final MetaProperty<Double> cleanPrice = DirectMetaProperty.ofImmutable(
        this, "cleanPrice", CapitalIndexedBondTrade.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "tradeInfo",
        "securityLink",
        "quantity",
        "cleanPrice");

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
        case 807992154:  // securityLink
          return securityLink;
        case -1285004149:  // quantity
          return quantity;
        case -861237120:  // cleanPrice
          return cleanPrice;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CapitalIndexedBondTrade.Builder builder() {
      return new CapitalIndexedBondTrade.Builder();
    }

    @Override
    public Class<? extends CapitalIndexedBondTrade> beanType() {
      return CapitalIndexedBondTrade.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code tradeInfo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<TradeInfo> tradeInfo() {
      return tradeInfo;
    }

    /**
     * The meta-property for the {@code securityLink} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityLink<CapitalIndexedBond>> securityLink() {
      return securityLink;
    }

    /**
     * The meta-property for the {@code quantity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Long> quantity() {
      return quantity;
    }

    /**
     * The meta-property for the {@code cleanPrice} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> cleanPrice() {
      return cleanPrice;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          return ((CapitalIndexedBondTrade) bean).getTradeInfo();
        case 807992154:  // securityLink
          return ((CapitalIndexedBondTrade) bean).getSecurityLink();
        case -1285004149:  // quantity
          return ((CapitalIndexedBondTrade) bean).getQuantity();
        case -861237120:  // cleanPrice
          return ((CapitalIndexedBondTrade) bean).getCleanPrice();
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
   * The bean-builder for {@code CapitalIndexedBondTrade}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CapitalIndexedBondTrade> {

    private TradeInfo tradeInfo;
    private SecurityLink<CapitalIndexedBond> securityLink;
    private long quantity;
    private double cleanPrice;

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
    private Builder(CapitalIndexedBondTrade beanToCopy) {
      this.tradeInfo = beanToCopy.getTradeInfo();
      this.securityLink = beanToCopy.getSecurityLink();
      this.quantity = beanToCopy.getQuantity();
      this.cleanPrice = beanToCopy.getCleanPrice();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          return tradeInfo;
        case 807992154:  // securityLink
          return securityLink;
        case -1285004149:  // quantity
          return quantity;
        case -861237120:  // cleanPrice
          return cleanPrice;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          this.tradeInfo = (TradeInfo) newValue;
          break;
        case 807992154:  // securityLink
          this.securityLink = (SecurityLink<CapitalIndexedBond>) newValue;
          break;
        case -1285004149:  // quantity
          this.quantity = (Long) newValue;
          break;
        case -861237120:  // cleanPrice
          this.cleanPrice = (Double) newValue;
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
    public CapitalIndexedBondTrade build() {
      return new CapitalIndexedBondTrade(
          tradeInfo,
          securityLink,
          quantity,
          cleanPrice);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the additional trade information, defaulted to an empty instance.
     * <p>
     * This allows additional information to be attached to the trade.
     * @param tradeInfo  the new value
     * @return this, for chaining, not null
     */
    public Builder tradeInfo(TradeInfo tradeInfo) {
      this.tradeInfo = tradeInfo;
      return this;
    }

    /**
     * Sets the link to the capital indexed bond that was traded.
     * <p>
     * This property returns a link to the security via a {@link StandardId}.
     * See {@link #getSecurity()} and {@link SecurityLink} for more details.
     * @param securityLink  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder securityLink(SecurityLink<CapitalIndexedBond> securityLink) {
      JodaBeanUtils.notNull(securityLink, "securityLink");
      this.securityLink = securityLink;
      return this;
    }

    /**
     * Sets the quantity, indicating the number of bond contracts in the trade.
     * <p>
     * This will be positive if buying and negative if selling.
     * @param quantity  the new value
     * @return this, for chaining, not null
     */
    public Builder quantity(long quantity) {
      this.quantity = quantity;
      return this;
    }

    /**
     * Sets the clean price.
     * <p>
     * The price to be paid/received under this trade.
     * This clean price is nominal price or real price depending on the yield convention of the product.
     * @param cleanPrice  the new value
     * @return this, for chaining, not null
     */
    public Builder cleanPrice(double cleanPrice) {
      this.cleanPrice = cleanPrice;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("CapitalIndexedBondTrade.Builder{");
      buf.append("tradeInfo").append('=').append(JodaBeanUtils.toString(tradeInfo)).append(',').append(' ');
      buf.append("securityLink").append('=').append(JodaBeanUtils.toString(securityLink)).append(',').append(' ');
      buf.append("quantity").append('=').append(JodaBeanUtils.toString(quantity)).append(',').append(' ');
      buf.append("cleanPrice").append('=').append(JodaBeanUtils.toString(cleanPrice));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
