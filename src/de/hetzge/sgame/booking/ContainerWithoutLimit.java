package de.hetzge.sgame.booking;

public class ContainerWithoutLimit<ITEM> extends Container<ITEM> {

	@Override
	public synchronized boolean can(ITEM item) {
		return true;
	}

	@Override
	public synchronized boolean canAddAmount(ITEM item, int amount) {
		return true;
	}

}
