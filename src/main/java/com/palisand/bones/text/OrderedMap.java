package com.palisand.bones.text;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class OrderedMap<K,V> extends AbstractMap<K,V> {
	
	class OrderedEntrySet extends AbstractSet<Entry<K,V>> {
		List<Entry<K,V>> list = new ArrayList<>();

		@Override
		public int size() {
			return list.size();
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return list.iterator();
		}
		
		@Override
		public boolean add(Entry<K,V> entry) {
			return list.add(entry);
		}
		
	}
	
	private OrderedEntrySet entries = new OrderedEntrySet();

	@Override
	public Set<Entry<K, V>> entrySet() {
		return entries;
	}
	
	@Override
	public V put(K key, V value) {
		for (Entry<K,V> entry: entries) {
			if (entry.getKey().equals(key)) {
				V result = entry.getValue();
				entry.setValue(value);
				return result;
			}
		}
		entries.add(new SimpleEntry<>(key,value));
		return null;
	}

}
