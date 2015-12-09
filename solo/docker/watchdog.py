# -*- coding: utf-8 -*-
# Copyright (c) 2012 Spotify AB

import socket
import asyncore
import time

PORT=33333
TIMEOUT=60

print "Listening for connection on port {}...".format(PORT)

connected=False
closed=False

class Server(asyncore.dispatcher):
    def __init__(self, host, port):
        asyncore.dispatcher.__init__(self)
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.set_reuse_addr()
        self.bind(('', port))
        self.listen(1)

    def handle_accept(self):
        # when we get a client connection start a dispatcher for that
        # client
        socket, address = self.accept()
        print 'Connection by', address
        # Don't accept more incoming connections
        self.close()
        global connected
        connected=True
        Handler(socket)

class Handler(asyncore.dispatcher_with_send):
    def handle_close(self):
        print "Connection closed"
        global closed
        closed=True
        self.close()

    def handle_read(self):
        self.recv(1)
        self.close()

s = Server('', PORT)

t0 = time.time()

while True:
    asyncore.loop(timeout=1, count=1)
    if closed:
        print "Exiting -- connection closed"
        break
    if connected == False and time.time()-t0 >= TIMEOUT:
        print "Exiting -- no connection established within timeout"
        break
