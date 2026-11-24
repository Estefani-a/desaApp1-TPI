package com.example.uniride.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A UADE campus with its meeting points. Not in preentrega 11.1: the campuses come from the
 * server (`GET /campuses`) and are kept in Room so the publish form (RF02) and the feed
 * filters (RF03) work without connection, as `CampusRepository` promises.
 *
 * There is no `isSync`: the client never creates or edits campuses. The list from the server
 * is always complete, so it replaces the table as a whole.
 */
@Entity(tableName = "campuses")
data class CampusEntity(
    @PrimaryKey val code: String,
    val name: String,
    /** Stored as a JSON array by `Converters`. */
    val meetingPoints: List<String>,
)
