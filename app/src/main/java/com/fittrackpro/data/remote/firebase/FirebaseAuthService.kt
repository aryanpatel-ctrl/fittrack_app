package com.fittrackpro.data.remote.firebase

import android.net.Uri
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthService @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = currentUser != null

    suspend fun signIn(email: String, password: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user
    }

    suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user
    }

    suspend fun signUp(name: String, email: String, password: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.let { user ->
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            user.updateProfile(profileUpdates).await()
        }
        return result.user
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() { auth.signOut() }

    suspend fun updateProfile(name: String?, photoUrl: String?) {
        currentUser?.let { user ->
            val builder = UserProfileChangeRequest.Builder()
            name?.let { builder.setDisplayName(it) }
            photoUrl?.let { builder.setPhotoUri(Uri.parse(it)) }
            user.updateProfile(builder.build()).await()
        }
    }

    suspend fun deleteAccount() { currentUser?.delete()?.await() }

    /**
     * Changes the user's password.
     * Requires re-authentication with current password for security.
     * @throws FirebaseAuthInvalidCredentialsException if current password is wrong
     */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = currentUser ?: throw Exception("No user logged in")
        val email = user.email ?: throw Exception("User has no email")

        // Re-authenticate user with current password (required by Firebase for sensitive operations)
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()

        // Update to new password
        user.updatePassword(newPassword).await()
    }
}
