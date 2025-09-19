package com.zteam.zvision.data.helper

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Small helper to await Google Tasks inside coroutines.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        cont.resume(result)
    }
    addOnFailureListener { e ->
        cont.resumeWithException(e)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}
