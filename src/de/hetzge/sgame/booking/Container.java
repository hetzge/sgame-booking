package de.hetzge.sgame.booking;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Container<ITEM> implements Serializable {

	private final Map<ITEM, Value> items = new HashMap<>();
	private final List<Booking<ITEM>> bookings = new LinkedList<>();

	public synchronized void transfer(Booking<ITEM> booking) {
		if (booking.from != this) {
			throw new IllegalArgumentException("You can only transfer bookings from self container.");
		}
		if (!this.hasBooking(booking) || !booking.to.hasBooking(booking)) {
			throw new IllegalArgumentException("Try to transfer a booking that not exists for container.");
		}
		synchronized (booking.to) {
			boolean transferSuccessful = this.transfer(booking.item, booking.amount, booking.to, true);
			if (transferSuccessful) {
				this.bookings.remove(booking);
				booking.to.bookings.remove(booking);
			} else {
				throw new IllegalStateException("Transfer booking failed.");
			}
		}
	}

	public synchronized boolean transfer(ITEM item, int amount, Container<ITEM> to) {
		return this.transfer(item, amount, to, false);
	}

	public synchronized boolean transfer(ITEM item, int amount, Container<ITEM> to, boolean booking) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Only positive amounts can be transfered");
		}
		synchronized (to) {
			if (!this.can(item) || !to.can(item) || !this.has(item)
					|| (booking ? !this.hasBooking(item, amount, this, to) : !to.canAddAmount(item, amount))
					|| (booking ? !this.hasBooking(item, amount, this, to) : !this.hasAmountAvailable(item, amount))) {
				return false;
			}
			Value value = this.items.get(item);
			value.amount -= amount;

			Value toValue = to.items.get(item);
			if (toValue == null) {
				to.createUnlimitedDefaultValue(item);
				toValue = to.items.get(item);
			}
			toValue.amount += amount;
		}

		return true;
	}

	private void createUnlimitedDefaultValue(ITEM item) {
		Value newValue = new Value();
		newValue.max = Integer.MAX_VALUE;
		this.items.put(item, newValue);
	}

	private boolean hasBooking(ITEM item, int amount, Container<ITEM> from, Container<ITEM> to) {
		return this.hasBooking(new Booking<ITEM>(item, amount, from, to));
	}

	private boolean hasBooking(Booking<ITEM> booking) {
		return this.bookings.contains(booking);
	}

	public synchronized Booking<ITEM> book(ITEM item, int amount, Container<ITEM> to) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Only positive amounts can be booked");
		}
		synchronized (to) {
			if (!this.hasAmountAvailable(item, amount)) {
				return null;
			}
			if (!to.canAddAmount(item, amount)) {
				return null;
			}

			Booking<ITEM> booking = new Booking<ITEM>(item, amount, this, to);
			this.addBooking(booking);
			to.addBooking(booking);

			return booking;
		}
	}

	protected synchronized void addBooking(Booking<ITEM> booking) {
		this.bookings.add(booking);
	}

	protected synchronized void removeBooking(Booking<ITEM> booking) {
		this.bookings.remove(booking);
	}

	public synchronized boolean hasAmountAvailable(ITEM item, int amount) {
		return this.has(item) && this.amount(item) - this.bookedFromAmount(item, false) >= amount;
	}

	public synchronized boolean canAddAmount(ITEM item, int amount) {
		Value value = this.items.get(item);
		return this.can(item) && this.amount(item) + this.bookedToAmount(item) + amount <= value.max;
	}

	// TODO test this
	public synchronized int getMissingAmount(ITEM item) {
		Value value = this.items.get(item);
		return value != null ? value.max - (value.amount + bookedToAmount(item)) : 0;
	}

	/**
	 * Returns the amount of a given item is booked from this container.
	 */
	private int bookedFromAmount(ITEM item, boolean hidden) {
		int amount = 0;
		for (Booking<ITEM> booking : this.bookings) {
			if (!hidden || booking.hide) {
				if (booking.from == this && booking.item.equals(item)) {
					amount += booking.amount;
				}
			}
		}
		return amount;
	}

	/**
	 * Returns the amount of a given item is reserved to bring to this
	 * container.
	 */
	private int bookedToAmount(ITEM item) {
		int amount = 0;
		for (Booking<ITEM> booking : this.bookings) {
			if (booking.to == this && booking.item.equals(item)) {
				amount += booking.amount;
			}
		}
		return amount;
	}

	public synchronized boolean can(ITEM item) {
		return this.items.get(item) != null;
	}

	public synchronized boolean has(ITEM item) {
		return this.amount(item) > 0;
	}

	public synchronized int amountWithoutHidden(ITEM item) {
		return amount(item) - bookedFromAmount(item, true);
	}

	public synchronized int amount(ITEM item) {
		Value value = this.items.get(item);
		if (value == null) {
			return 0;
		}
		return value.amount;
	}

	public synchronized Set<ITEM> getItems() {
		return this.items.keySet();
	}

	public synchronized void set(ITEM item, int amount, int max) {
		Value value = this.getOrCreateValue(item);
		value.amount = amount;
		value.max = max;
	}

	public synchronized void set(ITEM item, int amountAndMax) {
		this.set(item, amountAndMax, amountAndMax);
	}

	public synchronized void setAmount(ITEM item, int amount) {
		Value value = this.getOrCreateValue(item);
		value.amount = amount;
	}

	public synchronized void setMax(ITEM item, int max) {
		Value value = this.getOrCreateValue(item);
		value.max = max;
	}

	private Value getOrCreateValue(ITEM item) {
		Value value = this.items.get(item);
		if (value == null) {
			value = new Value();
			this.items.put(item, value);
		}
		return value;
	}

	public synchronized void unchain() {
		for (Booking<ITEM> booking : this.bookings) {
			booking.to.removeBooking(booking);
		}
		this.bookings.clear();
	}

	/**
	 * A container is empty if there are no bookings and every {@link Value}s
	 * amount is 0.
	 */
	public synchronized boolean isEmpty() {
		if (!this.bookings.isEmpty()) {
			return false;
		}
		for (Value value : this.items.values()) {
			if (value.amount != 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @see Container#isEmpty()
	 */
	public synchronized boolean isNotEmpty() {
		return !isEmpty();
	}

	private class Value implements Serializable {
		private int max;
		private int amount;
	}

}