package de.hetzge.sgame.booking;

import java.util.ArrayList;
import java.util.List;

public class Receipt<ITEM> {

	private final List<Ingredient<ITEM>> ingredients;
	private final ITEM result;

	public Receipt(List<Ingredient<ITEM>> ingredients, ITEM result) {
		this.ingredients = ingredients;
		this.result = result;
	}

	/**
	 * @return null if not possible
	 */
	public ITEM build(Container<ITEM> container) {
		if (!possible(container)) {
			return null;
		}
		List<Booking<ITEM>> bookings = book(container);
		if (bookings == null) {
			return null;
		}
		for (Booking<ITEM> booking : bookings) {
			booking.transfer();
		}
		return this.result;
	}

	public boolean possible(Container<ITEM> container) {
		for (Ingredient<ITEM> ingredient : this.ingredients) {
			if (!ingredient.available(container)) {
				return false;
			}
		}
		return true;
	}

	private List<Booking<ITEM>> book(Container<ITEM> container) {
		List<Booking<ITEM>> bookings = new ArrayList<>(this.ingredients.size());
		ContainerWithoutLimit<ITEM> receiptContainer = new ContainerWithoutLimit<>();
		for (Ingredient<ITEM> ingredient : this.ingredients) {
			Booking<ITEM> booking = container.book(ingredient.item, ingredient.amount, receiptContainer);
			if (booking == null) {
				// rollback
				bookings.stream().forEach(Booking::rollback);
				return null;
			}
			bookings.add(booking);
		}
		return bookings;
	}

	public ITEM getResult() {
		return this.result;
	}

}
