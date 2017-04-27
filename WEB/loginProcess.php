<?php

$host = "localhost";
$username = $data["username"];
$password = $data["password"];
$db = "mysql";

// Create connection
$conn = new mysqli($host, $username, $password, $db);

// Check connection
if ($conn->connect_error) {
         die("Connection failed: " . $conn->connect_error);
}else{
        echo "Successfully logged in as '$username'";
}

?>
