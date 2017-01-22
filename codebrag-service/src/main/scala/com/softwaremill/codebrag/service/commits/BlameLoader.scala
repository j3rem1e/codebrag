package com.softwaremill.codebrag.service.commits

import com.softwaremill.codebrag.repository.Repository

trait BlameLoader {
    def loadBlame(sha: String, file: String, repo: Repository): BlameInfo
}

case class BlameInfo(lines: List[BlameLineInfo])

case class BlameLineInfo(line: String, sha: String, path: String, lineNumber: Int)


