package com.softwaremill.codebrag.service.commits.jgit

import com.softwaremill.codebrag.service.commits.BlameLoader
import org.eclipse.jgit.api.BlameCommand
import com.softwaremill.codebrag.repository.{Repository => CBRepo}
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import org.fusesource.scalate.util.IOUtil
import scala.io.Source
import scala.collection.mutable.HashMap
import com.softwaremill.codebrag.domain.CommitInfo
import org.joda.time.DateTime
import com.softwaremill.codebrag.service.commits.BlameLineInfo
import com.softwaremill.codebrag.service.commits.BlameInfo

class JgitBlameLoader extends BlameLoader {
  
  def loadBlame(sha: String, name: String, repository: CBRepo): Option[BlameInfo] = {
    
    val blamer = new BlameCommand(repository.repo)
    val commitID = repository.repo.resolve(sha);
    
    blamer.setStartCommit(commitID);
    blamer.setFilePath(name);
    val blame = blamer.call();
    
    try {
      val files = getLines(repository.repo, commitID, name)
      
      Some(BlameInfo(files.zipWithIndex.map { 
        case (line,index) => {
          val commit = blame.getSourceCommit(index)
          BlameLineInfo(line, blame.getSourceCommit(index).getName, blame.getSourcePath(index), blame.getSourceLine(index))
        }
      }))
    } catch {
      case e: Exception => None
    }
  }
  
  private def getLines(repository: Repository, commitId: ObjectId, name: String): List[String] = {
    val revWalk = new RevWalk(repository)
    try {
      val commit = revWalk.parseCommit(commitId)
      val tree = commit.getTree()
      
      System.out.print("Having tree:" + tree)
      
      val treeWalk = new TreeWalk(repository)
      try {
        treeWalk.addTree(tree)
        treeWalk.setRecursive(true)
        treeWalk.setFilter(PathFilter.create(name))
        if (!treeWalk.next()) {
          throw new IllegalStateException(s"Did not find expected file $name")
        }
        
        val objectId = treeWalk.getObjectId(0)
        val loader = repository.open(objectId)
        
        val stream = new ByteArrayOutputStream()
        loader.copyTo(stream)
        
        revWalk.dispose()
        
        Source.fromBytes(stream.toByteArray()).getLines().toList
        
      } finally {
        treeWalk.close()
      }
    } finally {
      revWalk.close()
    }
  }
  
}