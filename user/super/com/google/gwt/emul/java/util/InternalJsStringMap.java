/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package java.util;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.Map.Entry;

/**
 * A simple wrapper around JavaScriptObject to provide {@link java.util.Map}-like semantics where
 * the key type is string.
 * <p>
 * Implementation notes:
 * <p>
 * String keys are mapped to their values via a JS associative map. String keys could collide with
 * intrinsic properties (like watch, constructor). To avoid that; {@link InternalJsStringMap})
 * prepends each key with a ':' while storing and {@link InternalJsStringMapModern} uses
 * {@code Object.create(null)} in the first place to avoid inheriting any properties (only available
 * in modern browsers).
 */
class InternalJsStringMap<K, V> {

  static class InternalJsStringMapModern<K, V> extends InternalJsStringMap<K, V> {
    @Override
    native JavaScriptObject createMap() /*-{
      return Object.create(null);
    }-*/;

    @Override
    String normalize(String key) {
      return key;
    }

    @Override
    public native boolean containsValue(Object value) /*-{
      var map = this.@InternalJsStringMap::backingMap;
      for (var key in map) {
        if (this.@InternalJsStringMap::equalsBridge(*)(value, map[key])) {
          return true;
        }
      }
      return false;
    }-*/;

    @Override
    public Iterator<Entry<K, V>> entries() {
      final String[] keys = keys();
      return new Iterator<Map.Entry<K,V>>() {
        int i = 0, last = -1;
        @Override
        public boolean hasNext() {
          return i < keys.length;
        }
        @Override
        public Entry<K, V> next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          return newMapEntry(keys[last = i++]);
        }
        @Override
        public void remove() {
          if (last < 0) {
            throw new IllegalStateException();
          }
          InternalJsStringMapModern.this.remove(keys[last]);
          last = -1;
        }
      };
    }

    private native String[] keys() /*-{
      return Object.keys(this.@InternalJsStringMap::backingMap);
    }-*/;
  }

  private final JavaScriptObject backingMap = createMap();
  private int size;
  AbstractHashMap<K,V> host;

  native JavaScriptObject createMap() /*-{
    return {};
  }-*/;

  String normalize(String key) {
    return ':' + key;
  }

  public final int size() {
    return size;
  }

  public final boolean contains(String key) {
    return !isUndefined(get(key));
  }

  public final V get(String key) {
    return at(normalize(key));
  }

  public final V put(String key, V value) {
    key = normalize(key);

    V oldValue = at(key);
    if (isUndefined(oldValue)) {
      size++;
    }

    set(key, toNullIfUndefined(value));

    return oldValue;
  }

  public final V remove(String key) {
    key = normalize(key);

    V value = at(key);
    if (!isUndefined(value)) {
      delete(key);
      size--;
    }

    return value;
  }

  private native V at(String key) /*-{
    return this.@InternalJsStringMap::backingMap[key];
  }-*/;

  private native void set(String key, V value) /*-{
    this.@InternalJsStringMap::backingMap[key] = value;
  }-*/;

  private native void delete(String key) /*-{
    delete this.@InternalJsStringMap::backingMap[key];
  }-*/;

  public native boolean containsValue(Object value) /*-{
    var map = this.@InternalJsStringMap::backingMap;
    for (var key in map) {
      // only keys that start with a colon ':' count
      if (key.charCodeAt(0) == 58) {
        var entryValue = map[key];
        if (this.@InternalJsStringMap::equalsBridge(*)(value, entryValue)) {
          return true;
        }
      }
    }
    return false;
  }-*/;

  public native Iterator<Entry<K, V>> entries() /*-{
    var list = this.@InternalJsStringMap::newEntryList()();
    for (var key in this.@InternalJsStringMap::backingMap) {
      // only keys that start with a colon ':' count
      if (key.charCodeAt(0) == 58) {
        var entry = this.@InternalJsStringMap::newMapEntry(*)(key.substring(1));
        list.@ArrayList::add(Ljava/lang/Object;)(entry);
      }
    }
    return list.@ArrayList::iterator()();
  }-*/;

  /**
   * Returns a custom ArrayList so that we could intercept removal to forward into our map.
   */
  private ArrayList<Entry<K, V>> newEntryList() {
    return new ArrayList<Entry<K, V>>() {
      @Override
      public Entry<K, V> remove(int index) {
        Entry<K, V> removed = super.remove(index);
        InternalJsStringMap.this.remove((String) removed.getKey());
        return removed;
      }
    };
  }

  protected final Entry<K, V> newMapEntry(final String key) {
    return new AbstractMapEntry<K, V>() {
      @Override
      public K getKey() {
        return (K) key;
      }
      @Override
      public V getValue() {
        return get(key);
      }
      @Override
      public V setValue(V object) {
        return put(key, object);
      }
    };
  }

  /**
   * Bridge method from JSNI that keeps us from having to make polymorphic calls
   * in JSNI. By putting the polymorphism in Java code, the compiler can do a
   * better job of optimizing in most cases.
   */
  protected final boolean equalsBridge(Object value1, Object value2) {
    return host.equals(value1, value2);
  }

  private static <T> T toNullIfUndefined(T value) {
    return isUndefined(value) ? null : value;
  }

  private static native boolean isUndefined(Object value) /*-{
    return value === undefined;
  }-*/;
}
