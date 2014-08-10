package models

import lib.mongo.Mongo
import org.specs2.mutable._

/**
 * Created by trupin on 8/10/14.
 */
class ClientSpec extends Specification with Mongo with lib.Util {

  sequential

  val clients = factory.clients

  "Clients" should {
    "relations" >> {
      clients.relations should beEmpty
    }

    ".validate" >> {
      val client = clients.generate
      result(clients.create(client))
      result(clients.validate(client._id, client.secret.get, client.grantTypes(0))) should beTrue
      result(clients.validate(client._id, client.secret.get, client.grantTypes(1))) should beTrue
      result(clients.validate(client._id, client.secret.get, PasswordGrandType)) should beFalse
    }

    ".find" >> {
      val client = clients.generate
      result(clients.create(client))
      "with id, secret and scope" >> {
        result(clients.find(client._id, client.secret.get, client.scope)) should equalTo(Some(client))
      }
    }
  }
}
