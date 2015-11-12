/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.future;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.id.LinkResolutionException;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.Resolvable;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;

/**
 * A generic futures option contract based on an expiry month.
 * <p>
 * A futures option is a financial instrument that is based on the future value of an underlying.
 * The buyer of a call option has the right, but not the obligation, to purchase the underlying
 * at a price fixed in advance.
 * This class represents the structure of a single futures option contract.
 * <p>
 * For example, an airline can use a futures option to ensure that the price of jet fuel
 * at some date in the future will not exceed a certain amount.
 */
@BeanDefinition
public final class GenericFutureOption
    implements Product, Resolvable<GenericFutureOption>, ImmutableBean, Serializable {

  /**
   * The base product identifier.
   * <p>
   * The identifier that is used for the base product, also known as the symbol.
   * A future option typically expires monthly or quarterly, thus the product referred to here
   * is the base product of a series of contracts. A unique identifier for the contract is formed
   * by combining the base product, put/call, strike and expiry month.
   * For example, 'Eurex~OGBL' could be used to refer to the Euro-Bund option base product at Eurex.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId productId;
  /**
   * The expiry month.
   * <p>
   * The month used to identify the expiry of the option.
   * When the option expires, trading stops.
   * <p>
   * Options expire on a specific date, but as there is typically only one contract per month,
   * the month is used to refer to the future. Note that it is possible for the expiry
   * date to be in a different calendar month to that used to refer to the option.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth expiryMonth;
  /**
   * Whether the option is put or call.
   * <p>
   * A call gives the owner the right, but not obligation, to buy the underlying at
   * an agreed price in the future. A put gives a similar option to sell.
   */
  @PropertyDefinition
  private final PutCall putCall;
  /**
   * The strike price, represented in decimal form.
   * <p>
   * This is the price at which the option applies and refers to the price of the underlying future.
   * This must be represented in decimal form.
   * <p>
   * No indication is provided as to the meaning of one unit of this price.
   * It may be an amount in a currency, a percentage or something else entirely.
   */
  @PropertyDefinition
  private final double strikePrice;
  /**
   * The expiry date, optional.
   * <p>
   * This is the date that the option expires.
   * A generic future option is intended to be used for future options that expire monthly or quarterly.
   * As such, the {@code expiryMonth} field is used to identify the contract and this
   * date is primarily for information.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate expiryDate;
  /**
   * The size of each tick.
   * <p>
   * The tick size is defined as a decimal number.
   * If the tick size is 1/32, the tick size would be 0.03125.
   */
  @PropertyDefinition
  private final double tickSize;
  /**
   * The monetary value of one tick.
   * <p>
   * When the price changes by one tick, this amount is gained/lost.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount tickValue;
  /**
   * The quantity of the underlying future that the option refers to, defaulted to 1.
   * <p>
   * An option typically refers to one future, and this will be set by default.
   */
  @PropertyDefinition
  private final long underlyingQuantity;
  /**
   * The link to the underlying future.
   * <p>
   * This property returns a link to the security via a {@link StandardId}.
   * See {@link #getUnderlying()} and {@link SecurityLink} for more details.
   */
  @PropertyDefinition(get = "optional")
  private final SecurityLink<GenericFuture> underlyingLink;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    builder.underlyingQuantity = 1;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the future.
   * <p>
   * The currency is derived from the tick value.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return tickValue.getCurrency();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying future security that was traded, throwing an exception if not resolved.
   * <p>
   * This method accesses the security via the {@link #getUnderlyingLink() underlyingLink} property.
   * The link has two states, resolvable and resolved.
   * <p>
   * In the resolved state, the security is known and available for use.
   * The security object will be directly embedded in the link held within this trade.
   * <p>
   * In the resolvable state, only the identifier and type of the security are known.
   * These act as a pointer to the security, and as such the security is not directly available.
   * The link must be resolved before use.
   * This can be achieved by calling {@link #resolveLinks(LinkResolver)} on this trade.
   * If the trade has not been resolved, then this method will throw a {@link LinkResolutionException}.
   * 
   * @return full details of the security
   * @throws LinkResolutionException if the security is not resolved
   */
  public Optional<Security<GenericFuture>> getUnderlyingSecurity() {
    return getUnderlyingLink().map(link -> link.resolvedTarget());
  }

  /**
   * Gets the underlying future that was traded, throwing an exception if not resolved.
   * <p>
   * Returns the underlying product that captures the contracted financial details of the trade.
   * This method accesses the security via the {@link #getUnderlyingLink() underlyingLink} property.
   * The link has two states, resolvable and resolved.
   * <p>
   * In the resolved state, the security is known and available for use.
   * The security object will be directly embedded in the link held within this trade.
   * <p>
   * In the resolvable state, only the identifier and type of the security are known.
   * These act as a pointer to the security, and as such the security is not directly available.
   * The link must be resolved before use.
   * This can be achieved by calling {@link #resolveLinks(LinkResolver)} on this trade.
   * If the trade has not been resolved, then this method will throw a {@link LinkResolutionException}.
   * 
   * @return the product underlying the option
   * @throws LinkResolutionException if the security is not resolved
   */
  public Optional<GenericFuture> getUnderlying() {
    return getUnderlyingLink().map(link -> link.resolvedTarget().getProduct());
  }

  //-------------------------------------------------------------------------
  @Override
  public GenericFutureOption resolveLinks(LinkResolver resolver) {
    return resolver.resolveLinksIn(this, underlyingLink, resolved -> toBuilder().underlyingLink(resolved).build());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code GenericFutureOption}.
   * @return the meta-bean, not null
   */
  public static GenericFutureOption.Meta meta() {
    return GenericFutureOption.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(GenericFutureOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static GenericFutureOption.Builder builder() {
    return new GenericFutureOption.Builder();
  }

  private GenericFutureOption(
      StandardId productId,
      YearMonth expiryMonth,
      PutCall putCall,
      double strikePrice,
      LocalDate expiryDate,
      double tickSize,
      CurrencyAmount tickValue,
      long underlyingQuantity,
      SecurityLink<GenericFuture> underlyingLink) {
    JodaBeanUtils.notNull(productId, "productId");
    JodaBeanUtils.notNull(expiryMonth, "expiryMonth");
    JodaBeanUtils.notNull(tickValue, "tickValue");
    this.productId = productId;
    this.expiryMonth = expiryMonth;
    this.putCall = putCall;
    this.strikePrice = strikePrice;
    this.expiryDate = expiryDate;
    this.tickSize = tickSize;
    this.tickValue = tickValue;
    this.underlyingQuantity = underlyingQuantity;
    this.underlyingLink = underlyingLink;
  }

  @Override
  public GenericFutureOption.Meta metaBean() {
    return GenericFutureOption.Meta.INSTANCE;
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
   * Gets the base product identifier.
   * <p>
   * The identifier that is used for the base product, also known as the symbol.
   * A future option typically expires monthly or quarterly, thus the product referred to here
   * is the base product of a series of contracts. A unique identifier for the contract is formed
   * by combining the base product, put/call, strike and expiry month.
   * For example, 'Eurex~OGBL' could be used to refer to the Euro-Bund option base product at Eurex.
   * @return the value of the property, not null
   */
  public StandardId getProductId() {
    return productId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry month.
   * <p>
   * The month used to identify the expiry of the option.
   * When the option expires, trading stops.
   * <p>
   * Options expire on a specific date, but as there is typically only one contract per month,
   * the month is used to refer to the future. Note that it is possible for the expiry
   * date to be in a different calendar month to that used to refer to the option.
   * @return the value of the property, not null
   */
  public YearMonth getExpiryMonth() {
    return expiryMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the option is put or call.
   * <p>
   * A call gives the owner the right, but not obligation, to buy the underlying at
   * an agreed price in the future. A put gives a similar option to sell.
   * @return the value of the property
   */
  public PutCall getPutCall() {
    return putCall;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike price, represented in decimal form.
   * <p>
   * This is the price at which the option applies and refers to the price of the underlying future.
   * This must be represented in decimal form.
   * <p>
   * No indication is provided as to the meaning of one unit of this price.
   * It may be an amount in a currency, a percentage or something else entirely.
   * @return the value of the property
   */
  public double getStrikePrice() {
    return strikePrice;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry date, optional.
   * <p>
   * This is the date that the option expires.
   * A generic future option is intended to be used for future options that expire monthly or quarterly.
   * As such, the {@code expiryMonth} field is used to identify the contract and this
   * date is primarily for information.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getExpiryDate() {
    return Optional.ofNullable(expiryDate);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the size of each tick.
   * <p>
   * The tick size is defined as a decimal number.
   * If the tick size is 1/32, the tick size would be 0.03125.
   * @return the value of the property
   */
  public double getTickSize() {
    return tickSize;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the monetary value of one tick.
   * <p>
   * When the price changes by one tick, this amount is gained/lost.
   * @return the value of the property, not null
   */
  public CurrencyAmount getTickValue() {
    return tickValue;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the quantity of the underlying future that the option refers to, defaulted to 1.
   * <p>
   * An option typically refers to one future, and this will be set by default.
   * @return the value of the property
   */
  public long getUnderlyingQuantity() {
    return underlyingQuantity;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the link to the underlying future.
   * <p>
   * This property returns a link to the security via a {@link StandardId}.
   * See {@link #getUnderlying()} and {@link SecurityLink} for more details.
   * @return the optional value of the property, not null
   */
  public Optional<SecurityLink<GenericFuture>> getUnderlyingLink() {
    return Optional.ofNullable(underlyingLink);
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
      GenericFutureOption other = (GenericFutureOption) obj;
      return JodaBeanUtils.equal(getProductId(), other.getProductId()) &&
          JodaBeanUtils.equal(getExpiryMonth(), other.getExpiryMonth()) &&
          JodaBeanUtils.equal(getPutCall(), other.getPutCall()) &&
          JodaBeanUtils.equal(getStrikePrice(), other.getStrikePrice()) &&
          JodaBeanUtils.equal(expiryDate, other.expiryDate) &&
          JodaBeanUtils.equal(getTickSize(), other.getTickSize()) &&
          JodaBeanUtils.equal(getTickValue(), other.getTickValue()) &&
          (getUnderlyingQuantity() == other.getUnderlyingQuantity()) &&
          JodaBeanUtils.equal(underlyingLink, other.underlyingLink);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getProductId());
    hash = hash * 31 + JodaBeanUtils.hashCode(getExpiryMonth());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPutCall());
    hash = hash * 31 + JodaBeanUtils.hashCode(getStrikePrice());
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(getTickSize());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTickValue());
    hash = hash * 31 + JodaBeanUtils.hashCode(getUnderlyingQuantity());
    hash = hash * 31 + JodaBeanUtils.hashCode(underlyingLink);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("GenericFutureOption{");
    buf.append("productId").append('=').append(getProductId()).append(',').append(' ');
    buf.append("expiryMonth").append('=').append(getExpiryMonth()).append(',').append(' ');
    buf.append("putCall").append('=').append(getPutCall()).append(',').append(' ');
    buf.append("strikePrice").append('=').append(getStrikePrice()).append(',').append(' ');
    buf.append("expiryDate").append('=').append(expiryDate).append(',').append(' ');
    buf.append("tickSize").append('=').append(getTickSize()).append(',').append(' ');
    buf.append("tickValue").append('=').append(getTickValue()).append(',').append(' ');
    buf.append("underlyingQuantity").append('=').append(getUnderlyingQuantity()).append(',').append(' ');
    buf.append("underlyingLink").append('=').append(JodaBeanUtils.toString(underlyingLink));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code GenericFutureOption}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code productId} property.
     */
    private final MetaProperty<StandardId> productId = DirectMetaProperty.ofImmutable(
        this, "productId", GenericFutureOption.class, StandardId.class);
    /**
     * The meta-property for the {@code expiryMonth} property.
     */
    private final MetaProperty<YearMonth> expiryMonth = DirectMetaProperty.ofImmutable(
        this, "expiryMonth", GenericFutureOption.class, YearMonth.class);
    /**
     * The meta-property for the {@code putCall} property.
     */
    private final MetaProperty<PutCall> putCall = DirectMetaProperty.ofImmutable(
        this, "putCall", GenericFutureOption.class, PutCall.class);
    /**
     * The meta-property for the {@code strikePrice} property.
     */
    private final MetaProperty<Double> strikePrice = DirectMetaProperty.ofImmutable(
        this, "strikePrice", GenericFutureOption.class, Double.TYPE);
    /**
     * The meta-property for the {@code expiryDate} property.
     */
    private final MetaProperty<LocalDate> expiryDate = DirectMetaProperty.ofImmutable(
        this, "expiryDate", GenericFutureOption.class, LocalDate.class);
    /**
     * The meta-property for the {@code tickSize} property.
     */
    private final MetaProperty<Double> tickSize = DirectMetaProperty.ofImmutable(
        this, "tickSize", GenericFutureOption.class, Double.TYPE);
    /**
     * The meta-property for the {@code tickValue} property.
     */
    private final MetaProperty<CurrencyAmount> tickValue = DirectMetaProperty.ofImmutable(
        this, "tickValue", GenericFutureOption.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code underlyingQuantity} property.
     */
    private final MetaProperty<Long> underlyingQuantity = DirectMetaProperty.ofImmutable(
        this, "underlyingQuantity", GenericFutureOption.class, Long.TYPE);
    /**
     * The meta-property for the {@code underlyingLink} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<SecurityLink<GenericFuture>> underlyingLink = DirectMetaProperty.ofImmutable(
        this, "underlyingLink", GenericFutureOption.class, (Class) SecurityLink.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "productId",
        "expiryMonth",
        "putCall",
        "strikePrice",
        "expiryDate",
        "tickSize",
        "tickValue",
        "underlyingQuantity",
        "underlyingLink");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1051830678:  // productId
          return productId;
        case 459635981:  // expiryMonth
          return expiryMonth;
        case -219971059:  // putCall
          return putCall;
        case 50946231:  // strikePrice
          return strikePrice;
        case -816738431:  // expiryDate
          return expiryDate;
        case 1936822078:  // tickSize
          return tickSize;
        case -85538348:  // tickValue
          return tickValue;
        case 1331585800:  // underlyingQuantity
          return underlyingQuantity;
        case 1497199863:  // underlyingLink
          return underlyingLink;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public GenericFutureOption.Builder builder() {
      return new GenericFutureOption.Builder();
    }

    @Override
    public Class<? extends GenericFutureOption> beanType() {
      return GenericFutureOption.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code productId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> productId() {
      return productId;
    }

    /**
     * The meta-property for the {@code expiryMonth} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YearMonth> expiryMonth() {
      return expiryMonth;
    }

    /**
     * The meta-property for the {@code putCall} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PutCall> putCall() {
      return putCall;
    }

    /**
     * The meta-property for the {@code strikePrice} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strikePrice() {
      return strikePrice;
    }

    /**
     * The meta-property for the {@code expiryDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> expiryDate() {
      return expiryDate;
    }

    /**
     * The meta-property for the {@code tickSize} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> tickSize() {
      return tickSize;
    }

    /**
     * The meta-property for the {@code tickValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> tickValue() {
      return tickValue;
    }

    /**
     * The meta-property for the {@code underlyingQuantity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Long> underlyingQuantity() {
      return underlyingQuantity;
    }

    /**
     * The meta-property for the {@code underlyingLink} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityLink<GenericFuture>> underlyingLink() {
      return underlyingLink;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1051830678:  // productId
          return ((GenericFutureOption) bean).getProductId();
        case 459635981:  // expiryMonth
          return ((GenericFutureOption) bean).getExpiryMonth();
        case -219971059:  // putCall
          return ((GenericFutureOption) bean).getPutCall();
        case 50946231:  // strikePrice
          return ((GenericFutureOption) bean).getStrikePrice();
        case -816738431:  // expiryDate
          return ((GenericFutureOption) bean).expiryDate;
        case 1936822078:  // tickSize
          return ((GenericFutureOption) bean).getTickSize();
        case -85538348:  // tickValue
          return ((GenericFutureOption) bean).getTickValue();
        case 1331585800:  // underlyingQuantity
          return ((GenericFutureOption) bean).getUnderlyingQuantity();
        case 1497199863:  // underlyingLink
          return ((GenericFutureOption) bean).underlyingLink;
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
   * The bean-builder for {@code GenericFutureOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<GenericFutureOption> {

    private StandardId productId;
    private YearMonth expiryMonth;
    private PutCall putCall;
    private double strikePrice;
    private LocalDate expiryDate;
    private double tickSize;
    private CurrencyAmount tickValue;
    private long underlyingQuantity;
    private SecurityLink<GenericFuture> underlyingLink;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(GenericFutureOption beanToCopy) {
      this.productId = beanToCopy.getProductId();
      this.expiryMonth = beanToCopy.getExpiryMonth();
      this.putCall = beanToCopy.getPutCall();
      this.strikePrice = beanToCopy.getStrikePrice();
      this.expiryDate = beanToCopy.expiryDate;
      this.tickSize = beanToCopy.getTickSize();
      this.tickValue = beanToCopy.getTickValue();
      this.underlyingQuantity = beanToCopy.getUnderlyingQuantity();
      this.underlyingLink = beanToCopy.underlyingLink;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1051830678:  // productId
          return productId;
        case 459635981:  // expiryMonth
          return expiryMonth;
        case -219971059:  // putCall
          return putCall;
        case 50946231:  // strikePrice
          return strikePrice;
        case -816738431:  // expiryDate
          return expiryDate;
        case 1936822078:  // tickSize
          return tickSize;
        case -85538348:  // tickValue
          return tickValue;
        case 1331585800:  // underlyingQuantity
          return underlyingQuantity;
        case 1497199863:  // underlyingLink
          return underlyingLink;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1051830678:  // productId
          this.productId = (StandardId) newValue;
          break;
        case 459635981:  // expiryMonth
          this.expiryMonth = (YearMonth) newValue;
          break;
        case -219971059:  // putCall
          this.putCall = (PutCall) newValue;
          break;
        case 50946231:  // strikePrice
          this.strikePrice = (Double) newValue;
          break;
        case -816738431:  // expiryDate
          this.expiryDate = (LocalDate) newValue;
          break;
        case 1936822078:  // tickSize
          this.tickSize = (Double) newValue;
          break;
        case -85538348:  // tickValue
          this.tickValue = (CurrencyAmount) newValue;
          break;
        case 1331585800:  // underlyingQuantity
          this.underlyingQuantity = (Long) newValue;
          break;
        case 1497199863:  // underlyingLink
          this.underlyingLink = (SecurityLink<GenericFuture>) newValue;
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
    public GenericFutureOption build() {
      preBuild(this);
      return new GenericFutureOption(
          productId,
          expiryMonth,
          putCall,
          strikePrice,
          expiryDate,
          tickSize,
          tickValue,
          underlyingQuantity,
          underlyingLink);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the base product identifier.
     * <p>
     * The identifier that is used for the base product, also known as the symbol.
     * A future option typically expires monthly or quarterly, thus the product referred to here
     * is the base product of a series of contracts. A unique identifier for the contract is formed
     * by combining the base product, put/call, strike and expiry month.
     * For example, 'Eurex~OGBL' could be used to refer to the Euro-Bund option base product at Eurex.
     * @param productId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder productId(StandardId productId) {
      JodaBeanUtils.notNull(productId, "productId");
      this.productId = productId;
      return this;
    }

    /**
     * Sets the expiry month.
     * <p>
     * The month used to identify the expiry of the option.
     * When the option expires, trading stops.
     * <p>
     * Options expire on a specific date, but as there is typically only one contract per month,
     * the month is used to refer to the future. Note that it is possible for the expiry
     * date to be in a different calendar month to that used to refer to the option.
     * @param expiryMonth  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryMonth(YearMonth expiryMonth) {
      JodaBeanUtils.notNull(expiryMonth, "expiryMonth");
      this.expiryMonth = expiryMonth;
      return this;
    }

    /**
     * Sets whether the option is put or call.
     * <p>
     * A call gives the owner the right, but not obligation, to buy the underlying at
     * an agreed price in the future. A put gives a similar option to sell.
     * @param putCall  the new value
     * @return this, for chaining, not null
     */
    public Builder putCall(PutCall putCall) {
      this.putCall = putCall;
      return this;
    }

    /**
     * Sets the strike price, represented in decimal form.
     * <p>
     * This is the price at which the option applies and refers to the price of the underlying future.
     * This must be represented in decimal form.
     * <p>
     * No indication is provided as to the meaning of one unit of this price.
     * It may be an amount in a currency, a percentage or something else entirely.
     * @param strikePrice  the new value
     * @return this, for chaining, not null
     */
    public Builder strikePrice(double strikePrice) {
      this.strikePrice = strikePrice;
      return this;
    }

    /**
     * Sets the expiry date, optional.
     * <p>
     * This is the date that the option expires.
     * A generic future option is intended to be used for future options that expire monthly or quarterly.
     * As such, the {@code expiryMonth} field is used to identify the contract and this
     * date is primarily for information.
     * @param expiryDate  the new value
     * @return this, for chaining, not null
     */
    public Builder expiryDate(LocalDate expiryDate) {
      this.expiryDate = expiryDate;
      return this;
    }

    /**
     * Sets the size of each tick.
     * <p>
     * The tick size is defined as a decimal number.
     * If the tick size is 1/32, the tick size would be 0.03125.
     * @param tickSize  the new value
     * @return this, for chaining, not null
     */
    public Builder tickSize(double tickSize) {
      this.tickSize = tickSize;
      return this;
    }

    /**
     * Sets the monetary value of one tick.
     * <p>
     * When the price changes by one tick, this amount is gained/lost.
     * @param tickValue  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder tickValue(CurrencyAmount tickValue) {
      JodaBeanUtils.notNull(tickValue, "tickValue");
      this.tickValue = tickValue;
      return this;
    }

    /**
     * Sets the quantity of the underlying future that the option refers to, defaulted to 1.
     * <p>
     * An option typically refers to one future, and this will be set by default.
     * @param underlyingQuantity  the new value
     * @return this, for chaining, not null
     */
    public Builder underlyingQuantity(long underlyingQuantity) {
      this.underlyingQuantity = underlyingQuantity;
      return this;
    }

    /**
     * Sets the link to the underlying future.
     * <p>
     * This property returns a link to the security via a {@link StandardId}.
     * See {@link #getUnderlying()} and {@link SecurityLink} for more details.
     * @param underlyingLink  the new value
     * @return this, for chaining, not null
     */
    public Builder underlyingLink(SecurityLink<GenericFuture> underlyingLink) {
      this.underlyingLink = underlyingLink;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("GenericFutureOption.Builder{");
      buf.append("productId").append('=').append(JodaBeanUtils.toString(productId)).append(',').append(' ');
      buf.append("expiryMonth").append('=').append(JodaBeanUtils.toString(expiryMonth)).append(',').append(' ');
      buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice)).append(',').append(' ');
      buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
      buf.append("tickSize").append('=').append(JodaBeanUtils.toString(tickSize)).append(',').append(' ');
      buf.append("tickValue").append('=').append(JodaBeanUtils.toString(tickValue)).append(',').append(' ');
      buf.append("underlyingQuantity").append('=').append(JodaBeanUtils.toString(underlyingQuantity)).append(',').append(' ');
      buf.append("underlyingLink").append('=').append(JodaBeanUtils.toString(underlyingLink));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
