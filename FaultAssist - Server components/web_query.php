<?php

$data["command"] = $_POST["command"];
$data["table"] = $_POST["table"];
$data["values"] = $_POST["values"];
$data["selection"] = $_POST["selection"];
$data["specification"] = $_POST["specification"];

include ('queryProcess.php');

?>
