package com.softwaremill.codebrag.dao.finders.dashboard

import com.softwaremill.codebrag.dao.finders.views.DashboardView

trait DashboardFinder {
  
  def buildDashboard(): DashboardView
}