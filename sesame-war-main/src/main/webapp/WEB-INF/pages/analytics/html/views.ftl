<#escape x as x?html>
<@page title="Analytic views">


<#-- SECTION View search -->
<@section title="View search">

<#-- SUBSECTION View results -->
<#if views??>
<@subsection title="Results">
  <@table items=views paging=paging empty="No views" headers=["Name","Reference","Actions"]; item>
      <td><a href="${uris.view(item)}">View ${item}</a></td>
      <td>${item.value}</td>
      <td><a href="${uris.view(item)}">View</a></td>
  </@table>
</@subsection>
</#if>
</@section>


<#-- SECTION Links -->
<@section title="Links">
  <p>
    <a href="${homeUris.home()}">Home</a><br />
  </p>
</@section>
<#-- END -->
</@page>
</#escape>
