package com.team10.instagram.domain.search.service

import com.team10.instagram.domain.search.dto.GetSearchHistoryResponse
import com.team10.instagram.domain.search.dto.GetSearchHistoryResponseUnit
import com.team10.instagram.domain.search.dto.SaveSearchResponse
import com.team10.instagram.domain.search.model.Search
import com.team10.instagram.domain.search.repository.SearchHistoryRepository
import com.team10.instagram.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SearchService(
    private val searchHistoryRepository: SearchHistoryRepository,
    private val userRepository: UserRepository,
) {
    fun saveSearchHistory(
        fromUserId: Long,
        toUserId: Long,
    ): SaveSearchResponse {
        val search =
            searchHistoryRepository.save(
                Search(
                    fromUserId = fromUserId,
                    toUserId = toUserId,
                    createdAt = LocalDateTime.now(),
                ),
            )

        return SaveSearchResponse(search.searchId!!)
    }

    fun getSearchHistory(fromUserId: Long): GetSearchHistoryResponse? {
        val searchHistory = searchHistoryRepository.findRecentByFromUserId(fromUserId)
        if (searchHistory == null) return null

        val userIds = searchHistory.map { it.toUserId }
        val usersMap = userRepository.findAllByUserIdIn(userIds).associateBy { it.userId }

        val items =
            searchHistory?.mapNotNull { search ->
                val user = usersMap[search.toUserId] ?: return@mapNotNull null
                GetSearchHistoryResponseUnit(
                    searchId = search.searchId!!,
                    userId = search.toUserId,
                    nickname = user.nickname,
                    profileImageUrl = user.profileImageUrl,
                )
            }

        return GetSearchHistoryResponse(items)
    }

    fun deleteSearchHistory(
        fromUserId: Long,
        toUserId: Long,
    ) {
        searchHistoryRepository.markDeleted(fromUserId, toUserId)
    }

    fun deleteAllSearchHistory(fromUserId: Long) {
        searchHistoryRepository.markAllDeleted(fromUserId)
    }
}
