<?php

$data["command"] = $_POST["command"];
$data["table"] = $_POST["table"];
$data["values"] = $_POST["values"];
$data["selection"] = $_POST["selection"];
$data["specification"] = $_POST["specification"];
$data["columns"] = $_POST["columns"];

include ('queryProcess.php');

?>
