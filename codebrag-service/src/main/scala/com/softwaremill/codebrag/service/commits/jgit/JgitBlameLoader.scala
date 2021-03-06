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
import org.eclipse.jgit.diff.{RawText, RawTextComparator}

import scala.annotation.tailrec
import org.eclipse.jgit.revwalk.RevCommit

class JgitBlameLoader extends BlameLoader {
  
  def loadBlame(sha: String, name: String, repository: CBRepo): Option[Either[BlameInfo, FolderInfo]] = {      
    val unquotedSha = sha.replaceAll("\\$", "/")
    val commitID = repository.repo.resolve(unquotedSha) match {
      case x if (x != null) => x
      case null => repository.repo.resolve("remotes/origin/" + unquotedSha);
    }
    val objectInfo = getObjectInfo(repository.repo, commitID, name)
        
    objectInfo.info match {
      case None => return None
      case Some(TreeInfo(path, name, FileMode.TREE, id)) => {
        val git = new Git(repository.repo)
        Some(Right(FolderInfo(objectInfo.children.map { tree => 
                    
          val commit = git.log().add(commitID).addPath(tree.path).call().iterator().next()
          FolderFileInfo(tree.name, tree.fileMode == FileMode.TREE,
              FileCommitInfo(commit.getName, 
                  commit.getFullMessage, 
                  commit.getAuthorIdent.getName, 
                  commit.getAuthorIdent.getEmailAddress, 
                  new DateTime(commit.getCommitTime * 1000l)))
          })))
      }
      case Some(TreeInfo(path, name, _, id)) => {
                
        val blamer = new BlameCommand(repository.repo)

        blamer.setStartCommit(commitID);
        blamer.setFilePath(path);
        blamer.setTextComparator(RawTextComparator.WS_IGNORE_ALL)

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
    
    def using[A <% { def close(): Unit }, B](resource: A)(f: A => B): B =
    try f(resource) finally {
      if(resource != null){
          resource.close()
      }
    }
    
     @tailrec
      def simplifyPath(treeInfo : TreeInfo): TreeInfo = treeInfo match {
        case TreeInfo(path, name, FileMode.TREE, oid) =>
          (using(new TreeWalk(repository)) { walk =>
            walk.addTree(oid)
            // single tree child, or None
            if(walk.next() && walk.getFileMode(0) == FileMode.TREE) {
              Some(TreeInfo(path + '/' + walk.getPathString, name + "/" + walk.getNameString, walk.getFileMode(0), walk.getObjectId(0))).filterNot(_ => walk.next())
            } else {
              None
            }
          }) match {
            case Some(treeInfo) => simplifyPath(treeInfo)
            case _ => treeInfo
          }
        case _ => treeInfo
      }
     
    (using(new RevWalk(repository))) { revWalk =>
      val ref = revWalk.parseAny(commitId)
      val commit = revWalk.parseCommit(commitId)
      val tree = commit.getTree()
            
      (using(new TreeWalk(repository))) { treeWalk =>
        treeWalk.addTree(tree)
        treeWalk.setRecursive(false)
        if (!path.isEmpty()) {
          treeWalk.setFilter(PathFilter.create(path))
        }
        
        val children = new ArrayBuffer[TreeInfo];
        var  target: Option[TreeInfo] = if (path.isEmpty()) Some(TreeInfo("/", "/", FileMode.TREE, null)) else None
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
                children.append(simplifyPath(createTreeInfo(treeWalk)))
            } else if (target.isEmpty && treeWalk.isSubtree()) {
              treeWalk.enterSubtree()
            }
          }
        }
        
        revWalk.dispose()

        return ObjectInfo(target, children.toList)
      }
    }
  }
  
  private def createTreeInfo(tree: TreeWalk) : TreeInfo = TreeInfo(tree.getPathString, tree.getNameString, tree.getFileMode(0), tree.getObjectId(0))
  
  
  
  case class TreeInfo(path: String, name: String, fileMode: FileMode, id: ObjectId)
  case class ObjectInfo(info: Option[TreeInfo], children: List[TreeInfo])
  
  private def getLines(repository: Repository, objectId: ObjectId): List[String] = {
      val loader = repository.open(objectId)
      val stream = new ByteArrayOutputStream()
      loader.copyTo(stream)
      
      val bytes = stream.toByteArray()
      if (RawText.isBinary(bytes)) {
        return List()
      } else {
        Source.fromBytes(stream.toByteArray()).getLines().toList
      }
  }
  
}