package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MyApp
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: AutoCloseKoinTest() {

    private lateinit var repository: ReminderDataSource

    private var appContext = ApplicationProvider.getApplicationContext<MyApp>()

    private lateinit var reminder: ReminderDTO

    private val dataBindingIdlingResource = DataBindingIdlingResource()

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
    fun initReminder(){
        reminder = ReminderDTO(
            title = "Title A",
            description = "Description A",
            location = "Location A",
            latitude = 0.0,
            longitude = 0.0,
            "A"
        )
    }

    @Before
    fun registerIdlingResource() {
        // Register Espresso's counting idling resource and the data binding idling resource
        // This is done to avoid flaky tests caused by asynchronous operations
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @Test
    fun navigate_To_Save_Reminder_Fragment(){
        //creating a scenario of the ReminderList fragment
        val scenario = launchFragmentInContainer<ReminderListFragment>(
            Bundle(),
            R.style.AppTheme
        )
        // Monitor the fragment with the data binding idling resource
        // This is done to wait for all data binding operations to complete before continuing with the test
        dataBindingIdlingResource.monitorFragment(scenario)

        //mocking a navController
        val navController = mock(NavController::class.java)

        //setting the scenario fragment with the mocked navController
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!,navController)
        }

        //WHEN - click on the fab button
        onView(withId(R.id.addReminderFAB)).perform(click())

        //VERIFY - that we actually navigating to the save reminder fragment
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun checkReminderDisplayedOnTheUI(){
        // Save a reminder to the repository using a coroutine
        runBlocking {
            repository.saveReminder(reminder)
        }

        // Launch the ReminderListFragment in a container
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(),R.style.AppTheme)
        // Monitor the fragment with the data binding idling resource
        // This is done to wait for all data binding operations to complete before continuing with the test
        dataBindingIdlingResource.monitorFragment(scenario)

        // Verify that the title, description, and location of the reminder are displayed on the UI
        onView(withText(reminder.title)).check(matches(isDisplayed()))
        onView(withText(reminder.description)).check(matches(isDisplayed()))
        onView(withText(reminder.location)).check(matches(isDisplayed()))
    }

    @Test
    fun noReminders_NoDataDisplayedOnTheUI(){
        // Launch the ReminderListFragment in a container
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(),R.style.AppTheme)
        // Monitor the fragment with the data binding idling resource
        // This is done to wait for all data binding operations to complete before continuing with the test
        dataBindingIdlingResource.monitorFragment(scenario)

        // Get the text displayed on the "no data" text view
        val textDisplayed = appContext.getString(R.string.no_data)

        // Verify that the "no data" text view and its text are displayed on the UI
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withText(textDisplayed)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check(matches(withText(textDisplayed)))
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