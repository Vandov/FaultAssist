<?php

header('Content-Type: application/json; charset=utf-8');

$host = "localhost";
$username = $data["username"];
$password = $data["password"];
$db = "faultassist";

$command = $data["command"];
$table = $data["table"];
$values = $data["values"];
$selection = $data["selection"];
$specification = $data["specification"];

function utf8ize($d) {
    if (is_array($d)) {
        foreach ($d as $k => $v) {
            $d[$k] = utf8ize($v);
        }
    } else if (is_string ($d)) {
        return utf8_encode($d);
    }
    return $d;
}

// Create connection
$conn = new mysqli($host, $username, $password, $db);

// Check connection
if ($conn->connect_error) {
         die("Connection failed: " . $conn->connect_error);
}else{
        if($command == "insert"){
                $query = "INSERT INTO $table VALUES ($values)";
        }else if($command == "select"){
                 $query = "SELECT $selection FROM $table WHERE $specification";
        }

        $result = $conn->query($query);

        if($result->num_rows > 0){
                for($set = array(); $row = $result->fetch_assoc(); $set[] = $row);
                echo json_encode(utf8ize($set));
        }else{
                echo "$query";
        }

}
?>
