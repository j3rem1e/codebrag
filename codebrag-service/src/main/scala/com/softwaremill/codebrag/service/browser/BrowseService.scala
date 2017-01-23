package com.softwaremill.codebrag.service.browser

import com.softwaremill.codebrag.cache.RepositoriesCache
import com.softwaremill.codebrag.service.commits.jgit.JgitBlameLoader
import com.softwaremill.codebrag.service.commits.jgit.JgitBlameLoader
import com.softwaremill.codebrag.dao.finders.reaction.ReactionFinder
import com.softwaremill.codebrag.service.commits.BlameLineInfo
import org.joda.time.DateTime
import com.softwaremill.codebrag.dao.commitinfo.CommitInfoDAO
import com.softwaremill.codebrag.dao.finders.views.CommitReactionsView
import com.softwaremill.codebrag.dao.finders.views.ReactionsView
import com.softwaremill.codebrag.service.diff.DiffService
import com.softwaremill.codebrag.domain.CommitFileDiff
import com.softwaremill.codebrag.domain.DiffLine
import scala.collection.mutable.HashMap
import org.bson.types.ObjectId
import com.softwaremill.codebrag.domain.User
import com.softwaremill.codebrag.domain.UserSettings

class BrowseService(repoCache: RepositoriesCache, commitInfoDAO: CommitInfoDAO, reactionFinder: ReactionFinder, diffService: DiffService) extends JgitBlameLoader {
  
  def loadFile(sha: String, file: String, repoName: String): Option[FileInfo] = {
    val repository = repoCache.getRepo(repoName).repository
    val blameResult = loadBlame(sha, file, repository)
    if (blameResult.isEmpty) {
      return None
    }
    val blame = blameResult.get
    val linesWithIndex = blame.lines.zipWithIndex
       
    val shaDiffs = for ((sha, lines) <- blame.lines.groupBy(_.sha)) yield { (sha, diffService.getDiffForFile(repoName, sha, lines.head.path)) }
    
    // All referenced commits
    val commitsInfo = commitInfoDAO.findByShaList(repoName, shaDiffs.keySet.toList)
      .map(c => FileCommitInfo(c.id, c.sha, c.message, c.authorName, c.authorEmail, UserSettings.defaultAvatarUrl(c.authorEmail), c.date))
        
    // All reactions
    val reactions = new HashMap[String, ReactionsView]
    reactionFinder.findReactionsForFileInCommits(file, commitsInfo.map(_.id)).foreach { case (commitId, Some(comments)) => 
        val sha = commitsInfo.find(_.id == commitId).get.sha
        val relatedDiff = shaDiffs.get(sha).get
        for ((diffNumber, threadReactions) <- comments) {
            reactions.put(translateDiffNumberToLineNumber(diffNumber.toInt, sha, relatedDiff, linesWithIndex).toString(), threadReactions)
        }
     };
    
    Some(FileInfo(linesWithIndex.map { 
        case (BlameLineInfo(line, sha, path, lineNumber), number) => LineInfo(line, translateLineNumberToDiffNumber(lineNumber + 1, shaDiffs.getOrElse(sha, List())), sha) 
      }, commitsInfo, reactions.toMap))
  }
  
  private def translateLineNumberToDiffNumber(lineNumber: Int, lines: List[DiffLine]): Int = 
    lines.indexWhere(line => line.lineNumberChanged == lineNumber && line.lineType == "added")
    
 private def translateDiffNumberToLineNumber(diffNumber: Int, sha: String, lines: List[DiffLine], blameLines: List[(BlameLineInfo, Int)]): Int = {
    if (diffNumber < 0 || diffNumber > lines.length) {
      return -1
    } else {
      val lineNumber = lines(diffNumber).lineNumberChanged
            
      val associatedBlameLine = blameLines.find { case (blameLine, index) => blameLine.lineNumber == lineNumber - 1 && blameLine.sha == sha }
      
      System.out.println("diffNumber=" + diffNumber + " associated line number=" + lineNumber + " found blame=" + associatedBlameLine);
      
      return if (associatedBlameLine.isDefined) (associatedBlameLine.get._2) else -1
    }
  }
  
}

case class FileInfo(lines: List[LineInfo], commits: List[FileCommitInfo], reactions: Map[String, ReactionsView])

case class LineInfo(line: String, diffLineNumber:Int, sha: String)

case class FileCommitInfo(id: ObjectId, sha: String, message: String, authorName: String, authorEmail: String, authorAvatarUrl: String, date: DateTime)