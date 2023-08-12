package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.MainCoroutineRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var remindersLocalRepository: RemindersLocalRepository

    private lateinit var remindersDatabase: RemindersDatabase

    private var reminders = emptyList<ReminderDTO>()

    private var context = ApplicationProvider.getApplicationContext<MyApp>()

    // Executes each task synchronously using Architecture Components
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Set up the main coroutine rule to enable testing of coroutines
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    // Set up the test by creating an in-memory instance of the RemindersDatabase and the RemindersLocalRepository
    fun setup(){
        // Since we are using MainCoroutineRule, which runs all code in the test block on the main thread,
        // you don't need to use allowMainThreadQueries()
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            context,
            RemindersDatabase::class.java
        ).build()

        remindersLocalRepository = RemindersLocalRepository(
            remindersDatabase.reminderDao(),
            Dispatchers.Main
        )
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
    fun saveReminderAndGetAllReminders() = mainCoroutineRule.runBlockingTest {
        // Arrange: Save reminders to the database
        reminders.forEach { reminderDTO ->
            remindersLocalRepository.saveReminder(reminderDTO)
        }

        // Act: Retrieve all reminders from the database
        val retrievedReminders = remindersLocalRepository.getReminders() as Result.Success
        val numOfRetrievedReminders = retrievedReminders.data.size

        // Assert: Verify that retrieved reminders match the saved reminders
        assertThat(retrievedReminders.data, `is`(reminders))
        assertThat(numOfRetrievedReminders, `is`(reminders.size))
    }


    @Test
    // Verify that when the database is empty, an empty list is returned when retrieving reminders
    fun getRemindersAndGetRemindersWhenDatabaseIsEmpty_returnEmptyList() = mainCoroutineRule.runBlockingTest {
        // Arrange: nothing needs to be done since the database is empty

        // Act: retrieve the reminders from the empty database
        val retrievedReminders = remindersLocalRepository.getReminders() as Result.Success
        val numOfRetrievedReminders = retrievedReminders.data.size

        // Assert: check that an empty list is returned and has size of 0
        assertThat(retrievedReminders.data, `is`(emptyList()))
        assertThat(numOfRetrievedReminders , `is`(0))
    }

    @Test
    // Save a reminder to the database and verify that it can be retrieved by its ID
    fun saveReminderAndGetReminderById_returnSuccess() = mainCoroutineRule.runBlockingTest {
        // Arrange: Save a reminder to the database
        remindersLocalRepository.saveReminder(
            reminder = reminders[0]
        )

        // Act: Retrieve the reminder by its ID
        val retrievedReminder = remindersLocalRepository.getReminder("A") as Result.Success
        val reminder = reminders[0]

        // Assert: Verify that the retrieved reminder matches the original reminder
        assertThat(retrievedReminder.data, `is`(reminder))
    }

    @Test
    // Test saving a reminder to the database,
    // attempting to retrieve it with the wrong ID,
    // and verifying that retrieved data has an error with the message "Reminder not found!"
    fun saveReminder_getReminderByWrongId_returnError() = mainCoroutineRule.runBlockingTest {
        // Arrange - save a reminder to the database
        remindersLocalRepository.saveReminder(
            reminder = reminders[0]
        )

        // Act - retrieve a reminder with a wrong ID from the database
        val retrievedReminder = remindersLocalRepository.getReminder("Wrong Id")  as Result.Error

        // Assert - the retrieved reminder should be null
        assertThat(retrievedReminder.message, `is`("Reminder not found!"))
    }

    fun saveReminder_getReminderByCorrectId_returnError() = mainCoroutineRule.runBlockingTest {
        // Arrange - save a reminder to the database
        remindersLocalRepository.saveReminder(
            reminder = reminders[0]
        )

        // Act - retrieve a reminder with a wrong ID from the database
        val retrievedReminder = remindersLocalRepository.getReminder("Wrong Id")  as Result.Error

        // Assert - the retrieved reminder should be null
        assertThat(retrievedReminder.message, `is`("Reminder not found!"))
    }

    @Test
    // Deleting all reminders from the database
    // and verifying that the number of reminders is zero
    // and the list of reminders is empty.
    fun deleteAllReminders_checkNumOfRemindersInDatabase_returnZeroAndEmptyList() =
        mainCoroutineRule.runBlockingTest {
            // Arrange - save reminders to the database
            reminders.forEach{reminderDTO ->
                remindersLocalRepository.saveReminder(
                    reminder = reminderDTO
                )
            }

            // Act - delete all reminders from the database
            remindersLocalRepository.deleteAllReminders()

            // Assert - no reminders should be returned when retrieving reminders from the database
            // And the size of the retrieved reminders list should be 0
            val retrievedReminders = remindersLocalRepository.getReminders() as Result.Success

            assertThat(retrievedReminders.data, `is`(emptyList()))
            assertThat(retrievedReminders.data.size, `is`(0))
        }

    @After
    // Close the database and set context to null
    fun closeDB(){
        remindersDatabase.close()
        context = null
    }

}