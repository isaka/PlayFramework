/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.streams

import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.reflectiveCalls
import scala.util.Try

import org.specs2.mutable._

class ExecutionSpec extends Specification {
  import Execution.trampoline

  val waitTime = Duration(5, SECONDS)

  "trampoline" should {
    "execute code in the same thread" in {
      val f = Future(Thread.currentThread())(using trampoline)
      Await.result(f, waitTime) must equalTo(Thread.currentThread())
    }

    "not overflow the stack" in {
      def executeRecursively(ec: ExecutionContext, times: Int): Unit = {
        if (times > 0) {
          ec.execute(() => executeRecursively(ec, times - 1))
        }
      }

      // Work out how deep to go to cause an overflow
      val overflowingExecutionContext = new ExecutionContext {
        def execute(runnable: Runnable): Unit = {
          runnable.run()
        }
        def reportFailure(t: Throwable): Unit = t.printStackTrace()
      }

      var overflowTimes = 1 << 10
      try {
        while (overflowTimes > 0) {
          executeRecursively(overflowingExecutionContext, overflowTimes)
          overflowTimes = overflowTimes << 1
        }
        sys.error("Can't get the stack to overflow")
      } catch {
        case _: StackOverflowError => ()
      }

      // Now verify that we don't overflow
      Try(executeRecursively(trampoline, overflowTimes)) must beSuccessfulTry[Unit]
    }

    "execute code in the order it was submitted" in {
      val runRecord = scala.collection.mutable.Buffer.empty[Int]
      case class TestRunnable(id: Int, children: Runnable*) extends Runnable {
        def run(): Unit = {
          runRecord += id
          for (c <- children) trampoline.execute(c)
        }
      }

      trampoline.execute(
        TestRunnable(
          0,
          TestRunnable(1),
          TestRunnable(2, TestRunnable(4, TestRunnable(6), TestRunnable(7)), TestRunnable(5, TestRunnable(8))),
          TestRunnable(3)
        )
      )

      runRecord must equalTo(0 to 8)
    }
  }
}
