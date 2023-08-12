package com.udacity.project4

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest : AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource

    private var appContext = getApplicationContext<MyApp>()

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    val activityRule = ActivityTestRule(RemindersActivity::class.java)
    // Get activity context
    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource() {
        // Register Espresso's counting idling resource and the data binding idling resource
        // This is done to avoid flaky tests caused by asynchronous operations
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @Test
    fun clickingSaveReminderWithNoTitle_showSnackBarWithMissingTitleErrorMessage(){
        // 1. Start TasksActivity.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 2. Click on add reminder FAB
        onView(withId(R.id.addReminderFAB)).perform(click())

        // 3. Click on save reminder FAB without any data inserted
        onView(withId(R.id.saveReminder)).perform(click())

        // 4. Check the error message displayed by SnackBar
        // Getting the message text
        val missingTitleMessage = appContext.getString(R.string.err_enter_title)
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(missingTitleMessage)))

        // 5. Make sure the activity is closed.
        activityScenario.close()
    }

    @Test
    fun clickingSaveReminderWithNoLocation_showSnackBarWithSelectLocationErrorMessage(){
        // 1. Start TasksActivity.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 2. Click on add reminder FAB
        onView(withId(R.id.addReminderFAB)).perform(click())

        // 3. Click on the title TextView
        onView(withId(R.id.reminderTitle)).perform(click())

        // 4. Type something in the title EditText
        val reminderTitle = "Cairo Tower"
        onView(withId(R.id.reminderTitle))
            .perform(typeText(reminderTitle), closeSoftKeyboard())

        // 5. Click on save reminder FAB without location selected
        onView(withId(R.id.saveReminder)).perform(click())

        // 6. Check the error message displayed by SnackBar
        // Getting the message text
        val selectLocationMessage = appContext.getString(R.string.select_location)
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(selectLocationMessage)))

        // 7. Make sure the activity is closed.
        activityScenario.close()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun addingReminder_ToastWithReminderSavedMessage_checkReminderIfVisible(){
        // 1. Start TasksActivity.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 2. Click on add reminder FAB
        onView(withId(R.id.addReminderFAB)).perform(click())

        // 3. Click on the title TextView
        onView(withId(R.id.reminderTitle)).perform(click())

        // 4. Type something in the title EditText
        val reminderTitle = "Cairo Tower"
        onView(withId(R.id.reminderTitle))
            .perform(typeText(reminderTitle), closeSoftKeyboard())

        // 5. Click on the title TextView
        onView(withId(R.id.reminderDescription)).perform(click())

        // 6. Type something in the description EditText
        val reminderDescription = "The highest building in Cairo"
        onView(withId(R.id.reminderDescription))
            .perform(typeText(reminderDescription), closeSoftKeyboard())

        // 7. Click on reminder location to select the location
        onView(withId(R.id.selectLocation)).perform(click())

        // 8. Long click on the map to select a location
        onView(withId(R.id.mapView)).perform(longClick())
        // Wait till zoom animations and button animation finish
        Thread.sleep(2000)

        // 9. Click on save button
        onView(withId(R.id.btn_saveReminder)).perform(click())

        // 10. Click on save reminder FAB
        onView(withId(R.id.saveReminder)).perform(click())

        // 11. Check if the toast is visible (This fails on API 30+)
        // instead we can use custom RootMatcher
        val reminderSaved = R.string.reminder_saved
        val decorView = getActivity(activityScenario)?.window?.decorView
        onView(withText(reminderSaved))
            .inRoot(withDecorView(not(`is`(decorView))))
            .check(matches(isDisplayed()))

        // 12. Check if the reminder is displayed
        onView(withText(reminderTitle)).check(matches(isDisplayed()))
        onView(withText(reminderDescription)).check(matches(isDisplayed()))

        // 13. Make sure the activity is closed.
        activityScenario.close()
    }

    @After
    fun clear(){
        // Set the app context to null to clean up after the tests
        appContext = null
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun unregisterIdlingResource(){
        // Register Espresso's counting idling resource and the data binding idling resource
        // This is done to avoid flaky tests caused by asynchronous operations
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

}
