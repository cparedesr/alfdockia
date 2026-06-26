<#if error??>
{
  "error": {
    "statusCode": ${error.statusCode?c},
    "code": "${error.code?js_string}",
    "message": "${error.message?js_string}"
  }
}
<#else>
{
  "data": {
    "agentId": "${data.agentId?js_string}",
    "name": "${data.name?js_string}",
    "image": "${data.image?js_string}",
    "documentType": <#if data.documentType??>"${data.documentType?js_string}"<#else>null</#if>,
    "desiredState": "${data.desiredState?js_string}",
    "currentState": "${data.currentState?js_string}",
    "health": <#if data.health??>"${data.health?js_string}"<#else>null</#if>,
    "containerId": <#if data.containerId??>"${data.containerId?js_string}"<#else>null</#if>,
    "createdAt": <#if data.createdAt??>"${data.createdAt?js_string}"<#else>null</#if>,
    "updatedAt": <#if data.updatedAt??>"${data.updatedAt?js_string}"<#else>null</#if>,
    "config": ${data.configJson}
  },
  "links": {
    "self": "${links.self?js_string}",
    "status": "${links.status?js_string}"
  }
}
</#if>
