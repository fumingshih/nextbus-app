package com.danielstiner.cyride.util;

import java.util.LinkedList;
import java.util.List;


public class CallbackManager<T> {
	
	private List<Callback<T>> listeners = new LinkedList<Callback<T>>();

	public boolean active() {
		return count() > 0;
	}
	
	public void addListener(Callback<T> listener) {
		listeners.add(listener);
	}

	public int count() {
		return listeners.size();
	}
	
	public boolean removeListener(Callback<T> listener) {
		return listeners.remove(listener);
	}

	public void runAll(T value) {
		for (Callback<T> to : this.listeners) {
			to.run(value);
		}
	}

}
