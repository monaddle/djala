# Validations with Idless

You wake up, spilling over with purpose. 

It came to you last night: you want to build a wanted board for dogs. People can post the name and the age of the dog they want to find, it will get saved into the database, and the universe will soon reward them by manifesting their intentions.

As a young wengineer, you were taught to always plan before you write code. No longer young, your tendency to plan has worn deep lines into your forehead. Your brow furrowed in thought, you move to your drafting table. You dip your pen into an inkwell and begin to write.
 
 The web application has a form with two fields: a dog's name, and their age. You also imagine a lovingly tended "submit" button.

Your keen technical mind reflexively traces the order of operations:

The aspiring dog owner fills out this form ("I want a dog named Hieronymus that is 7.24 Years old."), and clicks the button. The form contents are sent to a server, which parses the data. The server then saves it to a database. Confirmation is sent, and a 200 response is received. The cycle is complete. Inexorably, a life is improved.

This seems fine - you thought of it, after all - but you notice yourself frowning. You realize that this application is an idealization, a beautiful dream, but as a hoary software crone, you know that beautiful dreams are not meant for you. On a still day, you might visit a beautiful dream to prune the grounds, or perhaps till a row of fresh-bloomed daisies, but you can never stay. Your life is in a cottage on the edge of the dark forest, and you know that in each element of your simple form lurks modes of failure, myriad cases to be cornered and dealt with. In the dim light of your writing lamp, you shiver. A front is approaching. You hear the braying of hounds. 

You pause for a moment to review your work:
 
```scala
case class Dog(name: String, age: Int)

object Server {
  def saveDog(dogRequest: Request): Response = {
    val form = dogRequest.body.as[Form]
    
    val name: String = (form \ "name").as[String]
    val age: Int = (form \ "int").as[Int]
    
    val dog = Dog(name, age)
    db.saveDog(dog)
     Ok()
  }
  
  def showForm(request: Request): Response = {
    return
      Ok("""<html>
        |<body>
        |<form>
        |<input name="name"/>
        |<input name="age"/>
        |<submit>
        |</form>
        |</body>
        |</html>""".stripMargin)
  }
}
```

Before you, you see a rendering that satisfices. You feel pleased. You almost stand up and congratulate yourself for a job well done, but you pause. Doubt begins to creep in. Could you have missed something? You begin to talk yourself through it:

"It shows a form, lets users submit the form, then it saves the dog. It looks good. But... Hmm. Submitting HTML forms isn't really a best practice. Maybe  I should switch to JSON? The website is a little spartan, perhaps, but a reasonable person would just check the HTML to find out what the inputs mean." You wonder if this is true. "I think the real problem here is that someone could put in a negative age for a dog. Or, perish the thought, request a dog with *no name*." You shiver again, this time not from the cold.

Hurriedly, you cross out your `saveDog` function. You know better than to leave that kind of vulnerability in a spell, even for a moment. Not after last time.

You think back to the validation libraries you know. 