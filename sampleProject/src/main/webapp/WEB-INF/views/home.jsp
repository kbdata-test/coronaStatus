<!DOCTYPE html> 
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<html> 
	<head> 
		<meta charset="utf-8">
		<link rel="stylesheet" href="https://cdn.datatables.net/1.10.19/css/dataTables.bootstrap.min.css">
		<link rel="stylesheet" href="https://www.worldometers.info/css/bootstrap.min.css">
		<script src="https://www.worldometers.info/js/jquery.min.js"></script>
		<script src="https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js"></script>
		<script src="https://www.worldometers.info/js/bootstrap.min..js"></script>
		<title>코로나 세계 현황</title> 
	</head> 
	<body>
		${data}
	</body> 
	<script>
		$("#main_table_countries_today").attr("style", "width:100%;margin-top: 0px !important;")
		var eventTarget = $("#main_table_countries_today").children("tbody").children("tr");
		for(var i = 0; i < eventTarget.length; i++) {
			eventTarget.eq(i).children("td").eq(9).attr("style", "display:none;");
			eventTarget.eq(i).children("td").eq(12).attr("style", "display:none;");
			eventTarget.eq(i).children("td").eq(13).attr("style", "display:none;");
			eventTarget.eq(i).children("td").eq(15).attr("style", "display:none;");
			eventTarget.eq(i).children("td").eq(16).attr("style", "display:none;");
			eventTarget.eq(i).children("td").eq(17).attr("style", "display:none;");
			eventTarget.eq(i).children("td").eq(18).attr("style", "display:none;");
		}
		console.log(eventTarget.eq(0).children("td").length);
	</script>
</html>
