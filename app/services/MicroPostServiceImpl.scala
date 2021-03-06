package services

import javax.inject.Singleton
import models.{MicroPost, PagedItems, UserFollow}
import scalikejdbc._
import skinny.Pagination

import scala.util.Try

@Singleton
class MicroPostServiceImpl extends MicroPostService {

  override def create(microPost: MicroPost)(implicit dbSession: DBSession): Try[Long] = Try {
    MicroPost.create(microPost)
  }

  override def deleteById(microPostId: Long)(implicit dbSession: DBSession): Try[Int] = Try {
    MicroPost.deleteById(microPostId)
  }

  override def updateFavoriteFlag(microPostId: Long, favoriteFlag: Boolean, favoritedId: Long)(implicit dbSession: DBSession): Try[Int] = Try {
    MicroPost.updateFavoriteFlag(microPostId, favoriteFlag, favoritedId)
  }

  override def findByFavorite(pagination: Pagination)(
    implicit dbSession: DBSession
  ): Try[PagedItems[MicroPost]] = Try {
    val size = MicroPost.countBy(sqls.eq(MicroPost.defaultAlias.favorite_flag, true))
    PagedItems(pagination, size, findFavoritePostByWithLimitOffset()(pagination))
  }

  override def findByUserId(pagination: Pagination, userId: Long)(
    implicit dbSession: DBSession
  ): Try[PagedItems[MicroPost]] =
    countBy(userId).map { size =>
      PagedItems(pagination, size, findAllByWithLimitOffset(Seq(userId))(pagination))
    }

  override def countBy(userId: Long)(implicit dbSession: DBSession): Try[Long] = Try {
    MicroPost.countBy(sqls.eq(MicroPost.defaultAlias.userId, userId))
  }

  override def findAllByWithLimitOffset(pagination: Pagination, userId: Long)(
    implicit dbSession: DBSession
  ): Try[PagedItems[MicroPost]] = Try {
    val followingIds: Seq[Long] =
      UserFollow.findAllBy(sqls.eq(UserFollow.defaultAlias.userId, userId)).map(_.followId)
    val size = MicroPost.countBy(sqls.in(MicroPost.defaultAlias.userId, userId +: followingIds))
    PagedItems(pagination, size, findAllByWithLimitOffset(userId +: followingIds)(pagination))
  }

  private def findAllByWithLimitOffset(userIds: Seq[Long])(pagination: Pagination)(
    implicit dbSession: DBSession
  ): Seq[MicroPost] = MicroPost.findAllByWithLimitOffset(
    sqls.in(MicroPost.defaultAlias.userId, userIds),
    pagination.limit,
    pagination.offset,
    Seq(MicroPost.defaultAlias.id.desc)
  )

  private def findFavoritePostByWithLimitOffset()(pagination: Pagination)(
    implicit dbSession: DBSession
  ): Seq[MicroPost] = MicroPost.findAllByWithLimitOffset(
    sqls.eq(MicroPost.defaultAlias.favorite_flag, true),
    pagination.limit,
    pagination.offset,
    Seq(MicroPost.defaultAlias.id.desc)
  )

}