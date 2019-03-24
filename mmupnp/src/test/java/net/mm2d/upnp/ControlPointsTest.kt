/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import com.google.common.truth.Truth.assertThat
import net.mm2d.upnp.empty.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ControlPointsTest {
    @Test
    fun emptyControlPoint() {
        assertThat(ControlPoints.emptyControlPoint()).isInstanceOf(EmptyControlPoint::class.java)
    }

    @Test
    fun emptyDevice() {
        assertThat(ControlPoints.emptyDevice()).isInstanceOf(EmptyDevice::class.java)
    }

    @Test
    fun emptyService() {
        assertThat(ControlPoints.emptyService()).isInstanceOf(EmptyService::class.java)
    }

    @Test
    fun emptyAction() {
        assertThat(ControlPoints.emptyAction()).isInstanceOf(EmptyAction::class.java)
    }

    @Test
    fun emptySsdpMessage() {
        assertThat(ControlPoints.emptySsdpMessage()).isInstanceOf(EmptySsdpMessage::class.java)
    }
}
