package com.softwaremill.codebrag.dao.finders.dashboard

import com.softwaremill.codebrag.dao.finders.views.DashboardView
import org.bson.types.ObjectId

trait DashboardFinder {
  
  def buildDashboard(filter: Option[Either[FilterByAuthor, FilterWatchedRepo]]): DashboardView

}

case class FilterByAuthor(authorId: ObjectId)

case class FilterWatchedRepo(userId: ObjectId)


