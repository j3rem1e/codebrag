package com.softwaremill.codebrag.rest

import com.softwaremill.codebrag.service.user.Authenticator
import org.scalatra.json.JacksonJsonSupport
import org.bson.types.ObjectId
import org.scalatra.NotFound
import com.softwaremill.codebrag.dao.finders.followup.FollowupFinder
import com.softwaremill.codebrag.dao.finders.views.SingleFollowupView
import com.softwaremill.codebrag.usecases.reactions.FollowupDoneUseCase
import com.softwaremill.codebrag.dao.finders.dashboard.{DashboardFinder, FilterByAuthor, FilterWatchedRepo}

class DashboardServlet(val authenticator: Authenticator,
                       dashboardFinder: DashboardFinder)
  extends JsonServletWithAuthentication with JacksonJsonSupport {

  get("/") {
    haltIfNotAuthenticated()

    val filter = params.get("user") match {
      case None => params.get("watched") match {
        case None => None
        case Some(_) => Some(Right(FilterWatchedRepo(user.id)))
      }
      case Some(f) => Some(Left(FilterByAuthor(user.id)))
    }
    dashboardFinder.buildDashboard(filter)
  }

  
}

object DashboardServlet {
  val MappingPath = "dashboard"
}