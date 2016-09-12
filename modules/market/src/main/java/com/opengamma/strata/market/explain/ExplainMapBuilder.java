/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.explain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.opengamma.strata.collect.ArgChecker;

/**
 * A builder for the map of explanatory values.
 * <p>
 * This is a mutable builder for {@link ExplainMap} that must be used from a single thread.
 */
public final class ExplainMapBuilder {

  /**
   * The parent builder.
   */
  private final ExplainMapBuilder parent;
  /**
   * The map of explanatory values.
   */
  private final Map<ExplainKey<?>, Object> map = new LinkedHashMap<>();

  /**
   * Creates a new instance.
   */
  ExplainMapBuilder() {
    this.parent = null;
  }

  /**
   * Creates a new instance.
   * 
   * @param parent  the parent builder
   */
  ExplainMapBuilder(ExplainMapBuilder parent) {
    this.parent = parent;
  }

  //-------------------------------------------------------------------------
  /**
   * Opens a list entry to be populated.
   * <p>
   * This returns the builder for the new list entry.
   * If the list does not exist, it is created and the first entry added.
   * If the list has already been created, the entry is appended.
   * <p>
   * Once opened, the child builder resulting from this method must be used.
   * The method {@link #closeListEntry(ExplainKey)} must be used to close the
   * child and receive an instance of the parent back again.
   * 
   * @param <R>  the type of the value
   * @param key  the list key to open
   * @return the child builder
   */
  @SuppressWarnings("unchecked")
  public <R extends List<?>> ExplainMapBuilder openListEntry(ExplainKey<R> key) {
    // list entry is a ExplainMapBuilder, making use of erasure in generics
    // builder is converted to ExplainMap when entry is closed
    ExplainMapBuilder child = new ExplainMapBuilder(this);
    Object value = map.get(key);
    ArrayList<Object> list;
    if (value instanceof ArrayList) {
      list = (ArrayList<Object>) value;
    } else {
      list = new ArrayList<>();
      map.put(key, list);
    }
    list.add(child);
    return child;
  }

  /**
   * Closes the currently open list.
   * <p>
   * This returns the parent builder.
   * 
   * @param <R>  the type of the value
   * @param key  the list key to close
   * @return the parent builder
   */
  public <R extends List<?>> ExplainMapBuilder closeListEntry(ExplainKey<R> key) {
    Object value = parent.map.get(key);
    if (value instanceof ArrayList == false) {
      throw new IllegalStateException("ExplainMapBuilder.closeList() called but no list found to close");
    }
    // close entry by converting it from ExplainMapBuilder to ExplainMap
    @SuppressWarnings("unchecked")
    ArrayList<Object> list = (ArrayList<Object>) value;
    ExplainMapBuilder closedEntry = (ExplainMapBuilder) list.get(list.size() - 1);
    list.set(list.size() - 1, closedEntry.build());
    return parent;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a list entry using a consumer callback function.
   * <p>
   * This is an alternative to using {@link #openListEntry(ExplainKey)} and
   * {@link #closeListEntry(ExplainKey)} directly.
   * The consumer function receives the child builder and must add data to it.
   * 
   * @param <R>  the type of the value
   * @param key  the list key to open
   * @param consumer  the consumer that receives the list entry builder and adds to it
   * @return this builder
   */
  public <R extends List<?>> ExplainMapBuilder addListEntry(ExplainKey<R> key, Consumer<ExplainMapBuilder> consumer) {
    ExplainMapBuilder child = openListEntry(key);
    consumer.accept(child);
    return child.closeListEntry(key);
  }

  /**
   * Adds a list entry using a consumer callback function, including the list index.
   * <p>
   * This is an alternative to using {@link #openListEntry(ExplainKey)} and
   * {@link #closeListEntry(ExplainKey)} directly.
   * The consumer function receives the child builder and must add data to it.
   * 
   * @param <R>  the type of the value
   * @param key  the list key to open
   * @param consumer  the consumer that receives the list entry builder and adds to it
   * @return this builder
   */
  public <R extends List<?>> ExplainMapBuilder addListEntryWithIndex(ExplainKey<R> key, Consumer<ExplainMapBuilder> consumer) {
    ExplainMapBuilder child = openListEntry(key);
    // find index
    Object value = map.get(key);
    @SuppressWarnings("unchecked")
    ArrayList<Object> list = (ArrayList<Object>) value;
    child.put(ExplainKey.ENTRY_INDEX, list.size() - 1);
    consumer.accept(child);
    return child.closeListEntry(key);
  }

  //-------------------------------------------------------------------------
  /**
   * Puts a single value into the map.
   * <p>
   * If the key already exists, the value will be replaced.
   * 
   * @param <R>  the type of the value
   * @param key  the key to add
   * @param value  the value to add
   * @return this builder
   */
  public <R> ExplainMapBuilder put(ExplainKey<R> key, R value) {
    ArgChecker.notNull(key, "key");
    ArgChecker.notNull(value, "value");
    map.put(key, value);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the map.
   * 
   * @return the resulting map
   */
  public ExplainMap build() {
    return ExplainMap.of(map);
  }

}
