package com.osatks.mizan.platform

import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** ينفّذ على الخيط الرئيسي (ردود Foundation تأتي على خيوط أخرى) */
fun runOnMain(block: () -> Unit) = dispatch_async(dispatch_get_main_queue()) { block() }
