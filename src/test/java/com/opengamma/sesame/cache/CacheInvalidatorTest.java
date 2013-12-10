/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.inject.Provider;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.util.test.TestGroup;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;

/**
 *
 */
@Test(groups = TestGroup.UNIT)
public class CacheInvalidatorTest {

  private static final MethodInvocationKey METHOD_KEY_1 = methodKey(new ArrayList<>(), "subList", new Object[]{1, 2});
  private static final MethodInvocationKey METHOD_KEY_2 = methodKey(new LinkedList<>(), "set", new Object[]{3, "foo"});
  private static final MethodInvocationKey METHOD_KEY_3 = methodKey(new ArrayList<>(), "size", null);

  private static Cache createCache() {
    PersistenceConfiguration persistenceConfiguration =
        new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE);
    CacheConfiguration config = new CacheConfiguration("test", 100).eternal(true).persistence(persistenceConfiguration);
    Cache cache = new Cache(config);
    CacheManager.getInstance().addCache(cache);
    return cache;
  }

  private void populateCache(Cache cache) {
    cache.put(new Element(METHOD_KEY_1, new Object()));
    cache.put(new Element(METHOD_KEY_2, new Object()));
    cache.put(new Element(METHOD_KEY_3, new Object()));
  }

  private static MethodInvocationKey methodKey(Object receiver, String methodName, Object[] args) {
    Method method = ConfigUtils.getMethod(receiver.getClass(), methodName);
    return new MethodInvocationKey(receiver, method, args);
  }

  /**
   * test the invalidator in isolation
   */
  @Test
  public void registerAndInvalidate() {
    final LinkedList<MethodInvocationKey> keys = Lists.newLinkedList();
    Provider<Collection<MethodInvocationKey>> provider = new Provider<Collection<MethodInvocationKey>>() {
      @Override
      public Collection<MethodInvocationKey> get() {
        return keys;
      }
    };
    Cache cache = createCache();
    CacheInvalidator invalidator = new CacheInvalidator(provider, cache);
    // doesn't matter what the methods are
    ObjectId abc2 = ObjectId.of("abc", "2");
    ExternalId abc1 = ExternalId.of("abc", "1");
    ExternalId bnd1 = ExternalId.of("bnd", "1");
    ExternalId bnd2 = ExternalId.of("bnd", "2");

    keys.add(METHOD_KEY_1);
    invalidator.register(abc1);
    keys.add(METHOD_KEY_2);
    invalidator.register(abc2);
    keys.removeLast();
    invalidator.register(ExternalIdBundle.of(bnd1, bnd2));

    populateCache(cache);
    invalidator.invalidate(abc1);
    assertNull(cache.get(METHOD_KEY_1));
    assertNotNull(cache.get(METHOD_KEY_2));
    assertNotNull(cache.get(METHOD_KEY_3));

    populateCache(cache);
    invalidator.invalidate(abc2);
    assertNull(cache.get(METHOD_KEY_1));
    assertNull(cache.get(METHOD_KEY_2));
    assertNotNull(cache.get(METHOD_KEY_3));

    populateCache(cache);
    invalidator.invalidate(bnd1);
    assertNull(cache.get(METHOD_KEY_1));
    assertNotNull(cache.get(METHOD_KEY_2));
    assertNotNull(cache.get(METHOD_KEY_3));

    populateCache(cache);
    invalidator.invalidate(bnd2);
    assertNull(cache.get(METHOD_KEY_1));
    assertNotNull(cache.get(METHOD_KEY_2));
    assertNotNull(cache.get(METHOD_KEY_3));
  }
}
