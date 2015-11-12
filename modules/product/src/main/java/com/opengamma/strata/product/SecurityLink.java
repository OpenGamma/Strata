/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
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

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.Link;
import com.opengamma.strata.collect.id.LinkResolutionException;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.Resolvable;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.id.StandardLink;

/**
 * A link to a security.
 * <p>
 * A security link provides loose coupling between different parts of the object model.
 * For example, an equity trade can hold a link to the underlying equity.
 * The two objects can be updated independently of each other.
 * If the link resolver supports historic versions, then the correct version of the
 * target will be returned when resolved.
 * <p>
 * A link can be in one of two states, resolvable and resolved.
 * In the resolvable state, the link contains the identifier and product type of the security.
 * In the resolved state, the link directly embeds the security.
 * <p>
 * To obtain the target of the link, the {@link #resolve(LinkResolver)} method must be used.
 * When the link is in the resolved state, the resolver is not used.
 * When the link is in the resolvable state, the resolver is invoked.
 * <p>
 * The {@link LinkResolver} is a simple interface that is implemented to find the security by identifier.
 * It can be implemented in many ways, including an in-memory map, a database or a call to another service.
 * The resolver throws a {@link LinkResolutionException} if the security cannot be found.
 *
 * @param <P> the type of the product
 * @see StandardLink
 */
@BeanDefinition
public final class SecurityLink<P extends Product>
    implements Link<Security<P>>, Resolvable<SecurityLink<P>>, ImmutableBean, Serializable {

  /**
   * The primary standard identifier of the security.
   * <p>
   * The standard identifier is used to identify the security.
   * It will typically be an identifier in an external data system.
   * <p>
   * This is used when the link is in the resolvable state.
   */
  @PropertyDefinition(get = "field")
  private final StandardId standardId;
  /**
   * The type of the product.
   * <p>
   * This is used when the link is in the resolvable state.
   */
  @PropertyDefinition(get = "field")
  private final Class<P> productType;
  /**
   * The embedded link target.
   * <p>
   * This is used when the link is in the resolved state.
   */
  @PropertyDefinition(get = "field")
  private final Security<P> target;

  //-------------------------------------------------------------------------
  /**
   * Create a resolvable link for the specified identifier and product type.
   * <p>
   * The returned link will be in the resolvable state, containing the identifier and product type.
   * When the {@link #resolve(LinkResolver)} method is called, the resolver will be used
   * to find the security, for example from a database.
   *
   * @param identifier  the primary identifier of the security
   * @param productType  the type of the product
   * @return a new resolvable link
   */
  public static <R extends Product> SecurityLink<R> resolvable(StandardId identifier, Class<R> productType) {
    return new SecurityLink<>(identifier, productType);
  }

  /**
   * Create a link with the security embedded directly.
   * <p>
   * The returned link will be in the resolved state, directly embedding the security.
   * When the {@link #resolve(LinkResolver)} method is called, the embedded target will be
   * returned without needing to use the resolver.
   *
   * @param target  the target security
   * @return a new resolved link
   */
  public static <R extends Product> SecurityLink<R> resolved(Security<R> target) {
    return new SecurityLink<>(target);
  }

  //-------------------------------------------------------------------------
  // constructor for resolvable state
  private SecurityLink(StandardId identifier, Class<P> productType) {
    this.standardId = ArgChecker.notNull(identifier, "identifier");
    this.productType = ArgChecker.notNull(productType, "productType");
    this.target = null;
  }

  // constructor for resolved state
  private SecurityLink(Security<P> target) {
    this.standardId = null;
    this.productType = null;
    this.target = ArgChecker.notNull(target, "target");
  }

  // validate the internal state, allowing the builder to set more than necessary so long as it is valid
  @SuppressWarnings({"rawtypes", "unchecked"})
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.target != null) {
      if (builder.standardId != null) {
        if (builder.standardId.equals(builder.target.getStandardId())) {
          builder.standardId = null;
        } else {
          throw new IllegalArgumentException("Both target and identifier specified but with different identifiers");
        }
      }
      if (builder.productType != null) {
        if (builder.productType.isAssignableFrom(builder.target.getProduct().getClass())) {
          builder.productType = null;
        } else {
          throw new IllegalArgumentException("Both target and productType specified but with incompatible types");
        }
      }
    } else {
      if (builder.standardId == null || builder.productType == null) {
        throw new IllegalArgumentException("Either the target, or the identifer and productType must be specified");
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the link is resolved.
   * <p>
   * A resolved link contains the security directly.
   * A resolvable link only contains the identifier and product type.
   * 
   * @return true if the link is resolved, false if unresolved
   */
  @Override
  public boolean isResolved() {
    return (target != null);
  }

  /**
   * Gets the identifier of the security.
   * <p>
   * Returns the identifier, either directly or from the security.
   * 
   * @return the standard identifier of the security
   */
  @Override
  public StandardId getStandardId() {
    return (isResolved() ? target.getStandardId() : standardId);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public Class<Security<P>> getTargetType() {
    return (Class<Security<P>>) (Class) Security.class;
  }

  @SuppressWarnings("unchecked")
  @Override
  public TypeToken<Security<P>> getTargetTypeToken() {
    return typeToken(isResolved() ? (Class<P>) target.getProduct().getClass() : productType);
  }

  // create a TypeToken for Security<T> where T is set to be productType
  @SuppressWarnings("serial")
  private TypeToken<Security<P>> typeToken(Class<P> productClass) {
    return new TypeToken<Security<P>>() {}.where(new TypeParameter<P>() {}, productClass);
  }

  /**
   * Gets the product type.
   * <p>
   * Returns the product type, either directly or from the security.
   * 
   * @return the product type of the link
   */
  @SuppressWarnings("unchecked")
  public Class<? extends P> getProductType() {
    return (isResolved() ? (Class<? extends P>) target.getProduct().getClass() : productType);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the target, throwing an exception if the link is not resolved.
   *
   * @return the security
   * @throws IllegalStateException if the link is not in the resolved state
   */
  public Security<P> resolvedTarget() {
    if (!isResolved()) {
      throw new IllegalStateException(resolvedTargetMsg());
    }
    return target;
  }

  // extracted to aid inlining performance
  private String resolvedTargetMsg() {
    return "Security must be resolved before it can be used: " + standardId;
  }

  /**
   * Resolves this link using the specified resolver.
   * <p>
   * The resolver is used to find the linked security.
   * In the resolvable state, the resolver is used to find the security, throwing
   * an exception if the security cannot be found.
   * In the resolved state, the directly embedded security is returned without using the resolver.
   * <p>
   * The returned target may contain other unresolved links.
   * Use {@link #resolveLinks(LinkResolver)} to fully resolve the object graph.
   *
   * @param resolver  the resolver to use for the resolution
   * @return the security
   * @throws LinkResolutionException if the security cannot be resolved
   */
  @Override
  public Security<P> resolve(LinkResolver resolver) {
    return (isResolved() ? target : resolver.resolve(standardId, typeToken(productType)));
  }

  /**
   * Resolves this link, and any links that the security contains, using the specified resolver.
   * <p>
   * First, the security is resolved using {@link #resolve(LinkResolver)}.
   * Second, if the security implements {@link Resolvable}, it is resolved as well.
   * The returned security should not contain any unresolved links.
   * <p>
   * An exception is thrown if a link cannot be resolved.
   *
   * @param resolver  the resolver to use for the resolution
   * @return the fully resolved link
   * @throws LinkResolutionException if a link cannot be resolved
   */
  @Override
  public SecurityLink<P> resolveLinks(LinkResolver resolver) {
    Security<P> resolvedTarget = resolve(resolver);
    if (resolvedTarget instanceof Resolvable) {
      @SuppressWarnings("unchecked")
      Resolvable<Security<P>> resolvableTarget = (Resolvable<Security<P>>) resolvedTarget;
      resolvedTarget = resolvableTarget.resolveLinks(resolver);
    }
    return (resolvedTarget == this.target ? this : new SecurityLink<>(resolvedTarget));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SecurityLink}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("rawtypes")
  public static SecurityLink.Meta meta() {
    return SecurityLink.Meta.INSTANCE;
  }

  /**
   * The meta-bean for {@code SecurityLink}.
   * @param <R>  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static <R extends Product> SecurityLink.Meta<R> metaSecurityLink(Class<R> cls) {
    return SecurityLink.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SecurityLink.Meta.INSTANCE);
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
  public static <P extends Product> SecurityLink.Builder<P> builder() {
    return new SecurityLink.Builder<P>();
  }

  private SecurityLink(
      StandardId standardId,
      Class<P> productType,
      Security<P> target) {
    this.standardId = standardId;
    this.productType = productType;
    this.target = target;
  }

  @SuppressWarnings("unchecked")
  @Override
  public SecurityLink.Meta<P> metaBean() {
    return SecurityLink.Meta.INSTANCE;
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
      SecurityLink<?> other = (SecurityLink<?>) obj;
      return JodaBeanUtils.equal(standardId, other.standardId) &&
          JodaBeanUtils.equal(productType, other.productType) &&
          JodaBeanUtils.equal(target, other.target);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(standardId);
    hash = hash * 31 + JodaBeanUtils.hashCode(productType);
    hash = hash * 31 + JodaBeanUtils.hashCode(target);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SecurityLink{");
    buf.append("standardId").append('=').append(standardId).append(',').append(' ');
    buf.append("productType").append('=').append(productType).append(',').append(' ');
    buf.append("target").append('=').append(JodaBeanUtils.toString(target));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SecurityLink}.
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
        this, "standardId", SecurityLink.class, StandardId.class);
    /**
     * The meta-property for the {@code productType} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Class<P>> productType = DirectMetaProperty.ofImmutable(
        this, "productType", SecurityLink.class, (Class) Class.class);
    /**
     * The meta-property for the {@code target} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Security<P>> target = DirectMetaProperty.ofImmutable(
        this, "target", SecurityLink.class, (Class) Security.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "standardId",
        "productType",
        "target");

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
        case -1491615543:  // productType
          return productType;
        case -880905839:  // target
          return target;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public SecurityLink.Builder<P> builder() {
      return new SecurityLink.Builder<P>();
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public Class<? extends SecurityLink<P>> beanType() {
      return (Class) SecurityLink.class;
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
     * The meta-property for the {@code productType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Class<P>> productType() {
      return productType;
    }

    /**
     * The meta-property for the {@code target} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Security<P>> target() {
      return target;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1284477768:  // standardId
          return ((SecurityLink<?>) bean).standardId;
        case -1491615543:  // productType
          return ((SecurityLink<?>) bean).productType;
        case -880905839:  // target
          return ((SecurityLink<?>) bean).target;
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
   * The bean-builder for {@code SecurityLink}.
   * @param <P>  the type
   */
  public static final class Builder<P extends Product> extends DirectFieldsBeanBuilder<SecurityLink<P>> {

    private StandardId standardId;
    private Class<P> productType;
    private Security<P> target;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(SecurityLink<P> beanToCopy) {
      this.standardId = beanToCopy.standardId;
      this.productType = beanToCopy.productType;
      this.target = beanToCopy.target;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1284477768:  // standardId
          return standardId;
        case -1491615543:  // productType
          return productType;
        case -880905839:  // target
          return target;
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
        case -1491615543:  // productType
          this.productType = (Class<P>) newValue;
          break;
        case -880905839:  // target
          this.target = (Security<P>) newValue;
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
    public SecurityLink<P> build() {
      preBuild(this);
      return new SecurityLink<P>(
          standardId,
          productType,
          target);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the primary standard identifier of the security.
     * <p>
     * The standard identifier is used to identify the security.
     * It will typically be an identifier in an external data system.
     * <p>
     * This is used when the link is in the resolvable state.
     * @param standardId  the new value
     * @return this, for chaining, not null
     */
    public Builder<P> standardId(StandardId standardId) {
      this.standardId = standardId;
      return this;
    }

    /**
     * Sets the type of the product.
     * <p>
     * This is used when the link is in the resolvable state.
     * @param productType  the new value
     * @return this, for chaining, not null
     */
    public Builder<P> productType(Class<P> productType) {
      this.productType = productType;
      return this;
    }

    /**
     * Sets the embedded link target.
     * <p>
     * This is used when the link is in the resolved state.
     * @param target  the new value
     * @return this, for chaining, not null
     */
    public Builder<P> target(Security<P> target) {
      this.target = target;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("SecurityLink.Builder{");
      buf.append("standardId").append('=').append(JodaBeanUtils.toString(standardId)).append(',').append(' ');
      buf.append("productType").append('=').append(JodaBeanUtils.toString(productType)).append(',').append(' ');
      buf.append("target").append('=').append(JodaBeanUtils.toString(target));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
