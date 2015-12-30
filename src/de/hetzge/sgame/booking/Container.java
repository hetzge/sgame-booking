package de.hetzge.sgame.booking;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO make generic
 */
public class Container implements Serializable {

	private final Map<IF_Item, Value> items = new HashMap<>();
	private final List<Booking> bookings = new LinkedList<>();

	public synchronized void transfer(Booking booking) {
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

	public synchronized boolean transfer(IF_Item item, int amount, Container to) {
		return this.transfer(item, amount, to, false);
	}

	public synchronized boolean transfer(IF_Item item, int amount, Container to, boolean booking) {
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

	private void createUnlimitedDefaultValue(IF_Item item) {
		Value newValue = new Value();
		newValue.max = Integer.MAX_VALUE;
		this.items.put(item, newValue);
	}

	private boolean hasBooking(IF_Item item, int amount, Container from, Container to) {
		return this.hasBooking(new Booking(item, amount, from, to));
	}

	private boolean hasBooking(Booking booking) {
		return this.bookings.contains(booking);
	}

	public synchronized Booking book(IF_Item item, int amount, Container to) {
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

			Booking booking = new Booking(item, amount, this, to);
			this.addBooking(booking);
			to.addBooking(booking);

			return booking;
		}
	}

	protected synchronized void addBooking(Booking booking) {
		this.bookings.add(booking);
	}

	protected synchronized void removeBooking(Booking booking) {
		this.bookings.remove(booking);
	}

	public synchronized boolean hasAmountAvailable(IF_Item item, int amount) {
		return this.has(item) && this.amount(item) - this.bookedFromAmount(item, false) >= amount;
	}

	public synchronized boolean canAddAmount(IF_Item item, int amount) {
		Value value = this.items.get(item);
		return this.can(item) && this.amount(item) + this.bookedToAmount(item) + amount <= value.max;
	}

	// TODO test this
	public synchronized int getMissingAmount(IF_Item item) {
		Value value = this.items.get(item);
		return value != null ? value.max - (value.amount + bookedToAmount(item)) : 0;
	}

	/**
	 * Returns the amount of a given item is booked from this container.
	 */
	private int bookedFromAmount(IF_Item item, boolean hidden) {
		int amount = 0;
		for (Booking booking : this.bookings) {
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
	private int bookedToAmount(IF_Item item) {
		int amount = 0;
		for (Booking booking : this.bookings) {
			if (booking.to == this && booking.item.equals(item)) {
				amount += booking.amount;
			}
		}
		return amount;
	}

	public synchronized boolean can(IF_Item item) {
		return this.items.get(item) != null;
	}

	public synchronized boolean has(IF_Item item) {
		return this.amount(item) > 0;
	}

	public synchronized int amountWithoutHidden(IF_Item item) {
		return amount(item) - bookedFromAmount(item, true);
	}

	public synchronized int amount(IF_Item item) {
		Value value = this.items.get(item);
		if (value == null) {
			return 0;
		}
		return value.amount;
	}

	public synchronized Set<IF_Item> getItems() {
		return this.items.keySet();
	}

	public synchronized void set(IF_Item item, int amount, int max) {
		Value value = this.getOrCreateValue(item);
		value.amount = amount;
		value.max = max;
	}

	public synchronized void set(IF_Item item, int amountAndMax) {
		this.set(item, amountAndMax, amountAndMax);
	}

	public synchronized void setAmount(IF_Item item, int amount) {
		Value value = this.getOrCreateValue(item);
		value.amount = amount;
	}

	public synchronized void setMax(IF_Item item, int max) {
		Value value = this.getOrCreateValue(item);
		value.max = max;
	}

	private Value getOrCreateValue(IF_Item item) {
		Value value = this.items.get(item);
		if (value == null) {
			value = new Value();
			this.items.put(item, value);
		}
		return value;
	}

	public synchronized void unchain() {
		for (Booking booking : this.bookings) {
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