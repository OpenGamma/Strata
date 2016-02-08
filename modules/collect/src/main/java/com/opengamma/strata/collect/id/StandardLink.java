/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.id;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.opengamma.strata.collect.ArgChecker;

/**
 * Standard implementation of a link to a target object using an identifier.
 * <p>
 * A link provides loose coupling between different parts of the object model.
 * For example, an equity trade can hold a link to the underlying equity.
 * The two objects can be updated independently of each other.
 * If the link resolver supports historic versions, then the correct version of the
 * target will be returned when resolved.
 * The target object should be immutable to ensure the safety of the link.
 * <p>
 * A link can be in one of two states, resolvable and resolved.
 * <p>
 * In the resolvable state, the link contains the identifier and type of the target object.
 * The target can only be obtained using a {@link LinkResolver}.
 * <p>
 * In the resolved state, the link directly embeds the target object.
 * <p>
 * Links are expected to be resolvable. It is reasonable to expect that when
 * {@link #resolve(LinkResolver)} is called, the target of the link is available.
 * For this reason, if the target is not found, a {@link LinkResolutionException} will be thrown.
 * <p>
 * This is the standard implementation of {@link Link}.
 * It is suitable for all link targets except those that have generic parameters.
 *
 * @param <T> the type of the target
 */
@BeanDefinition(builderScope = "private")
public final class StandardLink<T extends IdentifiableBean>
    implements Link<T>, LinkResolvable<StandardLink<T>>, StandardIdentifiable, ImmutableBean, Serializable {

  /**
   * The primary standard identifier of the target.
   * <p>
   * The standard identifier is used to identify the target.
   * It will typically be an identifier in an external data system.
   * <p>
   * This is used when the link is in the resolvable state.
   */
  @PropertyDefinition(get = "field")
  private final StandardId standardId;
  /**
   * The type of the link target.
   * <p>
   * This is used when the link is in the resolvable state.
   */
  @PropertyDefinition(get = "field")
  private final Class<T> targetType;
  /**
   * The embedded link target.
   * <p>
   * This is used when the link is in the resolved state.
   */
  @PropertyDefinition(get = "field")
  private final T target;

  //-------------------------------------------------------------------------
  /**
   * Create a resolvable link for the specified identifier and type.
   * <p>
   * The returned link will be in the resolvable state, containing the identifier and type.
   * When the {@link #resolve(LinkResolver)} method is called, the resolver will be used
   * to find the target, for example from a database.
   * <p>
   * The type is expressed as a standard {@link Class} object.
   *
   * @param <R>  the type of the link
   * @param identifier  the primary identifier of the target
   * @param targetType  the type of the target
   * @return a new resolvable link
   */
  public static <R extends IdentifiableBean> StandardLink<R> resolvable(StandardId identifier, Class<R> targetType) {
    return new StandardLink<>(identifier, targetType);
  }

  /**
   * Create a link with the link target embedded directly.
   * <p>
   * The returned link will be in the resolved state, directly embedding the target object.
   * When the {@link #resolve(LinkResolver)} method is called, the embedded target will be
   * returned without needing to use the resolver.
   *
   * @param <R>  the type of the link
   * @param target  the link target
   * @return a new resolved link
   */
  public static <R extends IdentifiableBean> StandardLink<R> resolved(R target) {
    return new StandardLink<>(target);
  }

  //-------------------------------------------------------------------------
  // constructor for resolvable state
  private StandardLink(StandardId identifier, Class<T> targetType) {
    this.standardId = ArgChecker.notNull(identifier, "identifier");
    this.targetType = ArgChecker.notNull(targetType, "targetType");
    this.target = null;
  }

  // constructor for resolved state
  private StandardLink(T target) {
    this.standardId = null;
    this.targetType = null;
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
      if (builder.targetType != null) {
        if (builder.targetType.isAssignableFrom(builder.target.getClass())) {
          builder.targetType = null;
        } else {
          throw new IllegalArgumentException("Both target and targetType specified but with incompatible types");
        }
      }
    } else {
      if (builder.standardId == null || builder.targetType == null) {
        throw new IllegalArgumentException("Either the target, or the identifer and target type must be specified");
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean isResolved() {
    return (target != null);
  }

  @Override
  public StandardId getStandardId() {
    return (isResolved() ? target.getStandardId() : standardId);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<T> getTargetType() {
    return (isResolved() ? (Class<T>) target.getClass() : targetType);
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves this link using the specified resolver.
   * <p>
   * The resolver is used to find the linked target.
   * In the resolvable state, the resolver is used to find the target, throwing
   * an exception of the target cannot be found.
   * In the resolved state, the directly embedded target is returned,
   * without using the resolver.
   * <p>
   * The returned target may contain other unresolved links.
   *
   * @param resolver  the resolver to use for the resolution
   * @return the target
   * @throws LinkResolutionException if the target cannot be resolved
   */
  @Override
  @SuppressWarnings("unchecked")
  public T resolve(LinkResolver resolver) {
    return (isResolved() ? target : resolver.resolve(standardId, targetType));
  }

  /**
   * Resolves this link, and any links that the target contains, using the specified resolver.
   * <p>
   * First, the target is resolved using {@link #resolve(LinkResolver)}.
   * Second, if the target implements {@link LinkResolvable}, it is resolved as well.
   * The result is wrapped using {@link StandardLink#resolved(IdentifiableBean)}.
   * The returned target should not contain any unresolved links.
   *
   * @param resolver  the resolver to use for the resolution
   * @return the fully resolved link
   * @throws LinkResolutionException if a link cannot be resolved
   */
  @Override
  public StandardLink<T> resolveLinks(LinkResolver resolver) {
    T resolvedTarget = resolve(resolver);
    if (resolvedTarget instanceof LinkResolvable) {
      @SuppressWarnings("unchecked")
      LinkResolvable<T> resolvableTarget = (LinkResolvable<T>) resolvedTarget;
      resolvedTarget = resolvableTarget.resolveLinks(resolver);
    }
    return (resolvedTarget == this.target ? this : new StandardLink<>(resolvedTarget));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code StandardLink}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("rawtypes")
  public static StandardLink.Meta meta() {
    return StandardLink.Meta.INSTANCE;
  }

  /**
   * The meta-bean for {@code StandardLink}.
   * @param <R>  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static <R extends IdentifiableBean> StandardLink.Meta<R> metaStandardLink(Class<R> cls) {
    return StandardLink.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(StandardLink.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private StandardLink(
      StandardId standardId,
      Class<T> targetType,
      T target) {
    this.standardId = standardId;
    this.targetType = targetType;
    this.target = target;
  }

  @SuppressWarnings("unchecked")
  @Override
  public StandardLink.Meta<T> metaBean() {
    return StandardLink.Meta.INSTANCE;
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
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      StandardLink<?> other = (StandardLink<?>) obj;
      return JodaBeanUtils.equal(standardId, other.standardId) &&
          JodaBeanUtils.equal(targetType, other.targetType) &&
          JodaBeanUtils.equal(target, other.target);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(standardId);
    hash = hash * 31 + JodaBeanUtils.hashCode(targetType);
    hash = hash * 31 + JodaBeanUtils.hashCode(target);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("StandardLink{");
    buf.append("standardId").append('=').append(standardId).append(',').append(' ');
    buf.append("targetType").append('=').append(targetType).append(',').append(' ');
    buf.append("target").append('=').append(JodaBeanUtils.toString(target));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code StandardLink}.
   * @param <T>  the type
   */
  public static final class Meta<T extends IdentifiableBean> extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    @SuppressWarnings("rawtypes")
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code standardId} property.
     */
    private final MetaProperty<StandardId> standardId = DirectMetaProperty.ofImmutable(
        this, "standardId", StandardLink.class, StandardId.class);
    /**
     * The meta-property for the {@code targetType} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Class<T>> targetType = DirectMetaProperty.ofImmutable(
        this, "targetType", StandardLink.class, (Class) Class.class);
    /**
     * The meta-property for the {@code target} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<T> target = (DirectMetaProperty) DirectMetaProperty.ofImmutable(
        this, "target", StandardLink.class, Object.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "standardId",
        "targetType",
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
        case 486622315:  // targetType
          return targetType;
        case -880905839:  // target
          return target;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends StandardLink<T>> builder() {
      return new StandardLink.Builder<T>();
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public Class<? extends StandardLink<T>> beanType() {
      return (Class) StandardLink.class;
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
     * The meta-property for the {@code targetType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Class<T>> targetType() {
      return targetType;
    }

    /**
     * The meta-property for the {@code target} property.
     * @return the meta-property, not null
     */
    public MetaProperty<T> target() {
      return target;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1284477768:  // standardId
          return ((StandardLink<?>) bean).standardId;
        case 486622315:  // targetType
          return ((StandardLink<?>) bean).targetType;
        case -880905839:  // target
          return ((StandardLink<?>) bean).target;
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
   * The bean-builder for {@code StandardLink}.
   * @param <T>  the type
   */
  private static final class Builder<T extends IdentifiableBean> extends DirectFieldsBeanBuilder<StandardLink<T>> {

    private StandardId standardId;
    private Class<T> targetType;
    private T target;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1284477768:  // standardId
          return standardId;
        case 486622315:  // targetType
          return targetType;
        case -880905839:  // target
          return target;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<T> set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1284477768:  // standardId
          this.standardId = (StandardId) newValue;
          break;
        case 486622315:  // targetType
          this.targetType = (Class<T>) newValue;
          break;
        case -880905839:  // target
          this.target = (T) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder<T> set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder<T> setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder<T> setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder<T> setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public StandardLink<T> build() {
      preBuild(this);
      return new StandardLink<T>(
          standardId,
          targetType,
          target);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("StandardLink.Builder{");
      buf.append("standardId").append('=').append(JodaBeanUtils.toString(standardId)).append(',').append(' ');
      buf.append("targetType").append('=').append(JodaBeanUtils.toString(targetType)).append(',').append(' ');
      buf.append("target").append('=').append(JodaBeanUtils.toString(target));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
