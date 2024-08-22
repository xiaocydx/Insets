/*
 * Copyright 2022 xiaocydx
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

package com.xiaocydx.insets.systembar

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [SystemBarExtensions]的单元测试
 *
 * @author xcc
 * @date 2024/8/22
 */
internal class SystemBarExtensionsTest {

    @Test
    fun all() {
        val extensions = SystemBarExtensions.all()
        assertThat(extensions).hasSize(2)
        assertThat(extensions.filterIsInstance<TestSystemBarExtensions>()).hasSize(1)
        assertThat(extensions.filterIsInstance<DefaultSystemBarExtensions>()).hasSize(1)
    }
}