package us.pinguo.bigdata

import akka.actor.{Actor, ActorSystem}
import us.pinguo.bigdata.dataplus.DataPlusUtil


abstract class DataPlusActor extends Actor with DataPlusUtil {

  private implicit val system = ActorSystem("dataPlus")

}
