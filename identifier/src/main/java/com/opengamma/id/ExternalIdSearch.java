/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
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

import com.google.common.collect.ImmutableSet;

/**
 * A search request to match external identifiers.
 * <p>
 * The search combines a set of external identifiers and a matching rule.
 * <p>
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private")
public final class ExternalIdSearch
    implements ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The set of identifiers.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSet<ExternalId> externalIds;
  /**
   * The search type, default 'ANY'.
   */
  @PropertyDefinition(validate = "notNull")
  private final ExternalIdSearchType searchType;

  //-------------------------------------------------------------------------
  /**
   * Creates an empty search, with the search type set to any.
   * <p>
   * This uses {@link ExternalIdSearchType#ANY}.
   * <p>
   * This search will not match anything.
   * 
   * @return the external identifier search
   */
  public static ExternalIdSearch of() {
    return new ExternalIdSearch(ExternalIdSearchType.ANY, ImmutableSet.<ExternalId>of());
  }

  /**
   * Creates a search matching any of a collection of identifiers.
   * <p>
   * This uses {@link ExternalIdSearchType#ANY}.
   * 
   * @param externalIds  the identifiers
   * @return the external identifier search
   */
  public static ExternalIdSearch of(ExternalId... externalIds) {
    return ExternalIdSearch.of(ExternalIdSearchType.ANY, externalIds);
  }

  /**
   * Creates a search of the specified type matching a collection of identifiers.
   * 
   * @param searchType  the search type
   * @param externalIds  the identifiers
   * @return the external identifier search
   */
  public static ExternalIdSearch of(ExternalIdSearchType searchType, ExternalId... externalIds) {
    ImmutableSet<ExternalId> ids = ImmutableSet.copyOf(ArgChecker.noNulls(externalIds, "externalIds"));
    return new ExternalIdSearch(searchType, ids);
  }

  /**
   * Creates a search matching any of a collection of identifiers.
   * <p>
   * This uses {@link ExternalIdSearchType#ANY}.
   * 
   * @param externalIds  the collection of identifiers
   * @return the external identifier search
   */
  public static ExternalIdSearch of(Iterable<ExternalId> externalIds) {
    return ExternalIdSearch.of(ExternalIdSearchType.ANY, externalIds);
  }

  /**
   * Creates a search of the specified type matching a collection of identifiers.
   * 
   * @param searchType  the search type
   * @param externalIds  the collection of identifiers
   * @return the external identifier search
   */
  public static ExternalIdSearch of(ExternalIdSearchType searchType, Iterable<ExternalId> externalIds) {
    ImmutableSet<ExternalId> ids = ImmutableSet.copyOf(ArgChecker.noNulls(externalIds, "externalIds"));
    return new ExternalIdSearch(searchType, ids);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a search matching any of a collection of identifiers.
   * 
   * @param searchType  the search type
   * @param externalIds  the collection of identifiers
   */
  private ExternalIdSearch(ExternalIdSearchType searchType, ImmutableSet<ExternalId> externalIds) {
    this.externalIds = ArgChecker.notNull(externalIds, "externalIds");
    this.searchType = ArgChecker.notNull(searchType, "searchType");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this search with an additional identifier to search for.
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param externalIdToAdd  the identifier to add
   * @return the external identifier search with the specified identifier
   */
  public ExternalIdSearch withExternalIdAdded(ExternalId externalIdToAdd) {
    ArgChecker.notNull(externalIdToAdd, "externalId");
    ImmutableSet<ExternalId> ids = ImmutableSet.<ExternalId>builder()
        .addAll(externalIds)
        .add(externalIdToAdd)
        .build();
    return new ExternalIdSearch(searchType, ids);
  }

  /**
   * Returns a copy of this search with additional identifiers to search for.
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param externalIdsToAdd  the identifiers to add
   * @return the external identifier search with the specified identifier
   */
  public ExternalIdSearch withExternalIdsAdded(ExternalId... externalIdsToAdd) {
    ArgChecker.noNulls(externalIdsToAdd, "externalIds");
    ImmutableSet<ExternalId> ids = ImmutableSet.<ExternalId>builder()
        .addAll(this.externalIds)
        .addAll(Arrays.asList(externalIdsToAdd))
        .build();
    return new ExternalIdSearch(searchType, ids);
  }

  /**
   * Returns a copy of this search with additional identifiers to search for.
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param externalIdsToAdd  the identifiers to add
   * @return the external identifier search with the specified identifier
   */
  public ExternalIdSearch withExternalIdsAdded(Iterable<ExternalId> externalIdsToAdd) {
    ArgChecker.noNulls(externalIdsToAdd, "externalIds");
    ImmutableSet<ExternalId> ids = ImmutableSet.<ExternalId>builder()
        .addAll(this.externalIds)
        .addAll(externalIdsToAdd)
        .build();
    return new ExternalIdSearch(searchType, ids);
  }

  /**
   * Returns a copy of this search with the identifier removed.
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param externalId  the identifier to remove
   * @return the external identifier search with the specified identifier removed
   */
  public ExternalIdSearch withExternalIdRemoved(ExternalId externalId) {
    ArgChecker.notNull(externalId, "externalId");
    if (contains(externalId) == false) {
      return this;
    }
    Set<ExternalId> ids = new HashSet<>(this.externalIds);
    ids.remove(externalId);
    return new ExternalIdSearch(searchType, ImmutableSet.copyOf(ids));
  }

  /**
   * Returns a copy of this search with the specified search type.
   * 
   * @param searchType  the new search type
   * @return a copy of this search with the new search type
   */
  public ExternalIdSearch withSearchType(ExternalIdSearchType searchType) {
    ArgChecker.notNull(searchType, "searchType");
    if (searchType == this.searchType) {
      return this;
    }
    return new ExternalIdSearch(searchType, externalIds);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of identifiers.
   * 
   * @return the bundle size, zero or greater
   */
  public int size() {
    return externalIds.size();
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this search matches the identifier.
   * <p>
   * An EXACT match returns true if there is one stored identifier and it is equal to the passed in identifier.<br />
   * An ALL match returns true if this is empty or has a single identifier equal to the input.<br />
   * An ANY match returns true if the passed in identifier matches any of the stored identifiers.<br />
   * A NONE match returns true if the passed in identifier does not match any stored identifier.<br />
   * 
   * @param otherId  the identifier to search for
   * @return true if this search contains all of the keys specified
   */
  public boolean matches(ExternalId otherId) {
    ArgChecker.notNull(otherId, "otherId");
    switch (searchType) {
      case EXACT:
        return ImmutableSet.of(otherId).equals(externalIds);
      case ALL:
        return ImmutableSet.of(otherId).containsAll(externalIds);
      case ANY:
        return contains(otherId);
      case NONE:
        return contains(otherId) == false;
      default:
        return false;
    }
  }

  /**
   * Checks if this search matches the identifiers.
   * <p>
   * An EXACT match returns true if the passed in identifiers are the same set as the stored identifiers.<br />
   * An ALL match returns true if the passed in identifiers match are a superset or equal the stored identifiers.<br />
   * An ANY match returns true if the passed in identifiers match any of the stored identifiers.<br />
   * A NONE match returns true if none of the passed in identifiers match a stored identifier.<br />
   * 
   * @param otherIds  the identifiers to search for, empty returns true
   * @return true if this search contains all of the keys specified
   */
  public boolean matches(Iterable<ExternalId> otherIds) {
    ArgChecker.notNull(otherIds, "otherId");
    switch (searchType) {
      case EXACT:
        return ImmutableSet.copyOf(otherIds).equals(externalIds);
      case ALL:
        return ImmutableSet.copyOf(otherIds).containsAll(externalIds);
      case ANY:
        return containsAny(otherIds);
      case NONE:
        return containsAny(otherIds) == false;
      default:
        return false;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this search contains all the keys from the specified identifiers.
   * <p>
   * This is the opposite check to the ALL search type in {@code matches()}.
   * This method checks if this is a superset or equal to the passed in identifiers.
   * The ALL check checks the superset the other way around.
   * 
   * @param otherIds  the identifiers to search for, empty returns true
   * @return true if this search contains all of the keys specified
   */
  public boolean containsAll(Iterable<ExternalId> otherIds) {
    ArgChecker.notNull(otherIds, "otherId");
    return StreamSupport.stream(otherIds.spliterator(), false).allMatch(id -> externalIds.contains(id));
  }

  /**
   * Checks if this search contains any key from the specified identifiers.
   * 
   * @param otherIds  the identifiers to search for, empty returns false
   * @return true if this search contains any of the keys specified
   */
  public boolean containsAny(Iterable<ExternalId> otherIds) {
    ArgChecker.notNull(otherIds, "otherId");
    return StreamSupport.stream(otherIds.spliterator(), false).anyMatch(id -> externalIds.contains(id));
  }

  /**
   * Checks if this search contains the specified key.
   * 
   * @param otherId  the key to search for, null returns false
   * @return true if this search contains the specified key
   */
  public boolean contains(ExternalId otherId) {
    return otherId != null && externalIds.contains(otherId);
  }

  //-------------------------------------------------------------------
  /**
   * Checks if this search can match anything.
   * 
   * @return true if the search can match anything
   */
  public boolean canMatch() {
    return searchType == ExternalIdSearchType.NONE || size() > 0;
  }

  /**
   * Checks if this search always matches.
   * 
   * @return true if the search always matches
   */
  public boolean alwaysMatches() {
    return (searchType == ExternalIdSearchType.NONE && size() == 0);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExternalIdSearch}.
   * @return the meta-bean, not null
   */
  public static ExternalIdSearch.Meta meta() {
    return ExternalIdSearch.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExternalIdSearch.Meta.INSTANCE);
  }

  private ExternalIdSearch(
      Set<ExternalId> externalIds,
      ExternalIdSearchType searchType) {
    JodaBeanUtils.notNull(externalIds, "externalIds");
    JodaBeanUtils.notNull(searchType, "searchType");
    this.externalIds = ImmutableSet.copyOf(externalIds);
    this.searchType = searchType;
  }

  @Override
  public ExternalIdSearch.Meta metaBean() {
    return ExternalIdSearch.Meta.INSTANCE;
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
   * Gets the set of identifiers.
   * @return the value of the property, not null
   */
  public ImmutableSet<ExternalId> getExternalIds() {
    return externalIds;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the search type, default 'ANY'.
   * @return the value of the property, not null
   */
  public ExternalIdSearchType getSearchType() {
    return searchType;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ExternalIdSearch other = (ExternalIdSearch) obj;
      return JodaBeanUtils.equal(getExternalIds(), other.getExternalIds()) &&
          JodaBeanUtils.equal(getSearchType(), other.getSearchType());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getExternalIds());
    hash += hash * 31 + JodaBeanUtils.hashCode(getSearchType());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ExternalIdSearch{");
    buf.append("externalIds").append('=').append(getExternalIds()).append(',').append(' ');
    buf.append("searchType").append('=').append(JodaBeanUtils.toString(getSearchType()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExternalIdSearch}.
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
    private final MetaProperty<ImmutableSet<ExternalId>> externalIds = DirectMetaProperty.ofImmutable(
        this, "externalIds", ExternalIdSearch.class, (Class) ImmutableSet.class);
    /**
     * The meta-property for the {@code searchType} property.
     */
    private final MetaProperty<ExternalIdSearchType> searchType = DirectMetaProperty.ofImmutable(
        this, "searchType", ExternalIdSearch.class, ExternalIdSearchType.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "externalIds",
        "searchType");

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
        case -710454014:  // searchType
          return searchType;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ExternalIdSearch> builder() {
      return new ExternalIdSearch.Builder();
    }

    @Override
    public Class<? extends ExternalIdSearch> beanType() {
      return ExternalIdSearch.class;
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
    public MetaProperty<ImmutableSet<ExternalId>> externalIds() {
      return externalIds;
    }

    /**
     * The meta-property for the {@code searchType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ExternalIdSearchType> searchType() {
      return searchType;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1153096979:  // externalIds
          return ((ExternalIdSearch) bean).getExternalIds();
        case -710454014:  // searchType
          return ((ExternalIdSearch) bean).getSearchType();
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
   * The bean-builder for {@code ExternalIdSearch}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ExternalIdSearch> {

    private Set<ExternalId> externalIds = new HashSet<ExternalId>();
    private ExternalIdSearchType searchType;

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
        case -710454014:  // searchType
          return searchType;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1153096979:  // externalIds
          this.externalIds = (Set<ExternalId>) newValue;
          break;
        case -710454014:  // searchType
          this.searchType = (ExternalIdSearchType) newValue;
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
    public ExternalIdSearch build() {
      return new ExternalIdSearch(
          externalIds,
          searchType);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ExternalIdSearch.Builder{");
      buf.append("externalIds").append('=').append(JodaBeanUtils.toString(externalIds)).append(',').append(' ');
      buf.append("searchType").append('=').append(JodaBeanUtils.toString(searchType));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
