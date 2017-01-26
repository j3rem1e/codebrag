package com.softwaremill.codebrag.service.commits

import com.softwaremill.codebrag.repository.Repository
import org.joda.time.DateTime

trait BlameLoader {
    def loadBlame(sha: String, file: String, repo: Repository): Option[Either[BlameInfo, FolderInfo]]
}

case class BlameInfo(lines: List[BlameLineInfo])

case class BlameLineInfo(line: String, sha: String, path: String, lineNumber: Int)

case class FolderInfo(files: List[FolderFileInfo])

case class FolderFileInfo(name: String, tree: Boolean, commit: FileCommitInfo)

case class FileCommitInfo(sha: String, message: String, authorName: String, authorEmail: String, date: DateTime)
