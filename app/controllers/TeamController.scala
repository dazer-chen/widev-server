package controllers

import scala.concurrent.Future

import jp.t2v.lab.play2.auth.AuthElement
import lib.mongo.DuplicateModel
import models._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{BodyParsers, Controller}
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

/**
 * Created by benjamincanac on 23/10/2014.
 */
class TeamController(teams: Teams) extends Controller with AuthElement {
	self: AuthConfigImpl =>

	def getTeam(id: String) = AsyncStack(AuthorityKey -> Standard) {
		request =>
			teams.find(BSONObjectID(id)).map {
				case Some(team) => Ok(Json.toJson(team))
				case None => NotFound(s"Couldn't find team for id: $id")
			}
	}

	def getTeams = AsyncStack(AuthorityKey -> Standard) {
		implicit request =>
			val user = loggedIn
			teams.findByOwner(user._id).map(team => Ok(Json.toJson(team)))
	}

  def addUser(id: String) = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      val user = (request.body \ "user").validate[String]

      user.fold(
        errors => {
          Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        user => {
          teams.addUser(BSONObjectID.apply(id), BSONObjectID.apply(user)).map {
            case true => Ok("")
            case false => BadRequest("")
          }
        }
      )
  }

  def removeUser(id: String) = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      val user = (request.body \ "user").validate[String]

      user.fold(
        errors => {
          Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        user => {
          teams.removeUser(BSONObjectID.apply(id), BSONObjectID.apply(user)).map {
            case true => Ok("")
            case false => BadRequest("")
          }
        }
      )
  }

	def createTeam = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
		implicit request =>
			case class createTeam(name: String, users: Set[String])

			implicit val createTeamReads: Reads[createTeam] = (
				(JsPath \ "name").read[String] and
					(JsPath \ "users").read[Set[String]]
				)(createTeam.apply _)

			val team = request.body.validate[createTeam]

			team.fold(
				errors => {
					Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
				},
				team => {
					val user = loggedIn

					teams.create(Team(team.name, user._id, team.users.map(BSONObjectID(_)))).map {
						team => Ok(Json.toJson(team))
					} recover {
						case err: DuplicateModel =>
							NotAcceptable(s"Team already exists.")
					}
				}
			)

//			val teamName = request.getQueryString("name")
//			var users = request.getQueryString("users")
//			val user = loggedIn
//
//			if (teamName.isEmpty) {
//				Future(BadRequest(s"'name' parameter required."))
//			}
//			else {
//				teams.create(Team(teamName.get, user._id, Seq())).map {
//					bucket =>
//						Ok(Json.toJson(bucket))
//				} recover {
//					case err: DuplicateModel =>
//						NotAcceptable(s"User already exists.")
//				}
//			}
	}
}

object TeamController extends TeamController(Teams(ReactiveMongoPlugin.db)) with AuthConfigImpl