<#escape x as x?html>
<@page title="Analytic view">


<#-- SECTION Results -->
<@section title="Analytic view" if=results??>
<#if results.rows?size == 0>
  <p>No input trades</p>
<#else>
  <table>
   <tr><#rt>
    <th>Trade</th><#t>
<#list results.columnNames as header>
    <th>${header}</th><#t>
</#list>
   </tr><#lt>
<#list results.rows as row>
   <tr>
    <td title="${row.input.toString()}">
<#if row.input.class.simpleName = 'ManageableTrade'>
      Trade
<#else>
      ${row.input.name}
</#if>
    </td>
<#list row.items as item>
<#if item.result.isSuccess()>
    <td>
      ${item.result.value?string}
    </td>
<#else>
<#if item.result.status?string = 'NOT_APPLICABLE'>
    <td title="No applicable result for this cell">&nbsp;</td>
<#else>
    <td title="${item.result.failureMessage}">
      ${item.result.status.name()}
    </td>
</#if>
</#if>
</#list>
   </tr>
</#list>
  </table>
</#if>
<#if results.pendingMarketData>
  Pending market data
</#if>
</@section>


<#-- SECTION Links -->
<@section title="Links">
  <p>
    <a href="${uris.views()}">View search</a><br />
    <a href="${homeUris.home()}">Home</a><br />
  </p>
</@section>
<#-- END -->
</@page>
</#escape>
