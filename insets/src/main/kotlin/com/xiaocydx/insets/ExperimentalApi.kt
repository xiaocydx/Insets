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

package com.xiaocydx.insets

import kotlin.annotation.AnnotationTarget.*

/**
 * @author xcc
 * @date 2024/4/29
 */
@RequiresOptIn(
    message = "功能处于实验性阶段，将来改动的可能性很大",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, TYPEALIAS, PROPERTY)
annotation class ExperimentalApi