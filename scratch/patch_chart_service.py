path = "src/main/java/za/co/taloms/dashboard/application/service/DashboardChartService.java"
with open(path, "r") as f: c = f.read()

c = c.replace("query.setParameter(\"start\", start);", "query.setParameter(\"start\", start.atStartOfDay());")
c = c.replace("query.setParameter(\"end\", end);", "query.setParameter(\"end\", end.plusDays(1).atStartOfDay());")
c = c.replace("WHERE DATE(t.created_at) >= :start AND DATE(t.created_at) <= :end", "WHERE t.created_at >= :start AND t.created_at < :end")
c = c.replace("SELECT CAST(DATE(t.\" + dateCol + \") AS VARCHAR),", "SELECT TO_CHAR(t.\" + dateCol + \", 'YYYY-MM-DD'),")
c = c.replace("GROUP BY DATE(t.\" + dateCol + \")", "GROUP BY TO_CHAR(t.\" + dateCol + \", 'YYYY-MM-DD')")

with open(path, "w") as f: f.write(c)
