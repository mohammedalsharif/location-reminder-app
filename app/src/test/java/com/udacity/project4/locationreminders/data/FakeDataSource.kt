package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO> = mutableListOf()) : ReminderDataSource {

    // Flag to test error handling
    private var thereIsAnError = false

    // Setter for error handling tests
    fun setError(value: Boolean) {
        thereIsAnError = value
    }

    //1- returns a Result.Success containing a copy of the reminders list if it's not empty.
    //2- returns a Result.Success containing an empty list if it's empty.
    //3- returns a Result.Error with an error message.
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        //in case we want to test error handling
        if (thereIsAnError){
            return Result.Error("Unexpected Error!!")//returns a Result.Error with an error message
        }

        //in case we want to test the result as success
        return if (reminders.isEmpty()){ // in case we have an empty list we want the result to be success
            Result.Success(emptyList()) //so we return an empty list
        }else {
            //returns a Result.Success containing a copy of the reminders list
            Result.Success(reminders)
        }
    }

    // Adds the provided reminder object to the reminders list.
    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    //1- returns a Result.Success containing a copy of the ReminderDTO object
    //with the matching id if it exists in the reminders list.
    //2- returns a Result.Error with an error message if the reminder is not found.
    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        //in case we want to test error handling
        if (thereIsAnError){
            return Result.Error("Unexpected Error!!")
        }

        reminders.let { reminders ->
            val reminder = reminders.find { reminderDTO ->
                reminderDTO.id == id // Search the list of fake reminders for the given ID
            }
            return reminder?.let { reminderDTO ->
                Result.Success(reminderDTO) // returns Result.Success if the reminder is found.
            } ?: Result.Error("Reminder not found") //returns a Result.Error with an error message if the reminder is not found.
        }
    }

    // To delete all reminders
    // Clean DB
    override suspend fun deleteAllReminders() {
        reminders.clear()
    }
}