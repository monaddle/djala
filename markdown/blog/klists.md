# Metta Programming in Scala, Part 0: Motivations
##### TL;DR: I'm exploring highfalutin functional programming techniques by building an approximation of the Django framework in Scala. This will primarily be achieved through via shapeless and KLists, but will also end up exploring parser combinators, recursion schemes, and macros, such as they are in Scala.

The Django framework written in Python is my favorite framework for doing rapid web development. Django and the surrounding ecosystem supply some fabulously useful tools, empowered by using reflection.

The workhorse of Django is their simple `model` abstraction. Let's say you want to record and display a dataset of `Dog` objects. You'd define your model like this:

```python
from django import models

class Dog(models.Model):
    id = models.PrimaryKeyField()
    name = models.CharField(max_length=200)
    age = models.IntField(min=0)
```

Through the power of meta programming, they've crammed a terrific number of affordances into this compact code. From the command line, Django can treat this as a data description language, and convert it into a table structure using approximately this command:

```bash
python manage.py sqlall 
```
this generates approximately

```sql
create table dog (
  id integer primary key, 
  name character varying (200) not null,
  age integer not null
)
```


Unsurprisingly, django also gives you, for free, basic Create/Retrieve/Update/Delete tools. But wait! There's more. When you retrieve a database record, it gives you the most intuitive possible way to access elements from that record:

```python 
myDog = Dog.objects.get(id=1)
print(quincy.name)
# "Quincy"
print(type(quincy.name))
# <type 'str'>
```

Now, note that in our definition of our `Dog` class, we added a name field, but it was not of type `string` - it was a `models.CharField`. What Django is doing here is changing the type of the field based on the context that you're in. It treats the fields on the class as fields on a table (and serializes it as SQL) when you're querying the database, but it turns it into a plain-jane value when you have actual values to work with. This is profoundly useful.

If you like, take a moment to consider what other useful things a person could generate. 

## One Truth to Rule The All

##### Serialization
When working with applications that deal with data from multiple sources, a perennial design concern is making sure that information only has one source of truth. Let's say you have a dataset with tens of millions of records in a PostgreSQL database, and you want to do a bunch of aggregation queries that will be 100x more efficient if the data is stored in a columnar data store instead. 

A reasonable strategy would be to decide that your PostgreSQL database is the one source of truth, and then write a bunch of queries that update your columnar data store, and run them nightly. Later, you might discover that one of the queries inadvertently mangled the data, but it's no big deal, because you know what the one source of truth is. 

A terrible strategy would be to keep both databases up to date in the application layer, and run queries against either database. If there's ever a disagreement between the databases, the fact that you don't have a single source of truth means that you the potentially very costly proposition of needing to rectify these two datasets.

The benefits of having one source of truth carry over pretty directly from data to data models, and really, that's what Django's `models.Model` begins to achieve - it lets you have a single source of truth for your data model, and reuse it in different contexts.

Some other wicked cool stuff that Django lets you do, using this "one source of truth" strategy:

You can generate a `Form` from your model:
```python
from django import forms
from models import Dog

class DogForm(forms.ModelForm):
    class Meta:
        model = Dog
```

which can then be used to generate form HTML:
```python
# a command approximately like this
print(DogForm().to_html())
# yields approximately
# <form> 
#   <input id="id" name="id"/>
#   <input id="name" name="name"/>
#   <input id="age" name="age"/>
#   <submit>
# </form>
```

Using the model to generate SQL and generate HTML are both a type of serialization. SQL and HTML are common ways programmers want to serialize data structures, but there are certainly others - JSON and XML are also ubiquitous. Somewhat more exotic (but quite useful where performance is a concern) are binary serialization formats such as Protocol Buffers and BSON.

You can think of serialization as a function like `models.Model => String` (pronounced in English as "a function from a models.Model type to String"), or in the case of binary serialization, `models.Model => Array[Byte]`. 

Unsurprisingly, because Django is awesome, it lets you derive other arbitrary serialization functions using the Django Rest Framework package. It looks quite similar to the form example:

```python
class DogSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Dog
        
quincy = Dog(id=1, name="Quincle", age=73)

# Approximately this command will generate JSON
print(DogSerializer(quincy).as_json())
# {id: 1, name: "Quincle", age: 73}

# and approximately this command will parse JSON
DogSerializer().from_string(jsonString)
# Dog(id=1, name="Quincle", age=73)
```

##### Stacks on Stacks

In addition to some straightforward, flexible serialization, django will also let you generate functions that will handle GET/POST/etc requests based on your model. If you wanted to e.g. have a webpage that showed a list of dogs, you could simply define:

```python
from django.views.generic import ListView
from models import Dog

class DogList(ListView):
    model = Dog
```

and then wire it up to your URL routes like so:

```python
from django.urls import path
from animals.views import DogList

urlpatterns = [
    path('dogs/', DogList.as_view()),
]
```

This generates a url route that shows all of the dogs in your Dog table - it queries for the dogs, serializes them, and handles returning the http request. It begins stacking up these various uses for the data model.

In typical form, Django takes this even further and lets you generate an entire admin site complete with CRUD and relatively granular access control:

```python
# admin.py
from models import Dog
from django.contrib import admin

admin.site.register(Dog)
```

This is powerful stuff. It's powered tens of thousands of websites, helped people make gads of money, and added real value to the world. 


## This Day All Gods Die

Unfortunately, Django also has substantial limitations. Overwhelmingly, these limitations are from well considered design decisions - trade-offs that make it excel in some places, and impose significant penalties in others.

In practice, the way these tradeoffs play out that you pay an increase in complexity and development time as your application and/or development team gets larger. The codebase gets large enough that no one is familiar with all of it, and the lack of type signatures turns writing new code into an exercise in printing out variables. Django's design encourages [monkey patching](https://en.wikipedia.org/wiki/Monkey_patch), which is about as sustainable a development paradigm as an economy based on exploiting fossil fuels. Django glues things together using "magic", and when things go wrong in a slightly sophisticated way, running them down feels interminable. The codebase isn't *discoverable*. If you change your data model and don't successfully every reference in your application, you discover this at runtime. Because of the monkey patching problem and the lack of type signatures, trying to understand behavior by reading source code is a continual exercise in confusion: you basically always need to run code to understand it. Given that programming languages exist entirely to describe programs in terms humans can understand, there's a certain unwelcome irony to programs whose meaning can't be understood by reading them.

My first programming job was in a python shop, writing Django. I was studying geography, and had limited exposure to software or software development. I remember reading about the "language wars", and experiencing them vicariously through reading Hacker News/reddit/wherever I saw programmers congregating online. They felt unhelpful and basically religious, as though people were being guided solely by tribal associations and personal familiarity, and not objective truth. Believing in a "better" or "worse" programming language was just a trick people played on themselves to protect their psyche from the uncertainty and arbitrary cruelty of an unfeeling universe. Plus, I'd only been using python and javascript for 6 months, and previously done a smattering of C++ in high school. As arrogant as I was at 21, it was still clear to me I didn't know enough to have an informed perspective. To quote Hamlet, that old font of wisdom: "... there is nothing either good or bad, but thinking makes it so."

So then, at the time, I took what I believed was the "enlightened" position - programming languages are tools, and they all seem fit for various purposes - no better, no worse, just different. (My confidence in my own enlightenment was only briefly challenged when I was assigned a project where I had to write a website using cold fusion.)

A decade and a bunch of programming languages later, the steady erosion caused by time and experience have worn away at my cynicism, my agnosticism, my resolute moral relativism. I think to myself, "there is good and bad", and thus it is so. You might say I found a degree of religion. And have you heard the good news? Dynamic languages are cool, but static typing is *awesome*.

## Slick

After working in that first python shop, I bopped around and worked in a variety of other shops - PHP, Python, C#, and used a variety of other frameworks, and they all came up short. Django had my heart. In my spare time, I tried a little bit Ruby on Rails, and it seems seems comparable to Django - by which i mean it seems quite nice. But Ruby syntax kind of gave me a headache, and learning another language adjacent to Python just didn't hold a lot of appeal. (At the tail end of a contract with a super PAC, I was given a task where I was supposed to debug and improve on some code Aaron Schwartz had written for A/B testing campaign emails. Between the burnout from all of "our" candidates losing, and my unfamiliarity with the language, the experience gave me flashbacks to debugging monkey patched python code, i.e. it was basically an unending howl of confusion.)

Eventually I landed at an *innovation* lab at a behemoth financial services firm, where we were doing unspecified "data science". After wasting two weeks fucking around with Spring boot, we collectively decided to *innovate* ourselves into being a Scala shop. Through Scala, I fell in love with functional programming, but it always felt like something was missing. Django. Django is what was missing.

Then one day I discovered Slick, and had my mind blown - *it's like Django, but for Scala!*, I thought.

It didn't take me very long to figure out that Slick is not a lot like Django, but it's pretty danged cool, and it's the closest thing I've seen in a statically typed language that scratches the Django itch without committing sins against the gods of functional programming.
 
Like Django, Slick tries to help you have one source of truth for your data model. Instead of an instance of `models.Model`, however, Slick has something called a `Table`.

```scala
case class Dog(id: Option[Int], name: String, age: Int)

class Dogs(tag: Tag) extends Table[Dog](tag, "dogs") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def age = column[Int]("age")
  def * = (id.?, first, last) <> (Dog.tupled, Dog.unapply)
}
val dogs = TableQuery[Dogs]
```

There's a little more going on here than in our Django definition. Let's break it down:

```scala
case class Dog(id: Option[Int], name: String, age: Int)
```
Th
If you want to turn this TableQuery into a SQL string, it's as simple as 
```scala
println(dogs.schema.createStatements.mkString)
// "create table dog (
//      id integer primary key, 
//      name character varying (200) not null,
//      age integer not null
//    )"
```

