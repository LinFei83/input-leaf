package com.inputleaf.uhid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Android abstract-namespace socket used by the privileged UHID process. */
final class UhidLocalSockets {
    private static final String SOCKET_NAME = "inputleaf_uhid";

    private UhidLocalSockets() {}

    static UhidServer.ClientAcceptor bind() throws IOException {
        return new Acceptor(new android.net.LocalServerSocket(SOCKET_NAME));
    }

    private static final class Acceptor implements UhidServer.ClientAcceptor {
        private final android.net.LocalServerSocket server;

        Acceptor(android.net.LocalServerSocket server) {
            this.server = server;
        }

        @Override public UhidServer.ClientSession accept() throws IOException {
            return new Session(server.accept());
        }

        @Override public void close() throws IOException {
            server.close();
        }
    }

    private static final class Session implements UhidServer.ClientSession {
        private final android.net.LocalSocket client;

        Session(android.net.LocalSocket client) {
            this.client = client;
        }

        @Override public void verifyPeer() {
            int pid = -1;
            try {
                android.net.Credentials credentials = client.getPeerCredentials();
                pid = credentials.getPid();
                UhidServer.verifyPeerFromProc(pid);
            } catch (IOException failure) {
                throw UhidServer.cannotVerifyPeer(pid, failure);
            }
        }

        @Override public OutputStream getOutputStream() throws IOException {
            return client.getOutputStream();
        }

        @Override public InputStream getInputStream() throws IOException {
            return client.getInputStream();
        }

        @Override public void close() throws IOException {
            client.close();
        }
    }
}
