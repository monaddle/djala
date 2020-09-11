# A Type Astronaught's Guide for Solving the Uninstantiated ID Problem
*This post presents some speculative syntaxes for being able to modify products. These ideas are synthesized primarily from writing and talks by Rob Norris and John de Goes, and less directly by Dave Gurnell and Miles Sabin.*

Motivating problem:


Often when you're working with a data structure, you'll want it to have an extra field or two. Probably the most common example is a database record before it's been assigned an ID:

```scala
case class Turkey(id: Int, name: String, age: Int)
```

Before the record has been created in the database and an id assigned, the `id` field is worse than meaningless: it's misleading. The data structure is lying about what data you have. This can be dealt with by conventions, setting the id to e.g. zero, but obviously we'd prefer not to depend on human discipline, a notoriously lossy strategy. Plus, setting it to zero is arguably worse than a null pointer here: at least referencing a null pointer will blow up your program. Accidentally using an id of zero will silently propagate.

One way to handle this would be to make the `id` field an option:

```scala
case class Turkey(id: Option[Int], name: String, age: Int)
```

This is arguably better, because now we've encoded the uncertainty into the type system. In practice, though, it's kind of a drag. In any given context, you the developer likely *knows* if there's an id on `Turkey`. If it's in your `createTurkey` endpoint, there's no `id`. If it's referenced in your `retrieveTurkey` endpoint, there's an `id`. Whenever you reference the field, you need to do some kind of gyrations to determine if there's a valid `id`, and you end up with cases which should probably never happen. 

```scala
case class Turkey(id: Option[Int], name: String, age: Int)

def createTurkey(turkey: Turkey): Unit = {
  turkey.id match {
    case Some(id) => ??? // this shouldn't happen
    case None => ...
  }
}
```

Another, more sophisticated way to handle it is to replace the type of `id` with a type parameter:

```scala
case class Turkey[T](id: T, name: String, age: Int)
```

Then you can use `Int` when you have an `id`, and `None` when you don't. You can even rename None to something more clarifying, like `NoId`:

```scala
case class Turkey[T](id: T, name: String, age: Int)

type NoId = Unit
val withoutId = Turkey[NoId]((), "Genghis Khan", 27)

val withId = Turkey[Int](1, "Genghis Khan", 27)

def createTurkey(turkey: Turkey[NoId]) = {
  //...
}

```

This is perhaps an improvement: No one can ever get confused about the content of the `id` field, and it keeps you from needing any special incantations to get at the `id` field.

It has downsides, though. For one, you've increased the complexity of the data structure by adding a type parameter. Another is that it's goofy to pass an empty value into the `id` field to instantiate a `Turkey` instance.

Another solution is to use a type constructor rather than a simple type parameter:

```scala
case class Turkey[F[_]](id: F[Int], name: String, age: Int)
```

Looking at this, it may not be at all clear how this helps anything. Maybe you shouldn't do it.

The very obvious solution I've neglected is just to use two data structures:

```scala
case class Turkey(id: Int, name: String, age: Int)
case class TurkeyNoId(name: String, age: Int)
```

This isn't terrible, but it fails to capture the existence of any relationship between the data structures. If later down the road you add a field to Turkey, you'll likely need to add that field to TurkeyNoId.


What would be really lovely, though, is if we could find a way to
add an `id` field to the `TurkeyNoId` type. Something like this:

```scala
case class TurkeyNoId(name: String, age: Int)

case class Turkey derived (id: Int :: TurkeyNoId.unpack)
```

which would hypothetically result in the class Turkey having three fields: `id`, `name`, and `int`.

It turns out that functional programming in general, and Scala in particular, actually do provide tools that almost let you achieve what I'm talking about here.

## Typelevel programming

Typelevel programming is harrowing in the best sort of way. If you haven't heard of it, that makes sense - it can
only exist in statically typed langauges, but also only in statically typed *functional* languages. It is in the same ballpark as macros and , but tends to have some performance benefits over macros.

Defined directly (and opaquely): Typelevel programming lets you do
computation against types instead of values, run at compile time. While that definition is titilating, it leaves far too much to the imagination.

The canonical example for showing the power of typelevel programming is to show how to automate serializing something, and I see no reason to depart from that.

Imagine you want to serialize our `Turkey` case class to a CSV, (defined again here for clarity). Using all of the Scala magic at our disposal, it would be great to be able to write something like this (much like the Circe library lets you do with JSON):

```scala
case class Turkey(id: Int, name: String, age: Int)

val turkey = Turkey(1, "Brown, Bunnkins", 24)

turkey.toCSV()
// 1,"Brown, Bunnikins",24: String
```

This is basically the height of programming language usability - Scala can automatically derive a correct function `toCSV` that will serialize your arbitrary data structure.

As a baby programmer, your first impulse have been to use string concatenation:

```scala
val turkey = Turkey(1, "Mr. Brown", 24)

val turkeySerialized = turkey.id.toString + "," + turkey.name + "," + turkey.age.toString
print(turkeySerialized)
// "1,Mr. Brown,24
```

For a number of reasons, this is not a great approach. Two reasons in particular: one shortcoming is that it isn't "safe" - if there is a comma in the name - e.g. `Brown, Bunnikins` that will break the CSV scheme. The other is that it's maximally specific - it lets you serialize exactly only this single instance of `Turkey`. if you want to do this again, you need to copy and paste the code. You could slap it into - `def csvSerializeTurkey: Turkey => String`, and then it isn't maximally specific, but it's still really, very specific. Typelevel programming lets you solve both of these reliably.

In Scala 2.x, typelevel programming is achieved using a library
called `shapeless`. The basic data structure in `shapeless` is this thing called an `hlist`.

A first approximation for an hlist is that it's just another way to represent a tuple.

```scala

val tpl = (1, "Brown, Bunnikins", 24)

// eliding crucial imports
val gen = Generic[(Int, String, Int)]
val hlist: Int :: String :: Int :: HNil = Generic.to(tpl)

println(hlist)
// 1 :: "Brown, Bunnikins" :: 24 :: HNil

```

If you want to access specific elements of the hlist, you can do it
similarly to accessing elements of a tuple:

```scala
println(tpl._3)
// 24

// eliding more crucial imports

println(hlist.get(_2))
// 24
```

Hlists are zero indexed, where are tuples 1 indexed, but you get the idea - you can convert between tuples and hlists, and access specific elements. Hlists, however, 1. have type signatures that are harder to read, and 2. substantially more flexible.

I've never met anyone who loved working with tuples, though, so let's try using hlists with our turkey case class instead.
```scala
val turkey = Turkey(1, "Brown, Bunnikins", 24)

// eliding crucial imports
val gen = Generic[Turkey]
val hlist: Int :: String :: Int :: HNil = Generic.to(Turkey)

println(hlist)
// 1 :: "Brown, Bunnikins" :: 24 :: HNil
```

The hlist produced is identical to our tuple example - int, string, int. 

Because hlists are lists, you can prepend elements in just the way you'd expect:

```scala
val turkeyHListNoId = TurkeyNoId("Brown, Bunnikins", 24)
val turkeyToHList = Generic[TurkeyHListNoId]

1 :: turkeyToHList(turkeyHListNoId)
// Int :: String :: Int :: Hlist
```

Ta-da! We've arrived at another solution to our uninstantiated ID problem - just add an ID to the hlist! Unfortunately this solution is embarrasingly clunky - for one, it doesn't have any field names, which are a pretty useful piece of information for the next programmer who comes across our code - or for ourselves, if we decide we want to do any computation with them.

So another definition for hlists is that they're a list of elements, which has a known length and known types. While this is a substantial improvement over trying to represent this stuff in a normal, scala list:

```scala
val regularList: List[Any] = List("Vital Stuff", 1, "Brown, Bunnikins", 24) 

val id: Any = List(1)
```

it's still only marginally useful. The magic lies not so much in hlists, but in all of the computation you can *do* on hlists. However, our goal here isn't to explore that in depth - it's to solve, once and for all, the uninstantiated ID problem.

Again, HLists fall short as a solution because they keep the types, but they lose the field names. 