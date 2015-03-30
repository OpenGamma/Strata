/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.google.common.collect.ImmutableSortedSet;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An immutable bundle of external identifiers.
 * <p>
 * A bundle allows multiple {@link ExternalId external identifiers} to be grouped
 * together when they all refer to the same conceptual object.
 * For example, a Reuters RIC and Bloomberg Ticker might both refer to the same equity.
 * <p>
 * The bundle holds a <i>set</i> of external identifiers, not a <i>map</i> from scheme to value.
 * This permits multiple values within the same scheme to refer to the same conceptual object.
 * For example, a renamed ticker could be grouped as both the old and new value.
 * In general however, each external identifier in a bundle will be in a different scheme.
 * <p>
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private")
public final class ExternalIdBundle
    implements ImmutableBean, Iterable<ExternalId>, Serializable, ExternalBundleIdentifiable {

  /**
   * Singleton empty bundle.
   */
  public static final ExternalIdBundle EMPTY = new ExternalIdBundle(ImmutableSortedSet.of());

  /**
   * The set of identifiers in the bundle.
   * <p>
   * The identifiers are sorted in the natural order of {@link ExternalId} to provide
   * greater consistency in applications. The sort order is not suitable for a GUI.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSortedSet<ExternalId> externalIds;
  /**
   * The cached hash code.
   */
  private transient int hashCode;  // safe via racy single check idiom

  //-------------------------------------------------------------------------
  /**
   * Obtains an {@code ExternalIdBundle} from a single scheme and value.
   * This is most useful for testing, as a bundle normally contains more than one identifier.
   * 
   * @param scheme  the scheme of the single external identifier
   * @param value  the value of the single external identifier, not empty
   * @return the bundle
   */
  public static ExternalIdBundle of(ExternalScheme scheme, String value) {
    return of(ExternalId.of(scheme, value));
  }

  /**
   * Obtains an {@code ExternalIdBundle} from a single scheme and value.
   * This is most useful for testing, as a bundle normally contains more than one identifier.
   * 
   * @param scheme  the scheme of the single external identifier, not empty
   * @param value  the value of the single external identifier, not empty
   * @return the bundle
   */
  public static ExternalIdBundle of(String scheme, String value) {
    return of(ExternalId.of(scheme, value));
  }

  /**
   * Obtains an {@code ExternalIdBundle} from an identifier.
   * 
   * @param externalId  the external identifier to wrap in a bundle
   * @return the bundle
   */
  public static ExternalIdBundle of(ExternalId externalId) {
    ArgChecker.notNull(externalId, "externalId");
    return new ExternalIdBundle(ImmutableSortedSet.of(externalId));
  }

  /**
   * Obtains an {@code ExternalIdBundle} from an array of identifiers.
   * 
   * @param externalIds  the array of external identifiers, no nulls
   * @return the bundle
   */
  public static ExternalIdBundle of(ExternalId... externalIds) {
    ArgChecker.noNulls(externalIds, "externalIds");
    return new ExternalIdBundle(ImmutableSortedSet.copyOf(externalIds));
  }

  /**
   * Obtains an {@code ExternalIdBundle} from a collection of identifiers.
   * 
   * @param externalIds  the collection of external identifiers, no nulls
   * @return the bundle
   */
  public static ExternalIdBundle of(Iterable<ExternalId> externalIds) {
    ArgChecker.noNulls(externalIds, "externalIds");
    return create(externalIds);
  }

  /**
   * Parses a list of strings to an {@code ExternalIdBundle}.
   * <p>
   * This uses {@link ExternalId#parse(String)} to parse each string in the input collection.
   * 
   * @param strs  the external identifiers to parse
   * @return the bundle
   * @throws IllegalArgumentException if any identifier cannot be parsed
   */
  public static ExternalIdBundle parse(Iterable<String> strs) {
    ArgChecker.noNulls(strs, "strs");
    Set<ExternalId> ids = StreamSupport.stream(strs.spliterator(), false)
        .map(ExternalId::parse)
        .collect(Collectors.toSet());
    return create(ids);
  }

  /**
   * Obtains an {@code ExternalIdBundle} from a collection of identifiers.
   * 
   * @param externalIds  the collection of external identifiers, validated
   * @return the bundle
   */
  private static ExternalIdBundle create(Iterable<ExternalId> externalIds) {
    return new ExternalIdBundle(ImmutableSortedSet.copyOf(externalIds));
  }

  /**
   * Creates a bundle from a set of identifiers.
   * 
   * @param externalIds  the set of identifiers
   */
  private ExternalIdBundle(ImmutableSortedSet<ExternalId> externalIds) {
    this.externalIds = externalIds;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the external identifier for the specified scheme.
   * <p>
   * This returns the first identifier in the internal set that matches.
   * The set is not sorted, so this method is not consistent.
   * 
   * @param scheme  the scheme to query
   * @return the identifier, null if not found
   */
  public ExternalId getExternalId(ExternalScheme scheme) {
    ArgChecker.notNull(scheme, "scheme");
    return getExternalIds(scheme).stream().findFirst().orElse(null);
  }

  /**
   * Gets all the identifiers for a scheme.
   * 
   * @param scheme  the scheme
   * @return all identifiers for the scheme
   */
  public Set<ExternalId> getExternalIds(ExternalScheme scheme) {
    ArgChecker.notNull(scheme, "scheme");
    return externalIds.stream()
        .filter(eid -> eid.isScheme(scheme))
        .collect(Collectors.toSet());
  }

  /**
   * Gets the identifier value for the specified scheme.
   * <p>
   * This returns the first identifier in the internal set that matches.
   * The set is not sorted, so this method is not consistent.
   * 
   * @param scheme  the scheme to query
   * @return the identifier value, null if not found
   */
  public String getValue(ExternalScheme scheme) {
    ArgChecker.notNull(scheme, "scheme");
    return getValues(scheme).stream().findFirst().orElse(null);
  }

  /**
   * Gets all identifier values for a scheme.
   * 
   * @param scheme  the scheme
   * @return all values for the scheme
   */
  public Set<String> getValues(ExternalScheme scheme) {
    ArgChecker.notNull(scheme, "scheme");
    return externalIds.stream()
        .filter(eid -> eid.isScheme(scheme))
        .map(ExternalId::getValue)
        .collect(Collectors.toSet());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new bundle with the specified identifier added.
   * This instance is immutable and unaffected by this method call.
   * 
   * @param externalId  the identifier to add to the returned bundle
   * @return the new bundle
   */
  public ExternalIdBundle withExternalId(ExternalId externalId) {
    ArgChecker.notNull(externalId, "externalId");
    return withExternalIds(ImmutableSortedSet.of(externalId));
  }

  /**
   * Returns a new bundle with the specified identifier added.
   * This instance is immutable and unaffected by this method call.
   * 
   * @param externalIds  the identifiers to add to the returned bundle
   * @return the new bundle
   */
  public ExternalIdBundle withExternalIds(Iterable<ExternalId> externalIds) {
    ArgChecker.notNull(externalIds, "externalIds");
    Set<ExternalId> toAdd = ImmutableSortedSet.copyOf(externalIds);
    Set<ExternalId> ids = new HashSet<>(this.externalIds);
    if (ids.addAll(toAdd) == false) {
      return this;
    }
    return create(ids);
  }

  /**
   * Returns a new bundle with the specified identifier removed.
   * This instance is immutable and unaffected by this method call.
   * 
   * @param externalId  the identifier to remove from the returned bundle
   * @return the new bundle
   */
  public ExternalIdBundle withoutExternalId(ExternalId externalId) {
    ArgChecker.notNull(externalId, "externalId");
    Set<ExternalId> ids = new HashSet<>(this.externalIds);
    if (ids.remove(externalId) == false) {
      return this;
    }
    return create(ids);
  }

  /**
   * Returns a new bundle with all references to the specified scheme removed.
   * This instance is immutable and unaffected by this method call.
   * 
   * @param scheme  the scheme to remove from the returned bundle
   * @return the new bundle
   */
  public ExternalIdBundle withoutScheme(ExternalScheme scheme) {
    ArgChecker.notNull(scheme, "scheme");
    Set<ExternalId> ids = externalIds.stream()
        .filter(id -> id.isNotScheme(scheme))
        .collect(Collectors.toSet());
    return create(ids);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of identifiers in the bundle.
   * 
   * @return the bundle size, zero or greater
   */
  public int size() {
    return externalIds.size();
  }

  /**
   * Returns true if this bundle contains no identifiers.
   * 
   * @return true if this bundle contains no identifiers, false otherwise
   */
  public boolean isEmpty() {
    return externalIds.isEmpty();
  }

  /**
   * Returns an iterator over the identifiers in the bundle.
   * 
   * @return the identifiers in the bundle
   */
  @Override
  public Iterator<ExternalId> iterator() {
    return externalIds.iterator();
  }

  /**
   * Checks if this bundle contains all the keys from the specified bundle.
   * 
   * @param bundle  the bundle to search for, empty returns true
   * @return true if this bundle contains all the keys from the specified bundle
   */
  public boolean containsAll(ExternalIdBundle bundle) {
    ArgChecker.notNull(bundle, "bundle");
    return externalIds.containsAll(bundle.externalIds);
  }

  /**
   * Checks if this bundle contains any key from the specified bundle.
   * 
   * @param bundle  the bundle to search for, empty returns false
   * @return true if this bundle contains any key from the specified bundle
   */
  public boolean containsAny(ExternalIdBundle bundle) {
    ArgChecker.notNull(bundle, "bundle");
    return bundle.externalIds.stream().anyMatch(id -> externalIds.contains(id));
  }

  /**
   * Checks if this bundle contains the specified key.
   * 
   * @param externalId  the identifier to search for, null returns false
   * @return true if this bundle contains any key from the specified bundle
   */
  public boolean contains(ExternalId externalId) {
    return externalId != null && externalIds.contains(externalId);
  }

  /**
   * Converts this to an external identifier bundle.
   * <p>
   * This method trivially returns {@code this}
   * 
   * @return {@code this}
   */
  @Override
  public ExternalIdBundle getExternalIdBundle() {
    return this;
  }

  //-------------------------------------------------------------------
  /**
   * Checks if this bundle equals another.
   * 
   * @param obj  the other object
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ExternalIdBundle other = (ExternalIdBundle) obj;
    return externalIds.equals(other.externalIds);
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    // racy single check idiom allows non-volatile variable
    // requires only one read and one write of non-volatile
    int hashCode = this.hashCode;
    if (hashCode == 0) {
      hashCode = 31 + externalIds.hashCode();
      this.hashCode = hashCode;
    }
    return hashCode;
  }

  /**
   * Returns a string representation of the bundle.
   * 
   * @return a string representation of the bundle
   */
  @Override
  public String toString() {
    return externalIds.stream()
        .map(Object::toString)
        .collect(Collectors.joining(", ", "Bundle[", "]"));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExternalIdBundle}.
   * @return the meta-bean, not null
   */
  public static ExternalIdBundle.Meta meta() {
    return ExternalIdBundle.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExternalIdBundle.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ExternalIdBundle(
      SortedSet<ExternalId> externalIds) {
    JodaBeanUtils.notNull(externalIds, "externalIds");
    this.externalIds = ImmutableSortedSet.copyOfSorted(externalIds);
  }

  @Override
  public ExternalIdBundle.Meta metaBean() {
    return ExternalIdBundle.Meta.INSTANCE;
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
   * Gets the set of identifiers in the bundle.
   * <p>
   * The identifiers are sorted in the natural order of {@link ExternalId} to provide
   * greater consistency in applications. The sort order is not suitable for a GUI.
   * @return the value of the property, not null
   */
  public ImmutableSortedSet<ExternalId> getExternalIds() {
    return externalIds;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExternalIdBundle}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code externalIds} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSortedSet<ExternalId>> externalIds = DirectMetaProperty.ofImmutable(
        this, "externalIds", ExternalIdBundle.class, (Class) ImmutableSortedSet.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "externalIds");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1153096979:  // externalIds
          return externalIds;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ExternalIdBundle> builder() {
      return new ExternalIdBundle.Builder();
    }

    @Override
    public Class<? extends ExternalIdBundle> beanType() {
      return ExternalIdBundle.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code externalIds} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSortedSet<ExternalId>> externalIds() {
      return externalIds;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1153096979:  // externalIds
          return ((ExternalIdBundle) bean).getExternalIds();
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
   * The bean-builder for {@code ExternalIdBundle}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ExternalIdBundle> {

    private SortedSet<ExternalId> externalIds = ImmutableSortedSet.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1153096979:  // externalIds
          return externalIds;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1153096979:  // externalIds
          this.externalIds = (SortedSet<ExternalId>) newValue;
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
    public ExternalIdBundle build() {
      return new ExternalIdBundle(
          externalIds);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("ExternalIdBundle.Builder{");
      buf.append("externalIds").append('=').append(JodaBeanUtils.toString(externalIds));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
