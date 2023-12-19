/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("tests")
package dev.gitlive.firebase

import android.content.Context
import com.google.firebase.FirebasePlatform

actual val context: Any get() = Context().also { FirebasePlatform.initializeFirebasePlatform(MockFirebasePlatform()) }

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreForAndroidUnitTest
