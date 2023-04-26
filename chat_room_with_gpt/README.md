## How to run the code

### 1. In order to setup the project you need to add the dependency in your 
    > File->Project Structure-> Module

### 2. The next step is to import the Database. You can find the sql file named as BSDS.ncx in the root directory of the project. Import the file in your mysql workbench or any other database management tool.

### 3. The connection name is BSDS, and it has two databases db1 and db2. You can change the connection name in the code if you want to use a different name.

### 4. By default, the database is running on port 3306. Also modify the DBCred.properties file with your SQL server username,password and connection url for db1 and db2. Currently, it's set to root and no password and url is set to 
    
    jdbc:mysql://localhost:3306/db1

### The project also allow you to interact with ChatGPT server. In order to do so add your API_KEY in the DBCRed.properties file. You can get the API_KEY from https://api.openai.com/ and add it in the file.

### 5. Once you have imported the database, you can run the code. Start by running the server first using the command mentioned below. This will start and server and will delete the previous records in both client and messages table.

    > java -jar ChatServerV.jar 2020

### 6. Once the server is running, you can start the client using the command mentioned below.

    > java -jar Login.jar 2020

### 7. A Swing window will open up, you can enter your username and room id and click on Connect. 

### 8. You can open multiple clients and connect to the same room. The users who are connected to the same room can send messages to each others.

### 9. You can also send messages to the ChatGPT server by typing @BOT YOUR_MESSAGE in the message box and hit Send. The bot will reply to your message.


### Unable to import database or run jar files

### 1. If you are unable to import the database, you can create a new connection with the name BSDS and create two databases with the name db1 and db2. In both databases run these queries to create clients and messages table. 

    
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

### 2. This should create two tables in both databases. Also update the DB_Cred.properties file with your username, password and connection url for both databases.

### 3. Click on the + sign and add the dependency, you will find two jar files one for sql connector and other for json. Make sure you have selected the checkbox and click apply to run the application.

### 4. Once the dependencies are added, you can run the server by adding port as argument. 

### 5. Once the server is running, you can run the client by adding port as argument. This will open a window and will ask user to enter username and room id.

###6. Also make sure you have modified the location of DBCred.properties file in the code. You can find the file in the root directory of the project.