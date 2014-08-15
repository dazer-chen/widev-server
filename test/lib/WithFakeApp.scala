package lib

import play.api.test.{FakeApplication, WithApplication}

/**
 * Created by trupin on 8/15/14.
 */
case class WithFakeApp() extends WithApplication(FakeApplication(withoutPlugins = Seq("play.modules.reactivemongo.ReactiveMongoPlugin"))) {}
