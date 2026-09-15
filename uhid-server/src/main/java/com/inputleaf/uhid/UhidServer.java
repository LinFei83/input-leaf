package com.inputleaf.uhid;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class UhidServer implements Closeable {
    interface DeviceFactory {
        KeyboardDevice createKeyboard() throws IOException;
        MouseDevice createMouse() throws IOException;
    }

    interface ClientSession extends Closeable {
        void verifyPeer() throws IOException;
        OutputStream getOutputStream() throws IOException;
        InputStream getInputStream() throws IOException;
    }

    interface ClientAcceptor extends Closeable {
        ClientSession accept() throws IOException;
    }

    static final String EXPECTED_PACKAGE = "com.inputleaf.android";
    static final byte[] READY_MESSAGE = "READY\n".getBytes(StandardCharsets.US_ASCII);

    private static final DeviceFactory DEFAULT_DEVICE_FACTORY = new DeviceFactory() {
        @Override public KeyboardDevice createKeyboard() throws IOException {
            return new KeyboardDevice();
        }

        @Override public MouseDevice createMouse() throws IOException {
            return new MouseDevice();
        }
    };

    private final KeyboardDevice keyboard;
    private final MouseDevice mouse;
    private final UhidEventDispatcher dispatcher;

    public UhidServer() throws IOException {
        this(DEFAULT_DEVICE_FACTORY);
    }

    UhidServer(DeviceFactory deviceFactory) throws IOException {
        Objects.requireNonNull(deviceFactory, "deviceFactory");
        KeyboardDevice createdKeyboard = Objects.requireNonNull(
            deviceFactory.createKeyboard(), "deviceFactory keyboard"
        );
        MouseDevice createdMouse;
        try {
            createdMouse = Objects.requireNonNull(
                deviceFactory.createMouse(), "deviceFactory mouse"
            );
        } catch (IOException | RuntimeException | Error creationFailure) {
            closeAfterFailure(createdKeyboard, creationFailure);
            throw creationFailure;
        }

        keyboard = createdKeyboard;
        mouse = createdMouse;
        dispatcher = createDispatcher();
    }

    UhidServer(KeyboardDevice keyboard, MouseDevice mouse) {
        this.keyboard = Objects.requireNonNull(keyboard, "keyboard");
        this.mouse = Objects.requireNonNull(mouse, "mouse");
        dispatcher = createDispatcher();
    }

    private UhidEventDispatcher createDispatcher() {
        return new UhidEventDispatcher(new UhidEventDispatcher.EventSink() {
            @Override public void keyDown(int hidUsage, byte modifiers) throws IOException {
                keyboard.keyDown(hidUsage, modifiers);
            }

            @Override public void keyUp(int hidUsage, byte modifiers) throws IOException {
                keyboard.keyUp(hidUsage, modifiers);
            }

            @Override public void mouseMove(int dx, int dy) throws IOException {
                mouse.move(dx, dy);
            }

            @Override public void mouseButtonDown(byte button) throws IOException {
                mouse.buttonDown(button);
            }

            @Override public void mouseButtonUp(byte button) throws IOException {
                mouse.buttonUp(button);
            }

            @Override public void mouseWheel(short deltaX, short deltaY) throws IOException {
                mouse.wheel(deltaX, deltaY);
            }
        });
    }

    public void run() throws IOException {
        serve(UhidLocalSockets.bind());
    }

    void serve(ClientAcceptor server) throws IOException {
        Throwable serverFailure = null;
        try {
            System.out.println("READY");
            System.out.flush();

            ClientSession client = server.accept();
            Throwable clientFailure = null;
            try {
                client.verifyPeer();
                client.getOutputStream().write(READY_MESSAGE);
                client.getOutputStream().flush();

                runSession(new DataInputStream(client.getInputStream()), dispatcher);
            } catch (IOException | RuntimeException | Error failure) {
                clientFailure = failure;
                throw failure;
            } finally {
                close(client, clientFailure);
            }
        } catch (IOException | RuntimeException | Error failure) {
            serverFailure = failure;
            throw failure;
        } finally {
            close(server, serverFailure);
        }
    }

    static void runSession(DataInputStream input, UhidEventDispatcher dispatcher) throws IOException {
        while (true) {
            byte type;
            try {
                type = input.readByte();
            } catch (EOFException disconnected) {
                return;
            }
            if (type == EventProtocol.TYPE_SHUTDOWN) return;
            dispatcher.dispatch(type, input);
        }
    }

    static void close(Closeable socket, Throwable failure) throws IOException {
        try {
            socket.close();
        } catch (IOException closeFailure) {
            if (failure == null) throw closeFailure;
            failure.addSuppressed(closeFailure);
        }
    }

    static void closeAfterFailure(Closeable resource, Throwable failure) {
        try {
            resource.close();
        } catch (IOException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    static void verifyPeerFromProc(int pid) throws IOException {
        byte[] cmdline = Files.readAllBytes(Paths.get("/proc/" + pid + "/cmdline"));
        String processName = firstArgument(cmdline);
        if (!isAllowedProcessName(processName)) {
            throw new SecurityException("Rejected connection from unknown process: " + processName);
        }
    }

    static SecurityException cannotVerifyPeer(int pid, IOException failure) {
        String peer = pid < 0 ? "unknown" : Integer.toString(pid);
        return new SecurityException("Cannot verify peer PID " + peer, failure);
    }

    static String firstArgument(byte[] cmdline) {
        int end = 0;
        while (end < cmdline.length && cmdline[end] != 0) end++;
        return new String(cmdline, 0, end, StandardCharsets.UTF_8);
    }

    static boolean isAllowedProcessName(String processName) {
        if (processName.equals(EXPECTED_PACKAGE)) return true;
        String prefix = EXPECTED_PACKAGE + ":";
        if (!processName.startsWith(prefix)) return false;

        String suffix = processName.substring(prefix.length());
        if (suffix.isEmpty()) return false;
        for (int index = 0; index < suffix.length(); index++) {
            char character = suffix.charAt(index);
            if (!Character.isLetterOrDigit(character) && character != '_' && character != '.') return false;
        }
        return true;
    }

    @Override public void close() throws IOException {
        IOException failure = null;
        try {
            keyboard.close();
        } catch (IOException keyboardFailure) {
            failure = keyboardFailure;
        }
        try {
            mouse.close();
        } catch (IOException mouseFailure) {
            if (failure == null) {
                failure = mouseFailure;
            } else {
                failure.addSuppressed(mouseFailure);
            }
        }
        if (failure != null) throw failure;
    }
}
