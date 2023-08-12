package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var remindersDatabase: RemindersDatabase

    private var reminders = emptyList<ReminderDTO>()

    private var context = ApplicationProvider.getApplicationContext<MyApp>()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createDB(){
        // Use an in-memory database for testing, instead of a persistent database
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            context,
            RemindersDatabase::class.java
        ).build()
    }

    @Before
    // Initialize reminders to be used in tests
    fun initReminders(){
        reminders = listOf(
            ReminderDTO(
                title = "Title A",
                description = "Description A",
                location = "Location A",
                latitude = 0.0,
                longitude = 0.0,
                "A"
            ),
            ReminderDTO(
                title = "Title B",
                description = "Description B",
                location = "Location B",
                latitude = 0.0,
                longitude = 0.0,
                "B"
        ),
            ReminderDTO(
                title = "Title C",
                description = "Description C",
                location = "Location C",
                latitude = 0.0,
                longitude = 0.0,
                "C"
            )
        )
    }

    @Test
    // Save reminders to the database and verify that they can be retrieved
    fun saveReminderAndGetAllReminders() = runBlockingTest {
        // Arrange: Save reminders to the database
        reminders.forEach {reminderDTO ->
            remindersDatabase.reminderDao().saveReminder(reminderDTO)
        }

        // Act: Retrieve all reminders from the database
        val retrievedReminders = remindersDatabase.reminderDao().getReminders()
        val numOfReminders = reminders.size

        // Assert: Verify that retrieved reminders match the saved reminders
        assertThat(retrievedReminders,`is`(reminders))
        assertThat(retrievedReminders.size, `is`(numOfReminders))
    }

    @Test
    // Verify that when the database is empty, an empty list is returned when retrieving reminders
    fun getRemindersAndGetRemindersWhenDatabaseIsEmpty_returnEmptyList() = runBlockingTest{
        // Arrange: nothing needs to be done since the database is empty

        // Act: retrieve the reminders from the empty database
        val retrievedReminders = remindersDatabase.reminderDao().getReminders()

        // Assert: check that an empty list is returned and has size of 0
        assertThat(retrievedReminders, `is`(emptyList()))
        assertThat(retrievedReminders.size, `is`(0))
    }

    @Test
    // Save a reminder to the database and verify that it can be retrieved by its ID
    fun saveReminderAndGetReminderById() = runBlockingTest {
        // Arrange: Save a reminder to the database
        remindersDatabase.reminderDao().saveReminder(
            reminder = reminders[0]
        )

        // Act: Retrieve the reminder by its ID
        val retrievedReminder = remindersDatabase.reminderDao().getReminderById("A")
        val reminder = reminders[0]

        // Assert: Verify that the retrieved reminder matches the original reminder
        assertThat(retrievedReminder, `is`(reminder))
    }

    @Test
    // Test saving a reminder to the database,
    // attempting to retrieve it with the wrong ID,
    // and verifying that null is returned
    fun saveReminder_getReminderById_returnNull() = runBlockingTest{
        // Arrange - save a reminder to the database
        remindersDatabase.reminderDao().saveReminder(
            reminder = reminders[0]
        )

        // Act - retrieve a reminder with a wrong ID from the database
        val retrievedReminder = remindersDatabase.reminderDao().getReminderById("Wrong Id")

        // Assert - the retrieved reminder should be null
        assertThat(retrievedReminder, nullValue())
    }

    @Test
    // Deleting all reminders from the database
    // and verifying that the number of reminders is zero
    // and the list of reminders is empty.
    fun deleteAllReminders_checkNumOfRemindersInDatabase_returnZeroAndEmptyList() = runBlockingTest {
        // Arrange - save reminders to the database
        reminders.forEach{reminderDTO ->
            remindersDatabase.reminderDao().saveReminder(
                reminder = reminderDTO
            )
        }

        // Act - delete all reminders from the database
        remindersDatabase.reminderDao().deleteAllReminders()

        // Assert - no reminders should be returned when retrieving reminders from the database
        // And the size of the retrieved reminders list should be 0
        val retrievedReminders = remindersDatabase.reminderDao().getReminders()

        assertThat(retrievedReminders, `is`(emptyList()))
        assertThat(retrievedReminders.size, `is`(0))
    }

    @After
    // Close the database and set context to null
    fun closeDB(){
        remindersDatabase.close()
        context = null
    }

}