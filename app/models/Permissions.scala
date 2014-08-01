package models

import reactivemongo.bson.{BSONString, BSONHandler}

/**
 * Created by gaetansenn on 01/08/2014.
 */

sealed trait Permission
case object Administrator extends Permission
case object NormalUser extends Permission
case class PermissionHandlerError(message: String) extends Exception(message)

object Permission {
  implicit val PermissionHandler = new BSONHandler[BSONString, Permission] {
    def read(str: BSONString) = str.value match {
      case "Administrator" ⇒ Administrator
      case "NormalUser"  ⇒ NormalUser
      case x      ⇒ throw PermissionHandlerError(x)
    }
    def write(x: Permission) = x match {
      case Administrator => BSONString("Administrator")
      case NormalUser => BSONString("NormalUser")
      case _ => throw PermissionHandlerError("Unknown Permission Type : %s".format(x.toString))
    }
  }
}