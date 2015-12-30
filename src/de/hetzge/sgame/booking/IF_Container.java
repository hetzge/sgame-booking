package de.hetzge.sgame.booking;

import java.util.Set;

public interface IF_Container<ITEM> {

	void transfer(Booking<ITEM> booking);

	boolean transfer(ITEM item, int amount, Container<ITEM> to);

	boolean transfer(ITEM item, int amount, Container<ITEM> to, boolean booking);

	Booking<ITEM> book(ITEM item, int amount, Container<ITEM> to);

	boolean hasAmountAvailable(ITEM item, int amount);

	boolean canAddAmount(ITEM item, int amount);

	int getMissingAmount(ITEM item);

	boolean can(ITEM item);

	boolean has(ITEM item);

	int amountWithoutHidden(ITEM item);

	int amount(ITEM item);

	Set<ITEM> getItems();

	void set(ITEM item, int amount, int max);

	void set(ITEM item, int amountAndMax);

	void setAmount(ITEM item, int amount);

	void setMax(ITEM item, int max);

	void unchain();

	boolean isEmpty();

}
