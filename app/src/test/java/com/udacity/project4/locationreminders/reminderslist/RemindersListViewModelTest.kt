package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var fakeDataSource: FakeDataSource

    // Create a test ViewModel
    private lateinit var remindersListViewModel: RemindersListViewModel

    // Create reminders for test
    private var reminders = emptyList<ReminderDTO>()

    // Create application context
    private var context = ApplicationProvider.getApplicationContext<MyApp>()

    // Set up the MainCoroutineRule
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    private fun reminderDataItem_To_reminderDTO(reminders: List<ReminderDataItem>): List<ReminderDTO>{
        return reminders.map {
            ReminderDTO(
                it.title,
                it.description,
                it.location,
                it.longitude,
                it.longitude,
                it.id
            )
        }
    }

    // Initialize the test ViewModel, fakeDataSource and reminders before each test
    @Before
    fun setupViewModel(){
        stopKoin()
        fakeDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(
            context,
            fakeDataSource
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
    fun `checking the state of the loading`() = runBlockingTest{
        // Arrange: Pause the dispatcher and save a reminder to the fake data source
        mainCoroutineRule.pauseDispatcher()
        fakeDataSource.saveReminder(reminders[0])

        // Act: Call the loadReminders method on the ViewModel
        remindersListViewModel.loadReminders()

        // Assert: Check that the loading state LiveData is set to true
        val showLoadingBefore = remindersListViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoadingBefore,`is`(true))

        // Assert: Resume the dispatcher and assert that the loading state LiveData is set to false
        mainCoroutineRule.resumeDispatcher()

        val showLoadingAfter = remindersListViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoadingAfter,`is`(false))
    }

    @Test
    fun `getReminders should return all reminders`() = runBlockingTest {
        // Arrange: save three reminders to the fake data source
        reminders.forEach { reminderDTO ->
            fakeDataSource.saveReminder(reminderDTO)
        }

        // Act: load reminders from the view model
        remindersListViewModel.loadReminders()

        // Convert ReminderDataItems to ReminderDTOs
        val result = remindersListViewModel.remindersList.getOrAwaitValue()
        val reminders = reminderDataItem_To_reminderDTO(result)

        // Assert: verify that the reminders returned by the view model match the saved reminders
        assertThat(reminders,`is`(reminders))

        // Assert: verify that the correct number of reminders are returned
        val numOfReminders = remindersListViewModel.remindersList.getOrAwaitValue().size
        assertThat(numOfReminders,`is`(3))
    }

    @Test
    fun `deleteAllReminders should delete all reminders`() = runBlockingTest {
        // Arrange: save three reminders to the fake data source
        reminders.forEach { reminderDTO ->
            fakeDataSource.saveReminder(reminderDTO)
        }

        // Act: load reminders from the view model
        remindersListViewModel.loadReminders()

        // Convert ReminderDataItems to ReminderDTOs
        val resultBeforeDeletion = remindersListViewModel.remindersList.getOrAwaitValue()
        val remindersBeforeDeletion = reminderDataItem_To_reminderDTO(resultBeforeDeletion)

        // Assert: verify that the reminders returned by the view model match the saved reminders
        assertThat(remindersBeforeDeletion,`is`(reminders))

        // Act: delete all reminders
        fakeDataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()

        // Convert ReminderDataItems to ReminderDTOs
        val resultAfterDeletion = remindersListViewModel.remindersList.getOrAwaitValue()
        val remindersAfterDeletion = reminderDataItem_To_reminderDTO(resultAfterDeletion)

        // Assert: check that the reminders were deleted correctly
        assertThat(remindersAfterDeletion , `is`(emptyList()))

        // Assert: verify that there are no reminders
        val numOfReminders = remindersListViewModel.remindersList.getOrAwaitValue().size
        assertThat(numOfReminders,`is`(0))
    }

    @Test
    fun `checking showNoData if there are no reminders`() = runBlockingTest{
        // Arrange: load reminders from the view model
        remindersListViewModel.loadReminders()

        // Act: get the reminders and the number of reminders
        val reminders = remindersListViewModel.remindersList.getOrAwaitValue()
        val numOfReminders = remindersListViewModel.remindersList.getOrAwaitValue().size
        val showNoData = remindersListViewModel.showNoData.getOrAwaitValue()

        // Assert: verify that there are no reminders and showNoData is true
        assertThat(reminders, `is`(emptyList()))
        assertThat(numOfReminders, `is`(0))
        assertThat(showNoData, `is`(true))
    }

    @Test
    fun `checking if the list is empty should return empty list`()= runBlockingTest {
        // Arrange: load reminders from the view model
        remindersListViewModel.loadReminders()

        // Act: get the reminders and the number of reminders
        val reminders = remindersListViewModel.remindersList.getOrAwaitValue()
        val numOfReminders = remindersListViewModel.remindersList.getOrAwaitValue().size

        // Assert: verify that there are no reminders
        assertThat(reminders, `is`(emptyList()))
        assertThat(numOfReminders, `is`(0))
    }

    @Test
    fun `checking the error message in case of errors`()= runBlockingTest {
        // Arrange: set the fake data source to return an error, save a reminder to the fake data source,
        // load reminders from the view model
        fakeDataSource.setError(true)
        fakeDataSource.saveReminder(reminders[0])
        remindersListViewModel.loadReminders()

        // Act: get the error message from the view model
        val errorMessage = remindersListViewModel.showSnackBar.getOrAwaitValue()

        // Assert: verify that the error message is correct
        assertThat(errorMessage, `is`("Unexpected Error!!"))
    }

    /**
     * Deletes all reminders from the fake data source after each test.
     */
    @After
    fun clearDB() = runBlockingTest{
        fakeDataSource.deleteAllReminders()
        context = null
    }
}