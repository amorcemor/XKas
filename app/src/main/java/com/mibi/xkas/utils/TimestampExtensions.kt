package com.mibi.xkas.util

import com.google.firebase.Timestamp
import java.util.Date

/** Convert Firebase Timestamp? ke java.util.Date? dengan null-safe. */
fun Timestamp?.toDateOrNull(): Date? = this?.toDate()

/** Kalau butuh kebalikan: dari Date ke Timestamp (misal untuk push ke Firestore). */
fun Date.toTimestamp(): Timestamp = Timestamp(this)
