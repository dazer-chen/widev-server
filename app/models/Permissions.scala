package models

import reactivemongo.bson.{BSONInteger, BSONString, BSONHandler}

/**
 * Created by gaetansenn on 01/08/2014.
 */

sealed trait Permission { val value: Int }
case object Administrator extends Permission { val value = 0 }
case object Authenticated extends Permission { val value = 1 }
case object Visitor extends Permission { val value = 2 }
case class PermissionHandlerError(message: String) extends Exception(message)

object Permission {
  implicit val PermissionBSONHandler = new BSONHandler[BSONInteger, Permission] {
    def read(str: BSONInteger) = str.value match {
      case Administrator.value => Administrator
      case Authenticated.value => Authenticated
      case Visitor.value => Visitor
      case x => throw PermissionHandlerError(s"Unknown Permission value: '$x'")
    }
    def write(x: Permission) = x match {
      case Administrator => BSONInteger(Administrator.value)
      case Authenticated => BSONInteger(Authenticated.value)
      case Visitor => BSONInteger(Visitor.value)
      case _ => throw PermissionHandlerError(s"Unknown Permission Type: '${x.toString}'")
    }
  }
}