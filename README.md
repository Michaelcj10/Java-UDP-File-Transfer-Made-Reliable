# Java-UDP-File-Transfer-Made-Reliable
Project spec was to create an app that could transfer a file from client to server using UDP. The catch was to make it reliable. This required emulating TCP with UDP by adding sequence numbers to the datagrams and simulating the 3 way handshake.

To run;

run ServerDriver class
ex java ServerDriver

Now from the terminal, navigate to where the client class is.
Client class takes some arguments.
do java Client <loss rate> <host> <port> <filename local> <name you want to call file on server>
ex: java Client 100 localhost 5555 test.txt serverText.txt

(loss rate of 100 means all data sent, 0 would be none)
