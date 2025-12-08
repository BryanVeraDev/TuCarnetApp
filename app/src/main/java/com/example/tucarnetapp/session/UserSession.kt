package com.example.tucarnetapp.session

import com.example.tucarnetapp.data.remote.dto.StudentResponse

object UserSession {
    var currentUser: StudentResponse? = null

    fun setUser(student: StudentResponse) {
        currentUser = student
    }

    fun clear() {
        currentUser = null
    }
}