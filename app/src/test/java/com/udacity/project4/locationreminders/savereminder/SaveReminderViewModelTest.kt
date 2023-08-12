package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MyApp
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var fakeDataSource: FakeDataSource
    // Create a test ViewModel
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // Create a reminder for test
    private lateinit var reminder: ReminderDataItem

    // Create application context
    private var context = ApplicationProvider.getApplicationContext<MyApp>()

    // Set up the MainCoroutineRule
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private fun reminderDTO_To_reminderDataItem(reminder: ReminderDTO): ReminderDataItem{
        return ReminderDataItem(
            title = reminder.title,
            description = reminder.description,
            location = reminder.location,
            latitude = reminder.latitude,
            longitude = reminder.longitude,
            id = reminder.id
        )
    }

    // Initialize the test ViewModel, fakeDataSource and reminders before each test
    @Before
    fun setupViewModel() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            context ,
            fakeDataSource
        )

        reminder = ReminderDataItem(
            title = "Title",
            description = "Description",
            location = "Location",
            latitude = 0.0,
            longitude = 0.0,
            "A"
        )
    }

    @Test
    fun `checking the state of the loading`() = runBlockingTest{
        // Arrange: pause the coroutine dispatcher so that the loading state can be observed
        mainCoroutineRule.pauseDispatcher()

        // Act: save the reminder
        saveReminderViewModel.saveReminder(reminder)

        val showLoadingBefore = saveReminderViewModel.showLoading.getOrAwaitValue()

        // Assert: check that the loading state is set to true before the dispatcher is resumed
        assertThat(showLoadingBefore,`is`(true))

        // Resume the coroutine dispatcher and check that the loading state is set to false
        mainCoroutineRule.resumeDispatcher()
        val showLoadingAfter = saveReminderViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoadingAfter, `is`(false))
    }

    @Test
    fun `onClear setting LiveData to Null`()= runBlockingTest {
        // Arrange: Set the values of the LiveData objects to be cleared
        saveReminderViewModel.reminderTitle.value = reminder.title
        saveReminderViewModel.reminderDescription.value = reminder.description
        saveReminderViewModel.reminderSelectedLocationStr.value = reminder.location
        saveReminderViewModel.latitude.value = reminder.latitude
        saveReminderViewModel.longitude.value= reminder.longitude
        saveReminderViewModel.reminderId.value = reminder.id

        // Act: Call the onClear() method to clear the LiveData objects
        saveReminderViewModel.onClear()

        // Assert: Check that the values of the LiveData objects are null
        val reminderTitle = saveReminderViewModel.reminderTitle.getOrAwaitValue()
        val reminderDescription = saveReminderViewModel.reminderDescription.getOrAwaitValue()
        val reminderLocation = saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue()
        val latitude = saveReminderViewModel.latitude.getOrAwaitValue()
        val longitude = saveReminderViewModel.longitude.getOrAwaitValue()
        val reminderId = saveReminderViewModel.reminderId.getOrAwaitValue()

        assertThat(reminderTitle, `is`(nullValue()))
        assertThat(reminderDescription, `is`(nullValue()))
        assertThat(reminderLocation, `is`(nullValue()))
        assertThat(latitude, `is`(nullValue()))
        assertThat(longitude, `is`(nullValue()))
        assertThat(reminderId, `is`(nullValue()))
    }

    @Test
    fun `validate data when the data is valid should return true`() = runBlockingTest{
        // Arrange: Using the default reminder object in the before method

        // Act: Call the function to validate the reminder data
        val result = saveReminderViewModel.validateEnteredData(reminder)

        // Assert: Check that the result is true, indicating that the data is valid
        assertThat(result, `is`(true))
    }

    @Test
    fun `validate data when title is missing should return false and show warning message with missing title`(){
        // Arrange: Set up a reminder object with a missing title
        reminder.title = null

        // Act: Call the validateEnteredData function with the reminder object and get the result
        val result = saveReminderViewModel.validateEnteredData(reminder)

        // Get the error message shown in the snackbar
        val errorMessageShown = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        val correctErrorMessage = R.string.err_enter_title

        // Assert: Check that the result is false
        // and that the error message matches the expected message for a missing title
        assertThat(result, `is`(false))
        assertThat(errorMessageShown, `is`(correctErrorMessage))
    }

    @Test
    fun `validate data when location is not selected should return false and show warning message with select location`(){
        // Arrange: Create a reminder with no location set
        reminder.location = null

        // Act: Call the `validateEnteredData` method with the reminder object
        val result = saveReminderViewModel.validateEnteredData(reminder)

        // Get the error message that should be shown when the validation fails
        val errorMessageShown = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        val correctErrorMessage = R.string.err_select_location

        // Assert: The validation should return false and
        // the error message should be the "select location" message
        assertThat(result, `is`(false))
        assertThat(errorMessageShown, `is`(correctErrorMessage))
    }

    @Test
    fun `save reminder`() = runBlockingTest {
        //Arrange: Call the `saveReminder` method with the reminder object
        saveReminderViewModel.saveReminder(reminder)

        // Act: Fetch saved reminder from data source
        val localReminder = fakeDataSource.getReminder("A") as Result.Success
        val savedReminder = reminderDTO_To_reminderDataItem(localReminder.data)

        // Assert: Check saved reminder against original reminder
        assertThat(savedReminder, `is`(reminder))

        // Assert: Check saved reminder fields against original reminder fields
        assertThat(savedReminder.title, `is`(reminder.title))
        assertThat(savedReminder.description, `is`(reminder.description))
        assertThat(savedReminder.location, `is`(reminder.location))
        assertThat(savedReminder.latitude, `is`(reminder.latitude))
        assertThat(savedReminder.longitude, `is`(reminder.longitude))
        assertThat(savedReminder.id, `is`(reminder.id))
    }

    @Test
    fun `saving reminder showing a toast`() = runBlockingTest {
        //Arrange: Call the `saveReminder` method with the reminder object
        saveReminderViewModel.saveReminder(reminder)

        // Act: Get the message shown in the toast
        val messageShown = saveReminderViewModel.showToast.getOrAwaitValue()
        val correctMessage = context.getString(R.string.reminder_saved)

        // Assert: the message should be the "Reminder Saved!"
        assertThat(messageShown, `is`(correctMessage))
    }

    /**
     * Deletes all reminders from the fake data source after each test.
     */
    @After
    fun clearDB() = runBlockingTest{
        saveReminderViewModel.onClear()
        fakeDataSource.deleteAllReminders()
        context = null
    }
}