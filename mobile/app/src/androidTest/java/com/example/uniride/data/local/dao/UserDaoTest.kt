package com.example.uniride.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.uniride.data.local.database.UniRideDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: UniRideDatabase
    private lateinit var dao: UserDao

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        dao = database.userDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `sin sesion no hay usuario actual`() = runTest {
        assertNull(dao.observeCurrentUser().first())
    }

    @Test
    fun `un usuario guardado se observa como usuario actual`() = runTest {
        val user = userEntity()

        dao.upsert(user)

        assertEquals(user, dao.observeCurrentUser().first())
    }

    @Test
    fun `guardar el mismo usuario con otros datos lo actualiza`() = runTest {
        dao.upsert(userEntity())

        dao.upsert(userEntity().copy(phone = "1100000000"))

        assertEquals("1100000000", dao.observeCurrentUser().first()?.phone)
    }

    @Test
    fun `clear borra al usuario actual`() = runTest {
        dao.upsert(userEntity())

        dao.clear()

        assertNull(dao.observeCurrentUser().first())
    }
}
