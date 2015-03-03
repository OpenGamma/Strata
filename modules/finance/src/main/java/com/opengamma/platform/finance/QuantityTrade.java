/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.collect.id.IdentifiableBean;
import com.opengamma.collect.id.LinkResolutionException;
import com.opengamma.collect.id.LinkResolver;
import com.opengamma.collect.id.Resolvable;
import com.opengamma.collect.id.StandardId;

/**
 * A trade in a security, where the amount purchased is defined as a quantity,
 * such as 200 shares of OpenGamma equity.
 * <p>
 * A {@code QuantityTrade} is used for those trades where a quantity of an underlying
 * {@link Security} has been traded. For example, a trade in an equity.
 * If the security was bought, the quantity will be positive.
 * If the security was sold, the quantity will be negative.
 * The price paid or received, and any other fees, can also be expressed.
 * 
 * @param <P>  the type of the product
 */
@BeanDefinition
public final class QuantityTrade<P extends Product>
    implements Trade, Resolvable<QuantityTrade<P>>, ImmutableBean, Serializable {

  /**
   * The primary standard identifier for the trade.
   * <p>
   * The standard identifier is used to identify the trade.
   * It will typically be an identifier in an external data system.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final StandardId standardId;
  /**
   * The set of additional trade attributes.
   * <p>
   * Most data in the trade is available as bean properties.
   * Attributes are typically used to tag the object with additional information.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ImmutableMap<String, String> attributes;
  /**
   * The additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   */
  @PropertyDefinition(overrideGet = true)
  private final TradeInfo tradeInfo;
  /**
   * The link to the security that was traded.
   * <p>
   * The underlying security is the fungible instrument that was traded.
   * This property returns a link to the security via a {@link StandardId}.
   * See {@link #getSecurity()} and {@link SecurityLink} for more details.
   */
  @PropertyDefinition(validate = "notNull")
  private final SecurityLink<P> securityLink;
  /**
   * The quantity of the security that has been traded.
   * <p>
   * For example, 200 shares of an equity.
   */
  @PropertyDefinition
  private final long quantity;
  /**
   * The premium paid for the trade.
   * <p>
   * The premium may be negative if money was received rather than paid.
   */
  @PropertyDefinition(get = "optional")
  private final CurrencyAmount premium;

  //-------------------------------------------------------------------------
  @SuppressWarnings({"rawtypes", "unchecked"})
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.tradeInfo = TradeInfo.EMPTY;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the security that was traded, throwing an exception if not resolved.
   * <p>
   * The underlying security is the fungible instrument that was traded.
   * This property returns full details of the security.
   * <p>
   * This method accesses the security via the {@link #getSecurityLink() securityLink} property.
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
  public Security<P> getSecurity() {
    return securityLink.resolve(new LinkResolver() {
      @Override
      public <R extends IdentifiableBean> R resolve(StandardId identifier, TypeToken<R> targetType) {
        throw new LinkResolutionException("Security must be resolved before it can be used: " + identifier);
      }
    });
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves all links in this trade, returning a fully resolved trade.
   * <p>
   * This method examines the trade, locates any links and resolves them.
   * The result is fully resolved with all data available for use.
   * Calling {@link #getSecurity()} on the result will not throw an exception.
   * <p>
   * An exception is thrown if a link cannot be resolved.
   * 
   * @param resolver  the resolver to use
   * @return the fully resolved trade
   * @throws LinkResolutionException if a link cannot be resolved
   */
  @Override
  public QuantityTrade<P> resolveLinks(LinkResolver resolver) {
    return resolver.resolveLinksIn(this, securityLink, resolved -> toBuilder().securityLink(resolved).build());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a builder used to create an instance, specifying the security.
   * <p>
   * When using this method, the security link will be resolved,
   * directly containing the security.
   * 
   * @param <R>  the product type
   * @param security  the security to embed in the link
   * @return the builder, with the {@code securityLink} property set
   */
  public static <R extends Product> QuantityTrade.Builder<R> builder(Security<R> security) {
    return QuantityTrade.builder(SecurityLink.resolved(security));
  }

  /**
   * Returns a builder used to create an instance, specifying the security link.
   * <p>
   * When using this method, the security link will be set in the builder.
   * 
   * @param <R>  the product type
   * @param securityLink  the security link
   * @return the builder, with the {@code securityLink} property set
   */
  public static <R extends Product> QuantityTrade.Builder<R> builder(SecurityLink<R> securityLink) {
    return QuantityTrade.<R>builder().securityLink(securityLink);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code QuantityTrade}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("rawtypes")
  public static QuantityTrade.Meta meta() {
    return QuantityTrade.Meta.INSTANCE;
  }

  /**
   * The meta-bean for {@code QuantityTrade}.
   * @param <R>  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static <R extends Product> QuantityTrade.Meta<R> metaQuantityTrade(Class<R> cls) {
    return QuantityTrade.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(QuantityTrade.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @param <P>  the type
   * @return the builder, not null
   */
  public static <P extends Product> QuantityTrade.Builder<P> builder() {
    return new QuantityTrade.Builder<P>();
  }

  private QuantityTrade(
      StandardId standardId,
      Map<String, String> attributes,
      TradeInfo tradeInfo,
      SecurityLink<P> securityLink,
      long quantity,
      CurrencyAmount premium) {
    JodaBeanUtils.notNull(standardId, "standardId");
    JodaBeanUtils.notNull(attributes, "attributes");
    JodaBeanUtils.notNull(securityLink, "securityLink");
    this.standardId = standardId;
    this.attributes = ImmutableMap.copyOf(attributes);
    this.tradeInfo = tradeInfo;
    this.securityLink = securityLink;
    this.quantity = quantity;
    this.premium = premium;
  }

  @SuppressWarnings("unchecked")
  @Override
  public QuantityTrade.Meta<P> metaBean() {
    return QuantityTrade.Meta.INSTANCE;
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
   * Gets the primary standard identifier for the trade.
   * <p>
   * The standard identifier is used to identify the trade.
   * It will typically be an identifier in an external data system.
   * @return the value of the property, not null
   */
  @Override
  public StandardId getStandardId() {
    return standardId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the set of additional trade attributes.
   * <p>
   * Most data in the trade is available as bean properties.
   * Attributes are typically used to tag the object with additional information.
   * @return the value of the property, not null
   */
  @Override
  public ImmutableMap<String, String> getAttributes() {
    return attributes;
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
   * Gets the link to the security that was traded.
   * <p>
   * The underlying security is the fungible instrument that was traded.
   * This property returns a link to the security via a {@link StandardId}.
   * See {@link #getSecurity()} and {@link SecurityLink} for more details.
   * @return the value of the property, not null
   */
  public SecurityLink<P> getSecurityLink() {
    return securityLink;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the quantity of the security that has been traded.
   * <p>
   * For example, 200 shares of an equity.
   * @return the value of the property
   */
  public long getQuantity() {
    return quantity;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the premium paid for the trade.
   * <p>
   * The premium may be negative if money was received rather than paid.
   * @return the optional value of the property, not null
   */
  public Optional<CurrencyAmount> getPremium() {
    return Optional.ofNullable(premium);
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder<P> toBuilder() {
    return new Builder<P>(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      QuantityTrade<?> other = (QuantityTrade<?>) obj;
      return JodaBeanUtils.equal(getStandardId(), other.getStandardId()) &&
          JodaBeanUtils.equal(getAttributes(), other.getAttributes()) &&
          JodaBeanUtils.equal(getTradeInfo(), other.getTradeInfo()) &&
          JodaBeanUtils.equal(getSecurityLink(), other.getSecurityLink()) &&
          (getQuantity() == other.getQuantity()) &&
          JodaBeanUtils.equal(premium, other.premium);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getStandardId());
    hash = hash * 31 + JodaBeanUtils.hashCode(getAttributes());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTradeInfo());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSecurityLink());
    hash = hash * 31 + JodaBeanUtils.hashCode(getQuantity());
    hash = hash * 31 + JodaBeanUtils.hashCode(premium);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("QuantityTrade{");
    buf.append("standardId").append('=').append(getStandardId()).append(',').append(' ');
    buf.append("attributes").append('=').append(getAttributes()).append(',').append(' ');
    buf.append("tradeInfo").append('=').append(getTradeInfo()).append(',').append(' ');
    buf.append("securityLink").append('=').append(getSecurityLink()).append(',').append(' ');
    buf.append("quantity").append('=').append(getQuantity()).append(',').append(' ');
    buf.append("premium").append('=').append(JodaBeanUtils.toString(premium));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code QuantityTrade}.
   * @param <P>  the type
   */
  public static final class Meta<P extends Product> extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    @SuppressWarnings("rawtypes")
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code standardId} property.
     */
    private final MetaProperty<StandardId> standardId = DirectMetaProperty.ofImmutable(
        this, "standardId", QuantityTrade.class, StandardId.class);
    /**
     * The meta-property for the {@code attributes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<String, String>> attributes = DirectMetaProperty.ofImmutable(
        this, "attributes", QuantityTrade.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code tradeInfo} property.
     */
    private final MetaProperty<TradeInfo> tradeInfo = DirectMetaProperty.ofImmutable(
        this, "tradeInfo", QuantityTrade.class, TradeInfo.class);
    /**
     * The meta-property for the {@code securityLink} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<SecurityLink<P>> securityLink = DirectMetaProperty.ofImmutable(
        this, "securityLink", QuantityTrade.class, (Class) SecurityLink.class);
    /**
     * The meta-property for the {@code quantity} property.
     */
    private final MetaProperty<Long> quantity = DirectMetaProperty.ofImmutable(
        this, "quantity", QuantityTrade.class, Long.TYPE);
    /**
     * The meta-property for the {@code premium} property.
     */
    private final MetaProperty<CurrencyAmount> premium = DirectMetaProperty.ofImmutable(
        this, "premium", QuantityTrade.class, CurrencyAmount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "standardId",
        "attributes",
        "tradeInfo",
        "securityLink",
        "quantity",
        "premium");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1284477768:  // standardId
          return standardId;
        case 405645655:  // attributes
          return attributes;
        case 752580658:  // tradeInfo
          return tradeInfo;
        case 807992154:  // securityLink
          return securityLink;
        case -1285004149:  // quantity
          return quantity;
        case -318452137:  // premium
          return premium;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public QuantityTrade.Builder<P> builder() {
      return new QuantityTrade.Builder<P>();
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public Class<? extends QuantityTrade<P>> beanType() {
      return (Class) QuantityTrade.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code standardId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> standardId() {
      return standardId;
    }

    /**
     * The meta-property for the {@code attributes} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<String, String>> attributes() {
      return attributes;
    }

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
    public MetaProperty<SecurityLink<P>> securityLink() {
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
     * The meta-property for the {@code premium} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> premium() {
      return premium;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1284477768:  // standardId
          return ((QuantityTrade<?>) bean).getStandardId();
        case 405645655:  // attributes
          return ((QuantityTrade<?>) bean).getAttributes();
        case 752580658:  // tradeInfo
          return ((QuantityTrade<?>) bean).getTradeInfo();
        case 807992154:  // securityLink
          return ((QuantityTrade<?>) bean).getSecurityLink();
        case -1285004149:  // quantity
          return ((QuantityTrade<?>) bean).getQuantity();
        case -318452137:  // premium
          return ((QuantityTrade<?>) bean).premium;
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
   * The bean-builder for {@code QuantityTrade}.
   * @param <P>  the type
   */
  public static final class Builder<P extends Product> extends DirectFieldsBeanBuilder<QuantityTrade<P>> {

    private StandardId standardId;
    private Map<String, String> attributes = ImmutableMap.of();
    private TradeInfo tradeInfo;
    private SecurityLink<P> securityLink;
    private long quantity;
    private CurrencyAmount premium;

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
    private Builder(QuantityTrade<P> beanToCopy) {
      this.standardId = beanToCopy.getStandardId();
      this.attributes = beanToCopy.getAttributes();
      this.tradeInfo = beanToCopy.getTradeInfo();
      this.securityLink = beanToCopy.getSecurityLink();
      this.quantity = beanToCopy.getQuantity();
      this.premium = beanToCopy.premium;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1284477768:  // standardId
          return standardId;
        case 405645655:  // attributes
          return attributes;
        case 752580658:  // tradeInfo
          return tradeInfo;
        case 807992154:  // securityLink
          return securityLink;
        case -1285004149:  // quantity
          return quantity;
        case -318452137:  // premium
          return premium;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<P> set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1284477768:  // standardId
          this.standardId = (StandardId) newValue;
          break;
        case 405645655:  // attributes
          this.attributes = (Map<String, String>) newValue;
          break;
        case 752580658:  // tradeInfo
          this.tradeInfo = (TradeInfo) newValue;
          break;
        case 807992154:  // securityLink
          this.securityLink = (SecurityLink<P>) newValue;
          break;
        case -1285004149:  // quantity
          this.quantity = (Long) newValue;
          break;
        case -318452137:  // premium
          this.premium = (CurrencyAmount) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder<P> set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder<P> setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder<P> setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder<P> setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public QuantityTrade<P> build() {
      return new QuantityTrade<P>(
          standardId,
          attributes,
          tradeInfo,
          securityLink,
          quantity,
          premium);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code standardId} property in the builder.
     * @param standardId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder<P> standardId(StandardId standardId) {
      JodaBeanUtils.notNull(standardId, "standardId");
      this.standardId = standardId;
      return this;
    }

    /**
     * Sets the {@code attributes} property in the builder.
     * @param attributes  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder<P> attributes(Map<String, String> attributes) {
      JodaBeanUtils.notNull(attributes, "attributes");
      this.attributes = attributes;
      return this;
    }

    /**
     * Sets the {@code tradeInfo} property in the builder.
     * @param tradeInfo  the new value
     * @return this, for chaining, not null
     */
    public Builder<P> tradeInfo(TradeInfo tradeInfo) {
      this.tradeInfo = tradeInfo;
      return this;
    }

    /**
     * Sets the {@code securityLink} property in the builder.
     * @param securityLink  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder<P> securityLink(SecurityLink<P> securityLink) {
      JodaBeanUtils.notNull(securityLink, "securityLink");
      this.securityLink = securityLink;
      return this;
    }

    /**
     * Sets the {@code quantity} property in the builder.
     * @param quantity  the new value
     * @return this, for chaining, not null
     */
    public Builder<P> quantity(long quantity) {
      this.quantity = quantity;
      return this;
    }

    /**
     * Sets the {@code premium} property in the builder.
     * @param premium  the new value
     * @return this, for chaining, not null
     */
    public Builder<P> premium(CurrencyAmount premium) {
      this.premium = premium;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("QuantityTrade.Builder{");
      buf.append("standardId").append('=').append(JodaBeanUtils.toString(standardId)).append(',').append(' ');
      buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes)).append(',').append(' ');
      buf.append("tradeInfo").append('=').append(JodaBeanUtils.toString(tradeInfo)).append(',').append(' ');
      buf.append("securityLink").append('=').append(JodaBeanUtils.toString(securityLink)).append(',').append(' ');
      buf.append("quantity").append('=').append(JodaBeanUtils.toString(quantity)).append(',').append(' ');
      buf.append("premium").append('=').append(JodaBeanUtils.toString(premium));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
