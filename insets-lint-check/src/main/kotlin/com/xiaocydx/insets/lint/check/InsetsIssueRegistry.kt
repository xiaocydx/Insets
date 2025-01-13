/*
 * Copyright 2023 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.insets.lint.check

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

@Suppress("UnstableApiUsage")
internal class InsetsIssueRegistry : IssueRegistry() {

    override val issues = listOf(FitsSystemWindowsDetector.ISSUE)

    override val api = CURRENT_API

    override val vendor = Vendor(
        vendorName = "Insets",
        feedbackUrl = "https://github.com/xiaocydx/Insets/issues",
        contact = "https://github.com/xiaocydx/Insets",
    )
}