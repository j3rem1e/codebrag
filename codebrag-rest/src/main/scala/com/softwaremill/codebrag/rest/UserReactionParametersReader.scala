package com.softwaremill.codebrag.rest

trait UserReactionParametersReader {

  self: JsonServlet =>

  def readReactionParamsFromRequest = {
    val fileNameOpt = (parsedBody \ "fileName").extractOpt[String]
    val repoNameOpt = (parsedBody \ "repoName").extractOpt[String]
    val sha = (parsedBody \ "sha").extractOpt[String]

    val lineNumberOpt = (parsedBody \ "lineNumber").extractOpt[Int]
    val commitIdParam = params("id")
    if(fileNameOpt.isDefined ^ lineNumberOpt.isDefined) {
      halt(400, "File name and line number must be present for inline comment")
    }
    CommonReactionRequestParams(commitIdParam, sha, repoNameOpt, fileNameOpt, lineNumberOpt)
  }

  case class CommonReactionRequestParams(commitId: String, sha: Option[String], repoName: Option[String], fileName: Option[String], lineNumber: Option[Int])
}
