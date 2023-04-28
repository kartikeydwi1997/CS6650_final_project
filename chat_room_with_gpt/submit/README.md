# Chat Room Application with ChatGPT

## How to run the code
1. The first step is to import the database. Find the sql file named as BSDS.ncx in the submit folder of the project.
Import the file in your MySQL workbench or any other database management tool of your choice.

3. The connection name is BSDS, and it has two databases - db1 and db2. You can change the connection name in the code
if you want to use a different name. By default, the database is running on port 3306

4. Modify the DBCred.properties file with your SQL server username, password and connection url for db1 and db2.
Currently, it's set to root and no password and url is set to:
    
    > jdbc:mysql://localhost:3306/db1 
    > jdbc:mysql://localhost:3306/db2

5. The project also allows you to interact with ChatGPT. To do so, add your API_KEY in the DBCRed.properties file.
You can get the API_KEY from https://platform.openai.com/account/api-keys.

6. Once you have imported the database, you can run the code. Start by running the server first using the command
mentioned below. This will start and server and will delete the previous records in both client and messages table.

    > java -jar Server.jar 2020

7. Once the server is running, you can start the client using the command mentioned below.

    > java -jar Client.jar 2020

8. A Login window will open up where you can enter your username and room id you want to enter. Click on Connect to 
enter the chat room. 

9. Multiple clients can be opened to interact with each other. Clients receive messages from other clients of the same
room.
    
## Using the application

1. The first window that opens when client starts is the login window. Here, you can enter your username and the room
id you want to enter. Click on Connect.
2. This will open your chat console where there is a chat message box area to show the history of messages sent by users
active in that room. The message you want to send can be entered on the right text box area.
3. There is a list of active users on the bottom left.
4. To interact with GPT, type "@BOT" before your message.
5. To exit the client, close the chat console. A new client can be started and used in a similar way.

## Unable to import database or run jar files

1. If you are unable to import the database, you can create a new connection with the name BSDS and create two
databases with the name db1 and db2. In both databases run these queries to create clients and messages table. 

    
        > CREATE TABLE clients (
        > client_id VARCHAR(50) NOT NULL PRIMARY KEY,
        > room_id VARCHAR(50) NOT NULL
        > );
    
        > CREATE TABLE messages (
        > message_id INT PRIMARY KEY,
        > client_id VARCHAR(255) NOT NULL,
        > room_id VARCHAR(255) NOT NULL,
        > message_content VARCHAR(255) NOT NULL
        > ); 

2. This should create two tables in both databases. Also update the DB_Cred.properties file with your username,
password and connection url for both databases.
   > jdbc:mysql://localhost:3306/db1
   > jdbc:mysql://localhost:3306/db2

3. Click on the + sign and add the dependency, you will find two jar files one for sql connector and other for json.
Make sure you have selected the checkbox and click apply to run the application.

4. Once the dependencies are added, you can run the server by adding port as argument. 

5. Once the server is running, you can run the client by adding the same port as argument. This will open a window and
will ask user to enter username and room id.

6. Also make sure you have modified the location of DBCred.properties file in the code. You can find the file in the
root directory of the project.