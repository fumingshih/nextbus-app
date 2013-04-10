package com.danielstiner.cyride.util;

import java.util.LinkedList;
import java.util.List;


public class CallbackManager<T> {
	
	private List<Callback<T>> listeners = new LinkedList<Callback<T>>();

	public void addListener(Callback<T> listener) {
		listeners.add(listener);
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
