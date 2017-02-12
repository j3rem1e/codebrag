package com.softwaremill.codebrag.dao.finders.dashboard

import com.softwaremill.codebrag.dao.branch.WatchedBranchesDao
import com.softwaremill.codebrag.dao.reaction.SQLReactionSchema
import com.softwaremill.codebrag.dao.commitinfo.SQLCommitInfoSchema
import com.softwaremill.codebrag.dao.sql.SQLDatabase
import com.softwaremill.codebrag.dao.user.UserDAO
import com.softwaremill.codebrag.dao.finders.views.DashboardCommitView
import com.softwaremill.codebrag.dao.reaction.CommitCommentDAO
import com.softwaremill.codebrag.dao.reaction.LikeDAO
import com.softwaremill.codebrag.dao.finders.views.DashboardView
import org.bson.types.ObjectId
import com.softwaremill.codebrag.domain.UserReaction
import com.softwaremill.codebrag.domain.Like
import com.softwaremill.codebrag.dao.finders.views.LikeView
import com.softwaremill.codebrag.dao.finders.views.CommentView
import com.softwaremill.codebrag.dao.followup.SQLFollowupSchema
import com.softwaremill.codebrag.dao.finders.views.DashboardCommentView

class SQLDashboardFinder(val database: SQLDatabase, userDAO: UserDAO, watchedBranchDAO: WatchedBranchesDao) extends DashboardFinder
  with SQLReactionSchema with SQLCommitInfoSchema with SQLFollowupSchema {
  
  import database.driver.simple._
  import database._
  
  def buildDashboard(filter: Option[Either[FilterByAuthor, FilterWatchedRepo]]) = db.withTransaction { implicit session =>

    val filteredComments = filter match {
      case None => comments

      case Some(Left(FilterByAuthor(author))) =>
        val user = userDAO.findById(author).getOrElse(throw new IllegalArgumentException(s"User %author not found"))

        val authorId = user.id
        val authorEmail = user.emailLowerCase

        val userCommits = commitInfos.filter(c => c.authorEmail === authorEmail || comments.filter(comment => comment.commitId === c.id && comment.authorId === authorId).exists )

        for {
          commit <- userCommits
          comment <- comments if comment.commitId === commit.id
        } yield comment


      case Some(Right(FilterWatchedRepo(userId))) =>
        val repos = watchedBranchDAO.findAll(userId).map(_.repoName)
        if (repos.isEmpty) {
          comments
        } else {
          for {
            commit <- commitInfos
            comment <- comments if comment.commitId === commit.id && (commit.repoName inSet repos)
          } yield comment
        }
    }

    val query = filteredComments.sortBy(_.postingTime.desc).take(130)

    val allcomments = query.list()
    val userDetails = userDAO.findPartialUserDetails(allcomments.map(_.authorId)).map(user => user.id -> user).toMap
    val commentsByCommit = allcomments.groupBy(_.commitId)
    val orderedCommentsByCommit = commentsByCommit.toSeq.sortWith(_._2.head.postingTime.getMillis > _._2.head.postingTime.getMillis)

    val commits = commitInfos.filter(_.id inSet commentsByCommit.keys).list().map(commit => commit.id -> commit).toMap
    val relatedFollowups = followups.filter(_.lastReactionId inSet allcomments.map(_.id)).list().groupBy(_.lastReactionId)
    
    DashboardView(orderedCommentsByCommit.map { 
      case (mcommitId, mcomment) => 
        val commitInfo = commits.get(mcommitId).getOrElse(throw new IllegalStateException(s"Could not find commit mcommitId"))
        
        DashboardCommitView(
          commitInfo.id.toString,
          commitInfo.sha,
          commitInfo.repoName,
          commitInfo.authorName,
          commitInfo.message,
          commitInfo.commitDate,
          mcomment.sortWith(_.postingTime.getMillis < _.postingTime.getMillis).map( c => {
            
            val user = userDetails.getOrElse(c.authorId, throw new IllegalStateException(s"Could not find user $c.authorId"))
            
            DashboardCommentView(
              c.id.toString,
              user.name,
              c.authorId.toString, 
              c.message, 
              c.postingTime,
              user.avatarUrl,
              relatedFollowups.contains(c.id)) 
          })
         ) 
      } toList)
    
    

  }

}