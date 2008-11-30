package scalanlp.stats;

import scalanlp.counters.ints.Int2DoubleCounter;
import scalanlp.counters._;
import scalanlp.counters.Counters._;

/**
* Models the CRP over the non-negative integers. 
*
* @param theta the prior probability of a new class
* @param alpha the amount of discounting to current draws.
*
* @author dlwh
*/
class PitmanYorProcess(val theta: Double, val alpha:Double) extends Distribution[Int] {
  def this(theta: Double) = this(theta,0.0);

  assert( (alpha < 0 && theta % alpha == 0.0) || (0 <= alpha && alpha <= 1.0 && theta > -alpha));
  val drawn = new Int2DoubleCounter.FastMapCounter();
  drawn += (-1 -> theta);

  private var c = -1; 
  def numClasses = c;

  override def get() = getWithCounter(drawn);

  private def getWithCounter(cn : DoubleCounter[Int]) = {
     val d : Int = Multinomial(cn).get match {
      case -1 => c += 1; c;
      case x => x;
    }

    if(drawn.get(d) == None)
      drawn.incrementCount(d,1.0-alpha);
    else
      drawn.incrementCount(d,1.0);

    drawn.incrementCount(-1,alpha);
    d   
  }
  
  def probabilityOf(e: Int) = {
    if (e >= 0 && drawn.get(e) != None) {
      drawn(e) / drawn.total;
    } else {
      0.0;
    }
  }

  def probabilityOfUnobserved() = drawn(-1) / drawn.total;

  def observe(c: IntCounter[Int]) {
    for( (k,v) <- c) {
      if(k < 0) throw new IllegalArgumentException(k + " is not a valid draw from the PitmanYorProcess");

      observeOne(k,v);
    }
  }

  private def observeOne(k : Int, v: Int) {
    val newCount = drawn(k) + v;

    if (Math.abs(newCount - v) <= 1E-4) {
      drawn.incrementCount(k,newCount - alpha);
      drawn.incrementCount(-1,-alpha);
    } else if( newCount + alpha <= 1E-4) {
      drawn -= k;
      drawn.incrementCount(-1,v * alpha);
    } else {
      drawn(k) = newCount;
    }
  }

  def observe(t : Int*) { observe(count(t))}
  def unobserve(t: Int*) {
    val c = count(t);
    c.transform { (k,v) => v * -1};
    observe(c);
  }

  def withLikelihood(p : Option[Int]=>Double) = new Rand[Int] {
    def get = {
      val c2 = Int2DoubleCounter();
      for( (k,v) <- drawn) {
        c2.incrementCount(k, v * p(Some(k) filter(_!= -1)));
      }
      getWithCounter(c2);
    }
  }

  override def toString() = {
    "PY(" + alpha + "," + theta + ")";
  }

  def debugString() = {
    val str = drawn.elements.filter(_._1 != -1).map(kv => kv._1 + " -> " + kv._2).mkString("draws = (", ", ", ")");
    toString + "\n{newClass=" + drawn(-1) + ", " + str + "}";
  }

}