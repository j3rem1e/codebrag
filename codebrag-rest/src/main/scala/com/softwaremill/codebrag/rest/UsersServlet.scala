package com.softwaremill.codebrag.rest

import com.softwaremill.codebrag.service.user.{Authenticator, RegisterService}
import com.softwaremill.codebrag.service.config.CodebragConfig
import org.bson.types.ObjectId
import org.scalatra
import com.softwaremill.codebrag.finders.user.UserFinder
import com.softwaremill.codebrag.finders.user.ManagedUsersListView
import com.softwaremill.codebrag.usecases.user.{ModifyUserDetailsForm, ModifyUserDetailsUseCase, RegisterNewUserUseCase, UpdateUserFullNameUseCase}

class UsersServlet(
                    val authenticator: Authenticator,
                    userFinder: UserFinder,
                    modifyUserUseCase: ModifyUserDetailsUseCase,
                    updateUserFullNameUseCase: UpdateUserFullNameUseCase,
                    config: CodebragConfig) extends JsonServletWithAuthentication {

  get("/") {
    haltIfNotAuthenticated()
    if(!config.demo) {
      userFinder.findAllAsManagedUsers()
    } else {
      ManagedUsersListView(List.empty)
    }
  }

  put("/:userId") {
    haltIfNotAuthenticated()
    val targetUserId = new ObjectId(params("userId"))
    val newPassOpt = extractOpt[String]("newPass")
    val adminOpt = extractOpt[Boolean]("admin")
    val activeOpt = extractOpt[Boolean]("active")
    modifyUserUseCase.execute(user.id, ModifyUserDetailsForm(targetUserId, newPassOpt, adminOpt, activeOpt)) match {
      case Left(errors) => scalatra.BadRequest(errors)
      case _ => scalatra.Ok()
    }
  }

  put("/:userId/fullname") {
    haltIfNotCurrentUser()

    val name = extractOpt[String]("fullname").getOrElse(throw new IllegalArgumentException("parameters fullname is mandatory"))
    updateUserFullNameUseCase.execute(user.id, name) match {
      case Left(errors) => scalatra.BadRequest(errors)
      case _ => scalatra.Ok()
    }
  }

  private def haltIfNotCurrentUser() = {
    haltIfNotAuthenticated()
    haltWithForbiddenIf(new ObjectId(params("userId")) != user.id)
  }

}

object UsersServlet {
  val MappingPath = "users"
}
