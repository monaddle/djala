//// To execute Scala code, please define an object named Solution that extends App
//
//object Solution extends App {
//
//  final case class Process(
//                            projectId: String,
//                            name: String
//                          )
//
//  val processes: Seq[Process] = Seq(
//    Process("projectA", "a-1"), // 0
//    Process("projectA", "a-2"), // 1
//    Process("projectB", "b-1"), // 2
//    Process("projectA", "a-3"), // 3
//    Process("projectC", "c-1"), // 4
//    Process("projectA", "a-4")  // 5
//  )
//
//  /**
//    * @return Processes not in the same project that are occurring within current and previous instance of projectId
//    **/
//  def getProcessesAhead(processes: Seq[Process], projectId: String): Map[String, Int] =  {
//    val processesOfInterest =
//      processes.zipWithIndex
//        .filter(_._1.projectId == projectId)
//
//    val intermediateResult = {
//      processesOfInterest
//        .map { case (poi0, i0) =>
//          val res: Seq[(Process, Int)] =
//            processesOfInterest
//              .filterNot(_._2 == i0)
//              .map { case (poi1, i1) =>
//                (poi1, (i0 - i1).abs - 1)
//              }
//          val min = getProcessWithMinDistance(res)
//          min
//        }
//    }
//      intermediateResult.map { case ((process, distance), _) =>
//        (solution)
//      }
//  }
//
//  def getProcessWithMinDistance(processes: Seq[(Process, Int)]): ((Process, Int), Int) = {
//    processes
//      .zipWithIndex
//      .sortBy(_._2)
//      .head
//  }
//
//
////assert(getProcessesAhead(processes, "projectA") == Map("a-1" -> 0, "a-2" -> 0, "a-3" -> 1, "a-4" -> 1))
////assert(getProcessesAhead(processes, "projectB") == Map("b-1" -> 2))
////assert(getProcessesAhead(processes, "projectC") == Map("c-1" -> 4))
//}

