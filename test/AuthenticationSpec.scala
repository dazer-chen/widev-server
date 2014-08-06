/**
 * Created by gaetansenn on 02/08/2014.
 */

import controllers.AuthConfigImpl
import org.specs2.mutable.Specification

class AuthenticationSpec extends Specification {

  object config extends AuthConfigImpl

  implicit val timeout = scala.concurrent.duration.Duration.apply(1000, "milliseconds")

}