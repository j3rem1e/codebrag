package com.softwaremill.codebrag.usecases.user

import com.softwaremill.codebrag.dao.user.{UserAliasDAO, UserDAO}
import com.softwaremill.codebrag.domain.UserAlias
import com.softwaremill.scalaval.Validation.{Errors, rule, validate}
import com.typesafe.scalalogging.slf4j.Logging
import org.apache.commons.validator.routines.EmailValidator
import org.bson.types.ObjectId

/**
  *
  */
class UpdateUserFullNameUseCase(val userDao: UserDAO) extends Logging {

  def execute(userId: ObjectId, name: String): Either[Errors, Unit]  = {
    if (name.isEmpty) {
      throw new IllegalArgumentException("Name is empty");
    }
    userDao.modifyUserFullName(userId, name)

    Right(Unit)
  }
}
