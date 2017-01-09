package com.softwaremill.codebrag.dao.finders.views

import org.joda.time.DateTime


case class DashboardView(eventCommitsView:List[DashboardCommitView])

case class DashboardCommitView(commitId: String, sha: String, repoName: String, authorName: String, message: String, date: DateTime, reactions: List[CommentView])

