Usage
-----

Play the game with `lein run`. Run the tests with `lein test`. Use
`lein auto test` to re-run tests on save.

Design
------

### AI algorithm

The AI is implemented as a negamax algorithm with alpha beta pruning.
Though I originally strove to roll my own algorithm, the feedback I
received on de-coupling my implementation from a 3x3 board created too
many edge cases to implement through a series of heuristics. To limit
the time the algorithm takes to run, I implemented a `how-deep` function
that calculates a max depth of algorithm recursion to achieve a
specified number of computations. If the program takes too long, that
number can be decreased. I experimented with breaking the game tree 
into partitions and running them in `future`s. Apparently the overhead is
too high, because it was faster even with deep game trees to avoid `future`s.

I have ignored the `depth` argument on a 3x3
board because there are too many late checkmates possible if the AI is
player 2. Also, my original implementation could be beaten as player 2.

As a result of this refactoring, the number of full test games played in
the tests had to be scaled back radically. At best, the tests offer some
reassurance that refactoring has not broken anything. But to really
field test the AI, it might take several hours.

I also re-implemented the original algorithm (play center else play
random) and a purely random AI player. These AIs are available as
opponents.

### Program description

#### Bootstrap

The game begins by instantiating an implementer of the `Bootstrap`
protocol. `Bootstrap` has a single `bootstrap` function. Here it is
implemented by the `make-bootstrap` function in
`game.io.console-bootstrap`. On starting the game, `game.core` calls
`make-bootstrap`, and then calls `bootstrap` on the result. The
`Bootstrap` protocol can be implemented by an alternative source of IO.

The `bootstrap` function returns a map containing the type of player for
players 1 and 2, and the symbols for players 1 and 2. Bootstrap handles
identical player symbols by appending `(p1)` and `(p2)` as appropriate.

The `bootstrap` function now also allows users to choose a board size.
Boards can be any sized square, and the AI players and game management
can handle it. I contemplated allowing non-square rectangles, but that
raised questions about how the rules would work. How, for example, is a
diagonal defined in an 11x4 board? Permitting any sized square seemed to
me the most loosely coupled approach that is still recognizably
tic-tac-toe.

#### IO provider

The companion to the console-oriented `bootstrap` function (which
provides IO services before the game starts) is `ConsoleIO`, which
provides the IO services for the game itself. `ConsoleIO` implements
`IOProvider`, and offers a `make-console-io` factory function. The
factory functions are collected in `game.io.io-provider-factories`, and
the appropriate factory function is determined by a constant in
`game.core`.

An `IOProvider` must implement `draw-board`,
`request-move-from-human-player`, and `publish-message`. It is via these
abstractions that the `game.manager` and the instantiated players can
access IO facilities. Similarly to `ConsoleBootstrap`, `ConsoleIO` can
be replaced with any other implementer of its protocol in order to swap
out a console-based IO for some other IO. The new `IOProvider` should be
registered in `game.io.io-provider-factories` and selected by the
constant `io` in `game.core`. Previously, the legend presented to human
players was formatted by different helper functions than the game board
itself, but this has been refactored to use the same helper functions in
a win for DRY-minded persons the world over.

The `IOProvider` instance is maintained in
`game.io.io-provider-singleton`, as `io-provider-instance` beginning as
a promise that is delivered by the `-main` function in `game.core`. This
permits Players and other entities to access `io-provider` without it
being an argument to Players that have no use for it. `GameManager` does
not take `IOProvider` as an argument.

`IOProvider` now has a fuller test suite.

#### Player instantiation

Next, `game.core` uses the appropriate player type values in the map
returned by `bootstrap` to call appropriate player factory functions
from the lookup table at `game.players.player-factories`. Each concrete
player implements the `Player` protocol, and so must implement that
protocol’s `get-move` function.

#### Game management

Finally, `game.core` instantiates a `GameManager`, which implements
`Manager`. This permits game management to be modular, in case we want
to re-implement the game manager in some other way. `Manager` has a
single function, `run-game`. Once the players and IO provider are
instantiated, they are passed in to `run-game`, which uses them to run
the game.

`GameManager` now no longer throws errors as a result of improper player
implementations. I had thought this would ease the burden on `Player`
implementers if their implementation was causing confounding stack
traces coming from `GameManger`, but in fact the `catch` block was
catching *too many* errors during development, causing `GameManager` to
appear to be causing problems that were actually arising elsewhere.

`GameManager`’s loop has been refactored to abstract out much of its
functionality, leaving mostly flow control and function calls.

One possible enhancement would be to pause an AI vs. AI game and await
user input. This could be achieved by adding an `{:ai true}` to the
`record` types representing specific players, which the `GameManager`
instance could check when instantiating the players, and if both are
`:ai`, passing an argument to the passed-in `io-provider` on each loop
of `run-game`, triggering the IO to prompt the user to “Push any key” to
continue the game. I decided not to do this because a user may not want
to push a key repeatedly simply to see the outcome of a single
game—plus, all the moves are visible by scrolling up on the console.

### Mutability and state

I removed all mutability except for one set of tests, which models a
player by mutating an atom of game moves. This too could be eliminated,
but would be a bit less clean. The `io-provider-singleton` is a promise
delivered at runtime, but is not mutable.

Overall, I consider this to be a much better approach, though it
introduces a bit of incidental complexity. First, if the AI algorithm
tracked which move number it was on, it could avoid checking for game
states that could not exist at that stage of the game. For instance, if
the AI were player 1 and were on its second move, it could assume that
it had already played the center and avoid looking up that position on
the game board. Also, one of the plays involving a “checkmate” can only
occur if the AI moved first and it is the third round; a stateful AI
could avoid several calculations by exploiting that. An alternative
would be for the game manager to pass each player the round number. That
approach, however, merely moves statefulness up a level or requires
mutability—either the manager would need to mutate a counter, or the
manager would need to count moves on the AI’s behalf (which is wasted
effort if the player does not use that information, and violates the
Single Responsibility Principle and Rich Hickey’s
[intonations](http://www.infoq.com/presentations/Simple-Made-Easy)
against “complecting.”)

One further quirk of using pure functions is that the `bootstrap`
function in `game.io.console-bootstrap` works in a roundabout way. That
function builds a data structure representing the questions necessary to
set up the game (What kind of player for player 1 and player 2? What
symbols for each player?). The function then `reduce`s over that data
structure, performing IO within the reduction. The output is consumed by
`game.core` to instantiate the players and pass them to
`game.management.manager/run-game`, as described above.

### A note on protocols

I made more extensive use of protocols than I have in any previous
Clojure project. As pointed out in
[this](https://blog.8thlight.com/myles-megyesi/2012/04/26/polymorphism-in-clojure.html)
blog post, polymorphism may be easier to achieve by passing functions as
parameters. Following this pattern, the functions that belong to each
protocol could simply be standalone functions defined in a namespace
appropriate to each concrete version of one of the abstractions
(`Player`, `Game`, and `IO`).

The reason I took the approach I did is that the SOLID approach is new
to me, and I sought out information about SOLID principles as applied to
Clojure. The
[materials](http://www.lispcast.com/solid-principles-in-clojure) |
[I](http://thinkrelevance.com/blog/2013/11/07/when-should-you-use-clojures-object-oriented-features) |
[found](http://www.infoq.com/presentations/SOLID-Clojure) relied heavily
on protocols, and so I did too.

A benefit of my approach is that it brings the advantages of design by
contract. Relying on higher-level functions would require the passed-in
functions to have the correct signatures, but would lack a compile-time
check that the implementation is correct (unless checked by a `{:pre}`).

I considered subdividing the protocols further, in keeping with the
Interface Segregation Principle, but I think the `Player`, `Manager`,
and `IOProvider` are the right level of segregation.

Finally, I chose to use `defrecord` rather than `deftype` based on the
advice that `deftype` “is fundamentally intended to be used to define
low-level infrastructure types, as might be needed for implementing a
new data structure or reference type; in contrast, maps and records
should be used to house your application-level data.” Chas Emerick, *et
al.*, *Clojure Programming* 227 (2012) *available*
[here](https://goo.gl/vFxrp6).
