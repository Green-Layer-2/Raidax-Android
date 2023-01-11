package com.cloudcoin2.wallet.Utils

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CoroutinesUtils {
    // MAIN Compute work on main thread
    // DEFAULT for any heavy computation
    // IO for network request or local data base

    fun main(work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.Main).launch {
            work()
        }

    fun io(work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.IO).launch {
            work()
        }

    fun default(work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.Default).launch {
            work()
        }

    fun io(job: CompletableJob, work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.IO + job).launch {
            work()
        }

    fun default(job: CompletableJob, work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.Default + job).launch {
            work()
        }


    //Notes
    //withContext(Dispatchers.Main) {}
    // this can be used to start a work in one thread and the pass it to different thread
    // eg you have started a work in IO thread and then You want to update ui which can be done in MAIN
    // you should use withContext(MAIN) and inside this update the UI

}