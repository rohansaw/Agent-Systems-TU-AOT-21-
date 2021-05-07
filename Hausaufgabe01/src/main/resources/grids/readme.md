Example levels for Gridworld Game
=================================

This folder contains some example levels for the Gridworld game. Levels can also be randomly generated, but those levels provide a more consistent setup for testing and also some more sophisticated levels, especially when
using obstacles.

Note that in order to allow for some communication to get everything set up on the client side, the first order should not activate before, say, turn 5.

File Format
-----------

Each Grid file has to be formatted as follows:

* the first line consists of 6 integers separated by spaces or tabs: `width`, `height`, `turns`, `num_orders`, `num_brokers`, and `num_workers` per broker
* the next `height` lines consist of `width` characters describing the respective rows of the grid; here, a `#` marks an obstacle, and a `.` marks a free space; other characters can be used to indicate the positions of orders or workers, but will be _ignored_ here; only the obstacles are considered
* next `num_orders` lines each have a string `order_id` and six integers `x`, `y`, `turn_created`, `turn_deadline`, `value` and `penalty_per_turn`
* next, `num_brokers` blocks of `num_workers` lines, each with one string `worker_id` and three integers `x`, `y`, and `fuel` indicating the brokers' workers' IDs, and initial location and fuel

All lines after that are ignored and can be used e.g. for documentation.

Example
-------

	10 10 100 10 1 5
	.........o
	.w.#..o.w.
	...#....o.
	.oo####.#.
	..##.w..##
	...o....#o
	..####..#.
	....#...o.
	.w..#o..w.
	..o.#.....
	o1  3 5  0  10 10 0
	o2  8 1 10  20 11 1
	o3  0 9 20  30 12 1
	o4  2 3 30  40 13 1
	o5  6 1 40  50 14 2
	o6  5 8 50  60 15 2
	o7  1 3 60  70 16 2
	o8  9 5 70  80 17 3
	o9  8 7 80  90 18 3
	o10 2 9 90 100 19 3
	w11 1 1 999
	w12 8 8 999
	w13 1 8 999
	w14 8 1 999
	w15 4 5 999

