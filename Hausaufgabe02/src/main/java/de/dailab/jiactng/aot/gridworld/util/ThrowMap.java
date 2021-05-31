package de.dailab.jiactng.aot.gridworld.util;

import java.util.HashMap;

/**
 * Special map that throws an exception if a key is not present or defined twice.
 */
public class ThrowMap<K, V> extends HashMap<K, V> {

	private static final long serialVersionUID = 2226886042805612357L;

	@Override
	public V put(K key, V value) {
		if (key == null) {
			throw new IllegalArgumentException("Key must not be null");
		}
		if (containsKey(key)) {
			throw new IllegalArgumentException("Value already defined for key " + key);
		}
		return super.put(key, value);
	}
	
	@Override
	public V get(Object key) {
		if (containsKey(key)) {
			return super.get(key);
		}
		throw new IllegalArgumentException("No value found for key " + key);
	}
}