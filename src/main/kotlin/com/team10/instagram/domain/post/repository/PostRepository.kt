package com.team10.instagram.domain.post.repository

import com.team10.instagram.domain.post.model.Post
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface PostRepository : CrudRepository<Post, Long> {
    /* Apply pessimistic lock for transactional operations
     */
    @Query("SELECT * FROM post WHERE post_id = :id FOR UPDATE")
    fun findByIdWithLock(
        @Param("id") id: Long,
    ): Post?

    /* Change created_at to post_id because DATE_TIME precision issues on DB (seconds)
       Therefore, temporally use auto-increment property of Id
     */
    @Query("SELECT * FROM post ORDER BY post_id DESC")
    fun findAllByOrderByCreatedAtDesc(): List<Post>

    /* Change created_at to post_id because DATE_TIME precision issues on DB (seconds)
       Therefore, temporally use auto-increment property of Id
     */
    @Query("SELECT * FROM post WHERE user_id = :userId ORDER BY post_id DESC")
    fun findAllByUserIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
    ): List<Post>

    @Query(
        """
        SELECT * FROM post 
        WHERE user_id IN (:userIds) 
        ORDER BY post_id DESC 
        LIMIT :limit OFFSET :offset
    """,
    )
    fun findAllByUserIdsIn(
        @Param("userIds") userIds: List<Long>,
        @Param("limit") limit: Int,
        @Param("offset") offset: Long,
    ): List<Post>

    @Query("SELECT COUNT(*) FROM post WHERE user_id IN (:userIds)")
    fun countByUserIdsIn(
        @Param("userIds") userIds: List<Long>,
    ): Long
}
