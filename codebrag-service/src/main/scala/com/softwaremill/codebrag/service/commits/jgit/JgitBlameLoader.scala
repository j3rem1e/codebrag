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
import scala.collection.mutable.ArrayBuffer
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.api.Git
import com.softwaremill.codebrag.service.commits.FolderInfo
import com.softwaremill.codebrag.service.browser.FileInfo
import com.softwaremill.codebrag.service.commits.FolderFileInfo
import com.softwaremill.codebrag.service.commits.FileCommitInfo

class JgitBlameLoader extends BlameLoader {
  
  def loadBlame(sha: String, name: String, repository: CBRepo): Option[Either[BlameInfo, FolderInfo]] = {      
    val unquotedSha = sha.replaceAll("\\$", "/")
    val commitID = repository.repo.resolve(unquotedSha) match {
      case x if (x != null) => x
      case null => repository.repo.resolve("remotes/origin/" + unquotedSha);
    }
    System.out.println(commitID);
    val objectInfo = getObjectInfo(repository.repo, commitID, name)
        
    objectInfo.info match {
      case None => return None
      case Some(TreeInfo(path, name, true, id)) => {
        val git = new Git(repository.repo)
        Some(Right(FolderInfo(objectInfo.children.map { tree => 
          val commit = git.log().add(commitID).addPath(tree.path).call().iterator().next()
          FolderFileInfo(tree.name, tree.tree,
              FileCommitInfo(commit.getName, 
                  commit.getFullMessage, 
                  commit.getAuthorIdent.getName, 
                  commit.getAuthorIdent.getEmailAddress, 
                  new DateTime(commit.getCommitTime * 1000l)))
          })))
      }
      case Some(TreeInfo(path, name, false, id)) => {
                
        val blamer = new BlameCommand(repository.repo)

        blamer.setStartCommit(commitID);
        blamer.setFilePath(path);
        val blame = blamer.call();
                
        try {
          val files = getLines(repository.repo, id)

          Some(Left(BlameInfo(files.zipWithIndex.map { 
            case (line,index) => {
              val commit = blame.getSourceCommit(index)
              BlameLineInfo(line, blame.getSourceCommit(index).getName, blame.getSourcePath(index), blame.getSourceLine(index))
            }
          })))
        } catch {
          case e: Exception => e.printStackTrace(); None
        }
      }
    }
  }
  
  private def getObjectInfo(repository: Repository, commitId: ObjectId, path: String): ObjectInfo = {
    val revWalk = new RevWalk(repository)
    try {
      val ref = revWalk.parseAny(commitId)
      val commit = revWalk.parseCommit(commitId)
      val tree = commit.getTree()
            
      val treeWalk = new TreeWalk(repository)
      try {        
        treeWalk.addTree(tree)
        treeWalk.setRecursive(false)
        if (!path.isEmpty()) {
          treeWalk.setFilter(PathFilter.create(path))
        }
        
        val children = new ArrayBuffer[TreeInfo];
        var  target: Option[TreeInfo] = if (path.isEmpty()) Some(TreeInfo("/", "/", true, null)) else None
        while (treeWalk.next()) {
          
          val currentPath = treeWalk.getPathString
          val currentName = treeWalk.getNameString
          
          if (currentPath == path) {
            // Path found
           target = Some(createTreeInfo(treeWalk))
            if (treeWalk.isSubtree()) {
              treeWalk.enterSubtree()
            }
          } else {
            
            if (target.isDefined) {
               children.append(createTreeInfo(treeWalk))

               //val commit = new Git(repository).log().addPath(treeWalk.getPathString).setMaxCount(1).call().iterator().next()
             } else if (target.isEmpty && treeWalk.isSubtree()) {
              treeWalk.enterSubtree()
            }
          }
        }
        
        revWalk.dispose()

        return ObjectInfo(target, children.toList)
      } finally {
        treeWalk.close()
      }
    } finally {
      revWalk.close()
    }
  }
  
  private def createTreeInfo(tree: TreeWalk) : TreeInfo = TreeInfo(tree.getPathString, tree.getNameString, tree.isSubtree(), tree.getObjectId(0))
  
  
  
  case class TreeInfo(path: String, name: String, tree: Boolean, id: ObjectId)
  case class ObjectInfo(info: Option[TreeInfo], children: List[TreeInfo])
  
  private def getLines(repository: Repository, objectId: ObjectId): List[String] = {
      val loader = repository.open(objectId)
      val stream = new ByteArrayOutputStream()
      loader.copyTo(stream)
      Source.fromBytes(stream.toByteArray()).getLines().toList
  }
  
}