<?php
$host = "localhost";
$username = "root";
$password = "Frici951007";
$db = "mysql";

$reg_password = $data["password"];
$reg_password_conf = $data["confirm-password"];
$reg_user = $data["username"];

if ($reg_password == $reg_password_conf){
        // Create connection
        $conn = new mysqli($host, $username, $password, $db);

        // Check connection
        if ($conn->connect_error) {
                die("Connection failed: " . $conn->connect_error);
        }

        // Check registration enabled users' list
        $sql_reg_enable = "SELECT * FROM reg_enable WHERE id = '$reg_user'";
        $result_reg_enable = $conn->query($sql_reg_enable);

        if($result_reg_enable->num_rows > 0){
                // Check already registered users' list
                $sql_user = "SELECT * FROM user WHERE User = '$reg_user'";
                $result_user = $conn->query($sql_user);

                if($result_user->num_rows > 0){
                        echo "Already registered user! Try logging in instead";
                }else{
                        $sql_create_user = "CREATE USER '$reg_user'@'$host' IDENTIFIED BY '$reg_password'";
                        $conn->query($sql_create_user);

                        $sql_priv = "GRANT ALL PRIVILEGES ON *.* TO '$reg_user'@'$host' WITH GRANT OPTION";
                        $conn->query($sql_priv);

                        $sql_flush = "FLUSH PRIVILEGES";
                        $conn->query($sql_flush);

                        echo "Successfully registered as '$reg_user'!";

                }
        }else{
                echo "Not authenticated user";
        }

        $conn->close();
}else{
        echo "Given passwords don't match! Please try again";
}
?>
