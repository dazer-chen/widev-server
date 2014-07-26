package models

import reactivemongo.bson.{BSONInteger, BSONHandler}

/**
 * Created by trupin on 7/26/14.
 */
trait GrandType {
  val value: Int
}
case object AuthorizationCodeGrandType extends GrandType { override val value = 0 }
case object ImplicitGrandType extends GrandType { override val value = 1 }
case object PasswordGrandType extends GrandType { override val value = 2 }
case object ClientCredentialsGrandType extends GrandType { override val value = 3 }

object GrandType {
  implicit object BSONRoleHandler extends BSONHandler[BSONInteger, GrandType] {
    override def read(bson: BSONInteger): GrandType = bson.value match {
      case AuthorizationCodeGrandType.value => AuthorizationCodeGrandType
      case ImplicitGrandType.value => ImplicitGrandType
      case PasswordGrandType.value => PasswordGrandType
      case ClientCredentialsGrandType.value => ClientCredentialsGrandType
    }
    override def write(t: GrandType): BSONInteger = BSONInteger(t match {
      case AuthorizationCodeGrandType => AuthorizationCodeGrandType.value
      case ImplicitGrandType => ImplicitGrandType.value
      case PasswordGrandType => PasswordGrandType.value
      case ClientCredentialsGrandType => ClientCredentialsGrandType.value
    })
  }
}