package com.ontotext.trree.plugin.geo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestUtils {

	@Test
	public void doIntersect() {
		assertTrue(Utils.intersects(0, 0, 0, 0, 0, 0, 0, 0));
		assertTrue(Utils.intersects(-1, 0, 0, 0, -1, 0, 0, 0));
		assertTrue(Utils.intersects(0, -1, 0, 0, 0, -1, 0, 0));
		assertTrue(Utils.intersects(0, 0, 1, 0, 0, 0, 1, 0));
		assertTrue(Utils.intersects(0, 0, 0, 1, 0, 0, 0, 1));

		assertTrue(Utils.intersects(1, 179, 2, -179, 1, -180, 2, -179));
		assertTrue(Utils.intersects(1, 179, 2, -179, 1, 179, 2, 180));

		assertTrue(Utils.intersects(1, -180, 2, -179, 1, 179, 2, -179));
		assertTrue(Utils.intersects(1, 179, 2, 180, 1, 179, 2, -179));

		assertTrue(Utils.intersects(-1, -180, 1, 180, 0, -180, 0, -180));
		assertTrue(Utils.intersects(-1, -180, 1, 180, 0, 0, 0, 0));
		assertTrue(Utils.intersects(-1, -180, 1, 180, 0, +180, 0, +180));
	}

	@Test
	public void doNotIntersect() {
		assertFalse(Utils.intersects(0, 0, 0, 0, 1, 1, 1, 1));
		assertFalse(Utils.intersects(0, 0, 0, 0, -1, 1, 1, 1));
		assertFalse(Utils.intersects(0, 0, 0, 0, 1, -1, 1, 1));
		assertFalse(Utils.intersects(0, 0, 0, 0, 1, 1, -1, -1));

		assertFalse(Utils.intersects(0, 0, 0, 0, 1, 0, -1, 0));
		assertFalse(Utils.intersects(1, 0, -1, 0, 0, 0, 0, 0));

		assertFalse(Utils.intersects(-2, -179, 2, 179, 0, 180, 0, 180));
		assertFalse(Utils.intersects(-2, -179, 2, 179, 0, -180, 0, -180));
	}

	@Test
	public void isWithin() {
		assertTrue(Utils.within(0, 0, 0, 0, 0, 0));
		assertTrue(Utils.within(1, 1, 1, 1, 1, 1));
	}

	@Test
	public void isNotWithin() {
		assertFalse(Utils.within(0, 0, 0, 0, 1, 0));
		assertFalse(Utils.within(0, 0, 0, 0, 0, 1));
		assertFalse(Utils.within(0, 0, 0, 0, 1, 1));
		assertFalse(Utils.within(0, 0, 0, 0, -1, 0));
		assertFalse(Utils.within(0, 0, 0, 0, 0, -1));
		assertFalse(Utils.within(0, 0, 0, 0, -1, -1));

		assertFalse(Utils.within(1, 2, 3, 4, 2, 0));
		assertFalse(Utils.within(1, 2, 3, 4, 2, 5));
		assertFalse(Utils.within(1, 2, 3, 4, 0, 3));
		assertFalse(Utils.within(1, 2, 3, 4, 4, 3));
	}
}
