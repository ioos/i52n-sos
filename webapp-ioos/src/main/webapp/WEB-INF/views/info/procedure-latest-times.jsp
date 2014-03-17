<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="sos" uri="http://52north.org/communities/sensorweb/sos/tags" %>
<%@ taglib prefix="ioos" uri="/WEB-INF/tld/ioos_functions.tld" %>

<style>
  table.procedure_max_time_table {
    border-collapse: collapse;  
  }

  table.procedure_max_time_table th {
    text-align: left;
  }

  
  table.procedure_max_time_table td {
    padding:0 5px 0 0;  
  }

  tr.max_time_stale {
    color: red;
  }

  tr.max_time_warning {
    color: orange;
  }

  tr.max_time_null {
    color: blue;
  }
</style>

<table class="procedure_max_time_table">
  <tr>
    <th>Procedure</th>
    <th>Latest Time</th>
    <th>Age (minutes)</th>
  </tr>
<c:set var="now" value="<%=new org.joda.time.DateTime()%>" />
<c:forEach items="${procedure_latest_times}" var="entry">
  <c:set var="age_minutes" value="${ioos:minutesBetween(entry.value, now)}" />
  <c:choose>
    <c:when test="${empty age_minutes}">
      <c:set var="procedure_class" value="max_time_null" />
    </c:when>  
    <c:when test="${age_minutes ge 1440}">
      <c:set var="procedure_class" value="max_time_stale" />
    </c:when>
    <c:when test="${age_minutes ge 180}">    
      <c:set var="procedure_class" value="max_time_warning" />
    </c:when>
    <c:otherwise>    
      <c:set var="procedure_class" value="max_time_fresh" />
    </c:otherwise>    
  </c:choose>
  <tr class="${procedure_class}">
    <td>${entry.key}</td>
    <td>${entry.value}</td>
    <td>${age_minutes}</td>
  </tr>
</c:forEach>
</table>