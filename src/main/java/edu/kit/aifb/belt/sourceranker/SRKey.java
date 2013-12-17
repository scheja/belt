package edu.kit.aifb.belt.sourceranker;

import edu.kit.aifb.belt.db.Action;

public class SRKey {
	// Since we use a 64-bit system, we can speed up things by using packed
	// attributes.
	private final long action;
	private final long propertyAndPosition;

	public SRKey(long action, int property, int position) {
		this.action = action;
		this.propertyAndPosition = property & 0xFFFFFFFF | (position & 0xFF) << 32;
	}

	public SRKey(Action action, int property, int position) {
		this.action = action.getProperty() | (((long) action.getDomain()) << 32);
		this.propertyAndPosition = property & 0xFFFFFFFF | (position & 0xFF) << 32;
	}

	public int hashCode() {
		return (int) ((action >> 32) ^ action ^ propertyAndPosition ^ (propertyAndPosition >> 32));
	}

	public boolean equals(Object o) {
		if (o instanceof SRKey) {
			SRKey i = (SRKey) o;

			return i.action == action && i.propertyAndPosition == propertyAndPosition;
		} else {
			return false;
		}
	}

	public long getAction() {
		return action;
	}

	public Action getPlainAction() {
		return new Action((int) (action >> 32), (int) action);
	}

	public long getPropertyAndPosition() {
		return propertyAndPosition;
	}

	public int getProperty() {
		return (int) propertyAndPosition;
	}

	public int getPosition() {
		return (int) ((propertyAndPosition >> 32) & 0xFFFFFFFF);
	}
}