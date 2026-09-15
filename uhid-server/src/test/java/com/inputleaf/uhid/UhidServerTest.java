package com.inputleaf.uhid;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class UhidServerTest {
    @Test public void acceptsOnlyTheAppAndValidNamedProcesses() {
        assertThat(UhidServer.isAllowedProcessName("com.inputleaf.android")).isTrue();
        assertThat(UhidServer.isAllowedProcessName("com.inputleaf.android:uhid")).isTrue();
        assertThat(UhidServer.isAllowedProcessName("com.inputleaf.android:worker_2.remote")).isTrue();

        assertThat(UhidServer.isAllowedProcessName("")).isFalse();
        assertThat(UhidServer.isAllowedProcessName("com.inputleaf.android:")).isFalse();
        assertThat(UhidServer.isAllowedProcessName("com.inputleaf.android:worker process")).isFalse();
        assertThat(UhidServer.isAllowedProcessName("com.inputleaf.android:worker:extra")).isFalse();
        assertThat(UhidServer.isAllowedProcessName("com.inputleaf.android.evil")).isFalse();
        assertThat(UhidServer.isAllowedProcessName("evil.com.inputleaf.android")).isFalse();
    }

    @Test public void readsOnlyTheFirstProcCmdlineArgument() {
        byte[] cmdline = "com.inputleaf.android:worker\u0000--argument\u0000"
            .getBytes(StandardCharsets.UTF_8);

        assertThat(UhidServer.firstArgument(cmdline)).isEqualTo("com.inputleaf.android:worker");
        assertThat(UhidServer.firstArgument("without-separator".getBytes(StandardCharsets.UTF_8)))
            .isEqualTo("without-separator");
        assertThat(UhidServer.firstArgument(new byte[0])).isEmpty();
    }

    @Test public void treatsClientEofAsACleanSessionEnd() throws Exception {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(new byte[0]));

        UhidServer.runSession(input, new UhidEventDispatcher(new RecordingSink()));

        assertThat(input.available()).isEqualTo(0);
    }

    @Test public void dispatchesEventsInOrderAndStopsAtShutdown() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeByte(EventProtocol.TYPE_KEY_EVENT);
        output.writeInt('A');
        output.writeByte(EventProtocol.ACTION_DOWN);
        output.writeByte(2);
        output.writeByte(EventProtocol.TYPE_MOUSE_MOVE);
        output.writeInt(5);
        output.writeInt(-3);
        output.writeByte(EventProtocol.TYPE_SHUTDOWN);
        output.writeByte(42);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        RecordingSink sink = new RecordingSink();

        UhidServer.runSession(input, new UhidEventDispatcher(sink));

        assertThat(sink.events).containsExactly("keyDown:4:2", "move:5:-3").inOrder();
        assertThat(input.readUnsignedByte()).isEqualTo(42);
    }

    @Test public void preservesMalformedEventErrors() throws Exception {
        byte[] truncatedWheel = {
            EventProtocol.TYPE_MOUSE_WHEEL, 0, 1, 0
        };
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(truncatedWheel));

        IOException failure = assertThrows(IOException.class,
            () -> UhidServer.runSession(input, new UhidEventDispatcher(new RecordingSink())));

        assertThat(failure).hasMessageThat().isEqualTo("Truncated UHID mouse-wheel event");
    }

    @Test public void closesBothDevicesWhenNeitherCloseFails() throws Exception {
        TrackingOutputStream keyboardOutput = new TrackingOutputStream("keyboard", false);
        TrackingOutputStream mouseOutput = new TrackingOutputStream("mouse", false);
        UhidServer server = serverWith(keyboardOutput, mouseOutput);

        server.close();

        assertThat(keyboardOutput.closed).isTrue();
        assertThat(mouseOutput.closed).isTrue();
    }

    @Test public void attemptsToCloseMouseWhenKeyboardCloseFails() {
        TrackingOutputStream keyboardOutput = new TrackingOutputStream("keyboard", true);
        TrackingOutputStream mouseOutput = new TrackingOutputStream("mouse", false);
        UhidServer server = serverWith(keyboardOutput, mouseOutput);

        IOException failure = assertThrows(IOException.class, server::close);

        assertThat(keyboardOutput.closed).isTrue();
        assertThat(mouseOutput.closed).isTrue();
        assertThat(failure).hasMessageThat().isEqualTo("keyboard close failed");
        assertThat(failure.getSuppressed()).isEmpty();
    }

    @Test public void reportsMouseCloseFailureAfterClosingKeyboard() {
        TrackingOutputStream keyboardOutput = new TrackingOutputStream("keyboard", false);
        TrackingOutputStream mouseOutput = new TrackingOutputStream("mouse", true);
        UhidServer server = serverWith(keyboardOutput, mouseOutput);

        IOException failure = assertThrows(IOException.class, server::close);

        assertThat(keyboardOutput.closed).isTrue();
        assertThat(mouseOutput.closed).isTrue();
        assertThat(failure).hasMessageThat().isEqualTo("mouse close failed");
    }

    @Test public void preservesBothDeviceCloseFailures() {
        TrackingOutputStream keyboardOutput = new TrackingOutputStream("keyboard", true);
        TrackingOutputStream mouseOutput = new TrackingOutputStream("mouse", true);
        UhidServer server = serverWith(keyboardOutput, mouseOutput);

        IOException failure = assertThrows(IOException.class, server::close);

        assertThat(keyboardOutput.closed).isTrue();
        assertThat(mouseOutput.closed).isTrue();
        assertThat(failure).hasMessageThat().isEqualTo("keyboard close failed");
        assertThat(failure.getSuppressed()).asList().hasSize(1);
        assertThat(failure.getSuppressed()[0]).hasMessageThat().isEqualTo("mouse close failed");
    }

    @Test public void constructsFromASuccessfulDeviceFactoryAndDispatchesThroughIt() throws Exception {
        TrackingOutputStream keyboardOutput = new TrackingOutputStream("keyboard", false);
        TrackingOutputStream mouseOutput = new TrackingOutputStream("mouse", false);
        UhidServer.DeviceFactory factory = new UhidServer.DeviceFactory() {
            @Override public KeyboardDevice createKeyboard() {
                return new KeyboardDevice(keyboardOutput);
            }

            @Override public MouseDevice createMouse() {
                return new MouseDevice(mouseOutput);
            }
        };

        UhidServer server = new UhidServer(factory);
        java.lang.reflect.Field dispatcherField = UhidServer.class.getDeclaredField("dispatcher");
        dispatcherField.setAccessible(true);
        UhidEventDispatcher dispatcher = (UhidEventDispatcher) dispatcherField.get(server);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeByte(EventProtocol.TYPE_KEY_EVENT);
        output.writeInt('A');
        output.writeByte(EventProtocol.ACTION_DOWN);
        output.writeByte(0);
        output.writeByte(EventProtocol.TYPE_KEY_EVENT);
        output.writeInt('A');
        output.writeByte(EventProtocol.ACTION_UP);
        output.writeByte(0);
        output.writeByte(EventProtocol.TYPE_MOUSE_MOVE);
        output.writeInt(1);
        output.writeInt(2);
        output.writeByte(EventProtocol.TYPE_MOUSE_BTN);
        output.writeByte(1);
        output.writeByte(EventProtocol.ACTION_DOWN);
        output.writeByte(EventProtocol.TYPE_MOUSE_BTN);
        output.writeByte(1);
        output.writeByte(EventProtocol.ACTION_UP);
        output.writeByte(EventProtocol.TYPE_MOUSE_WHEEL);
        output.writeShort(3);
        output.writeShort(4);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        while (input.available() > 0) {
            byte type = input.readByte();
            dispatcher.dispatch(type, input);
        }

        server.close();
        assertThat(keyboardOutput.closed).isTrue();
        assertThat(mouseOutput.closed).isTrue();
    }

    @Test public void defaultFactoryFailsWithoutUhidOrClosesCleanly() {
        try {
            UhidServer server = new UhidServer();
            try {
                server.close();
            } catch (IOException ignored) {
            }
        } catch (IOException | RuntimeException | UnsatisfiedLinkError | NoClassDefFoundError ignored) {
        }
        try {
            new KeyboardDevice().close();
        } catch (IOException | RuntimeException ignored) {
        }
        try {
            new MouseDevice().close();
        } catch (IOException | RuntimeException ignored) {
        }
    }

    @Test public void closesKeyboardWhenMouseCreationFails() {
        TrackingOutputStream keyboardOutput = new TrackingOutputStream("keyboard", true);
        IOException creationFailure = new IOException("mouse creation failed");
        UhidServer.DeviceFactory factory = new UhidServer.DeviceFactory() {
            @Override public KeyboardDevice createKeyboard() {
                return new KeyboardDevice(keyboardOutput);
            }

            @Override public MouseDevice createMouse() throws IOException {
                throw creationFailure;
            }
        };

        IOException failure = assertThrows(IOException.class, () -> new UhidServer(factory));

        assertThat(failure).isSameInstanceAs(creationFailure);
        assertThat(keyboardOutput.closed).isTrue();
        assertThat(failure.getSuppressed()).asList().hasSize(1);
        assertThat(failure.getSuppressed()[0]).hasMessageThat().isEqualTo("keyboard close failed");
    }

    @Test public void serveWritesReadyRunsTheSessionAndClosesBothEnds() throws Exception {
        ByteArrayOutputStream clientOut = new ByteArrayOutputStream();
        ByteArrayOutputStream sessionBytes = new ByteArrayOutputStream();
        new DataOutputStream(sessionBytes).writeByte(EventProtocol.TYPE_SHUTDOWN);
        FakeSession client = new FakeSession(
            new ByteArrayInputStream(sessionBytes.toByteArray()),
            clientOut
        );
        FakeAcceptor acceptor = new FakeAcceptor(client);
        UhidServer server = serverWith(
            new TrackingOutputStream("keyboard", false),
            new TrackingOutputStream("mouse", false)
        );

        server.serve(acceptor);

        assertThat(client.verified).isTrue();
        assertThat(client.closed).isTrue();
        assertThat(acceptor.closed).isTrue();
        assertThat(clientOut.toByteArray()).isEqualTo(UhidServer.READY_MESSAGE);
    }

    @Test public void serveClosesTheAcceptorWhenAcceptFails() {
        IOException acceptFailure = new IOException("accept failed");
        FakeAcceptor acceptor = new FakeAcceptor(acceptFailure);
        UhidServer server = serverWith(
            new TrackingOutputStream("keyboard", false),
            new TrackingOutputStream("mouse", false)
        );

        IOException failure = assertThrows(IOException.class, () -> server.serve(acceptor));

        assertThat(failure).isSameInstanceAs(acceptFailure);
        assertThat(acceptor.closed).isTrue();
    }

    @Test public void serveSuppressesClientCloseErrorsAfterASessionFailure() {
        IOException sessionFailure = new IOException("session failed");
        FakeSession client = new FakeSession(sessionFailure, true);
        FakeAcceptor acceptor = new FakeAcceptor(client);
        UhidServer server = serverWith(
            new TrackingOutputStream("keyboard", false),
            new TrackingOutputStream("mouse", false)
        );

        IOException failure = assertThrows(IOException.class, () -> server.serve(acceptor));

        assertThat(failure).isSameInstanceAs(sessionFailure);
        assertThat(failure.getSuppressed()).asList().hasSize(1);
        assertThat(failure.getSuppressed()[0]).hasMessageThat().isEqualTo("client close failed");
        assertThat(client.closed).isTrue();
        assertThat(acceptor.closed).isTrue();
    }

    @Test public void closeRethrowsWhenTheResourceFailsWithoutAPriorError() {
        TrackingCloseable resource = new TrackingCloseable(true);

        IOException failure = assertThrows(IOException.class, () -> UhidServer.close(resource, null));

        assertThat(failure).hasMessageThat().isEqualTo("close failed");
        assertThat(resource.closed).isTrue();
    }

    @Test public void verifyPeerFromProcRejectsThisJvm() {
        int pid = (int) ProcessHandle.current().pid();
        SecurityException failure = assertThrows(
            SecurityException.class,
            () -> UhidServer.verifyPeerFromProc(pid)
        );
        assertThat(failure).hasMessageThat().contains("Rejected connection from unknown process");
    }

    @Test public void cannotVerifyPeerUsesUnknownWhenPidIsMissing() {
        IOException cause = new IOException("no credentials");
        SecurityException failure = UhidServer.cannotVerifyPeer(-1, cause);
        assertThat(failure).hasMessageThat().isEqualTo("Cannot verify peer PID unknown");
        assertThat(failure).hasCauseThat().isSameInstanceAs(cause);

        SecurityException numbered = UhidServer.cannotVerifyPeer(42, cause);
        assertThat(numbered).hasMessageThat().isEqualTo("Cannot verify peer PID 42");
    }

    @Test public void runFailsWithoutAndroidLocalSockets() throws Exception {
        UhidServer server = serverWith(
            new TrackingOutputStream("keyboard", false),
            new TrackingOutputStream("mouse", false)
        );

        assertThrows(Throwable.class, server::run);
        server.close();
    }

    @Test public void rejectsANullDeviceFactory() {
        assertThrows(NullPointerException.class, () -> new UhidServer((UhidServer.DeviceFactory) null));
    }

    private static final class FakeAcceptor implements UhidServer.ClientAcceptor {
        private final FakeSession client;
        private final IOException acceptFailure;
        boolean closed;

        FakeAcceptor(FakeSession client) {
            this.client = client;
            this.acceptFailure = null;
        }

        FakeAcceptor(IOException acceptFailure) {
            this.client = null;
            this.acceptFailure = acceptFailure;
        }

        @Override public UhidServer.ClientSession accept() throws IOException {
            if (acceptFailure != null) throw acceptFailure;
            return client;
        }

        @Override public void close() {
            closed = true;
        }
    }

    private static final class FakeSession implements UhidServer.ClientSession {
        private final ByteArrayInputStream input;
        private final ByteArrayOutputStream output;
        private final IOException verifyFailure;
        private final boolean failOnClose;
        boolean verified;
        boolean closed;

        FakeSession(ByteArrayInputStream input, ByteArrayOutputStream output) {
            this.input = input;
            this.output = output;
            this.verifyFailure = null;
            this.failOnClose = false;
        }

        FakeSession(IOException verifyFailure, boolean failOnClose) {
            this.input = new ByteArrayInputStream(new byte[0]);
            this.output = new ByteArrayOutputStream();
            this.verifyFailure = verifyFailure;
            this.failOnClose = failOnClose;
        }

        @Override public void verifyPeer() throws IOException {
            if (verifyFailure != null) throw verifyFailure;
            verified = true;
        }

        @Override public java.io.OutputStream getOutputStream() {
            return output;
        }

        @Override public java.io.InputStream getInputStream() {
            return input;
        }

        @Override public void close() throws IOException {
            closed = true;
            if (failOnClose) throw new IOException("client close failed");
        }
    }

    private static final class TrackingCloseable implements java.io.Closeable {
        private final boolean failOnClose;
        boolean closed;

        TrackingCloseable(boolean failOnClose) {
            this.failOnClose = failOnClose;
        }

        @Override public void close() throws IOException {
            closed = true;
            if (failOnClose) throw new IOException("close failed");
        }
    }

    private UhidServer serverWith(OutputStream keyboardOutput, OutputStream mouseOutput) {
        return new UhidServer(new KeyboardDevice(keyboardOutput), new MouseDevice(mouseOutput));
    }

    private static class RecordingSink implements UhidEventDispatcher.EventSink {
        final List<String> events = new ArrayList<>();

        @Override public void keyDown(int hidUsage, byte modifiers) {
            events.add("keyDown:" + hidUsage + ":" + modifiers);
        }
        @Override public void keyUp(int hidUsage, byte modifiers) {
            events.add("keyUp:" + hidUsage + ":" + modifiers);
        }
        @Override public void mouseMove(int dx, int dy) {
            events.add("move:" + dx + ":" + dy);
        }
        @Override public void mouseButtonDown(byte button) {
            events.add("buttonDown:" + button);
        }
        @Override public void mouseButtonUp(byte button) {
            events.add("buttonUp:" + button);
        }
        @Override public void mouseWheel(short deltaX, short deltaY) {
            events.add("wheel:" + deltaX + ":" + deltaY);
        }
    }

    private static class TrackingOutputStream extends OutputStream {
        private final String device;
        private final boolean failOnClose;
        boolean closed;

        TrackingOutputStream(String device, boolean failOnClose) {
            this.device = device;
            this.failOnClose = failOnClose;
        }

        @Override public void write(int value) {}

        @Override public void close() throws IOException {
            closed = true;
            if (failOnClose) throw new IOException(device + " close failed");
        }
    }
}
