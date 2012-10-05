<?php
/*
 * Server for MyHome System
 * 
 * This system can stores photo or something at server
 * and show photos on web or apps.
 * 
 * Author: tankery.chen@gmail.com
 */

define("WEB_HOME_LOCATION", "file:///E:/Share");
define("HOME_LOCATION", "E:/Share");
define("PHOTO_LOCATION", HOME_LOCATION."/Photos");
define("SUPPORT_FILE_TYPE", "{gif,jpg,png}");

$action = isset($_REQUEST['action']) ? $_REQUEST['action'] : NULL;
$actionPara = isset($_REQUEST['act_para']) ? $_REQUEST['act_para'] : NULL;

$out = NULL;

// if operation is failed by unknown reason
define("ACT_NOT_DEFINED", "[System]: action not defined");

error_log($action."\r\n", 3, "error.log");
switch($action)
{

	case "ls":
		$dirName = generatePhotoDirName($actionPara);
		$files = getFileList($dirName);
		array_walk($files, 'replaceHomeLocation');
		$out = implode(";", $files);
	break;

	default:
		$out = ACT_NOT_DEFINED;
	break;
}

echo $out;

////////////////////////////////////////////////////////////////////////
function generatePhotoDirName($subdir) {
	if ($subdir == NULL)
		return PHOTO_LOCATION;
	return PHOTO_LOCATION.$subdir;
}

function getFileList($dirname) {
	$patten = $dirname."/*.".SUPPORT_FILE_TYPE;
	$files = glob($patten, GLOB_BRACE);
	return $files;
}

function replaceHomeLocation(&$dir) {
	$dir = substr($dir, strlen(HOME_LOCATION));
	$dir = WEB_HOME_LOCATION . $dir;
}

?>