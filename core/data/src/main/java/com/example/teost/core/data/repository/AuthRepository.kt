package com.example.teost.data.repository

import com.example.teost.data.local.PreferencesManager
import com.example.teost.data.local.dao.TestResultDao
import com.example.teost.data.local.dao.DomainDao
import com.example.teost.data.model.User
import com.example.teost.util.Resource
import com.example.teost.util.AppError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager,
    private val testResultDao: TestResultDao,
    private val domainDao: DomainDao
) {
    
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    val userPreferences = preferencesManager.userPreferences

    fun getCurrentEmail(): String? = auth.currentUser?.email
    
    fun signUp(email: String, password: String, displayName: String): Flow<Resource<User>> = flow {
        try {
            emit(Resource.Loading())
            
            // Create user with email and password
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Update display name
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    this.displayName = displayName
                }
                firebaseUser.updateProfile(profileUpdates).await()
                
                // Send email verification
                firebaseUser.sendEmailVerification().await()
                
                // Create user document in Firestore
                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    displayName = displayName,
                    emailVerified = false,
                    credits = 100 // Starting credits
                )
                
                // Non-fatal Firestore write; avoid leaving user in broken state if Firestore unavailable
                try {
                    firestore.collection("users")
                        .document(firebaseUser.uid)
                        .set(user)
                        .await()
                } catch (e: Exception) {
                    // no-op, will backfill on next login
                }
                
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error(AppError.Authentication("Failed to create user")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Authentication(mapAuthError(e, action = "sign_up"))))
        }
    }
    
    fun signIn(email: String, password: String): Flow<Resource<User>> = flow {
        try {
            emit(Resource.Loading())
            
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Get user data from Firestore (fallback to Firebase user if Firestore unavailable)
                val user = try {
                    val userDoc = firestore.collection("users")
                        .document(firebaseUser.uid)
                        .get()
                        .await()
                    userDoc.toObject(User::class.java) ?: User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        emailVerified = firebaseUser.isEmailVerified
                    )
                } catch (e: Exception) {
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        emailVerified = firebaseUser.isEmailVerified
                    )
                }
                
                // Save session
                preferencesManager.saveUserSession(
                    userId = user.uid,
                    email = user.email,
                    name = user.displayName,
                    authToken = firebaseUser.uid // Using UID as token for now
                )
                
                // Update last login time (non-fatal)
                try {
                    firestore.collection("users")
                        .document(firebaseUser.uid)
                        .update("lastLoginAt", com.google.firebase.Timestamp.now())
                        .await()
                } catch (e: Exception) {
                    // no-op
                }
                
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error(AppError.Authentication("Sign in failed")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Authentication(mapAuthError(e, action = "sign_in"))))
        }
    }
    
    fun resendVerificationEmail(): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            val user = auth.currentUser
            if (user != null) {
                user.sendEmailVerification().await()
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error(AppError.Authentication("No user logged in")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Authentication(mapAuthError(e, action = "resend_verification"))))
        }
    }
    
    fun checkEmailVerification(): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            val user = auth.currentUser
            if (user != null) {
                user.reload().await()
                emit(Resource.Success(user.isEmailVerified))
            } else {
                emit(Resource.Error(AppError.Authentication("No user logged in")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Authentication(mapAuthError(e, action = "check_verification"))))
        }
    }
    
    fun forgotPassword(email: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            auth.sendPasswordResetEmail(email).await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Authentication(mapAuthError(e, action = "reset_password"))))
        }
    }
    
    fun signOut(): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            auth.signOut()
            preferencesManager.clearSession()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Authentication(mapAuthError(e, action = "sign_out"))))
        }
    }
    
    fun deleteAccount(): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            val user = auth.currentUser
            if (user != null) {
                // Delete user data from Firestore
                firestore.collection("users")
                    .document(user.uid)
                    .delete()
                    .await()
                
                // Delete user account
                user.delete().await()
                
                // Clear local data completely
                preferencesManager.clearAll()
                
                // Also clear Room database data for this user
                testResultDao.deleteAllTestResults(user.uid)
                domainDao.deleteAllDomains(user.uid)
                
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error(AppError.Authentication("No user logged in")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Authentication(mapAuthError(e, action = "delete_account"))))
        }
    }
    
    suspend fun updateLastActiveTime() {
        preferencesManager.updateLastActiveTime()
    }
    
    suspend fun updateSplashShownTime() {
        preferencesManager.updateSplashShownTime()
    }
}

private fun mapAuthError(e: Exception, action: String): String {
    return when (e) {
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address."
            "ERROR_USER_NOT_FOUND" -> "Account does not exist. Please sign up."
            "ERROR_WRONG_PASSWORD" -> "Email or password is incorrect."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already registered. Try signing in."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email/password sign-in is disabled. Please contact support."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later."
            "ERROR_WEAK_PASSWORD" -> "Password is too weak. Use at least 6 characters."
            else -> defaultMessageFor(action)
        }
        is FirebaseNetworkException -> "Network error. Please check your connection and try again."
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Service temporarily unavailable. Please try again later."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Service unavailable. Please try again later."
            else -> defaultMessageFor(action)
        }
        is IOException -> "Network error. Please check your connection and try again."
        else -> defaultMessageFor(action)
    }
}

private fun defaultMessageFor(action: String): String {
    return when (action) {
        "sign_in" -> "Sign in failed. Please try again."
        "sign_up" -> "Sign up failed. Please try again."
        "reset_password" -> "Could not send reset email. Please try again."
        "resend_verification" -> "Could not send verification email. Please try again."
        "check_verification" -> "Could not verify email status. Please try again."
        "sign_out" -> "Sign out failed. Please try again."
        "delete_account" -> "Account deletion failed. Please try again."
        else -> "Something went wrong. Please try again."
    }
}
