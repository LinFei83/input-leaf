package com.inputleaf.uhid;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class EventProtocolTest {
    @Test public void innerEventTypesExposeConstructorArguments() {
        EventProtocol.KeyEvent key = new EventProtocol.KeyEvent(0x61, EventProtocol.ACTION_DOWN, (byte) 2);
        assertThat(key.keysym).isEqualTo(0x61);
        assertThat(key.action).isEqualTo(EventProtocol.ACTION_DOWN);
        assertThat(key.modifiers).isEqualTo(2);

        EventProtocol.MouseMove move = new EventProtocol.MouseMove(3, -4);
        assertThat(move.dx).isEqualTo(3);
        assertThat(move.dy).isEqualTo(-4);

        EventProtocol.MouseButton button = new EventProtocol.MouseButton((byte) 1, EventProtocol.ACTION_UP);
        assertThat(button.button).isEqualTo(1);
        assertThat(button.action).isEqualTo(EventProtocol.ACTION_UP);

        EventProtocol.MouseWheel wheel = new EventProtocol.MouseWheel((short) 5, (short) 6);
        assertThat(wheel.deltaX).isEqualTo(5);
        assertThat(wheel.deltaY).isEqualTo(6);
    }
}
