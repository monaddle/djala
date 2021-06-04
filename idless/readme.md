KLists are heterogenous lists, but where every element of the heterogenous list is is of type K[_], where K is some type constructor.

```scala
// this is an Hlist of type Int :: String :: HNil
1 :: "Hello" :: HNil

// This is a KList of type Option[Int] :: Option[String] :: HNil
Some(1) :: Some("Hello") :: HNil
```

