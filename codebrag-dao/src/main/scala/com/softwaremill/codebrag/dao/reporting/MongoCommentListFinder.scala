package com.softwaremill.codebrag.dao.reporting

import org.bson.types.ObjectId
import com.softwaremill.codebrag.dao.{CommentRecord, UserRecord}
import com.foursquare.rogue.LiftRogue._

class MongoCommentListFinder extends CommentListFinder {


  override def findAllForCommit(commitId: ObjectId) = {
    val commentRecords = CommentRecord.where(_.commitId eqs commitId).fetch()
    CommentListDTO(buildCommentsFromRecord(commentRecords))
  }

  private def buildCommentsFromRecord(records: List[CommentRecord]): List[CommentListItemDTO] = {

    def buildCommentItem(record: CommentRecord, namesGroupedById: Map[ObjectId, String]): CommentListItemDTO = {
      val userName = namesGroupedById.getOrElse(record.authorId.get, "Unknown user")
      CommentListItemDTO(record.id.get.toString, userName, record.message.get, record.date.get)
    }

    val comments = records.sortBy(_.date.get)
    val authorIdSet = comments.map(_.authorId.get).toSet
    val idNamesPairs = UserRecord.select(_.id, _.name).where(_.id in authorIdSet).fetch
    val namesGroupedById = idNamesPairs.toMap
    comments.map(buildCommentItem(_, namesGroupedById))
  }
}