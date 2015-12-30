package de.hetzge.sgame.booking;

public class Ingredient<ITEM> {
	protected final ITEM item;
	protected final int amount;

	public Ingredient(ITEM item, int amount) {
		this.item = item;
		this.amount = amount;
	}

	public boolean available(Container<ITEM> container) {
		return container.hasAmountAvailable(this.item, this.amount);
	}

}